package org.bitcoinkernel.conformance.handlers;

import org.bitcoinkernel.KernelData.ScriptPubkey;
import org.bitcoinkernel.KernelTypes;
import org.bitcoinkernel.Transactions.Transaction;
import org.bitcoinkernel.Transactions.TransactionOutput;
import org.bitcoinkernel.conformance.protocol.ErrorType;
import org.bitcoinkernel.conformance.protocol.TestRequest;
import org.bitcoinkernel.conformance.protocol.TestResponse;
import org.bitcoinkernel.conformance.utils.HexUtils;

import java.util.List;
import java.util.Map;

public class ScriptPubkeyVerifyHandler implements MethodHandler {

    @Override
    public TestResponse handle(TestRequest request) {
        try {
            String scriptPubkeyHex = request.getParamAsString("script_pubkey_hex");
            Long amount = request.getParamAsLong("amount");
            String txToHex = request.getParamAsString("tx_hex");
            List<Map<String, Object>> spentOutputs = request.getParamAsList("spent_outputs");
            Integer inputIndex = request.getParamAsInt("input_index");

            // Parse flags - can be either a string name or integer
            Integer flags;
            Object flagsObj = request.getParams().get("flags");
            if (flagsObj instanceof String) {
                flags = parseFlagsFromString((String) flagsObj);
            } else if (flagsObj instanceof Number) {
                flags = ((Number) flagsObj).intValue();
            } else {
                flags = null;
            }

            if (scriptPubkeyHex == null || amount == null || txToHex == null || inputIndex == null || flags == null) {
                return TestResponse.error(
                        request.getId(),
                        ErrorType.PROTOCOL,
                        ErrorType.INVALID_PARAMS
                );
            }

            // Validate flags - check if any invalid bits are set
            int validFlagsMask = KernelTypes.ScriptVerificationFlags.SCRIPT_VERIFY_ALL;
            if ((flags & ~validFlagsMask) != 0) {
                return TestResponse.error(
                        request.getId(),
                        ErrorType.SCRIPT_VERIFY,
                        ErrorType.INVALID_FLAGS
                );
            }

            byte[] scriptPubkeyBytes = HexUtils.decode(scriptPubkeyHex);
            byte[] txToBytes = HexUtils.decode(txToHex);

            Transaction txTo;
            try {
                txTo = new Transaction(txToBytes);
            } catch (Exception e) {
                return TestResponse.error(
                        request.getId(),
                        ErrorType.BINDING,
                        ErrorType.BINDING_ERROR
                );
            }

            if (inputIndex < 0 || inputIndex >= txTo.countInputs()) {
                txTo.close();
                return TestResponse.error(
                        request.getId(),
                        ErrorType.SCRIPT_VERIFY,
                        ErrorType.TX_INPUT_INDEX
                );
            }

            TransactionOutput[] spentOutputsArray = null;
            if (spentOutputs != null && !spentOutputs.isEmpty()) {
                if (spentOutputs.size() != txTo.countInputs()) {
                    txTo.close();
                    return TestResponse.error(
                            request.getId(),
                            ErrorType.SCRIPT_VERIFY,
                            ErrorType.SPENT_OUTPUTS_MISMATCH
                    );
                }

                spentOutputsArray = new TransactionOutput[spentOutputs.size()];
                for (int i = 0; i < spentOutputs.size(); ++i) {
                    Map<String, Object> output = spentOutputs.get(i);
                    String scriptHex = (String) output.get("script_pubkey_hex");
                    // Try both "value" and "amount" field names
                    Object valueObj = output.get("value");
                    if (valueObj == null) {
                        valueObj = output.get("amount");
                    }

                    long value;
                    if (valueObj instanceof Number) {
                        value = ((Number) valueObj).longValue();
                    } else if (valueObj instanceof String) {
                        value = Long.parseLong((String) valueObj);
                    } else {
                        txTo.close();
                        return TestResponse.error(
                                request.getId(),
                                ErrorType.PROTOCOL,
                                ErrorType.INVALID_PARAMS
                        );
                    }

                    byte[] scriptBytes = HexUtils.decode(scriptHex);
                    ScriptPubkey spk = new ScriptPubkey(scriptBytes);
                    spentOutputsArray[i] = new TransactionOutput(spk, value);
                }
            }

            ScriptPubkey scriptPubkey;
            try {
                scriptPubkey = new ScriptPubkey(scriptPubkeyBytes);
            } catch (Exception e) {
                    txTo.close();
                    if (spentOutputsArray != null) {
                        for (TransactionOutput output : spentOutputsArray) {
                            output.close();
                        }
                    }
                    return TestResponse.error(
                            request.getId(),
                            ErrorType.BINDING,
                            ErrorType.BINDING_ERROR
                    );
            }

            // Verification
            try {
                scriptPubkey.verify(amount, txTo, spentOutputsArray, inputIndex, flags);

                // Success - clean up and return
                scriptPubkey.close();
                txTo.close();
                if (spentOutputsArray != null) {
                    for (TransactionOutput output : spentOutputsArray) {
                        output.close();
                    }
                }

                return TestResponse.success(request.getId());
            } catch (KernelTypes.KernelException e) {
                String variant = mapScriptVerifyError(e);

                scriptPubkey.close();
                if (spentOutputsArray != null) {
                    for (TransactionOutput output : spentOutputsArray) {
                        output.close();
                    }
                }

                return TestResponse.error(
                        request.getId(),
                        ErrorType.SCRIPT_VERIFY,
                        variant
                );
            } catch (Exception e) {
                // Unexpected error
                scriptPubkey.close();
                txTo.close();
                if (spentOutputsArray != null) {
                    for (TransactionOutput output : spentOutputsArray) {
                        output.close();
                    }
                }

                return TestResponse.error(
                        request.getId(),
                        ErrorType.BINDING,
                        ErrorType.BINDING_ERROR
                );
            }
        } catch (Exception e) {
            // Protocol level error
            return TestResponse.error(
                    request.getId(),
                    ErrorType.PROTOCOL,
                    ErrorType.INVALID_PARAMS
            );
        }
    }

    private String mapScriptVerifyError(KernelTypes.KernelException e) {
        KernelTypes.KernelException.ScriptVerifyError error = e.getScriptVerifyError();

        if (error == null) {
            return ErrorType.INVALID;
        }

        return switch (error) {
            case INVALID_FLAGS_COMBINATION -> ErrorType.INVALID_FLAGS_COMBINATION;
            case SPENT_OUTPUTS_REQUIRED -> ErrorType.SPENT_OUTPUTS_REQUIRED;
            case OK -> ErrorType.INVALID; // Should not happen, but handle it
            default -> ErrorType.INVALID;
        };
    }

    /**
     * Parse verification flags from string name.
     */
    private Integer parseFlagsFromString(String flagsStr) {
        if (flagsStr == null || flagsStr.isEmpty()) {
            return KernelTypes.ScriptVerificationFlags.SCRIPT_VERIFY_NONE;
        }

        return switch (flagsStr) {
            case "VERIFY_NONE" -> KernelTypes.ScriptVerificationFlags.SCRIPT_VERIFY_NONE;
            case "VERIFY_P2SH" -> KernelTypes.ScriptVerificationFlags.SCRIPT_VERIFY_P2SH;
            case "VERIFY_DERSIG" -> KernelTypes.ScriptVerificationFlags.SCRIPT_VERIFY_DERSIG;
            case "VERIFY_NULLDUMMY" -> KernelTypes.ScriptVerificationFlags.SCRIPT_VERIFY_NULLDUMMY;
            case "VERIFY_CHECKLOCKTIMEVERIFY" -> KernelTypes.ScriptVerificationFlags.SCRIPT_VERIFY_CHECKLOCKTIMEVERIFY;
            case "VERIFY_CHECKSEQUENCEVERIFY" -> KernelTypes.ScriptVerificationFlags.SCRIPT_VERIFY_CHECKSEQUENCEVERIFY;
            case "VERIFY_WITNESS" -> KernelTypes.ScriptVerificationFlags.SCRIPT_VERIFY_WITNESS;
            case "VERIFY_TAPROOT" -> KernelTypes.ScriptVerificationFlags.SCRIPT_VERIFY_TAPROOT;
            case "VERIFY_ALL" -> KernelTypes.ScriptVerificationFlags.SCRIPT_VERIFY_ALL;
            case "VERIFY_ALL_PRE_TAPROOT" ->
                KernelTypes.ScriptVerificationFlags.SCRIPT_VERIFY_P2SH |
                KernelTypes.ScriptVerificationFlags.SCRIPT_VERIFY_DERSIG |
                KernelTypes.ScriptVerificationFlags.SCRIPT_VERIFY_NULLDUMMY |
                KernelTypes.ScriptVerificationFlags.SCRIPT_VERIFY_CHECKLOCKTIMEVERIFY |
                KernelTypes.ScriptVerificationFlags.SCRIPT_VERIFY_CHECKSEQUENCEVERIFY |
                KernelTypes.ScriptVerificationFlags.SCRIPT_VERIFY_WITNESS;
            default -> {
                // Try parsing as integer
                try {
                    yield Integer.parseInt(flagsStr);
                } catch (NumberFormatException e) {
                    yield KernelTypes.ScriptVerificationFlags.SCRIPT_VERIFY_NONE;
                }
            }
        };
    }
}

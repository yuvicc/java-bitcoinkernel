package org.bitcoinkernel;

import java.lang.foreign.MemorySegment;

import static org.bitcoinkernel.BitcoinKernelBindings.*;

// Enum definitions and conversions for Bitcoin Kernel
public class KernelTypes {

    // Synchronization state enum - similar to rust-bitcoinkernel
    public enum SynchronizationState {
        INIT_REINDEX(kernel_INIT_REINDEX()),
        INIT_DOWNLOAD(kernel_INIT_DOWNLOAD()),
        POST_INIT(kernel_POST_INIT());

        private final int nativeValue;

        SynchronizationState(int nativeValue){
            this.nativeValue = nativeValue;
        }

        public int getNativeValue(){
            return nativeValue;
        }

        public static SynchronizationState fromNative(int state) {
            for (SynchronizationState value: values()) {
                if (value.nativeValue == state) {
                    return value;
                }
            }
            throw new IllegalArgumentException("Unknown synchronization state: " + state);
        }
    }

    // Warning State enum
    public enum KernelWarning {
        UNKNOWN_NEW_RULES_ACTIVATED(kernel_UNKNOWN_NEW_RULES_ACTIVATED()),
        LARGE_WORK_INVALID_CHAIN(kernel_LARGE_WORK_INVALID_CHAIN());

        private final int nativeValue;

        KernelWarning(int nativeValue) {
            this.nativeValue = nativeValue;
        }

        public int getNativeValue() {
            return nativeValue;
        }

        public static KernelWarning fromNative(int warning) {
            for (KernelWarning value: values()) {
                if (value.nativeValue == warning){
                    return value;
                }
            }
            throw new IllegalArgumentException("Unknown Warning Value: " + warning);
        }
    }

    public enum ChainType {
        MAINNET(kernel_CHAIN_TYPE_MAINNET()),
        SIGNET(kernel_CHAIN_TYPE_SIGNET()),
        REGTEST(kernel_CHAIN_TYPE_REGTEST()),
        TESTNET(kernel_CHAIN_TYPE_TESTNET()),
        TESTNET_4(kernel_CHAIN_TYPE_TESTNET_4());

        private final int nativeValue;

        ChainType(int nativeValue) {
            this.nativeValue = nativeValue;
        }

        public int toNative() {
            return nativeValue;
        }

        public static ChainType fromNative(int chainType) {
            for (ChainType value: values()) {
                if (value.nativeValue == chainType) {
                    return value;
                }
            }
            throw new IllegalArgumentException("Unknown Chain Type: " + chainType);
        }
    }

    // Validation mode enum
    public enum ValidationMode {
        VALID(kernel_VALIDATION_STATE_VALID()),
        INVALID(kernel_VALIDATION_STATE_INVALID()),
        ERROR(kernel_VALIDATION_STATE_ERROR());

        private final int nativeValue;

        ValidationMode(int nativeValue) {
            this.nativeValue = nativeValue;
        }

        public int getNativeValue() {
            return nativeValue;
        }

        public static ValidationMode fromNative(int mode) {
            for (ValidationMode value: values()) {
                if (value.nativeValue == mode) {
                    return value;
                }
            }
            return ERROR;
        }
    }

    // Block Validation result enums
    public enum BlockValidationResult {
        RESULT_UNSET(kernel_BLOCK_RESULT_UNSET()),
        CONSENSUS(kernel_BLOCK_CONSENSUS()),
        CACHED_INVALID(kernel_BLOCK_CACHED_INVALID()),
        INVALID_HEADER(kernel_BLOCK_INVALID_HEADER()),
        MUTATED(kernel_BLOCK_MUTATED()),
        MISSING_PREV(kernel_BLOCK_MISSING_PREV()),
        INVALID_PREV(kernel_BLOCK_INVALID_PREV()),
        TIME_FUTURE(kernel_BLOCK_TIME_FUTURE()),
        HEADER_LOW_WORK(kernel_BLOCK_HEADER_LOW_WORK());

        private final int nativeValue;

        BlockValidationResult(int nativeValue) {
            this.nativeValue = nativeValue;
        }

        public int getNativeValue() {
            return nativeValue;
        }

        public static BlockValidationResult fromNative(int result) {
            for (BlockValidationResult value: values()) {
                if (value.nativeValue == result) {
                    return value;
                }
            }
            return CONSENSUS;
        }
    }

    // Block Validation state
    public static class BlockValidationState {
        private final MemorySegment inner;

        public BlockValidationState(MemorySegment inner) {
            this.inner = inner;
        }

        public MemorySegment getInner() {
            return inner;
        }

        public ValidationMode getMode() {
            return ValidationMode.fromNative(kernel_get_validation_mode_from_block_validation_state(inner));
        }

        public BlockValidationResult getResult() {
            return BlockValidationResult.fromNative(kernel_get_block_validation_result_from_block_validation_state(inner));
        }
    }

    // Kernel Exception
    public static class KernelException extends Exception {
        public enum ScriptVerifyError {
            TX_INPUT_INDEX(kernel_SCRIPT_VERIFY_ERROR_TX_INPUT_INDEX()),
            INVALID_FLAGS(kernel_SCRIPT_VERIFY_ERROR_INVALID_FLAGS()),
            INVALD_FLAGS_COMBINATION(kernel_SCRIPT_VERIFY_ERROR_INVALID_FLAGS_COMBINATION()),
            SPENT_OUTPUTS_REQUIRED(kernel_SCRIPT_VERIFY_ERROR_SPENT_OUTPUTS_REQUIRED()),
            SPENT_OUTPUTS_MISMATCH(kernel_SCRIPT_VERIFY_ERROR_SPENT_OUTPUTS_MISMATCH()),
            INVALID(0);

            private final int nativeValue;

            ScriptVerifyError(int nativeValue) {
                this.nativeValue = nativeValue;
            }

            public int getNativeValue() {
                return nativeValue;
            }

            public static ScriptVerifyError fromNative(int status) {
                for (ScriptVerifyError value: values()) {
                    if (value.nativeValue == status) {
                        return value;
                    }
                }
                return INVALID;
            }
        }

        private final ScriptVerifyError scriptVerifyError;

        public KernelException(String message) {
            super(message);
            this.scriptVerifyError = null;
        }

        public KernelException(ScriptVerifyError error) {
            super(error.toString());
            this.scriptVerifyError = error;
        }

        public ScriptVerifyError getScriptVerifyError() {
            return scriptVerifyError;
        }
    }
}

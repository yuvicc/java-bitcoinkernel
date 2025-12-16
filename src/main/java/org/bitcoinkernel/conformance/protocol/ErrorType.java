package org.bitcoinkernel.conformance.protocol;

public class ErrorType {

    public static final String SCRIPT_VERIFY = "ScriptVerify";
    public static final String PROTOCOL = "Protocol";
    public static final String BINDING = "Binding";
    public static final String KERNEL = "Kernel";
    public static final String TX_INPUT_INDEX = "TxInputIndex";
    public static final String INVALID_FLAGS = "InvalidFlags";
    public static final String INVALID_FLAGS_COMBINATION = "InvalidFlagsCombination";
    public static final String SPENT_OUTPUTS_MISMATCH = "SpentOutputsMismatch";
    public static final String SPENT_OUTPUTS_REQUIRED = "SpentOutputsRequired";
    public static final String INVALID = "Invalid";
    public static final String UNKNOWN_METHOD = "UnknownMethod";
    public static final String INVALID_PARAMS = "InvalidParams";
    public static final String MALFORMED_REQUEST = "MalformedRequest";
    public static final String BINDING_ERROR = "BindingError";
    public static final String RESOURCE_CLOSED = "ResourceClosed";
    public static final String KERNEL_ERROR = "KernelError";

    private ErrorType() {

    }
}

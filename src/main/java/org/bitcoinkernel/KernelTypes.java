package org.bitcoinkernel;

import java.lang.foreign.MemorySegment;

import static org.bitcoinkernel.jextract.bitcoinkernel_h.*;

// Enum definitions and conversions for Bitcoin Kernel
public class KernelTypes {

    // ===== Log Category =====
    public enum LogCategory {
        ALL(btck_LogCategory_ALL()),
        BENCH(btck_LogCategory_BENCH()),
        BLOCKSTORAGE(btck_LogCategory_BLOCKSTORAGE()),
        COINDB(btck_LogCategory_COINDB()),
        LEVELDB(btck_LogCategory_LEVELDB()),
        MEMPOOL(btck_LogCategory_MEMPOOL()),
        PRUNE(btck_LogCategory_PRUNE()),
        RAND(btck_LogCategory_RAND()),
        REINDEX(btck_LogCategory_REINDEX()),
        VALIDATION(btck_LogCategory_VALIDATION()),
        KERNEL(btck_LogCategory_KERNEL());

        private final byte value;

        LogCategory(byte value) {
            this.value = value;
        }

        public byte getValue() {
            return value;
        }

        public static LogCategory fromByte(byte value) {
            for (LogCategory category : values()) {
                if (category.value == value) {
                    return category;
                }
            }
            throw new IllegalArgumentException("Invalid LogCategory: " + value);
        }
    }

    // ===== Log Level =====
    public enum LogLevel {
        TRACE(btck_LogLevel_TRACE()),
        DEBUG(btck_LogLevel_DEBUG()),
        INFO(btck_LogLevel_INFO());

        private final byte value;

        LogLevel(byte value) {
            this.value = value;
        }

        public byte getValue() {
            return value;
        }

        public static LogLevel fromByte(byte value) {
            for (LogLevel level : values()) {
                if (level.value == value) {
                    return level;
                }
            }
            throw new IllegalArgumentException("Invalid LogLevel: " + value);
        }
    }

    // ===== Script Verify Status =====
    public enum ScriptVerifyStatus {
        OK(btck_ScriptVerifyStatus_OK()),
        ERROR_INVALID_FLAGS_COMBINATION(btck_ScriptVerifyStatus_ERROR_INVALID_FLAGS_COMBINATION()),
        ERROR_SPENT_OUTPUTS_REQUIRED(btck_ScriptVerifyStatus_ERROR_SPENT_OUTPUTS_REQUIRED());

        private final byte value;

        ScriptVerifyStatus(byte value) {
            this.value = value;
        }

        public byte getValue() {
            return value;
        }

        public static ScriptVerifyStatus fromByte(byte value) {
            for (ScriptVerifyStatus status : values()) {
                if (status.value == value) {
                    return status;
                }
            }
            throw new IllegalArgumentException("Invalid ScriptVerifyStatus: " + value);
        }
    }

    // ===== Script Verification Flags =====
    public static class ScriptVerificationFlags {
        public static final int SCRIPT_VERIFY_NONE = btck_ScriptVerificationFlags_NONE();
        public static final int SCRIPT_VERIFY_P2SH = btck_ScriptVerificationFlags_P2SH();
        public static final int SCRIPT_VERIFY_DERSIG = btck_ScriptVerificationFlags_DERSIG();
        public static final int SCRIPT_VERIFY_NULLDUMMY = btck_ScriptVerificationFlags_NULLDUMMY();
        public static final int SCRIPT_VERIFY_CHECKLOCKTIMEVERIFY = btck_ScriptVerificationFlags_CHECKLOCKTIMEVERIFY();
        public static final int SCRIPT_VERIFY_CHECKSEQUENCEVERIFY = btck_ScriptVerificationFlags_CHECKSEQUENCEVERIFY();
        public static final int SCRIPT_VERIFY_WITNESS = btck_ScriptVerificationFlags_WITNESS();
        public static final int SCRIPT_VERIFY_TAPROOT = btck_ScriptVerificationFlags_TAPROOT();
        public static final int SCRIPT_VERIFY_ALL = btck_ScriptVerificationFlags_ALL();

        private ScriptVerificationFlags() {
            // Utility class, prevent instantiation
        }
    }

    // ===== Kernel Exception =====
    public static class KernelException extends Exception {
        public enum ScriptVerifyError {
            OK(btck_ScriptVerifyStatus_OK()),
            INVALID_FLAGS_COMBINATION(btck_ScriptVerifyStatus_ERROR_INVALID_FLAGS_COMBINATION()),
            SPENT_OUTPUTS_REQUIRED(btck_ScriptVerifyStatus_ERROR_SPENT_OUTPUTS_REQUIRED()),
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

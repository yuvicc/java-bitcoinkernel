package org.bitcoinkernel;

import java.lang.foreign.*;
import java.util.List;

import static org.bitcoinkernel.BitcoinKernelBindings.*;

// This serves as the entry point for the library
public class BitcoinKernel {

    // Script Verification Flags
    public static final int VERIFY_NONE = kernel_SCRIPT_FLAGS_VERIFY_NONE();
    public static final int VERIFY_P2SH = kernel_SCRIPT_FLAGS_VERIFY_P2SH();
    public static final int VERIFY_DERSIG = kernel_SCRIPT_FLAGS_VERIFY_DERSIG();
    public static final int VERIFY_NULLDUMMY = kernel_SCRIPT_FLAGS_VERIFY_NULLDUMMY();
    public static final int VERIFY_CHECKLOCKTIMEVERIFY = kernel_SCRIPT_FLAGS_VERIFY_CHECKLOCKTIMEVERIFY();
    public static final int VERIFY_CHECKSEQUENCEVERIFY = kernel_SCRIPT_FLAGS_VERIFY_CHECKSEQUENCEVERIFY();
    public static final int VERIFY_WITNESS = kernel_SCRIPT_FLAGS_VERIFY_WITNESS();
    public static final int VERIFY_TAPROOT = kernel_SCRIPT_FLAGS_VERIFY_TAPROOT();
    public static final int VERIFY_ALL = kernel_SCRIPT_FLAGS_VERIFY_ALL();

    public static void verify() {

    }
}

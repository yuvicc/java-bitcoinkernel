package org.bitcoinkernel;

import java.lang.foreign.*;
import java.nio.file.Path;
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

    private final ContextManager.Context context;
    private final MemorySegment chainstateManager;
    private final Logger<String> logger;

    /**
     * Constructs a BitcoinKernel instance with the specified chain type and data dirs
     *
     * @param chainType The chain type (e.g., MAINNET, TESTNET, REGTEST, SIGNET, TESTNET_4)
     * @param dataDir   The path location of the chainstate data (e.g. "$HOME/.bitcoin/")
     * @param blocksDir The path location for the blocks data (e.g. path_to_datadir/blocks)
     * @param logger    The logger for kernel events
     * @throws KernelTypes.KernelException If initialization fails
     */
    public BitcoinKernel(KernelTypes.ChainType chainType, Path dataDir, Path blocksDir, Logger<String> logger) throws KernelTypes.KernelException {
        this.logger = logger;
        try (var builder = new ContextManager.ContextBuilder()) {
            this.context = builder.chainType(chainType)
                    .notificationCallbacks(new NotificationsManager.KernelNotificationInterfaceCallbacks(
                    ) {
                        @Override
                        public void blockTip(int state, KernelData.BlockIndex blockIndex, double verificationProgress) {

                        }

                        @Override
                        public void headerTip(int state, long height, long timestamp, boolean presync) {

                        }

                        @Override
                        public void progress(String title, int progressPercent, boolean resumePossible) {

                        }

                        @Override
                        public void warningSet(int warning, String message) {

                        }

                        @Override
                        public void warningUnset(int warning, String message) {

                        }

                        @Override
                        public void flushError(String message) {

                        }

                        @Override
                        public void fatalError(String message) {

                        }
                    })
                    .validationiInterface(new NotificationsManager.ValidationInterfaceCallbacks(
                            (block, state) -> logger.log("Block Checked: mode=" + state.getMode() + ", result=" + state.getResult())
                    ))
                    .build();
        }

        try (var arena = Arena.ofConfined()) {
            MemorySegment options = kernel_chainstate_manager_options_create(
                    context.getInner(),
                    arena.allocateFrom(dataDir.toString()),
                    dataDir.toString().length(),
                    arena.allocateFrom(blocksDir.toString()),
                    blocksDir.toString().length()
            );

            if (options == MemorySegment.NULL) {
                throw new KernelTypes.KernelException("Failed to create chainstate manager options");
            }

            this.chainstateManager = kernel_chainstate_manager_create(context.getInner(), options);
            if (chainstateManager == MemorySegment.NULL){
                kernel_chainstate_manager_options_destroy(options);
                throw new KernelTypes.KernelException("Failed to create chainstate manager");
            }
            kernel_chainstate_manager_options_destroy(options);
        }
    }

    /**
     * Verifies a transaction input against its corresponding output script
     *
     * @param scriptPubkey ---- todo
     */
    public static void verify(KernelData.ScriptPubkey scriptPubkey, Long amount, KernelData.Transaction txTo, int inputIndex, Integer flags, List<KernelData.TxOut> spentOutputs) throws KernelTypes.KernelException {

        try (var arena = Arena.ofConfined()) {
            int kernelFlags = flags != null ? flags : kernel_SCRIPT_FLAGS_VERIFY_ALL();
            long kernelAmount = amount != null ? amount : 0;
            MemorySegment status = arena.allocate(ValueLayout.JAVA_INT, kernel_SCRIPT_VERIFY_OK());

            MemorySegment spentOutputsPtr;
            if (spentOutputs.isEmpty()) {
                spentOutputsPtr = MemorySegment.NULL;

            } else {
                    MemorySegment ptrArray = arena.allocate(ValueLayout.ADDRESS, spentOutputs.size());
                    for (int i = 0; i < spentOutputs.size(); i++) {
                        ptrArray.setAtIndex(ValueLayout.ADDRESS, i, spentOutputs.get(i).getInner());
                    }
                    spentOutputsPtr = ptrArray;
            }

            boolean success = kernel_verify_script(
                    scriptPubkey.getInner(),
                    kernelAmount,
                    txTo.getInner(),
                    spentOutputsPtr,
                    spentOutputs.size(),
                    inputIndex,
                    kernelFlags,
                    status
            );

            if (!success) {
                int errorCode = status.get(ValueLayout.JAVA_INT, 0);
                throw new KernelTypes.KernelException(KernelTypes.KernelException.ScriptVerifyError.fromNative(errorCode));
            }
        }
    }


}









package org.bitcoinkernel;

import java.lang.foreign.*;
import java.nio.file.Path;
import java.util.function.Consumer;

import static org.bitcoinkernel.jextract.bitcoinkernel_h.*;
import static org.bitcoinkernel.Chainstate.*;
import static org.bitcoinkernel.Blocks.*;
import static org.bitcoinkernel.KernelData.*;
import static org.bitcoinkernel.Transactions.*;

/**
 * Main entry point for the Bitcoin Kernel library.
 *
 * This class provides a high-level API for interacting with Bitcoin Core's
 * consensus validation engine.
 */
public class BitcoinKernel implements AutoCloseable {

    // Script Verification Flags - delegate to KernelTypes
    public static final int VERIFY_NONE = KernelTypes.ScriptVerificationFlags.SCRIPT_VERIFY_NONE;
    public static final int VERIFY_P2SH = KernelTypes.ScriptVerificationFlags.SCRIPT_VERIFY_P2SH;
    public static final int VERIFY_DERSIG = KernelTypes.ScriptVerificationFlags.SCRIPT_VERIFY_DERSIG;
    public static final int VERIFY_NULLDUMMY = KernelTypes.ScriptVerificationFlags.SCRIPT_VERIFY_NULLDUMMY;
    public static final int VERIFY_CHECKLOCKTIMEVERIFY = KernelTypes.ScriptVerificationFlags.SCRIPT_VERIFY_CHECKLOCKTIMEVERIFY;
    public static final int VERIFY_CHECKSEQUENCEVERIFY = KernelTypes.ScriptVerificationFlags.SCRIPT_VERIFY_CHECKSEQUENCEVERIFY;
    public static final int VERIFY_WITNESS = KernelTypes.ScriptVerificationFlags.SCRIPT_VERIFY_WITNESS;
    public static final int VERIFY_TAPROOT = KernelTypes.ScriptVerificationFlags.SCRIPT_VERIFY_TAPROOT;
    public static final int VERIFY_ALL = KernelTypes.ScriptVerificationFlags.SCRIPT_VERIFY_ALL;

    private final ContextManager.Context context;
    private final ChainstateManager chainstateManager;
    private final Consumer<String> logger;

    /**
     * Constructs a BitcoinKernel instance with the specified chain type and data directories.
     *
     * @param chainType The chain type (e.g., MAINNET, TESTNET, REGTEST, SIGNET, TESTNET_4)
     * @param dataDir   The path location of the chainstate data (e.g. "$HOME/.bitcoin/")
     * @param blocksDir The path location for the blocks data (e.g. path_to_datadir/blocks)
     * @param logger    The logger for kernel events
     * @throws KernelTypes.KernelException If initialization fails
     */
    public BitcoinKernel(ChainType chainType, Path dataDir, Path blocksDir, Consumer<String> logger) throws KernelTypes.KernelException {
        this.logger = logger;

        // Create chain parameters
        ChainParameters chainParams = new ChainParameters(chainType);

        // Create context options
        ContextManager.ContextOptions contextOptions = new ContextManager.ContextOptions();
        contextOptions.setChainParams(chainParams);

        // Set up kernel notifications
        NotificationsManager.KernelNotificationManager notificationManager =
            new NotificationsManager.KernelNotificationManager(
                new NotificationsManager.KernelNotificationInterfaceCallbacks() {
                    @Override
                    public void blockTip(SynchronizationState state, BlockTreeEntry blockIndex, double verificationProgress) {
                        logger.accept("Block tip: state=" + state + ", progress=" + verificationProgress +
                                    (blockIndex != null ? ", height=" + blockIndex.getHeight() : ""));
                    }

                    @Override
                    public void headerTip(SynchronizationState state, long height, long timestamp, boolean presync) {
                        logger.accept("Header tip: height=" + height + ", presync=" + presync);
                    }

                    @Override
                    public void progress(String title, int progressPercent, boolean resumePossible) {
                        logger.accept("Progress: " + title + " " + progressPercent + "%");
                    }

                    @Override
                    public void warningSet(Warning warning, String message) {
                        logger.accept("Warning: " + message);
                    }

                    @Override
                    public void warningUnset(Warning warning) {
                        logger.accept("Warning Unset: " + warning);
                    }

                    @Override
                    public void flushError(String message) {
                        logger.accept("Flush error: " + message);
                    }

                    @Override
                    public void fatalError(String message) {
                        logger.accept("Fatal error: " + message);
                    }
                }
            );

        contextOptions.setNotifications(notificationManager);

        // Set up validation interface
        NotificationsManager.ValidationInterfaceManager validationManager =
            new NotificationsManager.ValidationInterfaceManager(
                new NotificationsManager.ValidationInterfaceCallbacks() {
                    @Override
                    public void blockChecked(Block block, BlockValidationState state) {
                        logger.accept("Block checked - Mode: " + state.getValidationMode() +
                                    ", Result: " + state.getBlockValidationResult());
                    }

                    @Override
                    public void powValidBlock(Block block, BlockTreeEntry blockIndex) {
                        logger.accept("PoW valid block at height: " + blockIndex.getHeight());
                    }

                    @Override
                    public void blockConnected(Block block, BlockTreeEntry blockIndex) {
                        logger.accept("Block connected at height: " + blockIndex.getHeight());
                    }

                    @Override
                    public void blockDisconnected(Block block, BlockTreeEntry blockIndex) {
                        logger.accept("Block disconnected at height: " + blockIndex.getHeight());
                    }
                }
            );

        contextOptions.setValidationInterface(validationManager);

        // Create context
        this.context = new ContextManager.Context(contextOptions);

        // Create chainstate manager
        ChainstateManagerOptions chainstateOptions = new ChainstateManagerOptions(
            context,
            dataDir.toString(),
            blocksDir.toString()
        );

        this.chainstateManager = new ChainstateManager(context, chainstateOptions);
    }

    /**
     * Get the chainstate manager for this kernel instance.
     *
     * @return The chainstate manager
     */
    public ChainstateManager getChainstateManager() {
        return chainstateManager;
    }

    /**
     * Get the kernel context.
     *
     * @return The context
     */
    public ContextManager.Context getContext() {
        return context;
    }

    /**
     * Verifies a transaction input against its corresponding output script.
     *
     * @param scriptPubkey  The script pubkey to verify
     * @param amount        The amount in satoshis
     * @param txTo          The transaction being verified
     * @param spentOutputs  The outputs being spent
     * @param inputIndex    The index of the input being verified
     * @param flags         Verification flags
     * @throws KernelTypes.KernelException If verification fails
     */
    public static void verify(
            ScriptPubkey scriptPubkey,
            long amount,
            Transaction txTo,
            TransactionOutput[] spentOutputs,
            int inputIndex,
            int flags) throws KernelTypes.KernelException {

        scriptPubkey.verify(amount, txTo, spentOutputs, inputIndex, flags);
    }

    @Override
    public void close() throws Exception {
        if (chainstateManager != null) {
            chainstateManager.close();
        }
        if (context != null) {
            context.close();
        }
    }
}

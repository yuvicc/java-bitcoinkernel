package org.bitcoinkernel;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;

import java.io.*;
import java.nio.file.*;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.bitcoinkernel.Chainstate.*;
import static org.bitcoinkernel.Blocks.*;
import static org.bitcoinkernel.Transactions.*;
import static org.bitcoinkernel.KernelData.*;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class BitcoinKernelTest {

    private static boolean loggingSetup = false;
    private static Logger.LogCallbackHandler logHandler;

    // Test data directory
    private static final String BLOCK_DATA_FILE = "tests/block_data.txt";

    /**
     * Setup logging for tests - runs once per test class
     */
    @BeforeAll
    public static void setupLogging() {
        if (!loggingSetup) {
            // Create log handler that prints to console
            logHandler = new Logger.LogCallbackHandler(message -> {
                // Strip trailing newlines
                String formatted = message;
                if (formatted.endsWith("\r\n")) {
                    formatted = formatted.substring(0, formatted.length() - 2);
                } else if (formatted.endsWith("\n")) {
                    formatted = formatted.substring(0, formatted.length() - 1);
                }
                System.out.println("[libbitcoinkernel] " + formatted);
            });
            loggingSetup = true;
        }
    }

    @AfterAll
    public static void cleanupLogging() {
        if (logHandler != null) {
            try {
                logHandler.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * Helper class to hold test setup data
     */
    static class TestSetup {
        final ContextManager.Context context;
        final String dataDir;

        TestSetup(ContextManager.Context context, String dataDir) {
            this.context = context;
            this.dataDir = dataDir;
        }
    }

    /**
     * Creates a context with notification handlers for testing
     */
    private static ContextManager.Context createContext() throws KernelTypes.KernelException {
        ChainParameters chainParams = new ChainParameters(ChainType.REGTEST);
        ContextManager.ContextOptions options = new ContextManager.ContextOptions();
        options.setChainParams(chainParams);

        // Set up kernel notifications
        NotificationsManager.KernelNotificationManager notificationManager =
            new NotificationsManager.KernelNotificationManager(
                new NotificationsManager.KernelNotificationInterfaceCallbacks() {
                    @Override
                    public void blockTip(SynchronizationState state, BlockTreeEntry blockIndex, double verificationProgress) {
                        System.out.println("Received block tip.");
                    }

                    @Override
                    public void headerTip(SynchronizationState state, long height, long timestamp, boolean presync) {
                        assertTrue(timestamp > 0, "Timestamp should be positive");
                        System.out.println("Received header tip at height " + height + " and time " + timestamp);
                    }

                    @Override
                    public void progress(String title, int progressPercent, boolean resumePossible) {
                        System.out.println("Made progress: " + progressPercent);
                    }

                    @Override
                    public void warningSet(Warning warning, String message) {
                        System.out.println("Received warning: " + message);
                    }

                    @Override
                    public void warningUnset(Warning warning) {
                        System.out.println("Unsetting warning.");
                    }

                    @Override
                    public void flushError(String message) {
                        System.out.println("Flush error! " + message);
                    }

                    @Override
                    public void fatalError(String message) {
                        System.out.println("Fatal error! " + message);
                    }
                }
            );

        options.setNotifications(notificationManager);

        // Set up validation interface
        NotificationsManager.ValidationInterfaceManager validationManager =
            new NotificationsManager.ValidationInterfaceManager(
                new NotificationsManager.ValidationInterfaceCallbacks() {
                    @Override
                    public void blockChecked(Block block, BlockValidationState state) {
                        System.out.println("Block checked!");
                    }

                    @Override
                    public void powValidBlock(Block block, BlockTreeEntry blockIndex) {
                        System.out.println("New PoW valid block!");
                    }

                    @Override
                    public void blockConnected(Block block, BlockTreeEntry blockIndex) {
                        System.out.println("Block connected!");
                    }

                    @Override
                    public void blockDisconnected(Block block, BlockTreeEntry blockIndex) {
                        System.out.println("Block disconnected!");
                    }
                }
            );

        options.setValidationInterface(validationManager);

        return new ContextManager.Context(options);
    }

    /**
     * Sets up test environment with context and temp directory
     */
    private static TestSetup testingSetup(@TempDir Path tempDir) throws KernelTypes.KernelException {
        setupLogging();
        ContextManager.Context context = createContext();
        return new TestSetup(context, tempDir.toString());
    }

    /**
     * Reads block data from the test file
     */
    private static List<byte[]> readBlockData() throws IOException {
        List<byte[]> blocks = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(new FileReader(BLOCK_DATA_FILE))) {
            String line;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (!line.isEmpty()) {
                    blocks.add(hexToBytes(line));
                }
            }
        }
        return blocks;
    }

    /**
     * Converts hex string to byte array
     */
    private static byte[] hexToBytes(String hex) {
        int len = hex.length();
        if (len % 2 != 0) {
            throw new IllegalArgumentException("Hex string must have even length: got " + len);
        }
        byte[] data = new byte[len / 2];
        for (int i = 0; i < len; i += 2) {
            data[i / 2] = (byte) ((Character.digit(hex.charAt(i), 16) << 4)
                                 + Character.digit(hex.charAt(i+1), 16));
        }
        return data;
    }

    /**
     * Sets up chainstate manager with test blocks
     */
    private static ChainstateManager setupChainstateManagerWithBlocks(
            ContextManager.Context context,
            String dataDir) throws Exception {

        // Create blocks directory
        Path blocksDir = Paths.get(dataDir, "blocks");
        Files.createDirectories(blocksDir);

        // Read block data
        List<byte[]> blockData = readBlockData();

        // Create chainstate manager
        ChainstateManagerOptions options = new ChainstateManagerOptions(
            context,
            dataDir,
            blocksDir.toString()
        );
        ChainstateManager chainman = new ChainstateManager(context, options);

        // Process each block
        for (byte[] rawBlock : blockData) {
            try (Block block = new Block(rawBlock)) {
                boolean[] newBlock = new boolean[1];
                boolean success = chainman.ProcessBlock(block, newBlock);

                assertTrue(success, "Block processing should succeed");
                assertTrue(newBlock[0], "Block should be marked as new");
            }
        }

        return chainman;
    }

    // ========== TESTS ==========

    @Test
    @Order(1)
    @DisplayName("Test reindex - rebuild chainstate from persisted blocks")
    public void testReindex(@TempDir Path tempDir) throws Exception {
        TestSetup setup = testingSetup(tempDir);
        ContextManager.Context context = setup.context;
        String dataDir = setup.dataDir;
        Path blocksDir = Paths.get(dataDir, "blocks");
        Files.createDirectories(blocksDir);

        // First pass: process blocks normally
        List<byte[]> blockData = readBlockData();
        ChainstateManagerOptions options1 = new ChainstateManagerOptions(
            context,
            dataDir,
            blocksDir.toString()
        );

        try (ChainstateManager chainman1 = new ChainstateManager(context, options1)) {
            for (byte[] rawBlock : blockData) {
                try (Block block = new Block(rawBlock)) {
                    boolean[] newBlock = new boolean[1];
                    boolean success = chainman1.ProcessBlock(block, newBlock);

                    assertTrue(success, "Block processing should succeed");
                    assertTrue(newBlock[0], "Block should be marked as new");
                }
            }
        }

        // Second pass: wipe chainstate and reimport from blocks
        ChainstateManagerOptions options2 = new ChainstateManagerOptions(
            context,
            dataDir,
            blocksDir.toString()
        );

        // Wipe chainstate but keep blocks
        boolean wipeSuccess = options2.setWipeDbs(false, true);
        assertTrue(wipeSuccess, "Wipe chainstate should succeed");

        try (ChainstateManager chainman2 = new ChainstateManager(context, options2)) {
            // Import blocks from disk
            boolean importSuccess = chainman2.ImportBlocks(null);
            assertTrue(importSuccess, "Import blocks should succeed");
        }

        System.out.println("Reindex test passed");
    }

    @Test
    @Order(2)
    @DisplayName("Test invalid block - verify rejection of malformed blocks")
    public void testInvalidBlock(@TempDir Path tempDir) throws Exception {
        TestSetup setup = testingSetup(tempDir);
        Files.createDirectories(Paths.get(setup.dataDir, "blocks"));

        ChainstateManagerOptions options = new ChainstateManagerOptions(
            setup.context,
            setup.dataDir,
            Paths.get(setup.dataDir, "blocks").toString()
        );

        try (ChainstateManager chainman = new ChainstateManager(setup.context, options)) {
            // Test 1: Malformed data that cannot be deserialized - should fail during Block construction
            byte[] invalidData1 = hexToBytes("012300");
            assertThrows(KernelTypes.KernelException.class, () -> {
                new Block(invalidData1);
            }, "Block creation with malformed data should throw KernelException");

            // Test 2: Empty data
            byte[] emptyData = hexToBytes("");
            assertThrows(KernelTypes.KernelException.class, () -> {
                new Block(emptyData);
            }, "Block creation with empty data should throw KernelException");

            // Test 3: Valid block from wrong chain (Bitcoin mainnet genesis on regtest)
            // This will deserialize successfully but be rejected during processing
            String wrongChainHex = "0100000000000000000000000000000000000000000000000000000000000000000000003ba3edfd7a7b12b27ac72c3e67768f617fc81bc3888a51323a9fb8aa4b1e5e4a29ab5f49ffff001d1dac2b7c0101000000010000000000000000000000000000000000000000000000000000000000000000ffffffff4d04ffff001d0104455468652054696d65732030332f4a616e2f32303039204368616e63656c6c6f72206f6e206272696e6b206f66207365636f6e64206261696c6f757420666f722062616e6b73ffffffff0100f2052a01000000434104678afdb0fe5548271967f1a67130b7105cd6a828e03909a67962e0ea1f61deb649f6bc3f4cef38c4f35504e51ec112de5c384df7ba0b8d578a4c702b6bf11d5fac00000000";
            byte[] wrongChainData = hexToBytes(wrongChainHex);

            try (Block wrongChainBlock = new Block(wrongChainData)) {
                boolean[] newBlock = new boolean[1];
                chainman.ProcessBlock(wrongChainBlock, newBlock);

                // The block should be rejected (wrong chain)
                assertFalse(newBlock[0], "Block from wrong chain should not be marked as new");
            }
        }
        System.out.println("Invalid block test passed");
    }

    @Test
    @Order(3)
    @DisplayName("Test process data - process legitimate blocks from test file")
    public void testProcessData(@TempDir Path tempDir) throws Exception {
        TestSetup setup = testingSetup(tempDir);
        Path blocksDir = Paths.get(setup.dataDir, "blocks");
        Files.createDirectories(blocksDir);

        List<byte[]> blockData = readBlockData();
        ChainstateManagerOptions options = new ChainstateManagerOptions(
            setup.context,
            setup.dataDir,
            blocksDir.toString()
        );

        try (ChainstateManager chainman = new ChainstateManager(setup.context, options)) {
            for (byte[] rawBlock : blockData) {
                try (Block block = new Block(rawBlock)) {
                    boolean[] newBlock = new boolean[1];
                    boolean success = chainman.ProcessBlock(block, newBlock);

                    assertTrue(success, "Block processing should succeed");
                    assertTrue(newBlock[0], "Block should be marked as new");
                }
            }
        }
        System.out.println("Process data test passed - processed " + blockData.size() + " blocks");
    }

    @Test
    @Order(4)
    @DisplayName("Test validate any - duplicate block rejection")
    public void testValidateAny(@TempDir Path tempDir) throws Exception {
        TestSetup setup = testingSetup(tempDir);
        Path blocksDir = Paths.get(setup.dataDir, "blocks");
        Files.createDirectories(blocksDir);

        List<byte[]> blockData = readBlockData();
        ChainstateManagerOptions options = new ChainstateManagerOptions(
            setup.context,
            setup.dataDir,
            blocksDir.toString()
        );

        try (ChainstateManager chainman = new ChainstateManager(setup.context, options)) {
            // Import all blocks first
            chainman.ImportBlocks(null);

            // Try to process second block again (index 1)
            if (blockData.size() > 1) {
                try (Block block = new Block(blockData.get(1))) {
                    boolean[] newBlock = new boolean[1];
                    chainman.ProcessBlock(block, newBlock);

                    // Should be rejected as duplicate
                    assertFalse(newBlock[0], "Duplicate block should not be marked as new");
                }
            }
        }
        System.out.println("Validate any test passed");
    }

    @Test
    @Order(5)
    @DisplayName("Test script verification - validate transactions for different types")
    public void scriptVerifyTest() throws Exception {
        // Test 1: Old-style P2PKH transaction
        String scriptPubkey1 = "76a9144bfbaf6afb76cc5771bc6404810d1cc041a6933988ac";
        String tx1 = "02000000013f7cebd65c27431a90bba7f796914fe8cc2ddfc3f2cbd6f7e5f2fc854534da95000000006b483045022100de1ac3bcdfb0332207c4a91f3832bd2c2915840165f876ab47c5f8996b971c3602201c6c053d750fadde599e6f5c4e1963df0f01fc0d97815e8157e3d59fe09ca30d012103699b464d1d8bc9e47d4fb1cdaa89a1c5783d68363c4dbc4b524ed3d857148617feffffff02836d3c01000000001976a914fc25d6d5c94003bf5b0c7b640a248e2c637fcfb088ac7ada8202000000001976a914fbed3d9b11183209a57999d54d59f67c019e756c88ac6acb0700";
        assertDoesNotThrow(() -> {
            verifyTest(scriptPubkey1, tx1, 0, 0);
        }, "Old-style transaction verification should succeed");

        // Test 2: Segwit P2SH transaction
        String scriptPubkey2 = "a91434c06f8c87e355e123bdc6dda4ffabc64b6989ef87";
        String tx2 = "01000000000101d9fd94d0ff0026d307c994d0003180a5f248146efb6371d040c5973f5f66d9df0400000017160014b31b31a6cb654cfab3c50567bcf124f48a0beaecffffffff012cbd1c000000000017a914233b74bf0823fa58bbbd26dfc3bb4ae715547167870247304402206f60569cac136c114a58aedd80f6fa1c51b49093e7af883e605c212bdafcd8d202200e91a55f408a021ad2631bc29a67bd6915b2d7e9ef0265627eabd7f7234455f6012103e7e802f50344303c76d12c089c8724c1b230e3b745693bbe16aad536293d15e300000000";
        assertDoesNotThrow(() -> {
            verifyTest(scriptPubkey2, tx2, 1900000, 0);
        }, "Segwit P2SH transaction verification should succeed");

        // Test 3: Native Segwit transaction
        String scriptPubkey3 = "0020701a8d401c84fb13e6baf169d59684e17abd9fa216c8cc5b9fc63d622ff8c58d";
        String tx3 = "010000000001011f97548fbbe7a0db7588a66e18d803d0089315aa7d4cc28360b6ec50ef36718a0100000000ffffffff02df1776000000000017a9146c002a686959067f4866b8fb493ad7970290ab728757d29f0000000000220020701a8d401c84fb13e6baf169d59684e17abd9fa216c8cc5b9fc63d622ff8c58d04004730440220565d170eed95ff95027a69b313758450ba84a01224e1f7f130dda46e94d13f8602207bdd20e307f062594022f12ed5017bbf4a055a06aea91c10110a0e3bb23117fc014730440220647d2dc5b15f60bc37dc42618a370b2a1490293f9e5c8464f53ec4fe1dfe067302203598773895b4b16d37485cbe21b337f4e4b650739880098c592553add7dd4355016952210375e00eb72e29da82b89367947f29ef34afb75e8654f6ea368e0acdfd92976b7c2103a1b26313f430c4b15bb1fdce663207659d8cac749a0e53d70eff01874496feff2103c96d495bfdd5ba4145e3e046fee45e84a8a48ad05bd8dbb395c011a32cf9f88053ae00000000";
        assertDoesNotThrow(() -> {
            verifyTest(scriptPubkey3, tx3, 18393430, 0);
        }, "Native Segwit transaction verification should succeed");

        // Test 4: Wrong signature (error expected)
        String scriptPubkey4 = "76a9144bfbaf6afb76cc5771bc6404810d1cc041a6933988ff"; // Changed last byte
        assertThrows(KernelTypes.KernelException.class, () -> {
            verifyTest(scriptPubkey4, tx1, 0, 0);
        }, "Wrong signature should fail verification");

        // Test 5: Wrong amount (error expected)
        assertThrows(KernelTypes.KernelException.class, () -> {
            verifyTest(scriptPubkey2, tx2, 900000, 0); // Wrong amount
        }, "Wrong amount should fail verification");

        // Test 6: Wrong segwit data (error expected)
        String scriptPubkey6 = "0020701a8d401c84fb13e6baf169d59684e17abd9fa216c8cc5b9fc63d622ff8c58f"; // Changed last byte
        assertThrows(KernelTypes.KernelException.class, () -> {
            verifyTest(scriptPubkey6, tx3, 18393430, 0);
        }, "Wrong segwit data should fail verification");

        System.out.println("Script verify test passed");
    }

    /**
     * Helper method for script verification tests
     */
    private void verifyTest(String scriptPubkeyHex, String txHex, long amount, int inputIndex) throws Exception {
        byte[] scriptBytes = hexToBytes(scriptPubkeyHex);
        byte[] txBytes = hexToBytes(txHex);

        try (ScriptPubkey scriptPubkey = new ScriptPubkey(scriptBytes);
             Transaction tx = new Transaction(txBytes)) {

            // Use VERIFY_ALL_PRE_TAPROOT flags
            int flags = KernelTypes.ScriptVerificationFlags.SCRIPT_VERIFY_P2SH |
                       KernelTypes.ScriptVerificationFlags.SCRIPT_VERIFY_DERSIG |
                       KernelTypes.ScriptVerificationFlags.SCRIPT_VERIFY_NULLDUMMY |
                       KernelTypes.ScriptVerificationFlags.SCRIPT_VERIFY_CHECKLOCKTIMEVERIFY |
                       KernelTypes.ScriptVerificationFlags.SCRIPT_VERIFY_CHECKSEQUENCEVERIFY |
                       KernelTypes.ScriptVerificationFlags.SCRIPT_VERIFY_WITNESS;

            scriptPubkey.verify(amount, tx, new TransactionOutput[0], inputIndex, flags);
        }
    }

    @Test
    @Order(6)
    @DisplayName("Test verify input validation - bounds checking and validation")
    public void testVerifyInputValidation() throws Exception {
        String scriptPubkeyHex = "76a9144bfbaf6afb76cc5771bc6404810d1cc041a6933988ac";
        String txHex = "02000000013f7cebd65c27431a90bba7f796914fe8cc2ddfc3f2cbd6f7e5f2fc854534da95000000006b483045022100de1ac3bcdfb0332207c4a91f3832bd2c2915840165f876ab47c5f8996b971c3602201c6c053d750fadde599e6f5c4e1963df0f01fc0d97815e8157e3d59fe09ca30d012103699b464d1d8bc9e47d4fb1cdaa89a1c5783d68363c4dbc4b524ed3d857148617feffffff02836d3c01000000001976a914fc25d6d5c94003bf5b0c7b640a248e2c637fcfb088ac7ada8202000000001976a914fbed3d9b11183209a57999d54d59f67c019e756c88ac6acb0700";

        byte[] scriptBytes = hexToBytes(scriptPubkeyHex);
        byte[] txBytes = hexToBytes(txHex);

        try (ScriptPubkey scriptPubkey = new ScriptPubkey(scriptBytes);
             Transaction tx = new Transaction(txBytes)) {

            int validFlags = KernelTypes.ScriptVerificationFlags.SCRIPT_VERIFY_P2SH;

            // Test 1: Input index out of bounds
            assertThrows(IndexOutOfBoundsException.class, () -> {
                scriptPubkey.verify(0, tx, new TransactionOutput[0], 999, validFlags);
            }, "Out of bounds input index should fail");

            // Test 2: Invalid flags combination - WITNESS without required flags
            assertThrows(KernelTypes.KernelException.class, () -> {
                scriptPubkey.verify(0, tx, new TransactionOutput[0], 0,
                    KernelTypes.ScriptVerificationFlags.SCRIPT_VERIFY_WITNESS);
            }, "Invalid flags combination should fail");

            // Test 3: Spent outputs required for taproot
            assertThrows(KernelTypes.KernelException.class, () -> {
                scriptPubkey.verify(0, tx, new TransactionOutput[0], 0,
                    KernelTypes.ScriptVerificationFlags.SCRIPT_VERIFY_TAPROOT);
            }, "Taproot without spent outputs should fail");
        }

        System.out.println("Verify input validation test passed");
    }

    @Test
    @Order(7)
    @DisplayName("Test chain operations - iteration and query operations")
    public void testChainOperations(@TempDir Path tempDir) throws Exception {
        TestSetup setup = testingSetup(tempDir);
        try (ChainstateManager chainman = setupChainstateManagerWithBlocks(setup.context, setup.dataDir)) {
            Chain chain = chainman.getChain();

            // Test genesis block
            BlockTreeEntry genesis = chain.getByHeight(0);
            assertEquals(0, genesis.getHeight(), "Genesis height should be 0");
            BlockHash genesisHash = genesis.getBlockHash();

            // Test tip block
            int tipHeight = chain.getHeight();
            assertTrue(tipHeight > 0, "Chain should have blocks");

            // Test contains
            assertTrue(chain.contains(genesis), "Chain should contain genesis");

            // Test iteration
            int count = 0;
            int lastHeight = -1;

            for (BlockTreeEntry entry : chain) {
                assertEquals(count, entry.getHeight(), "Height should match iterator index");
                assertTrue(chain.contains(entry), "Chain should contain each entry");
                lastHeight = entry.getHeight();
                count++;
            }

            assertEquals(tipHeight, lastHeight, "Last height should equal tip height");
        }
        System.out.println("Chain operations test passed");
    }

    @Test
    @Order(8)
    @DisplayName("Test block transactions iterator")
    public void testBlockTransactionsIterator() throws Exception {
        List<byte[]> blockData = readBlockData();
        assertTrue(blockData.size() > 5, "Need at least 6 blocks for test");

        try (Block block = new Block(blockData.get(5))) {
            long txCount = block.countTransaction();
            assertTrue(txCount > 0, "Block should have transactions");

            // Test index access
            for (int i = 0; i < txCount; i++) {
                try (Transaction tx1 = block.getTransaction(i);
                     Transaction tx2 = block.getTransaction(i)) {
                    assertEquals(tx1.countInputs(), tx2.countInputs(), "Input counts should match");
                    assertEquals(tx1.countOutputs(), tx2.countOutputs(), "Output counts should match");
                }
            }
        }
        System.out.println("Block transactions iterator test passed");
    }

    @Test
    @Order(9)
    @DisplayName("Test block spent outputs iterator")
    public void testBlockSpentOutputsIterator(@TempDir Path tempDir) throws Exception {
        TestSetup setup = testingSetup(tempDir);
        try (ChainstateManager chainman = setupChainstateManagerWithBlocks(setup.context, setup.dataDir)) {
            Chain chain = chainman.getChain();
            BlockTreeEntry tip = chain.getByHeight(chain.getHeight());

            BlockSpentOutputs spentOutputs = chainman.readBlockSpentOutputs(tip);
            if (spentOutputs != null) {
                long count = spentOutputs.count();

                // Count via iterator
                long iteratorCount = 0;
                for (TransactionSpentOutputs txSpent : spentOutputs) {
                    iteratorCount++;
                }
                assertEquals(count, iteratorCount, "Iterator count should match");

                spentOutputs.close();
            }
        }
        System.out.println("Block spent outputs iterator test passed");
    }

    @Test
    @Order(10)
    @DisplayName("Test transaction spent outputs iterator")
    public void testTransactionSpentOutputsIterator(@TempDir Path tempDir) throws Exception {
        TestSetup setup = testingSetup(tempDir);
        try (ChainstateManager chainman = setupChainstateManagerWithBlocks(setup.context, setup.dataDir)) {
            Chain chain = chainman.getChain();
            BlockTreeEntry tip = chain.getByHeight(chain.getHeight());

            BlockSpentOutputs spentOutputs = chainman.readBlockSpentOutputs(tip);
            if (spentOutputs != null && spentOutputs.count() > 0) {
                TransactionSpentOutputs txSpent = spentOutputs.getTransactionSpentOutputs(0);
                long count = txSpent.count();

                // Count via iterator
                long iteratorCount = 0;
                long coinbaseCount = 0;
                for (Coin coin : txSpent) {
                    iteratorCount++;
                    if (coin.isCoinbase()) {
                        coinbaseCount++;
                    }
                }
                assertEquals(count, iteratorCount, "Iterator count should match");

                spentOutputs.close();
            }
        }
        System.out.println("Transaction spent outputs iterator test passed");
    }

    @Test
    @Order(11)
    @DisplayName("Test nested iteration")
    public void testNestedIteration(@TempDir Path tempDir) throws Exception {
        TestSetup setup = testingSetup(tempDir);
        try (ChainstateManager chainman = setupChainstateManagerWithBlocks(setup.context, setup.dataDir)) {
            Chain chain = chainman.getChain();
            if (chain.getHeight() >= 1) {
                BlockTreeEntry entry = chain.getByHeight(1);
                BlockSpentOutputs spentOutputs = chainman.readBlockSpentOutputs(entry);

                if (spentOutputs != null) {
                    long totalCoins = 0;
                    long expectedTotal = 0;

                    // Count via nested iteration
                    for (TransactionSpentOutputs txSpent : spentOutputs) {
                        for (Coin coin : txSpent) {
                            totalCoins++;
                        }
                    }

                    // Calculate expected total
                    for (int i = 0; i < spentOutputs.count(); i++) {
                        TransactionSpentOutputs txSpent = spentOutputs.getTransactionSpentOutputs(i);
                        expectedTotal += txSpent.count();
                    }

                    assertEquals(expectedTotal, totalCoins, "Nested iteration count should match");
                    spentOutputs.close();
                }
            }
        }
        System.out.println("Nested iteration test passed");
    }

    @Test
    @Order(12)
    @DisplayName("Test iterator with block transactions")
    public void testIteratorWithBlockTransactions(@TempDir Path tempDir) throws Exception {
        TestSetup setup = testingSetup(tempDir);
        try (ChainstateManager chainman = setupChainstateManagerWithBlocks(setup.context, setup.dataDir)) {
            Chain chain = chainman.getChain();
            if (chain.getHeight() >= 1) {
                BlockTreeEntry entry = chain.getByHeight(1);
                Block block = chainman.readBlock(entry);
                BlockSpentOutputs spentOutputs = chainman.readBlockSpentOutputs(entry);

                if (block != null && spentOutputs != null) {
                    long txCount = block.countTransaction();

                    // Skip coinbase (first transaction)
                    for (int i = 1; i < txCount && i - 1 < spentOutputs.count(); i++) {
                        try (Transaction tx = block.getTransaction(i)) {
                            TransactionSpentOutputs txSpent = spentOutputs.getTransactionSpentOutputs(i - 1);

                            // Verify input count matches spent outputs count
                            assertEquals(tx.countInputs(), txSpent.count(),
                                "Transaction input count should match spent outputs count");
                        }
                    }

                    if (block != null) block.close();
                    if (spentOutputs != null) spentOutputs.close();
                }
            }
        }
        System.out.println("Iterator with block transactions test passed");
    }
}

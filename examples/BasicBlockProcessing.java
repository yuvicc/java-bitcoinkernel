import org.bitcoinkernel.*;
import org.bitcoinkernel.Chainstate.*;
import org.bitcoinkernel.Blocks.*;

import java.io.*;
import java.nio.file.*;
import java.util.*;

/**
 * Example: Basic Block Processing
 *
 * This example demonstrates:
 * - Creating a Bitcoin Kernel instance
 * - Processing blocks from a file
 * - Querying chain state
 * - Iterating through the blockchain
 *
 * Usage:
 *   javac --class-path ../build/classes/java/main BasicBlockProcessing.java
 *   java --class-path ../build/classes/java/main:. \
 *        --enable-native-access=ALL-UNNAMED \
 *        -Djava.library.path=../bitcoinkernel/bitcoin/build/lib \
 *        BasicBlockProcessing
 */
public class BasicBlockProcessing {

    public static void main(String[] args) throws Exception {
        System.out.println("=".repeat(70));
        System.out.println("Basic Block Processing Example");
        System.out.println("=".repeat(70) + "\n");

        // Create a temporary directory for the blockchain data
        Path tempDir = Files.createTempDirectory("bitcoin-example");
        Path blocksDir = tempDir.resolve("blocks");
        Files.createDirectories(blocksDir);

        System.out.println("Data directory: " + tempDir);
        System.out.println("Blocks directory: " + blocksDir + "\n");

        // Initialize the Bitcoin Kernel
        try (BitcoinKernel kernel = new BitcoinKernel(
                ChainType.REGTEST,
                tempDir,
                blocksDir,
                msg -> System.out.println("  [Kernel] " + msg)
        )) {
            System.out.println("Bitcoin Kernel initialized\n");

            // Get the chainstate manager
            ChainstateManager chainman = kernel.getChainstateManager();

            // Load and process blocks from test data
            String blockDataFile = "../tests/block_data.txt";
            if (!new File(blockDataFile).exists()) {
                System.err.println("Block data file not found: " + blockDataFile);
                System.out.println("Run this from the examples/ directory");
                return;
            }

            System.out.println("Reading blocks from " + blockDataFile);
            List<byte[]> blocks = readBlockData(blockDataFile);
            System.out.println("Loaded " + blocks.size() + " blocks\n");

            // Process each block
            System.out.println("Processing blocks...");
            int processed = 0;
            for (int i = 0; i < Math.min(10, blocks.size()); i++) {
                try (Block block = new Block(blocks.get(i))) {
                    boolean[] isNewBlock = new boolean[1];
                    boolean success = chainman.ProcessBlock(block, isNewBlock);

                    if (success && isNewBlock[0]) {
                        processed++;
                        System.out.println("   Block " + i + ": processed");
                    }
                }
            }
            System.out.println("Processed " + processed + " new blocks\n");

            // Query chain state
            Chain chain = chainman.getChain();
            System.out.println("Chain Statistics:");
            System.out.println("Height: " + chain.getHeight());
            System.out.println("Total blocks: " + (chain.getHeight() + 1) + "\n");

            // Get genesis block
            BlockTreeEntry genesis = chain.getByHeight(0);
            System.out.println("Genesis Block:");
            System.out.println("Height: " + genesis.getHeight());
            System.out.println("Hash: " + bytesToHex(genesis.getBlockHash().getHash()) + "\n");

            // Get tip block
            if (chain.getHeight() > 0) {
                BlockTreeEntry tip = chain.getByHeight(chain.getHeight());
                System.out.println("Tip Block:");
                System.out.println("Height: " + tip.getHeight());
                System.out.println("Hash: " + bytesToHex(tip.getBlockHash().getHash()) + "\n");
            }

            // Iterate through the chain
            System.out.println("Chain blocks:");
            int count = 0;
            for (BlockTreeEntry entry : chain) {
                System.out.println("Block " + entry.getHeight() + ": " +
                                 bytesToHex(entry.getBlockHash().getHash()).substring(0, 16) + "...");
                count++;
                if (count >= 5) {
                    System.out.println("... and " + (chain.getHeight() - 4) + " more");
                    break;
                }
            }

        }

        System.out.println("\nCleanup complete");
        System.out.println("=".repeat(70));

        // Clean up temp directory
        deleteDirectory(tempDir.toFile());
    }

    private static List<byte[]> readBlockData(String filepath) throws IOException {
        List<byte[]> blocks = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(new FileReader(filepath))) {
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

    private static byte[] hexToBytes(String hex) {
        int len = hex.length();
        byte[] data = new byte[len / 2];
        for (int i = 0; i < len; i += 2) {
            data[i / 2] = (byte) ((Character.digit(hex.charAt(i), 16) << 4)
                                 + Character.digit(hex.charAt(i+1), 16));
        }
        return data;
    }

    private static String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }

    private static void deleteDirectory(File directory) {
        if (directory.isDirectory()) {
            File[] files = directory.listFiles();
            if (files != null) {
                for (File file : files) {
                    deleteDirectory(file);
                }
            }
        }
        directory.delete();
    }
}

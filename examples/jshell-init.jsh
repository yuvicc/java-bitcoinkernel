// JShell initialization script for java-bitcoinkernel
// This script provides helper functions and imports for interactive testing

System.out.println("=".repeat(70));
System.out.println("Java-Bitcoinkernel Interactive Shell");
System.out.println("=".repeat(70));

// Import all necessary classes
import org.bitcoinkernel.*;
import org.bitcoinkernel.Chainstate.*;
import org.bitcoinkernel.Blocks.*;
import org.bitcoinkernel.Transactions.*;
import org.bitcoinkernel.KernelData.*;
import org.bitcoinkernel.KernelTypes.*;
import org.bitcoinkernel.ContextManager.*;
import org.bitcoinkernel.NotificationsManager.*;

import java.nio.file.*;
import java.io.*;
import java.util.*;
import static org.bitcoinkernel.KernelTypes.ScriptVerificationFlags.*;

System.out.println("✓ Imports loaded");

// Helper function: Convert hex string to byte array
byte[] hexToBytes(String hex) {
    if (hex == null || hex.isEmpty()) {
        return new byte[0];
    }
    int len = hex.length();
    if (len % 2 != 0) {
        throw new IllegalArgumentException("Hex string must have even length");
    }
    byte[] data = new byte[len / 2];
    for (int i = 0; i < len; i += 2) {
        data[i / 2] = (byte) ((Character.digit(hex.charAt(i), 16) << 4)
                             + Character.digit(hex.charAt(i+1), 16));
    }
    return data;
}

// Helper function: Convert byte array to hex string
String bytesToHex(byte[] bytes) {
    if (bytes == null || bytes.length == 0) {
        return "";
    }
    StringBuilder sb = new StringBuilder();
    for (byte b : bytes) {
        sb.append(String.format("%02x", b));
    }
    return sb.toString();
}

// Helper function: Load block data from file
List<byte[]> loadBlockData(String filepath) throws IOException {
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

// Helper function: Create a temporary Bitcoin Kernel instance for testing
BitcoinKernel createTempKernel(ChainType chainType) throws Exception {
    Path tempDir = Files.createTempDirectory("bitcoin-kernel-jshell");
    Path blocksDir = tempDir.resolve("blocks");
    Files.createDirectories(blocksDir);

    System.out.println("Created temp kernel at: " + tempDir);

    return new BitcoinKernel(
        chainType,
        tempDir,
        blocksDir,
        msg -> System.out.println("[Kernel] " + msg)
    );
}

BitcoinKernel createTempKernel() throws Exception {
    return createTempKernel(ChainType.REGTEST);
}

// Helper function: Create a kernel with specific data directory
BitcoinKernel createKernel(String dataDir, String blocksDir) throws Exception {
    return new BitcoinKernel(
        ChainType.REGTEST,
        Path.of(dataDir),
        Path.of(blocksDir),
        msg -> System.out.println("[Kernel] " + msg)
    );
}

// Helper function: Process multiple blocks from file
int processBlocksFromFile(ChainstateManager chainman, String filepath) throws Exception {
    List<byte[]> blocks = loadBlockData(filepath);
    int count = 0;

    for (byte[] blockData : blocks) {
        try (Block block = new Block(blockData)) {
            boolean[] isNew = new boolean[1];
            boolean success = chainman.ProcessBlock(block, isNew);
            if (success && isNew[0]) {
                count++;
            }
        }
    }

    return count;
}

// Helper function: Print chain info
void printChainInfo(Chain chain) {
    System.out.println("Chain Information:");
    System.out.println("  Height: " + chain.getHeight());
    System.out.println("  Total blocks: " + (chain.getHeight() + 1));

    if (chain.getHeight() >= 0) {
        var genesis = chain.getByHeight(0);
        System.out.println("  Genesis height: " + genesis.getHeight());
    }
}

// Helper function: Verify a transaction script
boolean verifyScript(String scriptHex, String txHex, long amount, int inputIndex, int flags) {
    try (var script = new ScriptPubkey(hexToBytes(scriptHex));
         var tx = new Transaction(hexToBytes(txHex))) {

        script.verify(amount, tx, new TransactionOutput[0], inputIndex, flags);
        return true;
    } catch (Exception e) {
        System.err.println("Verification failed: " + e.getMessage());
        return false;
    }
}

// Constants for commonly used verification flags
int VERIFY_STANDARD = SCRIPT_VERIFY_P2SH | SCRIPT_VERIFY_DERSIG;
int VERIFY_SEGWIT = SCRIPT_VERIFY_P2SH | SCRIPT_VERIFY_DERSIG | SCRIPT_VERIFY_WITNESS;
int VERIFY_TAPROOT_FULL = SCRIPT_VERIFY_P2SH | SCRIPT_VERIFY_DERSIG |
                          SCRIPT_VERIFY_WITNESS | SCRIPT_VERIFY_TAPROOT;

System.out.println("✓ Helper functions loaded");
System.out.println("\nAvailable helpers:");
System.out.println("  hexToBytes(String)           - Convert hex to bytes");
System.out.println("  bytesToHex(byte[])           - Convert bytes to hex");
System.out.println("  loadBlockData(String)        - Load blocks from file");
System.out.println("  createTempKernel()           - Create temp kernel instance");
System.out.println("  createKernel(String, String) - Create kernel with paths");
System.out.println("  processBlocksFromFile(...)   - Process blocks from file");
System.out.println("  printChainInfo(Chain)        - Print chain statistics");
System.out.println("  verifyScript(...)            - Verify a transaction script");
System.out.println("\nVerification flag constants:");
System.out.println("  VERIFY_STANDARD              - P2SH + DERSIG");
System.out.println("  VERIFY_SEGWIT                - Standard + Witness");
System.out.println("  VERIFY_TAPROOT_FULL          - All flags including Taproot");
System.out.println("\n" + "=".repeat(70));
System.out.println("Ready! Try: var kernel = createTempKernel();");
System.out.println("=".repeat(70) + "\n");

# Java-Bitcoinkernel Examples

This directory contains examples and interactive testing guides for the java-bitcoinkernel library.

## Table of Contents

- [Interactive Testing with JShell](#interactive-testing-with-jshell)
- [Example Programs](#example-programs)
- [Test Data](#test-data)

---

## Interactive Testing with JShell

JShell provides an interactive REPL (Read-Eval-Print Loop) for quick testing and exploration of the Bitcoin Kernel API.

### Prerequisites

1. Build the project first:
```bash
./gradlew build
```

2. Make sure you have test block data available at `tests/block_data.txt`

### Starting JShell

```bash
# From the project root directory
jshell --class-path build/libs/java-bitcoinkernel.jar:build/classes/java/main \
       --enable-native-access=ALL-UNNAMED \
       -J-Djava.library.path=bitcoinkernel/bitcoin/build/lib \
       -R--enable-native-access=ALL-UNNAMED \
       examples/jshell-init.jsh
```

Or use the provided script:

```bash
./examples/jshell-run.sh
```

### JShell Quick Start

Once in JShell, you can interactively test the library:

```java
// Import necessary classes
import org.bitcoinkernel.*;
import org.bitcoinkernel.Chainstate.*;
import org.bitcoinkernel.Blocks.*;
import org.bitcoinkernel.Transactions.*;
import org.bitcoinkernel.KernelData.*;
import java.nio.file.Path;
import java.nio.file.Files;

// Create a temporary directory for testing
var tempDir = Files.createTempDirectory("bitcoin-test");
var blocksDir = tempDir.resolve("blocks");
Files.createDirectories(blocksDir);

// Initialize Bitcoin Kernel with regtest
var kernel = new BitcoinKernel(
    ChainType.REGTEST,
    tempDir,
    blocksDir,
    msg -> System.out.println("[Kernel] " + msg)
);

// Get chainstate manager
var chainman = kernel.getChainstateManager();

// Create a block from hex data (genesis block)
var genesisHex = "0100000000000000000000000000000000000000000000000000000000000000000000003ba3edfd7a7b12b27ac72c3e67768f617fc81bc3888a51323a9fb8aa4b1e5e4adae5494dffff7f20020000000101000000010000000000000000000000000000000000000000000000000000000000000000ffffffff4d04ffff001d0104455468652054696d65732030332f4a616e2f32303039204368616e63656c6c6f72206f6e206272696e6b206f66207365636f6e64206261696c6f757420666f722062616e6b73ffffffff0100f2052a01000000434104678afdb0fe5548271967f1a67130b7105cd6a828e03909a67962e0ea1f61deb649f6bc3f4cef38c4f35504e51ec112de5c384df7ba0b8d578a4c702b6bf11d5fac00000000";

var blockData = hexToBytes(genesisHex);
var block = new Block(blockData);

// Process the block
var isNew = new boolean[1];
var success = chainman.ProcessBlock(block, isNew);
System.out.println("Block processed: " + success);
System.out.println("Is new block: " + isNew[0]);

// Get chain information
var chain = chainman.getChain();
System.out.println("Chain height: " + chain.getHeight());

// Get genesis block entry
var genesis = chain.getByHeight(0);
System.out.println("Genesis height: " + genesis.getHeight());

// Cleanup
block.close();
kernel.close();
```

### Helper Functions in JShell

The `jshell-init.jsh` script provides these helper functions:

#### `hexToBytes(String hex)` - Convert hex string to byte array
```java
var data = hexToBytes("deadbeef");
// Returns: byte[] { 0xde, 0xad, 0xbe, 0xef }
```

#### `bytesToHex(byte[] bytes)` - Convert byte array to hex string
```java
var hex = bytesToHex(new byte[]{(byte)0xde, (byte)0xad});
// Returns: "dead"
```

#### `loadBlockData(String filepath)` - Load block data from file
```java
var blocks = loadBlockData("tests/block_data.txt");
// Returns: List<byte[]> of block data
```

#### `createTempKernel()` - Create a kernel instance with temp directory
```java
var kernel = createTempKernel();
// Returns: BitcoinKernel instance ready to use
```

### Common JShell Workflows

#### Testing Block Processing

```java
// Start JShell with init script
/open examples/jshell-init.jsh

// Create kernel
var kernel = createTempKernel();
var chainman = kernel.getChainstateManager();

// Load and process blocks
var blocks = loadBlockData("tests/block_data.txt");
for (var blockData : blocks) {
    var block = new Block(blockData);
    var isNew = new boolean[1];
    chainman.ProcessBlock(block, isNew);
    block.close();
}

// Check chain
var chain = chainman.getChain();
System.out.println("Processed " + (chain.getHeight() + 1) + " blocks");

// Iterate through chain
for (var entry : chain) {
    System.out.println("Block " + entry.getHeight());
}

kernel.close();
```

#### Testing Script Verification

```java
/open examples/jshell-init.jsh

// P2PKH transaction data
var scriptHex = "76a9144bfbaf6afb76cc5771bc6404810d1cc041a6933988ac";
var txHex = "02000000013f7cebd65c27431a90bba7f796914fe8cc2ddfc3f2cbd6f7e5f2fc854534da95000000006b483045022100de1ac3bcdfb0332207c4a91f3832bd2c2915840165f876ab47c5f8996b971c3602201c6c053d750fadde599e6f5c4e1963df0f01fc0d97815e8157e3d59fe09ca30d012103699b464d1d8bc9e47d4fb1cdaa89a1c5783d68363c4dbc4b524ed3d857148617feffffff02836d3c01000000001976a914fc25d6d5c94003bf5b0c7b640a248e2c637fcfb088ac7ada8202000000001976a914fbed3d9b11183209a57999d54d59f67c019e756c88ac6acb0700";

var script = new ScriptPubkey(hexToBytes(scriptHex));
var tx = new Transaction(hexToBytes(txHex));

// Verify with standard flags
import static org.bitcoinkernel.KernelTypes.ScriptVerificationFlags.*;
int flags = SCRIPT_VERIFY_P2SH | SCRIPT_VERIFY_DERSIG;

try {
    script.verify(0, tx, new TransactionOutput[0], 0, flags);
    System.out.println("Script verified!");
} catch (Exception e) {
    System.out.println("Script Verification failed: " + e.getMessage());
}

script.close();
tx.close();
```

#### Testing Block Serialization

```java
/open examples/jshell-init.jsh

var blockData = hexToBytes("your_block_hex_here");
var block = new Block(blockData);

// Serialize back to bytes
var serialized = block.toBytes();

// Verify round-trip
var matches = java.util.Arrays.equals(blockData, serialized);
System.out.println("Round-trip successful: " + matches);

block.close();
```

### Troubleshooting JShell

**Problem: UnsatisfiedLinkError when loading native library**

Solution: Make sure the library path is set correctly:
```bash
export LD_LIBRARY_PATH=/path/to/java-bitcoinkernel/bitcoinkernel/bitcoin/build/lib:$LD_LIBRARY_PATH
```

**Problem: Classes not found**

Solution: Ensure the class path includes both the JAR and compiled classes:
```bash
jshell --class-path "build/libs/java-bitcoinkernel.jar:build/classes/java/main"
```

**Problem: Native access errors**

Solution: Add the FFM enable flags:
```bash
jshell --enable-native-access=ALL-UNNAMED -R--enable-native-access=ALL-UNNAMED
```

---

## Example Programs

See the following example files:

- **`BasicBlockProcessing.java`** - Process blocks and manage chainstate
- **`ScriptVerification.java`** - Verify transaction scripts
- **`ChainExploration.java`** - Iterate through the blockchain
- **`TransactionAnalysis.java`** - Analyze transaction data

Each example can be run independently:

```bash
javac --class-path build/libs/java-bitcoinkernel.jar examples/BasicBlockProcessing.java
java --class-path build/libs/java-bitcoinkernel.jar:examples \
     --enable-native-access=ALL-UNNAMED \
     -Djava.library.path=bitcoinkernel/bitcoin/build/lib \
     BasicBlockProcessing
```

---

## Test Data

The `tests/block_data.txt` file contains serialized regtest blocks in hex format, one per line. You can use these for testing:

```bash
# First block
head -n 1 tests/block_data.txt

# Count total blocks
wc -l tests/block_data.txt
```

To generate your own test blocks:
1. Run Bitcoin Core in regtest mode
2. Mine some blocks: `bitcoin-cli -regtest generatetoaddress 10 <address>`
3. Export blocks using `bitcoin-cli -regtest getblock <hash> 0`

---

## Tips and Best Practices

1. **Always use try-with-resources** or explicit `.close()` calls for native objects
2. **Start simple** - Test basic operations before complex workflows
3. **Use JShell for rapid prototyping** then convert to proper Java programs
4. **Check the tests** - `src/test/java/` contains comprehensive examples
5. **Monitor memory** - Use `jcmd <pid> GC.run` if testing many operations

---

## Further Reading

- [Main README](../README.md) - Project overview and setup
- [JShell Tutorial](https://docs.oracle.com/en/java/javase/21/jshell/)
- [Bitcoin Core regtest guide](https://developer.bitcoin.org/examples/testing.html)
- [libbitcoinkernel API](https://github.com/bitcoin/bitcoin/pull/30595)

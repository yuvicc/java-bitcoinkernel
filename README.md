# Java-Bitcoinkernel ⚠️🚧

> Java FFM wrapper for Bitcoin Core's validation engine via [libbitcoinkernel](https://github.com/bitcoin/bitcoin/pull/30595)

## ⚠️ Status

This library is **alpha** - under active development. APIs may change and functionality is still being expanded. Contributions and bug reports are welcome!

## Overview

`java-bitcoinkernel` uses Java's Foreign Function & Memory API (FFM) to call into Bitcoin Core's `libbitcoinkernel`, exposing core functionalities—including block & transaction validation, chainstate management, and block data access—through a safe and idiomatic Java interface.

### Features

**Transaction validation** using Bitcoin Core's consensus engine
**Block validation** and processing
**Chainstate management** with full chain operations
**Block and transaction iteration** with spent outputs tracking
**Script verification** with all pre-taproot and taproot flags
**Clean Java-native bindings** via FFM with minimal overhead
**Memory-safe** with proper resource management using AutoCloseable

## Requirements

- **JDK 25+** (for FFM API support)
- **CMake 3.16+** fore Bitcoin Core compiling
- **C++20** for Bitcoin Core compiling
- **Boost libraries** - see Bitcoin Core [build documentation](https://github.com/bitcoin/bitcoin/blob/master/doc/build-unix.md)

## Quick Start

### Installation

```bash
# Clone repository with submodules
git clone --recursive https://github.com/yuvicc/java-bitcoinkernel
cd java-bitcoinkernel

# Build (this will automatically build libbitcoinkernel)
./gradlew build
```

### Running Tests

```bash
./gradlew test
```

### Interactive Testing with JShell

Launch an interactive JShell session to explore the API:

```bash
./examples/jshell-run.sh
```

Then try:
```java
var kernel = createTempKernel();
var chainman = kernel.getChainstateManager();
var blocks = loadBlockData("tests/block_data.txt");
// ... explore the API interactively
```

See [examples/README.md](examples/README.md) for detailed JShell usage and more examples.

### Usage Example

```java
import org.bitcoinkernel.*;
import org.bitcoinkernel.Chainstate.*;
import java.nio.file.Path;

public class Example {
    public static void main(String[] args) throws Exception {
        // Initialize Bitcoin Kernel with regtest parameters
        try (BitcoinKernel kernel = new BitcoinKernel(
                ChainType.REGTEST,
                Path.of("/path/to/datadir"),
                Path.of("/path/to/datadir/blocks"),
                System.out::println  // Logger
        )) {
            // Get the chainstate manager
            ChainstateManager chainman = kernel.getChainstateManager();

            // Process a block
            byte[] blockData = ...; // Your serialized block data
            try (Block block = new Block(blockData)) {
                boolean[] isNewBlock = new boolean[1];
                boolean success = chainman.ProcessBlock(block, isNewBlock);
                System.out.println("Block processed: " + success);
                System.out.println("Was new: " + isNewBlock[0]);
            }

            // Get chain information
            Chain chain = chainman.getChain();
            System.out.println("Chain height: " + chain.getHeight());

            // Iterate through blocks
            for (BlockTreeEntry entry : chain) {
                System.out.println("Block at height " + entry.getHeight());
            }
        }
    }
}
```

### Script Verification Example

```java
import static org.bitcoinkernel.KernelData.*;
import static org.bitcoinkernel.Transactions.*;
import static org.bitcoinkernel.BitcoinKernel.*;

// Verify a transaction input
byte[] scriptPubkeyBytes = ...; // P2PKH, P2SH, or witness script
byte[] txBytes = ...;           // Serialized transaction

try (ScriptPubkey scriptPubkey = new ScriptPubkey(scriptPubkeyBytes);
     Transaction tx = new Transaction(txBytes)) {

    int flags = VERIFY_P2SH | VERIFY_DERSIG | VERIFY_WITNESS;

    BitcoinKernel.verify(
        scriptPubkey,
        amount,
        tx,
        spentOutputs,
        inputIndex,
        flags
    );

    System.out.println("Transaction verified successfully");
}
```

### Memory Management

The wrapper follows Java's `AutoCloseable` pattern:
- Objects created from byte arrays **own** their native memory
- Objects obtained as views (e.g., `Block.getTransaction()`) **do not own** memory

## Development

### Updating Bitcoin Core Submodule

```bash
git subtree pull \
  --prefix bitcoinkernel/bitcoin \
  https://github.com/bitcoin/bitcoin \
  master --squash
```

### Building Manually

```bash
# Configure and build Bitcoin Core
cd bitcoinkernel/bitcoin
cmake -B build -DCMAKE_BUILD_TYPE=RelWithDebInfo -DBUILD_KERNEL_LIB=ON -DBUILD_SHARED_LIBS=ON
cmake --build build -j$(nproc)

# Build Java wrapper
cd ../..
./gradlew compileJava
```

## Platform Support

Currently tested on:
- Linux (Ubuntu 22.04+)
- macOS (12.0+)
- Windows (not tested yet)

## References

- [libbitcoinkernel RFC](https://github.com/bitcoin/bitcoin/pull/30595)
- [Bitcoin Core build documentation](https://github.com/bitcoin/bitcoin/blob/master/doc/build-unix.md)
- [rust-bitcoinkernel wrapper](https://github.com/yuvicc/rust-bitcoinkernel)
- [Java FFM API](https://openjdk.org/jeps/454)

## Current Status

See the project [milestone](https://github.com/yuvicc/java-bitcoinkernel/issues/1) for detailed status and roadmap.

## Contributing

Contributions are welcome! Please feel free to submit issues and pull requests.

## License

This project follows Bitcoin Core's licensing. See the Bitcoin Core repository for details.

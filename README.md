# Java‑Bitcoinkernel ⚠️🚧

> Java‑FFM wrapper for Bitcoin Core’s validation engine via [libbitcoinkernel](https://github.com/bitcoin/bitcoin/pull/30595)

## ⚠️ Status

This library is **alpha**—under work in progress. APIs may change and functionality is still incomplete. Contributions and bug reports are welcome!

## Overview

`java‑bitcoinkernel` uses Java's FFM (Foreign Function Mapping) to call into Bitcoin Core’s `libbitcoinkernel`, exposing core functionalities—including block & transaction validation, and block data access—through a safe Java interface.

### Features

- Transaction validation using Bitcoin Core’s validation engine
- Block validation - todo
- Clean Java-native bindings via FFM with minimal overhead

## Building

We vendor Bitcoin Core’s `libbitcoinkernel` using a Git subtree targeting the `kernelApi` branch from your fork:

```bash
git subtree pull \
  --prefix bitcoinkernel/bitcoin \
  https://github.com/TheCharlatan/bitcoin \
  kernelApi --squash
```

## Requirements
- CMake
- C++17 compiler (e.g. GCC/Clang)
- Boost
- JDK 21+ with FFM support

Refer to Bitcoin Core [docs](https://github.com/bitcoin/bitcoin/blob/master/doc/build-unix.md) for dependency specifics.

### Build
```bash
./gradlew compileJava
```

*Note: You might need to update the build.gradle file with different libraries extension(.so for linux, .dylib for macos, .dll for windows)*

```bash
        libraries = [
                ":/usr/local/lib/libbitcoinkernel.so"
        ]
```


## References
- Upstream PR of C header API library -> https://github.com/bitcoin/bitcoin/pull/30595
- [rust-bitcoinkernel wrapper](https://github.com/yuvicc/rust-bitcoinkernel)

## Current Status of the Library
see issue [milestone](https://github.com/yuvicc/java-bitcoinkernel/issues/1) for more info regarding the status of the library

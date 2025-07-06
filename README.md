# Java‑Bitcoinkernel ⚠️🚧

> Java‑FFM wrapper for Bitcoin Core’s validation engine via [libbitcoinkernel](https://github.com/bitcoin/bitcoin/pull/30595)

## ⚠️ Status

This library is **alpha**—under work in progress. APIs may change and functionality is still incomplete. Contributions and bug reports are welcome!

## Overview

`java‑bitcoinkernel` uses Java's FFM (Foreign Function Mapping) to call into Bitcoin Core’s `libbitcoinkernel`, exposing core functionalities—including block & transaction validation, and block data access—through a safe Java interface.

### Features

- Full block and transaction validation using Bitcoin Core’s validation engine
- Read block metadata (height, timestamp, hash)
- Block data parsing and inspection
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
- Rust (optional, if fuzzing enabled)

Refer to Bitcoin Core [docs](https://github.com/bitcoin/bitcoin/blob/master/doc/build-unix.md) for dependency specifics.

### Build
```bash
./gradlew compileJava
```


## Current Status of the Library
see issue [milestone](https://github.com/yuvicc/java-bitcoinkernel/issues/1)




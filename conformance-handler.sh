#!/bin/bash
# Wrapper script to run the conformance test handler

set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

JAR_PATH="$SCRIPT_DIR/build/libs/java-bitcoinkernel-conformance-handler-0.1.0.jar"
LIB_PATH="$SCRIPT_DIR/bitcoinkernel/bitcoin/build/lib"

if [ ! -f "$JAR_PATH" ]; then
    echo "Error: Conformance handler JAR not found at $JAR_PATH" >&2
    echo "Please run: ./gradlew buildConformanceJar" >&2
    exit 1
fi

if [ ! -f "$LIB_PATH/libbitcoinkernel.so" ]; then
    echo "Error: libbitcoinkernel.so not found at $LIB_PATH" >&2
    echo "Please build the shared library first" >&2
    exit 1
fi

# Run the handler with library path
export LD_LIBRARY_PATH="$LIB_PATH:$LD_LIBRARY_PATH"
exec java --enable-native-access=ALL-UNNAMED \
     -jar "$JAR_PATH" \
     "$@"
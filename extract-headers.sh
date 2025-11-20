#!/bin/sh
# This script generates Java FFM bindings from libbitcoinkernel headers using jextract
# It assumes:
# - jextract is installed at ~/.jextract/jextract-25/bin/jextract
# - Bitcoin Core has been built with BUILD_KERNEL_LIB=ON
# - The bitcoinkernel.h header is available at bitcoinkernel/bitcoin/src/kernel/bitcoinkernel.h

JEXTRACT="$HOME/.jextract/jextract-25/bin/jextract"
BITCOIN_DIR="./bitcoinkernel/bitcoin"
HEADER_FILE="$BITCOIN_DIR/src/kernel/bitcoinkernel.h"
BUILD_DIR="$BITCOIN_DIR/build"
OUTPUT_DIR="src/main/java"

# Check if jextract exists
if [ ! -f "$JEXTRACT" ]; then
    echo "Error: jextract not found at $JEXTRACT"
    echo "Please install jextract to ~/.jextract/jextract-25/"
    exit 1
fi

# Check if header file exists
if [ ! -f "$HEADER_FILE" ]; then
    echo "Error: bitcoinkernel.h not found at $HEADER_FILE"
    echo "Please ensure Bitcoin Core is checked out in bitcoinkernel/bitcoin/"
    exit 1
fi

# Check if Bitcoin Core has been built
if [ ! -d "$BUILD_DIR" ]; then
    echo "Error: Bitcoin Core build directory not found at $BUILD_DIR"
    echo "Please run 'gradle buildBitcoinCore' and 'gradle compileBitcoinCore' first"
    exit 1
fi

echo "Generating jextract bindings for libbitcoinkernel..."

$JEXTRACT --target-package org.bitcoinkernel.jextract \
  --output "$OUTPUT_DIR" \
  -I "$BITCOIN_DIR/src" \
  -I "$BITCOIN_DIR/src/kernel" \
  -I "$BUILD_DIR/src/kernel" \
  --header-class-name bitcoinkernel_h \
  "$HEADER_FILE"

echo "Done! Bindings generated in $OUTPUT_DIR/org/bitcoinkernel/jextract/"

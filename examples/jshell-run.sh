#!/bin/bash
# JShell launcher script for java-bitcoinkernel
# This script sets up the environment and launches JShell with the library loaded

set -e

# Colors for output
RED='\033[0:31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# Determine the script directory
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"

echo -e "${GREEN}==================================================================${NC}"
echo -e "${GREEN}Java-Bitcoinkernel JShell Launcher${NC}"
echo -e "${GREEN}==================================================================${NC}"
echo ""

# Check if build exists
if [ ! -d "$PROJECT_ROOT/build/classes/java/main" ]; then
    echo -e "${YELLOW}⚠ Build directory not found. Building project...${NC}"
    cd "$PROJECT_ROOT"
    ./gradlew build
    echo ""
fi

# Set library path
LIB_PATH="$PROJECT_ROOT/bitcoinkernel/bitcoin/build/lib"

# Check if native library exists
if [ ! -d "$LIB_PATH" ]; then
    echo -e "${RED}✗ Native library not found at: $LIB_PATH${NC}"
    echo -e "${YELLOW}  Please build the project first: ./gradlew build${NC}"
    exit 1
fi

# Set classpath
CLASS_PATH="$PROJECT_ROOT/build/classes/java/main"

# Check for JShell init script
INIT_SCRIPT="$SCRIPT_DIR/jshell-init.jsh"
if [ ! -f "$INIT_SCRIPT" ]; then
    echo -e "${RED}✗ JShell init script not found: $INIT_SCRIPT${NC}"
    exit 1
fi

echo -e "${GREEN}Project root: $PROJECT_ROOT${NC}"
echo -e "${GREEN}Library path: $LIB_PATH${NC}"
echo -e "${GREEN}Class path: $CLASS_PATH${NC}"
echo -e "${GREEN}Init script: $INIT_SCRIPT${NC}"
echo ""

# Export library path for dynamic linker
export LD_LIBRARY_PATH="$LIB_PATH${LD_LIBRARY_PATH:+:$LD_LIBRARY_PATH}"
export DYLD_LIBRARY_PATH="$LIB_PATH${DYLD_LIBRARY_PATH:+:$DYLD_LIBRARY_PATH}"

# Launch JShell
echo -e "${GREEN}Launching JShell...${NC}"
echo ""

exec jshell \
    --class-path "$CLASS_PATH" \
    --startup "$INIT_SCRIPT" \
    --enable-native-access=ALL-UNNAMED \
    -R--enable-native-access=ALL-UNNAMED \
    -J-Djava.library.path="$LIB_PATH" \
    "$@"

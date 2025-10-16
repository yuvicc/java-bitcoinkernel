package org.bitcoinkernel;

import java.lang.foreign.*;

import static org.bitcoinkernel.BitcoinKernelBindings.*;
import static org.bitcoinkernel.Transactions.*;

public class Blocks {

    // ===== Block Validation State =====
    public static class BlockValidationState {
        private final MemorySegment inner;

        public enum ValidationMode {
            VALID(0),
            INVALID(1),
            INTERNAL_ERROR(2);

            private final byte value;

            ValidationMode(int value) {
                this.value = (byte) value;
            }

            public byte getValue() {
                return value;
            }

            public static ValidationMode fromByte(byte value) {
                for (ValidationMode mode : values()) {
                    if (mode.value == value) {
                        return mode;
                    }
                }
                throw new IllegalArgumentException("Invalid Validation Mode: " + value);
            }
        }

        public enum BlockValidationResult {
            UNSET(0),
            CONSENSUS(1),
            CACHED_INVALID(2),
            INVALID_HEADER(3),
            MUTATED(4),
            MISSING_PREV(5),
            INVALID_PREV(6),
            TIME_FUTURE(7),
            HEADER_LOW_WORK(8);

            private final int value;

            BlockValidationResult(int value) {
                this.value = value;
            }

            public int getValue() {
                return value;
            }

            public static BlockValidationResult fromInt(int value) {
                for (BlockValidationResult result : values()) {
                    if (result.value == value) {
                        return result;
                    }
                }
                throw new IllegalArgumentException("Invalid BlockValidationResult: " + value);
            }
        }

        BlockValidationState(MemorySegment inner) {
            if (inner == MemorySegment.NULL) {
                throw new IllegalArgumentException("Block Validation State cannot be null");
            }
            this.inner = inner;
        }

        /**
         * Get the validation mode indicating whether the block is valid, invalid,
         * or an error occurred during validation.
         *
         * @return The validation mode
         */
        public ValidationMode getValidationMode() {
            byte mode = btck_block_validation_state_get_validation_mode(inner);
            return ValidationMode.fromByte(mode);
        }

        /**
         * Get the specific reason why a block was invalid (if applicable).
         * Only meaningful when ValidationMode is INVALID.
         *
         * @return The block validation result
         */
        public BlockValidationResult getBlockValidationResult() {
            int result = btck_block_validation_state_get_block_validation_result(inner);
            return BlockValidationResult.fromInt(result);
        }

        /**
         * Check if the block is valid.
         *
         * @return true if ValidationMode is VALID, false otherwise
         */
        public boolean isValid() {
            return getValidationMode() == ValidationMode.VALID;
        }

        /**
         * Check if the block is invalid.
         *
         * @return true if ValidationMode is INVALID, false otherwise
         */
        public boolean isInvalid() {
            return getValidationMode() == ValidationMode.INVALID;
        }

        /**
         * Check if there was an error during validation
         *
         * @return true if ValidationMode is INTERNAL_ERROR, false otherwise
         */
        public boolean hasError() {
            return getValidationMode() == ValidationMode.INTERNAL_ERROR;
        }

        /**
         * Get a human readable description of the validation state
         *
         * @return A string describing the validation state
         */
        public String getDescription() {
            ValidationMode mode = getValidationMode();
            if (mode == ValidationMode.VALID) {
                return "Block is Valid";
            } else if (mode == ValidationMode.INVALID) {
                BlockValidationResult result = getBlockValidationResult();
                return "Block is Invalid: " + result.name();
            } else {
                return "Internal error during validation";
            }
        }

        @Override
        public String toString() {
            return String.format("BlockValidationState{mode=%s, result=%s}",
                getValidationMode(),
                getBlockValidationResult());
        }

        MemorySegment getInner() {
            return inner;
        }
    }

    // ===== Block Hash =====
    public static class BlockHash implements AutoCloseable {
        private MemorySegment inner;
        private final Arena arena;

        public BlockHash(byte[] hash) throws KernelTypes.KernelException {
            if (hash.length != 32) {
                throw new IllegalStateException("Block Hash length should be 32 bytes");
            }

            this.arena = Arena.ofConfined();
            MemorySegment hashSegment = arena.allocateFrom(ValueLayout.JAVA_BYTE, hash);
            this.inner = btck_block_hash_create(hashSegment);
            if (inner == MemorySegment.NULL) {
                throw new KernelTypes.KernelException("Failed to instantiate Block Hash object");
            }
        }

        // Internal structure for hashes returned by the API
        BlockHash(MemorySegment inner) {
            this.inner = inner;
            this.arena = null;
        }

        public byte[] toBytes() {
            checkClosed();
            try (var tempArena = Arena.ofConfined()) {
                MemorySegment output = tempArena.allocate(32);
                btck_block_hash_to_bytes(inner, output);
                return output.toArray(ValueLayout.JAVA_BYTE);
            }
        }

        public boolean equals(BlockHash other) {
            checkClosed();
            other.checkClosed();
            return btck_block_hash_equals(inner, other.inner) != 0;
        }

        @Override
        public int hashCode() {
            byte[] bytes = toBytes();
            int result = 1;
            for (byte b: bytes) {
                result = 31 * result + b;
            }
            return result;
        }

        MemorySegment getInner() {
            return inner;
        }

        private void checkClosed() {
            if (inner == MemorySegment.NULL) {
                throw new IllegalStateException("Block Hash has been closed");
            }
        }

        @Override
        public void close() throws Exception {
            if (inner != MemorySegment.NULL) {
                btck_block_hash_destroy(inner);
                inner = MemorySegment.NULL;
            }

            if (arena != null) {
                arena.close();
            }
        }
    }

    // ===== Block Tree Entry =====
    public static class BlockTreeEntry {
        private final MemorySegment inner;

        BlockTreeEntry(MemorySegment inner) {
            if (inner == MemorySegment.NULL) {
                throw new IllegalArgumentException("Block Tree cannot be null!");
            }
            this.inner = inner;
        }

        public BlockTreeEntry getPrevious() {
            MemorySegment prev = btck_block_tree_entry_get_previous(inner);
            if (prev == MemorySegment.NULL) {
                return null;
            }
            return new BlockTreeEntry(prev);
        }

        public int getHeight() {
            return btck_block_tree_entry_get_height(inner);
        }

        public BlockHash getBlockHash() {
            MemorySegment hashPtr = btck_block_tree_entry_get_block_hash(inner);
            return new BlockHash(hashPtr);
        }

        MemorySegment getInner() {
            return inner;
        }
    }

    //todo!
    public static class Block implements AutoCloseable {


        @Override
        public void close() throws Exception {

        }
    }

    // ===== Block Spent Outputs =====
    public static class BlockSpentOutputs implements AutoCloseable {
        private final MemorySegment inner;



        @Override
        public void close() throws Exception {

        }
    }
}

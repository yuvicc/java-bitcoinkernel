package org.bitcoinkernel;

import java.lang.foreign.*;
import java.util.Iterator;
import java.util.NoSuchElementException;

import static org.bitcoinkernel.Transactions.*;

import static org.bitcoinkernel.jextract.bitcoinkernel_h.*;

public class Blocks {

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
        CACHE_INVALID(2),
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

    // ===== Block Validation State =====
    public static class BlockValidationState {
        private final MemorySegment inner;

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

        public ValidationMode getValidationMode() {
            byte mode = btck_block_validation_state_get_validation_mode(inner);
            return ValidationMode.fromByte(mode);
        }

        public BlockValidationResult getBlockValidationResult() {
            int result = btck_block_validation_state_get_block_validation_result(inner);
            return BlockValidationResult.fromInt(result);
        }

        public boolean isValid() {
            return getValidationMode() == ValidationMode.VALID;
        }

        public boolean isInvalid() {
            return getValidationMode() == ValidationMode.INVALID;
        }

        public boolean hasError() {
            return getValidationMode() == ValidationMode.INTERNAL_ERROR;
        }

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
        private final boolean ownsMemory;

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
            this.ownsMemory = true;
        }

        // Internal structure for hashes returned by the API
        BlockHash(MemorySegment inner, boolean ownsMemory) {
            this.inner = inner;
            this.arena = null;
            this.ownsMemory = ownsMemory;
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

        public BlockHash copy() {
            checkClosed();
            MemorySegment copied = btck_block_hash_copy(inner);
            if (copied == MemorySegment.NULL) {
                throw new RuntimeException("Failed to copy BlockHash");
            }
            return new BlockHash(copied, true);
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

        public void checkClosed() {
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
            return new BlockHash(hashPtr, false);
        }

        MemorySegment getInner() {
            return inner;
        }
    }

    public static class Block implements AutoCloseable {
        private MemorySegment inner;
        private final Arena arena;

        public Block(byte[] raw_block) throws KernelTypes.KernelException {
            this.arena = Arena.ofConfined();
            MemorySegment blockSegment = arena.allocateFrom(ValueLayout.JAVA_BYTE, raw_block);
            this.inner = btck_block_create(blockSegment, blockSegment.byteSize());
            if (inner == MemorySegment.NULL) {
                arena.close();
                throw new KernelTypes.KernelException("Failed to create block");
            }
        }

        Block(MemorySegment inner) {
            this.inner = inner;
            this.arena = null;
        }

        public BlockHash getHash() {
            checkClosed();
            MemorySegment hashPtr = btck_block_get_hash(inner);
            return new BlockHash(hashPtr, true);
        }

        public long countTransaction() {
            return btck_block_count_transactions(inner);
        }

        public Transaction getTransaction(long index) {
            checkClosed();
            if (index < 0 || index >= countTransaction()) {
                throw new IndexOutOfBoundsException("Transaction index out of bounds: " + index);
            }
            MemorySegment txPtr = btck_block_get_transaction_at(inner, index);
            return new Transaction(txPtr);
        }

        public byte[] toBytes() {
            checkClosed();
            throw new UnsupportedOperationException("Block serialization not yet implemented");
        }

        MemorySegment getInner() {
            return inner;
        }

        public void checkClosed() {
            if (inner == MemorySegment.NULL) {
                throw new IllegalStateException("Block has been closed");
            }
        }

        @Override
        public void close() throws Exception {
            if (inner != MemorySegment.NULL) {
                btck_block_destroy(inner);
                inner = MemorySegment.NULL;
            }
            if (arena != null) {
                arena.close();
            }
        }
    }

    // ===== Block Spent Outputs =====
    public static class BlockSpentOutputs implements AutoCloseable, Iterable<TransactionSpentOutputs> {
        private MemorySegment inner;
        private final boolean ownsMemory;

        BlockSpentOutputs(MemorySegment inner) {
            if (inner == MemorySegment.NULL) {
                throw new IllegalArgumentException("BlockSpentOutputs cannot be null");
            }
            this.inner = inner;
            this.ownsMemory = true;
        }

        private BlockSpentOutputs(MemorySegment inner, boolean ownsMemory) {
            this.inner = inner;
            this.ownsMemory = ownsMemory;
        }

        public long count() {
            checkClosed();
            return btck_block_spent_outputs_count(inner);
        }

        public TransactionSpentOutputs getTransactionSpentOutputs(long index) {
            checkClosed();
            if (index < 0 || index >= count()) {
                throw new IndexOutOfBoundsException("Transaction Spend Outputs index out of bounds: " + index);
            }
            MemorySegment txSpentOutputsPtr = btck_block_spent_outputs_get_transaction_spent_outputs_at(inner, index);
            return new TransactionSpentOutputs(txSpentOutputsPtr);
        }

        public BlockSpentOutputs copy() {
            checkClosed();
            MemorySegment copied = btck_block_spent_outputs_copy(inner);
            if (copied == MemorySegment.NULL) {
                throw new RuntimeException("Failed to copy block spent outputs");
            }
            return new BlockSpentOutputs(copied, true);
        }

        @Override
        public Iterator<TransactionSpentOutputs> iterator() {
            return new Iterator<>() {
                private long currentIndex = 0;
                private final long size = count();

                @Override
                public boolean hasNext() {
                    return currentIndex < size;
                }

                @Override
                public TransactionSpentOutputs next() {
                    if (!hasNext()) {
                        throw new NoSuchElementException();
                    }
                    return getTransactionSpentOutputs(currentIndex++);
                }
            };
        }

        MemorySegment getInner() {
            return inner;
        }

        private void checkClosed() {
            if (inner == MemorySegment.NULL) {
                throw new IllegalStateException("BlockSpentOutputs has been closed");
            }
        }

        @Override
        public void close() {
            if (inner != MemorySegment.NULL && ownsMemory) {
                btck_block_spent_outputs_destroy(inner);
                inner = MemorySegment.NULL;
            }
        }
    }
}

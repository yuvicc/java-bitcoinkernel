package org.bitcoinkernel;

import java.lang.foreign.*;

import static org.bitcoinkernel.BitcoinKernelBindings.*;

public class Blocks {

    //todo!
    public static class BlockValidationState {

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

    //todo!
    public static class BlockTreeEntry {

    }

    //todo!
    public static class Block implements AutoCloseable {


        @Override
        public void close() throws Exception {

        }
    }

    //todo!
    public static class BlockSpentOutputs implements AutoCloseable {


        @Override
        public void close() throws Exception {

        }
    }
}

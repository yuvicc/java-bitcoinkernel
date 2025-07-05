package org.bitcoinkernel;

import java.lang.foreign.*;
import java.lang.management.MemoryManagerMXBean;
import java.util.Arrays;

import jdk.jfr.MemoryAddress;
import org.bitcoinkernel.KernelTypes;
import static org.bitcoinkernel.BitcoinKernelBindings.*;


// Data structures for Bitcoin Kernel functions
public class KernelData {

    // Script pubkey
    public static class ScriptPubkey implements AutoCloseable {
        private final MemorySegment inner;

        public ScriptPubkey(byte[] rawScriptPubkey) throws KernelTypes.KernelException {
            try (var arena = Arena.ofConfined()) {
                MemorySegment raw = arena.allocateFrom(ValueLayout.JAVA_BYTE, rawScriptPubkey);
                this.inner = kernel_script_pubkey_create(raw, raw.byteSize());
                if (inner == MemorySegment.NULL) {
                    throw new KernelTypes.KernelException("Failed to create Script Pubkey");
                }
            }
        }

        public ScriptPubkey(MemorySegment inner) {
            this.inner = inner;
        }

        public byte[] get()  throws KernelTypes.KernelException {
            try (var arena = Arena.ofConfined()) {
                MemorySegment data = kernel_copy_script_pubkey_data(inner);
                if (data == MemorySegment.NULL) {
                    throw new KernelTypes.KernelException("Failed to copy script pubkey data");
                }
                long size = data.byteSize();
                if (size < 0 || size > Integer.MAX_VALUE) {
                    kernel_byte_array_destroy(data);
                    throw new KernelTypes.KernelException("Invalid script pubkey size: " + size);
                }
                byte[] result = new byte[(int) size];
                MemorySegment.copy(data, 0, MemorySegment.ofArray(result), 0, size);
                kernel_byte_array_destroy(data);
                return result;
            }
        }

        MemorySegment getInner() {
            return inner;
        }

        @Override
        public void close() {
            kernel_script_pubkey_destroy(inner);
        }
    }

    // Transaction Output
    public static class TxOut implements AutoCloseable {
        private final MemorySegment inner;

        public  TxOut(ScriptPubkey scriptPubkey, long amount) {
            this.inner = kernel_transaction_output_create(scriptPubkey.getInner(), amount);
        }

        public TxOut(MemorySegment inner) {
            this.inner = inner;
        }

        public long getValue() {
            return kernel_get_transaction_output_amount(inner);
        }

        public ScriptPubkey getScriptPubkey() {
            return new ScriptPubkey(kernel_copy_script_pubkey_from_output(inner));
        }

        MemorySegment getInner() {
            return inner;
        }

        public void close() {
            kernel_transaction_output_destroy(inner);
        }
    }

    // Transaction
    public static class Transaction implements AutoCloseable {
        private final MemorySegment inner;

        public Transaction(byte[] rawTransaction) throws KernelTypes.KernelException {
            try (var arena = Arena.ofConfined()) {
                MemorySegment raw = arena.allocateFrom(ValueLayout.JAVA_BYTE, rawTransaction);
                this.inner = kernel_transaction_create(raw, rawTransaction.length);

                if (inner == MemorySegment.NULL) {
                    throw new KernelTypes.KernelException("Failed to decode raw transaction");
                }
            }
        }

        MemorySegment getInner() {
            return inner;
        }

        @Override
        public void close() {
            kernel_transaction_destroy(inner);
        }
    }

    // Unowned Block
    public static class UnknownedBlock {
        private final MemorySegment inner;

        public UnknownedBlock(MemorySegment inner) {
            this.inner = inner;
        }

        public BlockHash getHash() {
            try (var arena = Arena.ofConfined()) {
                MemorySegment hashPtr = kernel_block_pointer_get_hash(inner);
                byte[] hash = new byte[32];
                MemorySegment.copy(hashPtr, 0, MemorySegment.ofArray(hash), 0, 32);
                kernel_block_hash_destroy(hashPtr);
                return new BlockHash(hash);
            }
        }

        public byte[] getdata() throws KernelTypes.KernelException {
            try (var arena = Arena.ofConfined()) {
                MemorySegment data = kernel_copy_block_pointer_data(inner);
                if (data == MemorySegment.NULL) {
                    throw new KernelTypes.KernelException("Failed to copy Block pointer data");
                }
                long size = data.byteSize();
                if (size < 0 || size > Integer.MAX_VALUE) {
                    kernel_byte_array_destroy(data);
                    throw new KernelTypes.KernelException("Invalid Block pointer data size: " + size);
                }
                byte[] result = new byte[32];
                MemorySegment.copy(data, 0, MemorySegment.ofArray(result), 0, size);
                kernel_byte_array_destroy(data);
                return result;
            }
        }
    }

    // Block
    public static class Block implements AutoCloseable {
        private final MemorySegment inner;

        public Block(byte[] rawBlock) throws KernelTypes.KernelException {
            try (var arena = Arena.ofConfined()) {
                MemorySegment raw = arena.allocateFrom(ValueLayout.JAVA_BYTE, rawBlock);
                this.inner = kernel_block_create(raw, rawBlock.length);
                if (inner == MemorySegment.NULL) {
                    throw new KernelTypes.KernelException("Failed to deserialize block");
                }
            }
        }

        public Block(MemorySegment inner) {
            this.inner = inner;
        }

        public BlockHash getHash() {
            try (var arena  = Arena.ofConfined()) {
                MemorySegment hashPtr = kernel_block_get_hash(inner);
                byte[] hash = new byte[32];
                MemorySegment.copy(hashPtr, 0, MemorySegment.ofArray(hash), 0, 32);
                kernel_block_hash_destroy(hashPtr);
                return new BlockHash(hash);
            }
        }

        public byte[] getdata() throws KernelTypes.KernelException {
            try (var arena = Arena.ofConfined()) {
                MemorySegment data = kernel_copy_block_data(inner);
                if (data == MemorySegment.NULL) {
                    throw new KernelTypes.KernelException("Failed to copy block data");
                }
                long size = data.byteSize();
                if (size < 0 || size > Integer.MAX_VALUE) {
                    kernel_byte_array_destroy(data);
                    throw new KernelTypes.KernelException("Invalid Block Data Size: " + size);
                }
                byte[] result = new byte[(int) size];
                MemorySegment.copy(data, 0, MemorySegment.ofArray(result), 0, size);
                kernel_byte_array_destroy(data);
                return result;
            }
        }

        MemorySegment getInner() {
            return inner;
        }

        @Override
        public void close() {
            kernel_block_destroy(inner);
        }
    }

    // BlockHash
    public static class BlockHash {
        public final byte[] hash;

        public BlockHash(byte[] hash) {
            if (hash.length != 32) {
                throw new IllegalArgumentException("Block hash must be 32 bytes");
            }
            this.hash = hash.clone();
        }

        public byte[] getHash() {
            return hash.clone();
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            BlockHash blockHash = (BlockHash) o;
            return Arrays.equals(hash, blockHash.hash);
        }

        @Override
        public int hashCode() {
            return Arrays.hashCode(hash);
        }
    }


    // BlockIndex
    public static class BlockIndex implements AutoCloseable {
        private final MemorySegment inner;

        public BlockIndex(MemorySegment inner) {
            this.inner = inner;
        }

        public BlockIndex prev() throws KernelTypes.KernelException {
            MemorySegment prev = kernel_get_previous_block_index(inner);
            if (prev == MemorySegment.NULL) {
                throw new KernelTypes.KernelException("No previous block index found");
            }
            kernel_block_index_destroy(inner);
            return new BlockIndex(prev);
        }

        public int height() {
            return kernel_block_index_get_height(inner);
        }

        public BlockHash blockHash() {
            try (var arena = Arena.ofConfined()) {
                MemorySegment hashPtr = kernel_block_index_get_block_hash(inner);
                byte[] hash = new byte[32];
                MemorySegment.copy(hashPtr, 0, MemorySegment.ofArray(hash), 0, 32);
                kernel_block_hash_destroy(hashPtr);
                return new BlockHash(hash);
            }
        }

        @Override
        public void close() {
            kernel_block_index_destroy(inner);
        }
    }

    // BlockUndo
    public static class BlockUndo implements AutoCloseable {
        private final MemorySegment inner;
        private final int nTxUndo;

        public BlockUndo(MemorySegment inner, int nTxUndo) {
            this.inner = inner;
            this.nTxUndo = nTxUndo;
        }

        public long getTransactionUndoSize(long transactionIndex) {
            return kernel_get_transaction_undo_size(inner, transactionIndex);
        }

        public TxOut getPrevoutByIndex(long transactionIndex, long prevoutIndex) throws KernelTypes.KernelException {
            MemorySegment prevOut = kernel_get_undo_output_by_index(inner, transactionIndex, prevoutIndex);
            if (prevOut == MemorySegment.NULL) {
                throw new KernelTypes.KernelException("Out of Bounds");
            }
            return new TxOut(prevOut);
        }

        @Override
        public void close() {
            kernel_block_undo_destroy(inner);
        }
    }
}

package org.bitcoinkernel;

import java.lang.foreign.*;
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

    // Unowned Block - todo




    // Block - todo



    // BlockHash - todo



    // BlockIndex - todo




    // BlockUndo - todo
    
}

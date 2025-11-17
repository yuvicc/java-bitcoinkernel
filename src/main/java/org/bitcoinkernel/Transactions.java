package org.bitcoinkernel;

import java.lang.foreign.*;
import java.util.Iterator;
import java.util.NoSuchElementException;

import static org.bitcoinkernel.jextract.bitcoinkernel_h.*;

public class Transactions {
    // ===== Transaction =====
    public static class Transaction {
        private final MemorySegment inner;

        Transaction(MemorySegment inner) {
            if (inner == MemorySegment.NULL) {
                throw new IllegalStateException("Transaction Object cannot be null")
            }
            this.inner = inner;
        }

        public long countInputs() {
            return btck_transaction_count_inputs(inner);
        }

        public long countOutputs() {
            return btck_transaction_count_outputs(inner);
        }

        public TransactionInput getInput(long index) {
            if (index < 0 || index >= countInputs()) {
                throw new IndexOutOfBoundsException("Index out of bounds: " + index);
            }
            MemorySegment inputStr = btck_transaction_get_input_at(inner, index);
            return new TransactionInput(inputStr);
        }

        public TransactionOutput getOutput(long index) {
            if (index < 0 || index >= countOutputs()) {
                throw new IndexOutOfBoundsException("Output index out of bounds: " + index);
            }
            MemorySegment outputPtr = btck_transaction_get_output_at(inner, index);
            return new TransactionOutput(outputPtr);
        }

        public Txid getTxid() {
            MemorySegment txidPtr = btck_transaction_get_txid(inner);
            return new Txid(txidPtr);
        }

        MemorySegment getInner() {
            return inner;
        }
    }

    // ===== Transaction Input =====
    public static class TransactionInput {
        private final MemorySegment inner;

        TransactionInput(MemorySegment inner) {
            if (inner == MemorySegment.NULL) {
                throw new IllegalStateException("Transasction Input object cannot be null");
            }
            this.inner = inner;
        }

        public TransactionOutPoint getOutPoint() {
            MemorySegment outPointPtr = btck_transaction_input_get_out_point(inner);
            return new TransactionOutPoint(outPointPtr);
        }

        MemorySegment getInner() {
            return inner;
        }
    }

    // ===== Transaction OutPoint =====
    public static class TransactionOutPoint {
        private final MemorySegment inner;

        TransactionOutPoint(MemorySegment inner) {
            if (inner == MemorySegment.NULL) {
                throw new IllegalArgumentException("TransactionOutPoint cannot be null");
            }
            this.inner = inner;
        }

        public long getIndex() {
            return Integer.toUnsignedLong(btck_transaction_out_point_get_index(inner));
        }

        public Txid getTxid() {
            MemorySegment txidPtr = btck_transaction_get_txid(inner);
            return new Txid(txidPtr);
        }
    }

    // ===== Transaction Output =====
    public static class TransactionOutput {
        private final MemorySegment inner;

        TransactionOutput(MemorySegment inner) {
            if (inner == MemorySegment.NULL) {
                throw new IllegalArgumentException("TransactionOutput cannot be null");
            }
            this.inner = inner;
        }

        public long getAmount() {
            return btck_transaction_output_get_amount(inner);
        }

        public ScriptPubkey getScriptPubKey() {
            MemorySegment scriptPtr = btck_transaction_output_get_script_pubkey(inner);
            return new ScriptPubkey(scriptPtr);
        }

        MemorySegment getInner() {
            return inner;
        }
    }

    // ===== Coin =====
    public static class Coin implements AutoCloseable {
        private MemorySegment inner;
        private final boolean ownsMemory;

        Coin(MemorySegment inner) {
            if (inner == MemorySegment.NULL) {
                throw new IllegalArgumentException("Coin cannot be null");
            }
            this.inner = inner;
            this.ownsMemory = false;
        }

        private Coin(MemorySegment inner, boolean ownsMemory) {
            this.inner = inner;
            this.ownsMemory = ownsMemory;
        }

        public long getConfirmationHeight() {
            checkClosed();
            return Integer.toUnsignedLong(btck_coin_confirmation_height(inner));
        }

        public boolean isCoinbase() {
            checkClosed();
            return btck_coin_is_coinbase(inner) != 0;
        }

        public TransactionOutput getOutput() {
            checkClosed();
            MemorySegment outputPtr = btck_coin_get_output(inner);
            return new TransactionOutput(outputPtr);
        }

        public Coin copy() {
            checkClosed();
            MemorySegment copied = btck_coin_copy(inner);
            if (copied == MemorySegment.NULL) {
                throw new RuntimeException("Failed to copy coin");
            }
            return new Coin(copied, true);
        }

        private void checkClosed() {
            if (inner == MemorySegment.NULL) {
                throw new IllegalStateException("Coin object has been closed");
            }
        }

        MemorySegment getInner() {
            return inner;
        }

        @Override
        public void close() throws Exception {
            if (inner != MemorySegment.NULL && ownsMemory) {
                btck_coin_destroy(inner);
                inner = MemorySegment.NULL;
            }
        }
    }

    // ===== Txid =====
    public static class Txid {
        private final MemorySegment inner;

        Txid(MemorySegment inner) {
            if (inner == MemorySegment.NULL) {
                throw new IllegalArgumentException("Txid cannot be null");
            }
            this.inner = inner;
        }

        public byte[] toBytes() {
            try (var arena = Arena.ofConfined()) {
                MemorySegment output = arena.allocate(32);
                btck_txid_to_bytes(inner, output);
                return output.toArray(ValueLayout.JAVA_BYTE);
            }
        }

        public boolean equals(Txid other) {
            return btck_txid_equals(inner, other.getInner()) != 0;
        }

        @Override
        public int hashCode() {
            byte[] bytes = toBytes();
            int result = 1;
            for (byte b : bytes) {
                result = 31 * result + b;
            }
            return result;
        }

        MemorySegment getInner() {
            return inner;
        }
    }

    // ===== Script Pubkey =====
    public static class ScriptPubkey {
        private final MemorySegment inner;

        ScriptPubkey(MemorySegment inner) {
            if (inner == MemorySegment.NULL) {
                throw new IllegalArgumentException("ScriptPubKey cannot be null");
            }
            this.inner = inner;
        }

        MemorySegment getInner() {
            return inner;
        }
    }

    // ===== Transaction Spent Outputs
    public static class TransactionSpentOutputs implements Iterable<Coin> {
        private final MemorySegment inner;
        private final boolean ownsMemory;

        TransactionSpentOutputs(MemorySegment inner) {
            if (inner == MemorySegment.NULL) {
                throw new IllegalArgumentException("Transaction Outputs cannot be null");
            }
            this.inner = inner;
            this.ownsMemory = false;
        }

        private TransactionSpentOutputs(MemorySegment inner, boolean ownsMemory) {
            this.inner = inner;
            this.ownsMemory = ownsMemory;
        }

        public long count() {
            return btck_transaction_spent_outputs_count(inner);
        }

        public Coin getCoin(long index) {
            if (index < 0 || index >= count()) {
                throw new IndexOutOfBoundsException("Coin index out of bounds: " + index);
            }
            MemorySegment coinPtr = btck_transaction_spent_outputs_get_coin_at(inner, index);
            return new Coin(coinPtr);
        }

        public TransactionSpentOutputs copy() {
            MemorySegment copied = btck_transaction_spent_outputs_copy(inner);
            if (copied == MemorySegment.NULL) {
                throw new RuntimeException("Failed to copy coin object");
            }
            return new TransactionSpentOutputs(inner, true);
        }

        @Override
        public Iterator<Coin> iterator() {
            return new Iterator<>() {
                private long currentIndex = 0;
                private final long size = count();

                @Override
                public boolean hasNext() {
                    return currentIndex < size;
                }

                @Override
                public Coin next() {
                    if (!hasNext()) {
                        throw new NoSuchElementException();
                    }
                    return getCoin(currentIndex++);
                }
            };
        }

        MemorySegment getInner() {
            return inner;
        }

        public void close() {
            if (inner != MemorySegment.NULL) {
                btck_transaction_spent_outputs_destroy(inner);
            }
        }
    }
}

package org.bitcoinkernel;

import java.lang.foreign.*;
import java.util.Arrays;

import org.bitcoinkernel.KernelTypes;
import static org.bitcoinkernel.jextract.bitcoinkernel_h.*;
import static org.bitcoinkernel.Transactions.*;


// Data structures for Bitcoin Kernel functions
public class KernelData {

    // ===== ScriptPubkey =====
    public static class ScriptPubkey implements AutoCloseable {
        private MemorySegment inner;
        private final Arena arena;
        private final boolean ownsMemory;

        public ScriptPubkey(byte[] scriptPubkey) throws KernelTypes.KernelException {
            this.arena = Arena.ofConfined();
            MemorySegment scriptSegment = arena.allocateFrom(ValueLayout.JAVA_BYTE, scriptPubkey);
            this.inner = btck_script_pubkey_create(scriptSegment, scriptPubkey.length);
            if (inner == MemorySegment.NULL) {
                arena.close();
                throw new KernelTypes.KernelException("Failed to create ScriptPubkey");
            }
            this.ownsMemory = true;
        }

        ScriptPubkey(MemorySegment inner) {
            if (inner == MemorySegment.NULL) {
                throw new IllegalArgumentException("ScriptPubkey cannot be null");
            }
            this.inner = inner;
            this.arena = null;
            this.ownsMemory = false;
        }

        private ScriptPubkey(MemorySegment inner, boolean ownsMemory) {
            this.inner = inner;
            this.arena = null;
            this.ownsMemory = ownsMemory;
        }

        public int verify(long amount, Transaction txTo, TransactionOutput[] spentOutputs,
                         int inputIndex, int flags) throws KernelTypes.KernelException {
            checkClosed();
            txTo.checkClosed();

            // Bounds check input_index to prevent assertion failure in native code
            if (inputIndex < 0 || inputIndex >= txTo.countInputs()) {
                throw new IndexOutOfBoundsException(
                    "Input index " + inputIndex + " is out of bounds for transaction with " +
                    txTo.countInputs() + " inputs");
            }

            try (var arena = Arena.ofConfined()) {
                // Prepare spent outputs array
                int numOutputs = (spentOutputs != null) ? spentOutputs.length : 0;
                MemorySegment outputPtrs;

                if (numOutputs > 0) {
                    // Allocate array of output pointers
                    outputPtrs = arena.allocate(ValueLayout.ADDRESS, numOutputs);
                    for (int i = 0; i < spentOutputs.length; i++) {
                        outputPtrs.setAtIndex(ValueLayout.ADDRESS, i, spentOutputs[i].getInner());
                    }
                } else {
                    // Pass NULL for empty/null spent outputs
                    outputPtrs = MemorySegment.NULL;
                }

                MemorySegment statusPtr = arena.allocate(ValueLayout.JAVA_BYTE);

                int result = btck_script_pubkey_verify(
                    inner,
                    amount,
                    txTo.getInner(),
                    outputPtrs,
                    numOutputs,
                    inputIndex,
                    flags,
                    statusPtr
                );

                // Note: return value 1 = success, 0 = error
                if (result == 0) {
                    byte status = statusPtr.get(ValueLayout.JAVA_BYTE, 0);
                    throw new KernelTypes.KernelException(
                        KernelTypes.KernelException.ScriptVerifyError.fromNative(status)
                    );
                }

                return result;
            }
        }

        public byte[] toBytes() {
            checkClosed();

            try (var arena = Arena.ofConfined()) {
                // Create a container to collect bytes
                java.io.ByteArrayOutputStream baos = new java.io.ByteArrayOutputStream();

                // Create the writer callback
                org.bitcoinkernel.jextract.btck_WriteBytes.Function writer = (bytes, size, userData) -> {
                    try {
                        byte[] data = bytes.reinterpret(size).toArray(ValueLayout.JAVA_BYTE);
                        baos.write(data);
                        return 0; // success
                    } catch (Exception e) {
                        return 1; // error
                    }
                };

                MemorySegment writerSegment = org.bitcoinkernel.jextract.btck_WriteBytes.allocate(writer, arena);

                int result = btck_script_pubkey_to_bytes(inner, writerSegment, MemorySegment.NULL);
                if (result != 0) {
                    throw new RuntimeException("Failed to serialize ScriptPubkey");
                }

                return baos.toByteArray();
            } catch (Exception e) {
                throw new RuntimeException("Failed to serialize ScriptPubkey", e);
            }
        }

        public ScriptPubkey copy() {
            checkClosed();
            MemorySegment copied = btck_script_pubkey_copy(inner);
            if (copied == MemorySegment.NULL) {
                throw new RuntimeException("Failed to copy ScriptPubkey");
            }
            return new ScriptPubkey(copied, true);
        }

        private void checkClosed() {
            if (inner == MemorySegment.NULL) {
                throw new IllegalStateException("ScriptPubkey has been closed");
            }
        }

        MemorySegment getInner() {
            return inner;
        }

        @Override
        public void close() {
            if (inner != MemorySegment.NULL && ownsMemory) {
                btck_script_pubkey_destroy(inner);
                inner = MemorySegment.NULL;
            }
            if (arena != null) {
                arena.close();
            }
        }
    }

    // ===== Script Verification Status =====
    public static class ScriptVerifyStatus {
        private final MemorySegment inner;

        ScriptVerifyStatus(MemorySegment inner) {
            if (inner == MemorySegment.NULL) {
                throw new IllegalArgumentException("ScriptVerifyStatus cannot be null");
            }
            this.inner = inner;
        }

        MemorySegment getInner() {
            return inner;
        }
    }
}

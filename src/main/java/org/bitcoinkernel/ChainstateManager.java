package org.bitcoinkernel;

import java.lang.foreign.*;
import java.nio.charset.StandardCharsets;
import org.bitcoinkernel.KernelTypes;
import org.bitcoinkernel.NotificationsManager;
import org.bitcoinkernel.ContextManager;

import static org.bitcoinkernel.BitcoinKernelBindings.*;

// Manage Chainstate Operations
public class ChainstateManager {
    // Chainstate manager options
    public static class ChainstateManagerOptions implements AutoCloseable {
        private final MemorySegment inner;

        public ChainstateManagerOptions(ContextManager.Context context, String dataDir, String blocksDir) throws KernelTypes.KernelException {
            try (var arena = Arena.ofConfined()) {
                MemorySegment cDataDir = arena.allocateFrom(ValueLayout.JAVA_BYTE, dataDir.getBytes(StandardCharsets.UTF_8));
                this.inner = kernel_chainstate_manager_options_create(
                        context.getInner(),
                        cDataDir
                );
                // todo: add check here for chainstate manager options create
//                if (inner = MemorySegment.NULL) {
//                    throw new KernelTypes.KernelException("Failed to create chainstate manager options");
//                }
            }
        }

//        public void setWorkerThreads(int workerThreads) {
//            kernel_set
//        }

        MemorySegment getInner() {
            return inner;
        }

        public void close() {
            kernel_chainstate_manager_options_destroy(inner);
        }
    }




}

















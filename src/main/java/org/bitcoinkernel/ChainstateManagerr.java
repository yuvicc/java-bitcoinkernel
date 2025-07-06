package org.bitcoinkernel;

import java.lang.foreign.*;
import java.nio.charset.StandardCharsets;

import static org.bitcoinkernel.BitcoinKernelBindings.*;

// Manage Chainstate Operations, to avoid duplicate declaration of chainstate manager, I've kept extra r in root class as a workaround.
public class ChainstateManagerr {
    // Chainstate manager options
    public static class ChainstateManagerOptions implements AutoCloseable {
        private final MemorySegment inner;

        public ChainstateManagerOptions(ContextManager.Context context, String dataDir, String blocksDir) throws KernelTypes.KernelException {
            try (var arena = Arena.ofConfined()) {
                MemorySegment cDataDir = arena.allocateFrom(ValueLayout.JAVA_BYTE, dataDir.getBytes(StandardCharsets.UTF_8));
                MemorySegment cBlocksDir = arena.allocateFrom(ValueLayout.JAVA_BYTE, blocksDir.getBytes(StandardCharsets.UTF_8));
                this.inner = kernel_chainstate_manager_options_create(
                        context.getInner(),
                        cDataDir,
                        cDataDir.byteSize(),
                        cBlocksDir,
                        cBlocksDir.byteSize()
                );
                // todo: add check here for chainstate manager options create
//                if (inner = MemorySegment.NULL) {
//                    throw new KernelTypes.KernelException("Failed to create chainstate manager options");
//                }
            }
        }

        public void setWorkerThreads(int workerThreads) {
            kernel_chainstate_manager_options_set_worker_threads_num(inner, workerThreads);
        }

        public ChainstateManagerOptions setWipeDb(boolean wipeBlockTree, boolean wipeChainstate) {
            kernel_chainstate_manager_options_set_wipe_dbs(inner, wipeBlockTree, wipeChainstate);
            return this;
        }

        public ChainstateManagerOptions setBlockTreeDbInMemory(boolean inMemory) {
            kernel_chainstate_manager_options_set_block_tree_db_in_memory(inner, inMemory);
            return this;
        }

        public ChainstateManagerOptions setChainstateMemoryInDb(boolean inMemory) {
            kernel_chainstate_manager_options_set_chainstate_db_in_memory(inner, inMemory);
            return this;
        }

        MemorySegment getInner() {
            return inner;
        }

        public void close() {
            kernel_chainstate_manager_options_destroy(inner);
        }
    }

    // Chainstate Manager
    public static class ChainstateManager implements AutoCloseable {
        private final MemorySegment inner;
        private final ContextManager.Context context;

        public ChainstateManager(ChainstateManagerOptions options, ContextManager.Context context) throws KernelTypes.KernelException {
            this.inner = kernel_chainstate_manager_create(context.getInner(), options.getInner());
            if (inner == MemorySegment.NULL) {
                throw new KernelTypes.KernelException("Failed to create chainstate manager");
            }
            this.context = context;
        }

        public boolean[] processBlock(KernelData.Block block) {
            try (var arena = Arena.ofConfined()) {
                MemorySegment newBlock = arena.allocate(ValueLayout.JAVA_BOOLEAN);
                boolean accepted = kernel_chainstate_manager_process_block(
                        context.getInner(),
                        inner,
                        block.getInner(),
                        newBlock
                );
                return new boolean[]{accepted, newBlock.get(ValueLayout.JAVA_BOOLEAN, 0)};
            }
        }

        public void importBlocks() throws KernelTypes.KernelException {
            boolean success = kernel_import_blocks(context.getInner(), inner, MemorySegment.NULL, MemorySegment.NULL, 0);
            if (!success) {
                throw new KernelTypes.KernelException("Failed to import blocks");
            }
        }

        public KernelData.BlockIndex getBlockIndexGenesis() {
            return new KernelData.BlockIndex(kernel_get_block_index_from_genesis(context.getInner(), inner));
        }

        public KernelData.BlockIndex getBlockIndexByHeight(int blockHeight) throws KernelTypes.KernelException {
            MemorySegment index = kernel_get_block_index_from_height(context.getInner(), inner, blockHeight);
            if (index == MemorySegment.NULL) {
                throw new KernelTypes.KernelException("Block index not found for height " + blockHeight);
            }
            return new KernelData.BlockIndex(index);
        }

        public KernelData.BlockIndex getBlockIndexByHash(KernelData.BlockHash hash) throws KernelTypes.KernelException {
            try (var arena = Arena.ofConfined()) {
                MemorySegment blockHash = arena.allocateFrom(ValueLayout.JAVA_BYTE, hash.getHash());
                MemorySegment index = kernel_get_block_index_from_hash(context.getInner(), inner, blockHash);
                if (index == MemorySegment.NULL) {
                    throw new KernelTypes.KernelException("Block index not found for hash " + blockHash);
                }
                return new KernelData.BlockIndex(index);
            }
        }

        public KernelData.BlockIndex getNextBlockIndex(KernelData.BlockIndex blockIndex) throws KernelTypes.KernelException {
            MemorySegment next = kernel_get_next_block_index(context.getInner(), inner, blockIndex.getInner());
            if (next == MemorySegment.NULL) {
                throw new KernelTypes.KernelException("No next block index");
            }
            return new KernelData.BlockIndex(next);
        }

        public KernelData.Block readBlockData (KernelData.BlockIndex blockIndex) throws KernelTypes.KernelException {
            MemorySegment block = kernel_read_block_from_disk(context.getInner(), inner, blockIndex.getInner());
            if (block == MemorySegment.NULL) {
                throw new KernelTypes.KernelException("Failed to read block");
            }
            return new KernelData.Block(block);
        }

        public KernelData.BlockUndo readUndoData (KernelData.BlockIndex blockIndex) throws KernelTypes.KernelException {
            MemorySegment undo = kernel_read_block_undo_from_disk(context.getInner(), inner, blockIndex.getInner());
            if (undo == MemorySegment.NULL) {
                throw new KernelTypes.KernelException("Failed to read undo data");
            }
            long nTxUndo = kernel_block_undo_size(undo);
            return new KernelData.BlockUndo(undo, nTxUndo);
        }

        @Override
        public void close() {
            kernel_chainstate_manager_destroy(inner, context.getInner());
        }
    }
}

















package org.bitcoinkernel;

import com.sun.source.tree.BlockTree;

import java.lang.foreign.*;
import java.nio.charset.StandardCharsets;
import java.util.Iterator;
import java.util.NoSuchElementException;

import static org.bitcoinkernel.jextract.bitcoinkernel_h.*;
import static org.bitcoinkernel.Blocks.*;
import static org.bitcoinkernel.ContextManager.*;


public class Chainstate {

    public enum ChainType {
        MAINNET(0),
        TESTNET(1),
        TESTNET_4(2),
        SIGNET(3),
        REGTEST(4);

        private final byte value;

        ChainType(int value) {
            this.value = (byte) value;
        }

        public byte getValue() {
            return value;
        }
    }

    public enum SynchronizationState {
        INIT_REINDEX(0),
        INIT_DOWNLOAD(1),
        POST_INIT(2);

        private final byte value;

        SynchronizationState(int value){
            this.value = (byte) value;
        }

        public int getValue(){
            return value;
        }

        public static SynchronizationState fromByte (byte value) {
            for (SynchronizationState state: values()) {
                if (state.value == value) {
                    return state;
                }
            }
            throw new IllegalArgumentException("Unknown synchronization state: " + value);
        }
    }

    public enum Warning {
        UNKNOWN_RULES_ACTIVATED(0),
        LARGE_WORK_INVALID_CHAIN(1);

        private final byte value;

        Warning(int value) {
            this.value = (byte) value;
        }

        public static Warning fromByte(byte value) {
            for (Warning warning: values()) {
                if (warning.value == value) {
                    return warning;
                }
            }
            throw new IllegalArgumentException("Invalid Warning: " + value);
        }
    }

    // ===== Chain Parameters =====
    public static class ChainParameters implements AutoCloseable {
        private MemorySegment inner;

        public ChainParameters(ChainType chainType) throws KernelTypes.KernelException {
            this.inner = btck_chain_parameters_create(chainType.getValue());
            if (inner == MemorySegment.NULL) {
                throw new KernelTypes.KernelException("Failed to create chain parameters object");
            }
        }

        MemorySegment getInner() {
            return inner;
        }

        @Override
        public void close() {
            if (inner != MemorySegment.NULL) {
                btck_chain_parameters_destroy(inner);
                inner = MemorySegment.NULL;
            }
        }
    }

    // ===== Chainstate Manager Options =====
    public static class ChainstateManagerOptions implements AutoCloseable {
        private MemorySegment inner;
        private final Arena arena;

        public ChainstateManagerOptions(Context context, String dataDir, String blocksDir) throws KernelTypes.KernelException {
            this.arena = Arena.ofConfined();

            MemorySegment cDataDir = arena.allocateFrom(ValueLayout.JAVA_BYTE, dataDir.getBytes(StandardCharsets.UTF_8));
            MemorySegment cBlocksDir = arena.allocateFrom(ValueLayout.JAVA_BYTE, blocksDir.getBytes(StandardCharsets.UTF_8));

            this.inner = btck_chainstate_manager_options_create(
                    context.getInner(),
                    cDataDir, cDataDir.byteSize(),
                    cBlocksDir, cBlocksDir.byteSize());
            if (inner == MemorySegment.NULL) {
                arena.close();
                throw new KernelTypes.KernelException("Failed to create chainstate manager options");
            }
        }

        public void setWorkerThreads(int workerThreads) {
            checkClosed();
            btck_chainstate_manager_options_set_worker_threads_num(inner, workerThreads);
        }

        public boolean setWipeDbs(boolean wipeBlockTree, boolean wipeChainstate) {
            checkClosed();
            return btck_chainstate_manager_options_set_wipe_dbs(
                    inner,
                    wipeBlockTree ? 1 : 0,
                    wipeChainstate ? 1 : 0
            ) == 0;
        }

        public void updateBlockTreeDbInMemory(boolean inMemory) {
            checkClosed();
            btck_chainstate_manager_options_update_block_tree_db_in_memory(
                    inner,
                    inMemory ? 1 : 0
            );
        }

        public void updateChainstateDbInMemory(boolean inMemory) {
            checkClosed();
            btck_chainstate_manager_options_update_chainstate_db_in_memory(
                    inner,
                    inMemory ? 1 : 0
            );
        }

        MemorySegment getInner() {
            return inner;
        }

        private void checkClosed() {
            if (inner == MemorySegment.NULL) {
                throw new IllegalStateException("Chainstate Manager Options object has been closed");
            }
        }

        @Override
        public void close() {
            if (inner != MemorySegment.NULL) {
                btck_chainstate_manager_options_destroy(inner);
                inner = MemorySegment.NULL;
            }

            if (arena != null) {
                arena.close();
            }
        }
    }

    public static class ChainstateManager implements AutoCloseable {
        private MemorySegment inner;

        public ChainstateManager(Context context, ChainstateManagerOptions options) throws KernelTypes.KernelException {
            this.inner = btck_chainstate_manager_create(options.getInner());
            if (inner == MemorySegment.NULL) {
                throw new KernelTypes.KernelException("Failed to allocate Chainstate Manager object");
            }
        }

        public boolean ImportBlocks(String[] paths) {
            checkClosed();
            if (paths == null || paths.length == 0) {
                return btck_chainstate_manager_import_blocks(
                        inner,
                        MemorySegment.NULL,
                        MemorySegment.NULL,
                        0
                ) == 0;
            }

            try (var arena = Arena.ofConfined()) {
                MemorySegment pathPtrs = arena.allocate(
                        ValueLayout.ADDRESS,
                        paths.length
                );
                MemorySegment pathLens = arena.allocate(
                        ValueLayout.JAVA_LONG,
                        paths.length
                );

                // Convert each path to C String
                for (int i = 0; i < paths.length; ++i) {
                    byte[] pathBytes = paths[i].getBytes(StandardCharsets.UTF_8);
                    MemorySegment pathSegment = arena.allocateFrom(ValueLayout.JAVA_BYTE, pathBytes);
                    pathPtrs.setAtIndex(ValueLayout.ADDRESS, i, pathSegment);
                    pathLens.setAtIndex(ValueLayout.JAVA_LONG, i, pathBytes.length);
                }

                return btck_chainstate_manager_import_blocks(inner, pathPtrs, pathLens, paths.length) == 0;
            }
        }

        public boolean ProcessBlock(Block block, boolean[] newBlock) {
            checkClosed();
            block.checkClosed();

            try (var arena = Arena.ofConfined()) {
                MemorySegment newBlockPtr = arena.allocate(ValueLayout.JAVA_INT);
                int result = btck_chainstate_manager_process_block(inner, block.getInner(), newBlockPtr);

                if (newBlock != null && newBlock.length > 0) {
                    newBlock[0] = newBlockPtr.get(ValueLayout.JAVA_INT, 0) != 0;
                }

                return result == 0;
            }
        }

        public Chain getChain() {
            checkClosed();
            MemorySegment chainPtr = btck_chainstate_manager_get_active_chain(inner);
            return new Chain(chainPtr);
        }

        public BlockTreeEntry getBlockTreeEntry(BlockHash blockHash) {
            checkClosed();
            blockHash.checkClosed();
            MemorySegment entry = btck_chainstate_manager_get_block_tree_entry_by_hash(inner, blockHash.getInner());
            if (entry == null) {
                return null;
            }
            return new BlockTreeEntry(entry);
        }

        public Block readBlock(BlockTreeEntry entry) {
            checkClosed();
            MemorySegment blockPtr = btck_block_read(inner, entry.getInner());
            if (blockPtr == MemorySegment.NULL) {
                return null;
            }

            return new Block(blockPtr);
        }

        public BlockSpentOutputs readBlockSpentOutputs(BlockTreeEntry entry) {
            checkClosed();
            MemorySegment undoPtr = btck_block_spent_outputs_read(inner, entry.getInner());
            if (undoPtr == MemorySegment.NULL) {
                return null;
            }
            return new BlockSpentOutputs(undoPtr);
        }

        void checkClosed() {
            if (inner == MemorySegment.NULL) {
                throw new IllegalStateException("ChainstateManager has been closed");
            }
        }

        @Override
        public void close() throws Exception {
            if (inner != MemorySegment.NULL) {
                btck_chainstate_manager_destroy(inner);
                inner = MemorySegment.NULL;
            }
        }
    }

    // Chainstate class
    public static class Chain implements Iterable<BlockTreeEntry> {
        private final MemorySegment inner;

        Chain(MemorySegment inner) {
            if (inner == MemorySegment.NULL) {
                throw new IllegalArgumentException("Chain cannot be null");
            }
            this.inner = inner;
        }

        public int getHeight() {
            return btck_chain_get_height(inner);
        }

        public BlockTreeEntry getByHeight(int height) {
            MemorySegment entry = btck_chain_get_by_height(inner, height);
            if (entry == MemorySegment.NULL) {
                throw new IllegalArgumentException("No entry at height: " + height);
            }
            return new BlockTreeEntry(inner);
        }

        public boolean contains(BlockTreeEntry entry) {
            return btck_chain_contains(inner, entry.getInner()) != 0;
        }

        @Override
        public Iterator<BlockTreeEntry> iterator() {
            return new ChainIterator(this);
        }

        MemorySegment getInner() {
            return inner;
        }

        public static class ChainIterator implements Iterator<BlockTreeEntry> {
            private final Chain chain;
            private int currentHeight;
            private final int maxHeight;

            ChainIterator(Chain chain) {
                this.chain = chain;
                this.currentHeight = 0;
                this.maxHeight = chain.getHeight();
            }

            @Override
            public boolean hasNext() {
                return currentHeight <= maxHeight;
            }

            @Override
            public BlockTreeEntry next() {
                if (!hasNext()) {
                    throw new NoSuchElementException();
                }
                return chain.getByHeight(currentHeight++);
            }
        }
    }
}
package org.bitcoinkernel;

import java.lang.foreign.*;

import static org.bitcoinkernel.BitcoinKernelBindings.*;
import static org.bitcoinkernel.Blocks.*;
import static org.bitcoinkernel.ContextManager.*;


public class Chainstate {

    // ===== Chain Parameters =====
    public static class ChainParameters implements AutoCloseable {
        private MemorySegment inner;

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

        // Todo
        public ChainstateManagerOptions() throws KernelTypes.KernelException {

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

    // Chainstate Manager
    public static class ChainstateManager implements AutoCloseable {
        private MemorySegment inner;

        public ChainstateManager(Context context, ChainstateManagerOptions options) throws KernelTypes.KernelException {
            this.inner = btck_chainstate_manager_create(options.getInner());
            if (inner == MemorySegment.NULL) {
                throw new KernelTypes.KernelException("Failed to allocate Chainstate Manager object");
            }
        }

        // todo!
        public boolean ImportBlocks() {

        }

        // todo!
        public boolean ProcessBlock() {

        }

        public Chain getChain() {
            checkClosed();
            MemorySegment chainPtr = btck_chainstate_manager_get_active_chain(inner);
            return new Chain(chainPtr);
        }

        // todo!
        public BlockTreeEntry getBlockTreeEntry(BlockHash blockHash) {

        }

        // todo!
        public Block readBlock(BlockTreeEntry entry) {

        }

        private void checkClosed() {
            if (inner == MemorySegment.NULL) {
                throw new IllegalStateException("Chainstate Manager has been closed");
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

    // Chainstate class - todo!
    public static class Chain implements AutoCloseable {


        @Override
        public void close() throws Exception {

        }
    }
}
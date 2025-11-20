package org.bitcoinkernel;

import org.bitcoinkernel.KernelTypes;
import org.bitcoinkernel.KernelData;

import java.lang.foreign.*;
import java.nio.charset.StandardCharsets;

import org.bitcoinkernel.jextract.btck_NotificationInterfaceCallbacks;
import org.bitcoinkernel.jextract.btck_ValidationInterfaceCallbacks;
import static org.bitcoinkernel.jextract.bitcoinkernel_h.*;
import static org.bitcoinkernel.Blocks.*;
import static org.bitcoinkernel.Chainstate.*;

public class NotificationsManager {

    // ===== Kernel Notification Interface Callbacks =====
    public interface KernelNotificationInterfaceCallbacks {
        void blockTip(SynchronizationState state, BlockTreeEntry blockIndex, double verificationProgress);
        void headerTip(SynchronizationState state, long height, long timestamp, boolean presync);
        void progress(String title, int progressPercent, boolean resumePossible);
        void warningSet(Warning warning, String message);
        void warningUnset(Warning warning);
        void flushError(String message);
        void fatalError(String message);
    }

    // ===== Validation Interface Callbacks =====
    public interface ValidationInterfaceCallbacks {
        void blockChecked(Block block, BlockValidationState state);
        void powValidBlock(Block block, BlockTreeEntry blockIndex);
        void blockConnected(Block block, BlockTreeEntry blockIndex);
        void blockDisconnected(Block block, BlockTreeEntry blockIndex);
    }

    // ===== Kernel Notification Manager =====
    public static class KernelNotificationManager implements AutoCloseable {
        private final MemorySegment callbackStruct;
        private final Arena arena;
        private final KernelNotificationInterfaceCallbacks callbacks;

        public KernelNotificationManager(KernelNotificationInterfaceCallbacks callbacks) {
            this.callbacks = callbacks;
            this.arena = Arena.ofShared();
            this.callbackStruct = btck_NotificationInterfaceCallbacks.allocate(arena);

            // Set user_data to null
            btck_NotificationInterfaceCallbacks.user_data(callbackStruct, MemorySegment.NULL);
            btck_NotificationInterfaceCallbacks.user_data_destroy(callbackStruct, MemorySegment.NULL);

            // Set up block_tip callback
            var blockTipStub = org.bitcoinkernel.jextract.btck_NotifyBlockTip.allocate(
                (userData, state, entry, verificationProgress) -> {
                    SynchronizationState syncState = SynchronizationState.fromByte(state);
                    BlockTreeEntry blockEntry = entry != MemorySegment.NULL ? new BlockTreeEntry(entry) : null;
                    callbacks.blockTip(syncState, blockEntry, verificationProgress);
                }, arena
            );
            btck_NotificationInterfaceCallbacks.block_tip(callbackStruct, blockTipStub);

            // Set up header_tip callback
            var headerTipStub = org.bitcoinkernel.jextract.btck_NotifyHeaderTip.allocate(
                (userData, state, height, timestamp, presync) -> {
                    SynchronizationState syncState = SynchronizationState.fromByte(state);
                    callbacks.headerTip(syncState, height, timestamp, presync != 0);
                }, arena
            );
            btck_NotificationInterfaceCallbacks.header_tip(callbackStruct, headerTipStub);

            // Set up progress callback
            var progressStub = org.bitcoinkernel.jextract.btck_NotifyProgress.allocate(
                (userData, title, titleLen, progressPercent, resumePossible) -> {
                    String titleStr = title.reinterpret(titleLen).getString(0, StandardCharsets.UTF_8);
                    callbacks.progress(titleStr, progressPercent, resumePossible != 0);
                }, arena
            );
            btck_NotificationInterfaceCallbacks.progress(callbackStruct, progressStub);

            // Set up warning_set callback
            var warningSetStub = org.bitcoinkernel.jextract.btck_NotifyWarningSet.allocate(
                (userData, warning, message, messageLen) -> {
                    Warning warn = Warning.fromByte(warning);
                    String messageStr = message.reinterpret(messageLen).getString(0, StandardCharsets.UTF_8);
                    callbacks.warningSet(warn, messageStr);
                }, arena
            );
            btck_NotificationInterfaceCallbacks.warning_set(callbackStruct, warningSetStub);

            // Set up warning_unset callback
            var warningUnsetStub = org.bitcoinkernel.jextract.btck_NotifyWarningUnset.allocate(
                (userData, warning) -> {
                    Warning warn = Warning.fromByte(warning);
                    callbacks.warningUnset(warn);
                }, arena
            );
            btck_NotificationInterfaceCallbacks.warning_unset(callbackStruct, warningUnsetStub);

            // Set up flush_error callback
            var flushErrorStub = org.bitcoinkernel.jextract.btck_NotifyFlushError.allocate(
                (userData, message, messageLen) -> {
                    String messageStr = message.reinterpret(messageLen).getString(0, StandardCharsets.UTF_8);
                    callbacks.flushError(messageStr);
                }, arena
            );
            btck_NotificationInterfaceCallbacks.flush_error(callbackStruct, flushErrorStub);

            // Set up fatal_error callback
            var fatalErrorStub = org.bitcoinkernel.jextract.btck_NotifyFatalError.allocate(
                (userData, message, messageLen) -> {
                    String messageStr = message.reinterpret(messageLen).getString(0, StandardCharsets.UTF_8);
                    callbacks.fatalError(messageStr);
                }, arena
            );
            btck_NotificationInterfaceCallbacks.fatal_error(callbackStruct, fatalErrorStub);
        }

        public MemorySegment getCallbackStruct() {
            return callbackStruct;
        }

        @Override
        public void close() {
            if (arena != null) {
                arena.close();
            }
        }
    }

    // ===== Validation Interface Manager =====
    public static class ValidationInterfaceManager implements AutoCloseable {
        private final MemorySegment callbackStruct;
        private final Arena arena;
        private final ValidationInterfaceCallbacks callbacks;

        public ValidationInterfaceManager(ValidationInterfaceCallbacks callbacks) {
            this.callbacks = callbacks;
            this.arena = Arena.ofShared();
            this.callbackStruct = btck_ValidationInterfaceCallbacks.allocate(arena);

            // Set user_data to null
            btck_ValidationInterfaceCallbacks.user_data(callbackStruct, MemorySegment.NULL);
            btck_ValidationInterfaceCallbacks.user_data_destroy(callbackStruct, MemorySegment.NULL);

            // Set up block_checked callback
            var blockCheckedStub = org.bitcoinkernel.jextract.btck_ValidationInterfaceBlockChecked.allocate(
                (userData, block, state) -> {
                    Block blk = new Block(block);
                    BlockValidationState validationState = new BlockValidationState(state);
                    callbacks.blockChecked(blk, validationState);
                }, arena
            );
            btck_ValidationInterfaceCallbacks.block_checked(callbackStruct, blockCheckedStub);

            // Set up pow_valid_block callback
            var powValidBlockStub = org.bitcoinkernel.jextract.btck_ValidationInterfacePoWValidBlock.allocate(
                (userData, block, blockIndex) -> {
                    Block blk = new Block(block);
                    BlockTreeEntry entry = new BlockTreeEntry(blockIndex);
                    callbacks.powValidBlock(blk, entry);
                }, arena
            );
            btck_ValidationInterfaceCallbacks.pow_valid_block(callbackStruct, powValidBlockStub);

            // Set up block_connected callback
            var blockConnectedStub = org.bitcoinkernel.jextract.btck_ValidationInterfaceBlockConnected.allocate(
                (userData, block, blockIndex) -> {
                    Block blk = new Block(block);
                    BlockTreeEntry entry = new BlockTreeEntry(blockIndex);
                    callbacks.blockConnected(blk, entry);
                }, arena
            );
            btck_ValidationInterfaceCallbacks.block_connected(callbackStruct, blockConnectedStub);

            // Set up block_disconnected callback
            var blockDisconnectedStub = org.bitcoinkernel.jextract.btck_ValidationInterfaceBlockDisconnected.allocate(
                (userData, block, blockIndex) -> {
                    Block blk = new Block(block);
                    BlockTreeEntry entry = new BlockTreeEntry(blockIndex);
                    callbacks.blockDisconnected(blk, entry);
                }, arena
            );
            btck_ValidationInterfaceCallbacks.block_disconnected(callbackStruct, blockDisconnectedStub);
        }

        public MemorySegment getCallbackStruct() {
            return callbackStruct;
        }

        @Override
        public void close() {
            if (arena != null) {
                arena.close();
            }
        }
    }
}

package org.bitcoinkernel;

import org.bitcoinkernel.KernelTypes;
import org.bitcoinkernel.KernelData;

public class NotificationsManager {
    // Callbacks interfaces
    public interface KernelNotificationInterfaceCallbacks {

        void blockTip(int state, KernelData.BlockIndex blockIndex, double verificationProgress);
        void headerTip(int state, long height, long timestamp, boolean presync);
        void progress(String title, int progressPercent, boolean resumePossible);
        void warningSet(int warning, String message);
        void warningUnset(int warning, String message);
        void flushError(String message);
        void fatalError(String message);
    }

    public static class ValidationInterfaceCallbacks {
        public final BlockCheckedCallback blockChecked;

        public ValidationInterfaceCallbacks(BlockCheckedCallback blockChecked) {
            this.blockChecked = blockChecked;
        }

        @FunctionalInterface
        public interface BlockCheckedCallback {
            void blockChecked(KernelData.Block block, KernelTypes.BlockValidationState state);
        }

    }













}

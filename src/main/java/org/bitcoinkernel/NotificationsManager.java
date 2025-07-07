package org.bitcoinkernel;

import org.bitcoinkernel.KernelTypes;
import org.bitcoinkernel.KernelData;

import java.lang.foreign.MemorySegment;

public class NotificationsManager {
    // Callbacks interfaces
    public interface KernelNotificationInterfaceCallbacks {

        void blockTip(int state, MemorySegment blockIndex, double verificationProgress);
        void headerTip(int state, long height, long timestamp, boolean presync);
        void progress(MemorySegment title, int progressPercent, boolean resumePossible);
        void warningSet(int warning, MemorySegment message);
        void warningUnset(int warning);
        void flushError(MemorySegment message);
        void fatalError(MemorySegment message);
    }

    public interface ValidationInterfaceCallbacks {

        void blockChecked(MemorySegment block, MemorySegment state);

    }













}

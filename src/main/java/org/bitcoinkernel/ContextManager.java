package org.bitcoinkernel;

import java.lang.foreign.*;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;

import static org.bitcoinkernel.jextract.bitcoinkernel_h.*;
import static org.bitcoinkernel.Chainstate.*;
import static org.bitcoinkernel.NotificationsManager.*;

public class ContextManager {

    // ===== Context Options =====
    public static class ContextOptions implements AutoCloseable {
        private MemorySegment inner;

        public ContextOptions() throws KernelTypes.KernelException {
            this.inner = btck_context_options_create.makeInvoker().apply();
            if (inner == MemorySegment.NULL) {
                throw new KernelTypes.KernelException("Failed to instantiate ContextOptions object");
            }
        }

        public void setChainParams(ChainParameters chainParams) {
            checkClosed();
            btck_context_options_set_chainparams(inner, chainParams.getInner());
        }

        public void setNotifications(NotificationsManager.KernelNotificationManager notificationManager) {
            checkClosed();
            btck_context_options_set_notifications(inner, notificationManager.getCallbackStruct());
        }

        public void setValidationInterface(NotificationsManager.ValidationInterfaceManager validationManager) {
            checkClosed();
            btck_context_options_set_validation_interface(inner, validationManager.getCallbackStruct());
        }

        MemorySegment getInner() {
            return inner;
        }

        private void checkClosed() {
            if (inner == MemorySegment.NULL) {
                throw new IllegalStateException("ContextOptions object has been closed already");
            }
        }

        @Override
        public void close() throws Exception {
            if (inner != MemorySegment.NULL) {
                btck_context_options_destroy(inner);
                inner = MemorySegment.NULL;
            }
        }
    }

    // ===== Context =====
    public static class Context implements AutoCloseable {
        private MemorySegment inner;

        public Context() throws KernelTypes.KernelException {
            this.inner = btck_context_create(MemorySegment.NULL);
            if (inner == MemorySegment.NULL) {
                throw new KernelTypes.KernelException("Failed to instantiate Context object");
            }
        }

        public Context(ContextOptions options) throws KernelTypes.KernelException {
            this.inner = btck_context_create(options.getInner());
            if (inner == MemorySegment.NULL) {
                throw new KernelTypes.KernelException("Failed to instantiate Context object");
            }
        }

        public boolean interrupt() {
            checkClosed();
            return btck_context_interrupt(inner) == 0;
        }

        MemorySegment getInner() {
            return inner;
        }

        private void checkClosed() {
            if (inner == MemorySegment.NULL) {
                throw new IllegalStateException("Context object is already closed");
            }
        }

        @Override
        public void close() throws Exception {
            if (inner != MemorySegment.NULL) {
                btck_context_destroy(inner);
                inner = MemorySegment.NULL;
            }
        }
    }
}
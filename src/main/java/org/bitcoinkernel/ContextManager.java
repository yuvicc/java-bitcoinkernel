package org.bitcoinkernel;

import java.lang.foreign.*;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;

import static org.bitcoinkernel.BitcoinKernelBindings.*;

public class ContextManager {
    // Structs for Validation Callbacks Layout
    private static final StructLayout VALIDATION_CALLBACK_LAYOUT = MemoryLayout.structLayout(
            ValueLayout.ADDRESS.withName("user_data"),
            ValueLayout.ADDRESS.withName("block_checked")
    );

    // Structs for Notification Callbacks Layout
    private static final StructLayout NOTIFICATION_CALLBACK_LAYOUT = MemoryLayout.structLayout(
            ValueLayout.ADDRESS.withName("user_data"),
            ValueLayout.ADDRESS.withName("block_tip"),
            ValueLayout.ADDRESS.withName("header_tip"),
            ValueLayout.ADDRESS.withName("progress"),
            ValueLayout.ADDRESS.withName("warning_set"),
            ValueLayout.ADDRESS.withName("warning_unset"),
            ValueLayout.ADDRESS.withName("flush_error"),
            ValueLayout.ADDRESS.withName("fatal_error")
    );

    // Upcall stubs for notification and validation callbacks - best way to call java code from native(C) code, read here https://docs.oracle.com/en/java/javase/24/core/upcalls-passing-java-code-function-pointer-foreign-function.html#GUID-908061BA-DC97-4524-A390-8FCEF7C5978F
    private static final MethodHandle BLOCK_TIP_MH;
    private static final MethodHandle HEADER_TIP_MH;
    private static final MethodHandle PROGRESS_MH;
    private static final MethodHandle WARNING_SET_MH;
    private static final MethodHandle WARNING_UNSET_MH;
    private static final MethodHandle FLUSH_ERROR_MH;
    private static final MethodHandle FATAL_ERROR_MH;
    private static final MethodHandle BLOCK_CHECKED_MH;

    static {
        try {
            BLOCK_TIP_MH = MethodHandles.lookup().findVirtual(
                    NotificationsManager.KernelNotificationInterfaceCallbacks.class,
                    "blockTip",
                    MethodType.methodType(void.class, int.class, MemorySegment.class, double.class)
            );
            HEADER_TIP_MH = MethodHandles.lookup().findVirtual(
                    NotificationsManager.KernelNotificationInterfaceCallbacks.class,
                    "headerTip",
                    MethodType.methodType(void.class, int.class, long.class, long.class, boolean.class)
            );
            PROGRESS_MH = MethodHandles.lookup().findVirtual(
                    NotificationsManager.KernelNotificationInterfaceCallbacks.class,
                    "progress",
                    MethodType.methodType(void.class, MemorySegment.class, int.class, boolean.class)
            );
            WARNING_SET_MH = MethodHandles.lookup().findVirtual(
                    NotificationsManager.KernelNotificationInterfaceCallbacks.class,
                    "warningSet",
                    MethodType.methodType(void.class, int.class, MemorySegment.class)
            );
            WARNING_UNSET_MH = MethodHandles.lookup().findVirtual(
                    NotificationsManager.KernelNotificationInterfaceCallbacks.class,
                    "warningUnset",
                    MethodType.methodType(void.class, int.class)
            );
            FATAL_ERROR_MH = MethodHandles.lookup().findVirtual(
                    NotificationsManager.KernelNotificationInterfaceCallbacks.class,
                    "fatalError",
                    MethodType.methodType(void.class, MemorySegment.class)
            );
            FLUSH_ERROR_MH = MethodHandles.lookup().findVirtual(
                    NotificationsManager.KernelNotificationInterfaceCallbacks.class,
                    "flushError",
                    MethodType.methodType(void.class, MemorySegment.class)
            );
            BLOCK_CHECKED_MH = MethodHandles.lookup().findVirtual(
                    NotificationsManager.ValidationInterfaceCallbacks.class,
                    "blockChecked",
                    MethodType.methodType(void.class, MemorySegment.class, MemorySegment.class)
            );
        } catch (NoSuchMethodException | IllegalAccessException e) {
            throw new ExceptionInInitializerError(e);
        }
    }

    // Context
    public static class Context implements AutoCloseable {
        private final MemorySegment inner;
        private final NotificationsManager.KernelNotificationInterfaceCallbacks knCallbacks;
        private final NotificationsManager.ValidationInterfaceCallbacks viCallbacks;
        private final Arena callBackArena;

        public Context(MemorySegment inner,
                       NotificationsManager.KernelNotificationInterfaceCallbacks knCallbacks,
                       NotificationsManager.ValidationInterfaceCallbacks viCallbacks,
                       Arena callBackArena) {
            this.inner = inner;
            this.knCallbacks = knCallbacks;
            this.viCallbacks = viCallbacks;
            this.callBackArena = callBackArena;
        }

        public boolean interrupt() {
            return kernel_context_interrupt(inner);
        }

        MemorySegment getInner() {
            return inner;
        }

        @Override
        public void close() {
            kernel_context_destroy(inner);
            callBackArena.close();
        }
    }

    // Context Builder
    public static class ContextBuilder implements AutoCloseable {
        private MemorySegment inner;
        private NotificationsManager.KernelNotificationInterfaceCallbacks knCallbacks;
        private NotificationsManager.ValidationInterfaceCallbacks viCallbacks;
        private final Arena callbackArena;
        private boolean built = false;

        public ContextBuilder() throws KernelTypes.KernelException {
            this.callbackArena = Arena.ofConfined();
            try (var arena = Arena.ofConfined()) {
                this.inner = kernel_context_options_create.makeInvoker().apply();
                if (inner == MemorySegment.NULL) {
                    throw new KernelTypes.KernelException("Failed to create context options");
                }
            }
        }

        public ContextBuilder chainType(KernelTypes.ChainType chainType) {
            try (var params = new ChainParams(chainType)) {
                kernel_context_options_set_chainparams(inner, params.getInner());
            } catch (KernelTypes.KernelException e) {
                throw new RuntimeException(e);
            }
            return this;
        }

        public ContextBuilder notificationCallbacks(NotificationsManager.KernelNotificationInterfaceCallbacks callBacks) {
            this.knCallbacks = callBacks;
            MemorySegment holder = callbackArena.allocate(NOTIFICATION_CALLBACK_LAYOUT);
            holder.set(ValueLayout.ADDRESS, 0, MemorySegment.ofAddress(System.identityHashCode(callBacks)));
            MemorySegment blockTipStub = Linker.nativeLinker().upcallStub(
                    BLOCK_TIP_MH.bindTo(callBacks),
                    FunctionDescriptor.ofVoid(ValueLayout.JAVA_INT, ValueLayout.ADDRESS, ValueLayout.JAVA_DOUBLE),
                    callbackArena
            );
            MemorySegment headerTipStub = Linker.nativeLinker().upcallStub(
                    HEADER_TIP_MH.bindTo(callBacks),
                    FunctionDescriptor.ofVoid(ValueLayout.JAVA_INT, ValueLayout.JAVA_LONG, ValueLayout.JAVA_LONG, ValueLayout.JAVA_BOOLEAN),
                    callbackArena
            );
            MemorySegment progressStub = Linker.nativeLinker().upcallStub(
                    PROGRESS_MH.bindTo(callBacks),
                    FunctionDescriptor.ofVoid(ValueLayout.ADDRESS, ValueLayout.JAVA_INT, ValueLayout.JAVA_BOOLEAN),
                    callbackArena
            );
            MemorySegment warningSetStub = Linker.nativeLinker().upcallStub(
                    WARNING_SET_MH.bindTo(callBacks),
                    FunctionDescriptor.ofVoid(ValueLayout.JAVA_INT, ValueLayout.ADDRESS),
                    callbackArena
            );
            MemorySegment warningUnsetStub = Linker.nativeLinker().upcallStub(
                    WARNING_UNSET_MH.bindTo(callBacks),
                    FunctionDescriptor.ofVoid(ValueLayout.JAVA_INT),
                    callbackArena
            );
            MemorySegment flushErrorStub = Linker.nativeLinker().upcallStub(
                    FLUSH_ERROR_MH.bindTo(callBacks),
                    FunctionDescriptor.ofVoid(ValueLayout.ADDRESS),
                    callbackArena
            );
            MemorySegment fatalErrorStub = Linker.nativeLinker().upcallStub(
                    FATAL_ERROR_MH.bindTo(callBacks),
                    FunctionDescriptor.ofVoid(ValueLayout.ADDRESS),
                    callbackArena
            );
            holder.set(ValueLayout.ADDRESS, ValueLayout.ADDRESS.byteSize(), blockTipStub);
            holder.set(ValueLayout.ADDRESS, 2 * ValueLayout.ADDRESS.byteSize(), headerTipStub);
            holder.set(ValueLayout.ADDRESS, 3 * ValueLayout.ADDRESS.byteSize(), progressStub);
            holder.set(ValueLayout.ADDRESS, 4 * ValueLayout.ADDRESS.byteSize(), warningSetStub);
            holder.set(ValueLayout.ADDRESS, 5 * ValueLayout.ADDRESS.byteSize(), warningUnsetStub);
            holder.set(ValueLayout.ADDRESS, 6 * ValueLayout.ADDRESS.byteSize(), flushErrorStub);
            holder.set(ValueLayout.ADDRESS, 7 * ValueLayout.ADDRESS.byteSize(), fatalErrorStub);
            kernel_context_options_set_notifications(inner, holder);
            return this;
        }

        public ContextBuilder validationiInterface(NotificationsManager.ValidationInterfaceCallbacks callbacks) throws KernelTypes.KernelException{
            this.viCallbacks = callbacks;
            MemorySegment holder = callbackArena.allocate(VALIDATION_CALLBACK_LAYOUT);
            holder.set(ValueLayout.ADDRESS, 0, MemorySegment.ofAddress(System.identityHashCode(callbacks)));

            MemorySegment blockCheckStub = Linker.nativeLinker().upcallStub(
                    BLOCK_CHECKED_MH.bindTo(callbacks),
                    FunctionDescriptor.ofVoid(ValueLayout.ADDRESS, ValueLayout.ADDRESS),
                    callbackArena
            );
            holder.set(ValueLayout.ADDRESS, ValueLayout.ADDRESS.byteSize(), blockCheckStub);
            kernel_context_options_set_validation_interface(inner, holder);
            return this;
        }

        public Context build() throws KernelTypes.KernelException {
            MemorySegment contextInner = kernel_context_create(inner);
            if (contextInner == MemorySegment.NULL) {
                throw new KernelTypes.KernelException("Failed to create context");
            }
            kernel_context_options_destroy(inner);
            inner = MemorySegment.NULL;
            built = true;
            return new Context(contextInner, knCallbacks, viCallbacks, callbackArena);
        }

        @Override
        public void close() {
            if (!built && inner != MemorySegment.NULL) {
                kernel_context_options_destroy(inner);
                inner = MemorySegment.NULL;
            }
//            callbackArena.close();
        }
    }

    // Chain Parameters
    public static class ChainParams implements AutoCloseable {
        private final MemorySegment inner;

        public ChainParams(KernelTypes.ChainType chainType) throws KernelTypes.KernelException {
            try (var arena = Arena.ofConfined()) {
                this.inner = kernel_chain_parameters_create(chainType.toNative());
                if (inner == MemorySegment.NULL) {
                    throw new KernelTypes.KernelException("Failed to create chain parameters");
                }
            }
        }

        MemorySegment getInner() {
            return inner;
        }

        @Override
        public void close() {
            if (inner != MemorySegment.NULL) {
                kernel_context_options_destroy(inner);
            }
        }
    }
}
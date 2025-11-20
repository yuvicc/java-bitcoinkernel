package org.bitcoinkernel;

import java.lang.foreign.*;
import java.util.function.Consumer;

import org.bitcoinkernel.KernelTypes;
import org.bitcoinkernel.jextract.btck_LoggingOptions;
import static org.bitcoinkernel.jextract.bitcoinkernel_h.*;
import static org.bitcoinkernel.KernelTypes.*;

public class Logger {

    // ===== Logging Options =====
    public static class LoggingOptions implements AutoCloseable {
        private final MemorySegment inner;
        private final Arena arena;

        public LoggingOptions() {
            this.arena = Arena.ofConfined();
            this.inner = btck_LoggingOptions.allocate(arena);

            // Set default values
            setLogTimestamps(true);
            setLogTimeMicros(false);
            setLogThreadNames(false);
            setLogSourceLocations(false);
            setAlwaysPrintCategoryLevels(false);
        }

        public void setLogTimestamps(boolean enabled) {
            btck_LoggingOptions.log_timestamps(inner, enabled ? 1 : 0);
        }

        public void setLogTimeMicros(boolean enabled) {
            btck_LoggingOptions.log_time_micros(inner, enabled ? 1 : 0);
        }

        public void setLogThreadNames(boolean enabled) {
            btck_LoggingOptions.log_threadnames(inner, enabled ? 1 : 0);
        }

        public void setLogSourceLocations(boolean enabled) {
            btck_LoggingOptions.log_sourcelocations(inner, enabled ? 1 : 0);
        }

        public void setAlwaysPrintCategoryLevels(boolean enabled) {
            btck_LoggingOptions.always_print_category_levels(inner, enabled ? 1 : 0);
        }

        public boolean getLogTimestamps() {
            return btck_LoggingOptions.log_timestamps(inner) != 0;
        }

        public boolean getLogTimeMicros() {
            return btck_LoggingOptions.log_time_micros(inner) != 0;
        }

        public boolean getLogThreadNames() {
            return btck_LoggingOptions.log_threadnames(inner) != 0;
        }

        public boolean getLogSourceLocations() {
            return btck_LoggingOptions.log_sourcelocations(inner) != 0;
        }

        public boolean getAlwaysPrintCategoryLevels() {
            return btck_LoggingOptions.always_print_category_levels(inner) != 0;
        }

        MemorySegment getInner() {
            return inner;
        }

        @Override
        public void close() {
            if (arena != null) {
                arena.close();
            }
        }
    }

    // ===== Logging Manager =====
    public static class LoggingManager {
        private LoggingManager() {
            // Utility class, prevent instantiation
        }

        public static void disable() {
            btck_logging_disable.makeInvoker().apply();
        }

        public static void setOptions(LoggingOptions options) {
            btck_logging_set_options(options.getInner());
        }

        public static void setLevelCategory(LogCategory category, LogLevel level) {
            btck_logging_set_level_category(category.getValue(), level.getValue());
        }

        public static void enableCategory(LogCategory category) {
            btck_logging_enable_category(category.getValue());
        }

        public static void disableCategory(LogCategory category) {
            btck_logging_disable_category(category.getValue());
        }
    }

    // ===== Log Callback Handler =====
    public static class LogCallbackHandler implements AutoCloseable {
        private final MemorySegment callbackStub;
        private final Arena arena;
        private final Consumer<String> logConsumer;

        public LogCallbackHandler(Consumer<String> logConsumer) {
            this.logConsumer = logConsumer;
            this.arena = Arena.ofShared();

            // Create upcall stub for log callback
            this.callbackStub = org.bitcoinkernel.jextract.btck_LogCallback.allocate(
                (userData, message, messageLen) -> {
                    String logMessage = message.reinterpret(messageLen).getString(0, java.nio.charset.StandardCharsets.UTF_8);
                    logConsumer.accept(logMessage);
                }, arena
            );
        }

        public MemorySegment getCallbackStub() {
            return callbackStub;
        }

        @Override
        public void close() {
            if (arena != null) {
                arena.close();
            }
        }
    }

    // ===== Simple Logger (Original) =====
    private final Consumer<String> output;

    public Logger(Consumer<String> output) {
        this.output = output;
    }

    public void log(String message) {
        output.accept(message);
    }
}

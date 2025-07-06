package org.bitcoinkernel;

import java.util.function.Consumer;

public class Logger<T> {
    private final Consumer<T> output;

    public Logger(Consumer<T> ouput) {
        this.output = ouput;
    }

    public void log(T message) {
        output.accept(message);
    }
}

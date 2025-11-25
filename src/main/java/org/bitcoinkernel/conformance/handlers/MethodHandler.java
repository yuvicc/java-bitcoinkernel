package org.bitcoinkernel.conformance.handlers;

import org.bitcoinkernel.conformance.protocol.TestResponse;
import org.bitcoinkernel.conformance.protocol.TestRequest;

@FunctionalInterface
public interface MethodHandler {
    TestResponse handle(TestRequest request);
}

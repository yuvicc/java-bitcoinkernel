package org.bitcoinkernel.conformance.utils;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonSyntaxException;
import org.bitcoinkernel.conformance.protocol.TestRequest;
import org.bitcoinkernel.conformance.protocol.TestResponse;
import org.bitcoinkernel.conformance.protocol.ErrorType;

public class JsonProtocol {

    private static final Gson GSON = new GsonBuilder()
            .create();

    // Parse request
    public static TestRequest parseRequest(String json) throws JsonSyntaxException {
        return GSON.fromJson(json, TestRequest.class);
    }

    // Serialize response to JSON format
    public static String serializeResponse(TestResponse response) {
        return GSON.toJson(response);
    }

    // Create error response for malformed requests
    public static TestResponse createErrorResponse(String requestId, String errorType, String variant) {
        return TestResponse.error(
                requestId != null ? requestId : "unknown",
                errorType,
                variant
        );
    }

    // Create protocol error
    public static TestResponse createProtocolError(String requestId, String variant) {
        return createErrorResponse(requestId, ErrorType.PROTOCOL, variant);
    }

    private JsonProtocol() {}
}

package org.bitcoinkernel.conformance.protocol;

import com.sun.net.httpserver.Authenticator;

import java.util.Map;

/**
 * Success JSON format:
 * {
 *     "id": "unique-request-id",
 *     "success": { ... }
 * }
 *
 * Error JSON format:
 * {
 *     "id": "unique-request-id",
 *     "error": {
 *         "type": "error_category",
 *         "variant": "specific_error"
 *     }
 * }
 */
public class TestResponse {
    private String id;
    private Map<String, Object> success;
    private ErrorResponse error;

    private TestResponse() {}

    public static TestResponse success(String id, Map<String, Object> result) {
        TestResponse response = new TestResponse();
        response.id = id;
        response.success = result;
        return response;
    }

    public static TestResponse success(String id) {
        return success(id, Map.of());
    }

    public static TestResponse error(String id, String errorType, String varint) {
        TestResponse response = new TestResponse();
        response.id = id;
        response.error = new ErrorResponse(errorType, varint);
        return response;
    }

    public String getId() {
        return id;
    }

    public Map<String, Object> getSuccess() {
        return success;
    }

    public ErrorResponse getError() {
        return error;
    }

    public boolean isSuccess() {
        return success != null;
    }

    public static class ErrorResponse {
        private String type;
        private String variant;

        public ErrorResponse() {}

        public ErrorResponse(String type, String variant) {
            this.type = type;
            this.variant = variant;
        }

        public String getType() {
            return type;
        }

        public void setType(String type) {
            this.type = type;
        }

        public String getVariant() {
            return variant;
        }

        public void setVariant(String variant) {
            this.variant = variant;
        }
    }

    @Override
    public String toString() {
        if (success != null) {
            return "TestResponse{id='" + id + "', success=" + success + "}";
        } else {
            return "TestResponse{id='" + id + "', error=" + error.type + "." + error.variant + "}";
        }
    }
}

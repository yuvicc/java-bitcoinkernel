package org.bitcoinkernel.conformance.protocol;

import java.util.List;
import java.util.Map;

/**
 * JSON format:
 * {
 *     "id": "unique-request-id",
 *     "method": "method_name",
 *     "params": { ... }
 * }
 */
public class TestRequest {
    private String id;
    private String method;
    private Map<String, Object> params;

    public TestRequest() {}

    public TestRequest(String id, String method, Map<String, Object> params) {
        this.id = id;
        this.method = method;
        this.params = params;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getMethod() {
        return method;
    }

    public void setMethod(String method) {
        this.method = method;
    }

    public Map<String, Object> getParams() {
        return params;
    }

    public void setParams(Map<String, Object> params) {
        this.params = params;
    }

    public String getParamAsString(String key) {
        Object value = params.get(key);
        return value != null ? value.toString() : null;
    }

    public Integer getParamAsInt(String key) {
        Object value = params.get(key);
        if (value == null) return null;
        if (value instanceof Number) {
            return (((Number) value).intValue());
        }
        if (value instanceof String) {
            return Integer.parseInt((String) value);
        }
        throw new IllegalArgumentException("Cannot convert parameter " + key + " to integer");
    }

    public Long getParamAsLong(String key) {
        Object value = params.get(key);
        if (value == null) return null;
        if (value instanceof Number) {
            return ((Number) value).longValue();
        }
        if (value instanceof String) {
            return Long.parseLong((String) value);
        }
        throw new IllegalArgumentException("Cannot convert parameter " + key + " to long");
    }

    @SuppressWarnings("unchecked")
    public List<Map<String, Object>> getParamAsList(String key) {
        Object value = params.get(key);
        if (value == null) {
            return null;
        }
        if (value instanceof List) {
            return (List<Map<String, Object>>) value;
        }
        throw new IllegalArgumentException("Parameter " + key + " is not a list");
    }

    @Override
    public String toString() {
        return "TestRequest{id='" + id + "', method='" + method + "', params=" + params + "}";
    }
}

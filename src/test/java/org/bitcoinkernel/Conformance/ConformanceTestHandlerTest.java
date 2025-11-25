package org.bitcoinkernel.Conformance;


import org.bitcoinkernel.conformance.protocol.TestRequest;
import org.bitcoinkernel.conformance.protocol.TestResponse;
import org.bitcoinkernel.conformance.utils.JsonProtocol;
import org.bitcoinkernel.conformance.utils.HexUtils;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

public class ConformanceTestHandlerTest {

    @Test
    void testHexEncodeDecode() {
        byte[] original = new byte[]{(byte) 0xde, (byte) 0xad, (byte) 0xbe, (byte) 0xef};
        String hex = HexUtils.encode(original);
        assertEquals("deadbeef", hex);

        byte[] decoded = HexUtils.decode(hex);
        assertArrayEquals(original, decoded);
    }

    @Test
    void testHexDecodeWithPrefix() {
        byte[] decoded = HexUtils.decode("0xdeadbeef");
        byte[] expected = new byte[]{(byte) 0xde, (byte) 0xad, (byte) 0xbe, (byte) 0xef};
        assertArrayEquals(expected, decoded);
    }

    @Test
    void testRequestParsing() {
        String json = """
            {
              "id": "test-123",
              "method": "script_pubkey.verify",
              "params": {
                "script_pubkey": "76a914...",
                "amount": 5000000000,
                "input_index": 0,
                "flags": 0
              }
            }
            """;

        TestRequest request = JsonProtocol.parseRequest(json);
        assertEquals("test-123", request.getId());
        assertEquals("script_pubkey.verify", request.getMethod());
        assertEquals("76a914...", request.getParamAsString("script_pubkey"));
        assertEquals(5000000000L, request.getParamAsLong("amount"));
        assertEquals(0, request.getParamAsInt(("input_index")));
        assertEquals(0, request.getParamAsInt("flags"));
    }

    @Test
    void testSuccessResponseSerialization() {
        TestResponse response = TestResponse.success("test-123", Map.of("result", "ok"));
        String json = JsonProtocol.serializeResponse(response);

        assertTrue(json.contains("\"id\":\"test-123\""));
        assertTrue(json.contains("\"success\""));
        assertFalse(json.contains("\"error\""));
    }

    @Test
    void testErrorResponseSerialization() {
        TestResponse response = TestResponse.error("test-456", "ScriptVerify", "Invalid");
        String json = JsonProtocol.serializeResponse(response);

        assertTrue(json.contains("\"id\":\"test-456\""));
        assertTrue(json.contains("\"error\""));
        assertTrue(json.contains("\"type\":\"ScriptVerify\""));
        assertTrue(json.contains("\"variant\":\"Invalid\""));
    }

}

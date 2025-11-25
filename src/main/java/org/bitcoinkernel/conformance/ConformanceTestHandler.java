package org.bitcoinkernel.conformance;

import org.bitcoinkernel.conformance.handlers.MethodHandler;
import org.bitcoinkernel.conformance.handlers.ScriptPubkeyVerifyHandler;
import org.bitcoinkernel.conformance.protocol.TestRequest;
import org.bitcoinkernel.conformance.protocol.TestResponse;
import org.bitcoinkernel.conformance.protocol.ErrorType;
import org.bitcoinkernel.conformance.utils.JsonProtocol;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

// Main conformance test handler
public class ConformanceTestHandler {

    private static final Map<String, MethodHandler> METHOD_HANDLERS = new HashMap<>();

    static {
        METHOD_HANDLERS.put("script_pubkey.verify", new ScriptPubkeyVerifyHandler());
    }

    public static void main(String[] args) {
        // set up stdin/stdout with UTF-8 encoding
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(System.in, StandardCharsets.UTF_8));
             PrintWriter writer = new PrintWriter(System.out, true, StandardCharsets.UTF_8)) {

             String line;
             while ((line = reader.readLine()) != null) {
                 if (line.trim().isEmpty()) {
                     continue;
                 }

                 TestResponse response = processRequest(line);
                 String responseJson = JsonProtocol.serializeResponse(response);
                 writer.println(responseJson);
             }

        } catch (IOException e) {
                 System.err.println("Fatal I/O error: " + e.getMessage());
                 System.exit(1);
        }

        // Normal exit when stdin closes
        System.exit(0);
    }

    // Process a single request line
    private static TestResponse processRequest(String line) {
        TestRequest request;

        try {
            request = JsonProtocol.parseRequest(line);
        } catch (Exception e) {
            return JsonProtocol.createProtocolError(null, ErrorType.MALFORMED_REQUEST);
        }

        if (request.getId() == null || request.getMethod() == null) {
            return JsonProtocol.createProtocolError(
                    request.getId(),
                    ErrorType.MALFORMED_REQUEST
            );
        }

        MethodHandler handler = METHOD_HANDLERS.get(request.getMethod());
        if (handler == null) {
            return JsonProtocol.createProtocolError(
                    request.getId(),
                    ErrorType.UNKNOWN_METHOD
            );
        }

        try {
            return handler.handle(request);
        } catch (Exception e) {
            System.err.println("Error processing request: " + e.getMessage());
            e.printStackTrace(System.err);

            return TestResponse.error(
                    request.getId(),
                    ErrorType.BINDING,
                    ErrorType.BINDING_ERROR
            );
        }
    }
}

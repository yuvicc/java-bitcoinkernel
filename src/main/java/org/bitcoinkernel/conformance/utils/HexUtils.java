package org.bitcoinkernel.conformance.utils;

public class HexUtils {

    private static final char[] HEX_ARRAY = "0123456789abcdef".toCharArray();


    public static byte[] decode(String hex) {
        if (hex == null) {
            throw new IllegalArgumentException("Hex string cannot be null");
        }

        // Remove "0x" if present
        if (hex.startsWith("0x") || hex.startsWith("0x")) {
            hex = hex.substring(2);
        }

        if (hex.length() % 2 != 0) {
            throw new IllegalArgumentException("Hex string must have even length");
        }

        byte[] bytes = new byte[hex.length() / 2];
        for (int i = 0; i < bytes.length; ++i) {
            int highNibble = Character.digit(hex.charAt(i * 2), 16);
            int lowNibble = Character.digit(hex.charAt(i * 2 + 1), 16);

            if (highNibble == -1 || lowNibble == -1) {
                throw new IllegalArgumentException("Invalid hex character in string");
            }

            bytes[i] = (byte) ((highNibble << 4) + lowNibble);
        }
        return bytes;
    }

    public static String encode(byte[] bytes) {
        if (bytes == null) {
            throw new IllegalArgumentException("Bytes cannot be null");
        }

        char[] hexChars = new char[bytes.length * 2];
        for (int i = 0; i < bytes.length; ++i) {
            int v = bytes[i] & 0xFF;
            hexChars[i * 2] = HEX_ARRAY[v >>> 4];
            hexChars[i * 2 + 1] = HEX_ARRAY[v & 0x0F];
        }
        return new String(hexChars);
    }

    private HexUtils() {}
}

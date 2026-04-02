package com.sleekydz86.carebridge.backend.global.security;

import java.util.regex.Pattern;

public final class InputSanitizer {

    private static final Pattern HTML_TAG_PATTERN = Pattern.compile("<[^>]*>", Pattern.DOTALL);
    private static final Pattern JS_EVENT_PATTERN = Pattern.compile("on\\w+\\s*=", Pattern.CASE_INSENSITIVE);
    private static final Pattern JS_PROTO_PATTERN = Pattern.compile("javascript:", Pattern.CASE_INSENSITIVE);
    private InputSanitizer()  {}


    public static String sanitize(String input, int maxLength) {
        if (input == null) {
            return "";
        }

        String result = input.trim();
        result = HTML_TAG_PATTERN.matcher(result).replaceAll("");
        result = JS_EVENT_PATTERN.matcher(result).replaceAll("");
        result = JS_PROTO_PATTERN.matcher(result).replaceAll("");
        result = result.replace("<", "&lt;").replace(">", "&gt;");

        if (result.length() > maxLength) {
            result = result.substring(0, maxLength);
        }

        return result;
    }


    public static String sanitizePayload(String payload, int maxLength) {
        if (payload == null) {
            return "";
        }

        String result = payload.trim();
        result = HTML_TAG_PATTERN.matcher(result).replaceAll("");
        result = JS_EVENT_PATTERN.matcher(result).replaceAll("");
        result = JS_PROTO_PATTERN.matcher(result).replaceAll("");

        if (result.length() > maxLength) {
            result = result.substring(0, maxLength);
        }

        return result;
    }
}

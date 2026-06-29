package com.buldreinfo.util;

public final class StringUtils {
    
    private StringUtils() {
    }

    public static String stripToNull(String str) {
        if (str == null) return null;
        String stripped = str.strip();
        return stripped.isBlank() ? null : stripped;
    }
}
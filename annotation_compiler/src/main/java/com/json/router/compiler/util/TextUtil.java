package com.json.router.compiler.util;

public class TextUtil {
    public static boolean isEmpty(String str) {
        if (str == null || str.length() == 0) {
            return true;
        }
        return false;
    }
}

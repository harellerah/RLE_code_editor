package com.example.javafx_firstproject;

public class TokenStorage {
    private static String token;

    public static void setToken(String jwt) {
        token = jwt;
    }

    public static String getToken() {
        return token;
    }

    public static boolean hasToken() {
        return token != null && !token.isEmpty();
    }

    public static void clear() {
        token = null;
    }
}

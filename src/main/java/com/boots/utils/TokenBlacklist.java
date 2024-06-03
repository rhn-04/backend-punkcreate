package com.boots.utils;

import java.util.HashSet;
import java.util.Set;

public class TokenBlacklist {

    private static Set<String> blacklist = new HashSet<>();

    public static void invalidateToken(String token) {
        blacklist.add(token);
    }

    public static boolean isTokenInvalid(String token) {
        return blacklist.contains(token);
    }
}

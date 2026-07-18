package com.submeter.util;

import java.time.Instant;
import java.util.Base64;
import java.util.UUID;

/**
 * Utility for encoding and decoding keyset cursors.
 *
 * <p>Uses the standard (createdAt, id) tuple to guarantee deterministic
 * ordering when timestamps are identical.
 */
public final class CursorUtil {

    private CursorUtil() {}

    public static String encode(Instant createdAt, UUID id) {
        if (createdAt == null || id == null) return null;
        String raw = createdAt.toEpochMilli() + "_" + id.toString();
        return Base64.getUrlEncoder().withoutPadding().encodeToString(raw.getBytes());
    }

    public static Cursor parse(String encoded) {
        if (encoded == null || encoded.isBlank()) return null;
        try {
            String raw = new String(Base64.getUrlDecoder().decode(encoded));
            String[] parts = raw.split("_");
            if (parts.length != 2) return null;
            return new Cursor(Instant.ofEpochMilli(Long.parseLong(parts[0])), UUID.fromString(parts[1]));
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid cursor format");
        }
    }

    public record Cursor(Instant createdAt, UUID id) {}
}

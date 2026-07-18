package com.submeter.security.ratelimit;

import com.submeter.config.AppProperties;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.concurrent.ConcurrentHashMap;

/**
 * In-memory sliding-window rate limiter for login attempts.
 *
 * <p>Key strategy: {@code "ip:email"} — so the same user from different IPs
 * accumulates across IPs, and different users on the same IP don't share limits.
 *
 * <p>Window: configurable (default 15 minutes). Once the attempt count within
 * the window reaches {@code loginMaxAttempts}, all further attempts (even with
 * the correct password) are rejected with HTTP 429 until the window rolls past.
 *
 * <p>State is in-memory only — resets on application restart. This is acceptable
 * for v1 (portfolio demo). Production would use Redis for distributed state.
 *
 * <p>Thread safety: {@link ConcurrentHashMap} for map operations; inner
 * {@link LinkedList} operations are synchronized on the list itself.
 */
@Service
@RequiredArgsConstructor
public class RateLimiterService {

    private static final Logger log = LoggerFactory.getLogger(RateLimiterService.class);

    private final AppProperties props;

    /** Key: "ip:email" → timestamps of failed attempts within the window. */
    private final ConcurrentHashMap<String, LinkedList<Instant>> attempts =
            new ConcurrentHashMap<>();

    /**
     * Returns true if the ip+email combination has hit the rate limit.
     * Old attempts outside the window are evicted before checking.
     */
    public boolean isBlocked(String ip, String email) {
        String key = buildKey(ip, email);
        LinkedList<Instant> window = evict(key);
        return window.size() >= props.getRateLimit().getLoginMaxAttempts();
    }

    /** Records a failed login attempt. */
    public void recordFailure(String ip, String email) {
        String key = buildKey(ip, email);
        attempts.computeIfAbsent(key, k -> new LinkedList<>());
        LinkedList<Instant> window = attempts.get(key);
        synchronized (window) {
            window.add(Instant.now());
        }
        log.debug("Rate-limit: recorded failure for key={}, count={}",
                key, evict(key).size());
    }

    /** Clears the attempt history on successful login. */
    public void clearAttempts(String ip, String email) {
        attempts.remove(buildKey(ip, email));
    }

    /**
     * How many seconds until the oldest attempt expires.
     * Used to populate the {@code Retry-After} response header.
     */
    public long retryAfterSeconds(String ip, String email) {
        String key = buildKey(ip, email);
        LinkedList<Instant> window = attempts.get(key);
        if (window == null || window.isEmpty()) return 0;
        synchronized (window) {
            Instant oldest = window.peek();
            if (oldest == null) return 0;
            long windowMs = props.getRateLimit().getLoginWindowMinutes() * 60_000L;
            long expiresAt = oldest.toEpochMilli() + windowMs;
            return Math.max(0, (expiresAt - System.currentTimeMillis()) / 1000);
        }
    }

    // ── Internal ──────────────────────────────────────────────────────────────

    private String buildKey(String ip, String email) {
        return ip + ":" + email.toLowerCase();
    }

    private LinkedList<Instant> evict(String key) {
        LinkedList<Instant> window = attempts.computeIfAbsent(key, k -> new LinkedList<>());
        Instant cutoff = Instant.now().minus(
                props.getRateLimit().getLoginWindowMinutes(), ChronoUnit.MINUTES);
        synchronized (window) {
            Iterator<Instant> it = window.iterator();
            while (it.hasNext()) {
                if (it.next().isBefore(cutoff)) {
                    it.remove();
                } else {
                    break; // LinkedList is insertion-ordered; no need to scan further
                }
            }
        }
        return window;
    }
}

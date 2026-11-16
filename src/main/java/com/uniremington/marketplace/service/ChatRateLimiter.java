package com.uniremington.marketplace.service;

import jakarta.servlet.http.HttpSession;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class ChatRateLimiter {

    private static final String SESSION_KEY = ChatRateLimiter.class.getName() + ".timestamps";
    private static final long WINDOW_MILLIS = 60_000L;
    private static final int MAX_REQUESTS = 20;

    public void check(HttpSession session) {
        synchronized (session) {
            long now = System.currentTimeMillis();
            @SuppressWarnings("unchecked")
            List<Long> timestamps = (List<Long>) session.getAttribute(SESSION_KEY);
            if (timestamps == null) {
                timestamps = new ArrayList<>();
                session.setAttribute(SESSION_KEY, timestamps);
            }

            timestamps.removeIf(timestamp -> now - timestamp >= WINDOW_MILLIS);
            if (timestamps.size() >= MAX_REQUESTS) {
                throw new GeminiChatException(
                        HttpStatus.TOO_MANY_REQUESTS,
                        "Has enviado muchos mensajes. Espera un minuto e inténtalo de nuevo."
                );
            }
            timestamps.add(now);
        }
    }
}

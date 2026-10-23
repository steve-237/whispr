package com.whispr.backend.security;

import com.whispr.backend.util.ClientMetadataResolver;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

@Component
@RequiredArgsConstructor
public class RateLimitInterceptor implements HandlerInterceptor {

    private final ClientMetadataResolver clientMetadataResolver;

    private static final int MAX_REQUESTS = 50;
    private static final long TIME_WINDOW = 3600 * 1000L;

    // Cache d'adresses IP vers historique des requêtes
    private final Map<String, Queue<Long>> requestCounts = new ConcurrentHashMap<>();

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            return true;
        }

        String ip = clientMetadataResolver.extractClientIp(request);
        if (ip == null || ip.isBlank()) {
            ip = "unknown";
        }

        long now = System.currentTimeMillis();
        Queue<Long> requests = requestCounts.computeIfAbsent(ip, k -> new ConcurrentLinkedQueue<>());

        while (!requests.isEmpty() && (now - requests.peek()) > TIME_WINDOW) {
            requests.poll();
        }

        // Si la file est vide pour cette IP, nettoyer la clé pour éviter une fuite mémoire
        if (requests.isEmpty()) {
            requestCounts.remove(ip, requests);
        }

        if (requests.size() >= MAX_REQUESTS) {
            response.setStatus(429);
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write("{\"message\":\"Vous envoyez trop de messages. Veuillez patienter.\"}");
            return false;
        }

        requests.add(now);
        return true;
    }
}

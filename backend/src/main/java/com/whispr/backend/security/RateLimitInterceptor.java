package com.whispr.backend.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.Iterator;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

@Component
public class RateLimitInterceptor implements HandlerInterceptor {

    // Limite: 5 requêtes
    private static final int MAX_REQUESTS = 5;
    // Fenêtre: 1 heure en millisecondes
    private static final long TIME_WINDOW = 3600 * 1000L;

    // Stockage en mémoire (Map de IP -> Liste des timestamps des requêtes)
    private final Map<String, Queue<Long>> requestCounts = new ConcurrentHashMap<>();

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        
        // Extraction de l'IP
        String ip = request.getHeader("CF-Connecting-IP");
        if (ip == null || ip.isBlank()) ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isBlank()) ip = request.getRemoteAddr();
        
        if (ip == null) ip = "unknown";

        long now = System.currentTimeMillis();
        
        Queue<Long> requests = requestCounts.computeIfAbsent(ip, k -> new ConcurrentLinkedQueue<>());
        
        // Nettoyer les requêtes trop vieilles (hors de la fenêtre d'1 heure)
        while (!requests.isEmpty() && (now - requests.peek()) > TIME_WINDOW) {
            requests.poll();
        }

        if (requests.size() >= MAX_REQUESTS) {
            // Limite atteinte, on rejette
            response.setStatus(429); // 429 Too Many Requests
            response.getWriter().write("Too many requests. Vous avez atteint la limite de messages pour cette heure.");
            return false; // Bloque la requête
        }

        // On enregistre la nouvelle requête
        requests.add(now);
        return true; // Autorise la requête
    }
}

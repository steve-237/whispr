package com.whispr.backend.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    @Value("${cors.allowed-origins:http://localhost:4200,https://whispr-theta-smoky.vercel.app}")
    private String allowedOrigins;

    @Override
    public void configureMessageBroker(MessageBrokerRegistry config) {
        // Active un broker simple en mémoire pour pousser les messages aux clients
        // Les clients s'abonneront à /topic/user/{email}/queue/messages (on simplifie avec /topic pour le MVP)
        config.enableSimpleBroker("/topic", "/queue");
        config.setApplicationDestinationPrefixes("/app");
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        String[] origins = java.util.Arrays.stream(allowedOrigins.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .toArray(String[]::new);

        // Point d'entrée WebSocket pour le client frontend
        registry.addEndpoint("/ws-whispr")
                .setAllowedOrigins(origins)
                .setAllowedOriginPatterns("http://localhost:[*]", "https://*.vercel.app");
        registry.addEndpoint("/ws-whispr")
                .setAllowedOrigins(origins)
                .setAllowedOriginPatterns("http://localhost:[*]", "https://*.vercel.app")
                .withSockJS(); // Fallback si WebSocket n'est pas supporté (et utilisé par StompJs dans certains cas)
    }
}


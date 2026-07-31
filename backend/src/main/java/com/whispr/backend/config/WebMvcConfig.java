package com.whispr.backend.config;

import com.whispr.backend.security.RateLimitInterceptor;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
@RequiredArgsConstructor
public class WebMvcConfig implements WebMvcConfigurer {

    private final RateLimitInterceptor rateLimitInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        // Appliquer la limite de requêtes uniquement sur la route d'envoi de messages
        registry.addInterceptor(rateLimitInterceptor)
                .addPathPatterns("/api/messages/send/**");
    }
}

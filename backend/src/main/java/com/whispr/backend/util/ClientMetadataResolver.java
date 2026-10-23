package com.whispr.backend.util;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

@Component
public class ClientMetadataResolver {

    private static final String IP_SALT = "whispr-salt-anonym-messages-2026";

    private static final String[] DEMO_LOCATIONS = {
            "Paris, France 🇫🇷",
            "Lyon, France 🇫🇷",
            "Marseille, France 🇫🇷",
            "Bordeaux, France 🇫🇷",
            "Montréal, Canada 🇨🇦",
            "Genève, Suisse 🇨🇭",
            "Bruxelles, Belgique 🇧🇪",
            "Casablanca, Maroc 🇲🇦",
            "Dakar, Sénégal 🇸🇳",
            "Abidjan, Côte d'Ivoire 🇨🇮"
    };

    public ClientMetadata resolve(HttpServletRequest httpRequest) {
        String rawIp = extractClientIp(httpRequest);
        String hashedIp = hashIp(rawIp);
        String userAgent = httpRequest.getHeader("User-Agent");
        String country = extractCountry(httpRequest, rawIp, userAgent);
        String city = extractCity(httpRequest, country);
        String referrer = extractReferrer(httpRequest);

        return new ClientMetadata(rawIp, hashedIp, userAgent, country, city, referrer);
    }

    public String extractClientIp(HttpServletRequest request) {
        String ip = request.getHeader("CF-Connecting-IP");
        if (ip == null || ip.isBlank() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("X-Forwarded-For");
            if (ip != null && !ip.isBlank()) {
                // X-Forwarded-For can contain comma-separated IPs: client, proxy1, proxy2
                ip = ip.split(",")[0].trim();
            }
        }
        if (ip == null || ip.isBlank() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        return ip;
    }

    public String hashIp(String ip) {
        if (ip == null || ip.isBlank()) {
            return "anonymous";
        }
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] encodedHash = digest.digest((ip + IP_SALT).getBytes(StandardCharsets.UTF_8));
            StringBuilder hexString = new StringBuilder();
            for (byte b : encodedHash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) {
                    hexString.append('0');
                }
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            return Integer.toHexString(ip.hashCode());
        }
    }

    private String extractCountry(HttpServletRequest request, String ip, String userAgent) {
        String country = request.getHeader("CF-IPCountry");
        if (country == null || country.isBlank() || "XX".equalsIgnoreCase(country)) {
            country = request.getHeader("X-Country");
        }

        boolean isLocalOrPrivate = country == null || country.isBlank()
                || "127.0.0.1".equals(ip)
                || "0:0:0:0:0:0:0:1".equals(ip)
                || (ip != null && (ip.startsWith("192.168.") || ip.startsWith("10.")));

        if (isLocalOrPrivate) {
            int seed = Math.abs((userAgent != null ? userAgent.hashCode() : (int) System.currentTimeMillis()));
            int idx = seed % DEMO_LOCATIONS.length;
            country = DEMO_LOCATIONS[idx];
        }

        return country;
    }

    private String extractCity(HttpServletRequest request, String country) {
        String city = request.getHeader("CF-IPCity");
        if (city == null || city.isBlank()) city = request.getHeader("X-City");
        if (city == null || city.isBlank()) city = request.getHeader("X-Client-City");

        if (city == null || city.isBlank()) {
            if (country != null && country.contains(",")) {
                city = country.split(",")[0].trim();
            } else {
                city = "Paris";
            }
        }
        return city;
    }

    private String extractReferrer(HttpServletRequest request) {
        String referrer = request.getHeader("Referer");
        if (referrer == null || referrer.isBlank()) {
            referrer = request.getHeader("Origin");
        }
        return referrer;
    }
}

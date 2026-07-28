package com.whispr.backend.util;

public class DeviceUtil {

    public static String parseDeviceHint(String userAgent) {
        if (userAgent == null || userAgent.isBlank()) {
            return "🌐 Appareil Inconnu";
        }

        String ua = userAgent.toLowerCase();

        // Réseaux sociaux / Navigateurs intégrés
        if (ua.contains("instagram")) {
            return "📲 Story / Navigateur Instagram";
        }
        if (ua.contains("tiktok")) {
            return "📲 Navigateur TikTok";
        }
        if (ua.contains("snapchat")) {
            return "📲 Navigateur Snapchat";
        }
        if (ua.contains("fb_iab") || ua.contains("fban") || ua.contains("fbav")) {
            return "📲 Navigateur Facebook";
        }
        if (ua.contains("twitter") || ua.contains("x-app")) {
            return "📲 Navigateur X (Twitter)";
        }

        // Téléphones et Mobiles
        if (ua.contains("iphone") || ua.contains("ios")) {
            return "📱 iPhone (iOS)";
        }
        if (ua.contains("ipad")) {
            return "📱 iPad (iOS)";
        }
        if (ua.contains("android")) {
            if (ua.contains("mobile")) {
                return "📱 Smartphone Android";
            } else {
                return "📱 Tablette Android";
            }
        }

        // Ordinateurs et Bureau
        if (ua.contains("macintosh") || ua.contains("mac os")) {
            return "💻 Apple Mac";
        }
        if (ua.contains("windows") || ua.contains("win32") || ua.contains("win64")) {
            return "💻 PC Windows";
        }
        if (ua.contains("linux") || ua.contains("x11")) {
            return "💻 PC Linux";
        }

        return "🌐 Navigateur Web";
    }
}

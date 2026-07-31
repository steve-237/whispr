package com.whispr.backend.service;

import org.springframework.stereotype.Service;

import java.text.Normalizer;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;

@Service
public class ModerationService {

    // Liste de base pour le MVP (mots insultants, haineux en FR et EN)
    private static final List<String> BANNED_KEYWORDS = Arrays.asList(
            "connard", "salope", "pute", "enculé", "pd", "pédé", "bougnoule", "nègre", "negre",
            "sale race", "suicide toi", "meurs", "crève", "creve", "fils de pute", "fdp", "ntm",
            "nique", "bitch", "whore", "slut", "faggot", "nigger", "kill yourself", "kys",
            "retard", "mongol", "trisomique", "tocard", "grosse vache", "gros porc"
    );

    public boolean isToxic(String content) {
        if (content == null || content.isBlank()) {
            return false;
        }

        // Nettoyage : retirer les accents, passer en minuscules
        String normalizedContent = normalizeText(content);

        // Recherche d'un mot-clé exact (pour éviter les faux positifs)
        // ex: "pute" ne doit pas bloquer "réputation"
        for (String keyword : BANNED_KEYWORDS) {
            String regex = "(?i)\\b" + Pattern.quote(normalizeText(keyword)) + "\\b";
            if (Pattern.compile(regex).matcher(normalizedContent).find()) {
                return true;
            }
        }
        
        return false;
    }

    private String normalizeText(String input) {
        String normalized = Normalizer.normalize(input, Normalizer.Form.NFD);
        return normalized.replaceAll("\\p{M}", "").toLowerCase();
    }
}

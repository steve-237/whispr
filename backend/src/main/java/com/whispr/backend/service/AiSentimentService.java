package com.whispr.backend.service;

import org.springframework.stereotype.Service;

import java.text.Normalizer;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;

@Service
public class AiSentimentService {

    // MVP Mock : Dictionnaires de mots
    private static final List<String> POSITIVE_WORDS = Arrays.asList(
            "super", "génial", "genial", "top", "merci", "cool", "bravo", "magnifique",
            "beau", "belle", "joli", "aime", "adore", "parfait", "incroyable", "fou", "love", "crush"
    );

    private static final List<String> NEGATIVE_WORDS = Arrays.asList(
            "nul", "bête", "bete", "moche", "déçu", "decu", "triste", "mauvais", "pire",
            "horrible", "déteste", "deteste", "honte", "dommage"
    );

    public String analyzeSentiment(String content) {
        if (content == null || content.isBlank()) {
            return "NEUTRAL";
        }

        String normalizedContent = normalizeText(content);
        
        int positiveScore = countMatches(normalizedContent, POSITIVE_WORDS);
        int negativeScore = countMatches(normalizedContent, NEGATIVE_WORDS);

        if (positiveScore > negativeScore) {
            return "POSITIVE";
        } else if (negativeScore > positiveScore) {
            return "NEGATIVE";
        } else {
            return "NEUTRAL";
        }
    }

    private int countMatches(String text, List<String> words) {
        int count = 0;
        for (String word : words) {
            String regex = "(?i)\\b" + Pattern.quote(normalizeText(word)) + "\\b";
            if (Pattern.compile(regex).matcher(text).find()) {
                count++;
            }
        }
        return count;
    }

    private String normalizeText(String input) {
        String normalized = Normalizer.normalize(input, Normalizer.Form.NFD);
        return normalized.replaceAll("\\p{M}", "").toLowerCase();
    }
}

package com.whispr.backend.service;

import org.springframework.stereotype.Service;

import java.text.Normalizer;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;

@Service
public class AiSentimentService {

    // ===== DICTIONNAIRES BILINGUES (FR + EN) =====

    private static final List<String> POSITIVE_WORDS = Arrays.asList(
            // Français
            "super", "genial", "top", "merci", "cool", "bravo", "magnifique",
            "beau", "belle", "joli", "jolie", "aime", "adore", "parfait", "parfaite",
            "incroyable", "fou", "folle", "crush", "gentil", "gentille",
            "respect", "felicitations", "chapeau", "classe", "stylé", "style",
            "formidable", "fantastique", "excellent", "excellente", "drole",
            "marrant", "marrante", "sympa", "sympathique", "courageux", "courageuse",
            "talentueux", "talentueuse", "fier", "fiere", "admire", "inspire",
            "inspirant", "inspirante", "adorable", "charmant", "charmante",
            "sourire", "bonheur", "heureux", "heureuse", "content", "contente",
            "apprecie", "reconnaissant", "reconnaissante", "bien", "mieux",
            "meilleur", "meilleure", "force", "fort", "forte", "intelligent",
            "intelligente", "brillant", "brillante", "lumineux", "lumineuse",
            "doux", "douce", "tendre", "attentionne", "attentionnee",
            "exceptionnel", "exceptionnelle", "remarquable", "impressionnant",
            "impressionnante", "magnifique", "splendide", "sublime", "superbe",
            "ravissant", "ravissante", "agreable", "plaisant", "plaisante",
            "positif", "positive", "encourageant", "encourageante",
            "confiance", "espoir", "chance", "chanceux", "chanceuse",
            "amour", "bisou", "bisous", "calin", "calins", "coeur",
            // Anglais
            "love", "great", "amazing", "awesome", "wonderful", "beautiful",
            "thank", "thanks", "kind", "nice", "good", "best", "better",
            "happy", "glad", "proud", "sweet", "cute", "lovely", "perfect",
            "fantastic", "incredible", "brilliant", "talented", "smart",
            "funny", "handsome", "pretty", "gorgeous", "stunning",
            "admire", "respect", "inspire", "strong", "brave", "gentle",
            "warm", "caring", "thoughtful", "excellent", "outstanding",
            "remarkable", "exceptional", "magnificent", "superb", "fabulous",
            "delightful", "charming", "graceful", "blessed", "grateful",
            "cheerful", "joyful", "smile", "hope", "dream",
            // Expressions courtes
            "bien joue", "trop bien", "trop cool", "trop beau", "trop belle",
            "je t'aime", "je t aime", "t es beau", "t es belle",
            "tu me plais", "tu es genial", "tu es geniale",
            "keep going", "well done", "good job", "so proud"
    );

    private static final List<String> NEGATIVE_WORDS = Arrays.asList(
            // Français
            "nul", "nulle", "bete", "moche", "decu", "decue", "triste",
            "mauvais", "mauvaise", "pire", "horrible", "deteste", "honte",
            "dommage", "laid", "laide", "ennuyeux", "ennuyeuse", "chiant",
            "chiante", "insupportable", "agacant", "agacante", "enervant",
            "enervante", "desagreable", "decevant", "decevante", "mediocre",
            "pitoyable", "lamentable", "minable", "pathétique", "pathetique",
            "ridicule", "stupide", "idiot", "idiote", "debile",
            "imbecile", "cretin", "cretine", "abruti", "abrutie",
            "faux", "fausse", "hypocrite", "menteur", "menteuse",
            "mechant", "mechante", "cruel", "cruelle", "mesquin", "mesquine",
            "jaloux", "jalouse", "envieux", "envieuse", "arrogant", "arrogante",
            "pretentieux", "pretentieuse", "egoiste", "narcissique",
            "toxique", "manipulateur", "manipulatrice",
            "enerve", "en colere", "furieux", "furieuse", "rage",
            "degoute", "degoutant", "degoutante", "repugnant", "repugnante",
            "inquiet", "inquiete", "anxieux", "anxieuse", "stresse", "stressant",
            "fatigue", "fatiguant", "epuise", "epuisant",
            "echec", "echoue", "rate", "perdu", "perdant", "perdante",
            "ennui", "galere",
            // Anglais
            "ugly", "bad", "worst", "hate", "horrible", "terrible",
            "awful", "disgusting", "annoying", "boring", "stupid",
            "dumb", "idiot", "fool", "foolish", "pathetic",
            "ridiculous", "mean", "cruel", "selfish", "arrogant",
            "fake", "liar", "hypocrite", "toxic", "manipulative",
            "angry", "furious", "sad", "depressed", "disappointed",
            "failure", "loser", "weak", "coward", "jealous",
            "mediocre", "useless", "worthless", "hopeless", "miserable",
            "disgusted", "ashamed", "embarrassing", "creepy", "weird",
            // Expressions courtes
            "tu es nul", "tu es nulle", "t es nul", "t es nulle",
            "tu me degoutes", "je te deteste", "go away", "shut up",
            "so bad", "so ugly", "you suck", "no one likes"
    );

    /**
     * Analyse le sentiment d'un message et retourne POSITIVE, NEGATIVE ou NEUTRAL.
     * Utilise un scoring pondéré : les expressions multi-mots comptent double.
     */
    public String analyzeSentiment(String content) {
        if (content == null || content.isBlank()) {
            return "NEUTRAL";
        }

        String normalizedContent = normalizeText(content);

        int positiveScore = countMatches(normalizedContent, POSITIVE_WORDS);
        int negativeScore = countMatches(normalizedContent, NEGATIVE_WORDS);

        // Les emojis positifs/négatifs influencent aussi le score
        positiveScore += countEmojiSentiment(content, true);
        negativeScore += countEmojiSentiment(content, false);

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
            String normalized = normalizeText(word);
            // Les expressions multi-mots (contenant un espace) comptent double
            boolean isExpression = normalized.contains(" ");
            if (isExpression) {
                if (text.contains(normalized)) {
                    count += 2;
                }
            } else {
                String regex = "(?i)\\b" + Pattern.quote(normalized) + "\\b";
                if (Pattern.compile(regex).matcher(text).find()) {
                    count++;
                }
            }
        }
        return count;
    }

    /**
     * Comptabilise les emojis positifs ou négatifs dans le texte brut.
     */
    private int countEmojiSentiment(String rawText, boolean positive) {
        int count = 0;
        if (positive) {
            String[] positiveEmojis = {
                "\u2764", "\uD83D\uDE0D", "\uD83D\uDE0A", "\uD83D\uDE04", "\uD83D\uDE01",
                "\uD83D\uDE03", "\uD83D\uDE02", "\uD83E\uDD70", "\uD83D\uDC96", "\uD83D\uDC95",
                "\uD83D\uDC4D", "\uD83D\uDC4F", "\uD83C\uDF1F", "\u2B50", "\uD83C\uDF89",
                "\uD83C\uDF8A", "\uD83E\uDD29", "\uD83D\uDE18", "\uD83D\uDE07", "\uD83E\uDD17",
                "\uD83D\uDCAA", "\uD83D\uDD25"
            };
            for (String emoji : positiveEmojis) {
                if (rawText.contains(emoji)) count++;
            }
        } else {
            String[] negativeEmojis = {
                "\uD83D\uDE21", "\uD83D\uDE20", "\uD83D\uDE22", "\uD83D\uDE2D", "\uD83D\uDE24",
                "\uD83D\uDE29", "\uD83D\uDE1E", "\uD83D\uDE1F", "\uD83D\uDE14", "\uD83D\uDE23",
                "\uD83D\uDC4E", "\uD83D\uDE44", "\uD83E\uDD2E", "\uD83D\uDE12", "\uD83D\uDE15",
                "\uD83D\uDC94", "\uD83D\uDE31", "\uD83D\uDE30", "\uD83E\uDD22"
            };
            for (String emoji : negativeEmojis) {
                if (rawText.contains(emoji)) count++;
            }
        }
        return count;
    }

    private String normalizeText(String input) {
        String normalized = Normalizer.normalize(input, Normalizer.Form.NFD);
        return normalized.replaceAll("\\p{M}", "").toLowerCase();
    }
}

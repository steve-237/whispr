package com.whispr.backend.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class AiSentimentServiceTest {

    private AiSentimentService sentimentService;

    @BeforeEach
    void setUp() {
        sentimentService = new AiSentimentService();
    }

    @Test
    void analyzeSentiment_ShouldReturnPositive_ForPositiveWords() {
        assertEquals("POSITIVE", sentimentService.analyzeSentiment("Tu es super et génial, merci pour tout !"));
        assertEquals("POSITIVE", sentimentService.analyzeSentiment("You are amazing and wonderful!"));
        assertEquals("POSITIVE", sentimentService.analyzeSentiment("Je t'aime trop beau ❤️"));
    }

    @Test
    void analyzeSentiment_ShouldReturnNegative_ForNegativeWords() {
        assertEquals("NEGATIVE", sentimentService.analyzeSentiment("C'est nul, moche et décevant."));
        assertEquals("NEGATIVE", sentimentService.analyzeSentiment("You suck, this is horrible and ugly"));
        assertEquals("NEGATIVE", sentimentService.analyzeSentiment("Je te déteste 😡"));
    }

    @Test
    void analyzeSentiment_ShouldReturnNeutral_ForNeutralOrEmptyText() {
        assertEquals("NEUTRAL", sentimentService.analyzeSentiment("Le ciel est bleu aujourd'hui."));
        assertEquals("NEUTRAL", sentimentService.analyzeSentiment(""));
        assertEquals("NEUTRAL", sentimentService.analyzeSentiment(null));
        assertEquals("NEUTRAL", sentimentService.analyzeSentiment("   "));
    }
}

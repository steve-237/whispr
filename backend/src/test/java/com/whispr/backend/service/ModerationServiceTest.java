package com.whispr.backend.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ModerationServiceTest {

    private ModerationService moderationService;

    @BeforeEach
    void setUp() {
        moderationService = new ModerationService();
    }

    @Test
    void isToxic_ShouldReturnTrue_ForBannedWords() {
        assertTrue(moderationService.isToxic("Tu es un gros connard"));
        assertTrue(moderationService.isToxic("Va te faire fdp"));
        assertTrue(moderationService.isToxic("Sale race va mourir"));
        assertTrue(moderationService.isToxic("You are a bitch"));
        assertTrue(moderationService.isToxic("just kill yourself"));
    }

    @Test
    void isToxic_ShouldReturnFalse_ForBenignWordsWithSubstrings() {
        // "réputation" contains "pute" as substring, but word boundary regex must avoid false positives
        assertFalse(moderationService.isToxic("Tu as une excellente réputation professionnelle"));
        assertFalse(moderationService.isToxic("Bonjour, j'adore ton profil et ce que tu fais !"));
        assertFalse(moderationService.isToxic("Quel est ton film préféré ?"));
    }

    @Test
    void isToxic_ShouldReturnFalse_ForEmptyOrNull() {
        assertFalse(moderationService.isToxic(""));
        assertFalse(moderationService.isToxic(null));
        assertFalse(moderationService.isToxic("   "));
    }
}

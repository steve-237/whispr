package com.whispr.backend.service;

import com.whispr.backend.domain.*;
import com.whispr.backend.dto.*;
import com.whispr.backend.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PromptServiceTest {

    @Mock
    private PromptRepository promptRepository;

    @Mock
    private PromptOptionRepository promptOptionRepository;

    @Mock
    private PromptAnswerRepository promptAnswerRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private LinkRepository linkRepository;

    @Mock
    private MessageRepository messageRepository;

    @Mock
    private ModerationService moderationService;

    @InjectMocks
    private PromptService promptService;

    private User mockUser;
    private Link mockLink;

    @BeforeEach
    void setUp() {
        mockUser = User.builder()
                .id(UUID.randomUUID())
                .email("creator@whispr.com")
                .pseudo("creatorVIP")
                .build();

        mockLink = Link.builder()
                .id(UUID.randomUUID())
                .user(mockUser)
                .slug("creatorVIP")
                .isActive(true)
                .build();
    }

    @Test
    void createPrompt_Text_ShouldDeactivatePreviousAndSaveNewPrompt() {
        when(userRepository.findByEmailIgnoreCase("creator@whispr.com")).thenReturn(Optional.of(mockUser));

        Prompt existingActive = Prompt.builder()
                .id(UUID.randomUUID())
                .user(mockUser)
                .question("Ancienne question ?")
                .isActive(true)
                .build();
        when(promptRepository.findByUserIdAndIsActiveTrue(mockUser.getId())).thenReturn(Optional.of(existingActive));

        when(promptRepository.save(any(Prompt.class))).thenAnswer(inv -> {
            Prompt p = inv.getArgument(0);
            if (p.getId() == null) p.setId(UUID.randomUUID());
            return p;
        });

        CreatePromptRequest req = new CreatePromptRequest("Quel est mon pire défaut ?", "TEXT", null, null);
        PromptDto dto = promptService.createPrompt("creator@whispr.com", req);

        assertNotNull(dto);
        assertEquals("Quel est mon pire défaut ?", dto.question());
        assertEquals(PromptType.TEXT, dto.type());
        assertTrue(dto.isActive());
        assertFalse(existingActive.getIsActive());
        verify(promptRepository, atLeast(2)).save(any(Prompt.class));
    }

    @Test
    void createPrompt_Poll_ShouldSavePromptAndOptions() {
        when(userRepository.findByEmailIgnoreCase("creator@whispr.com")).thenReturn(Optional.of(mockUser));
        when(promptRepository.findByUserIdAndIsActiveTrue(mockUser.getId())).thenReturn(Optional.empty());
        when(promptRepository.save(any(Prompt.class))).thenAnswer(inv -> {
            Prompt p = inv.getArgument(0);
            if (p.getId() == null) p.setId(UUID.randomUUID());
            return p;
        });

        CreatePromptRequest req = new CreatePromptRequest(
                "Dois-je me teindre en blond ?",
                "POLL",
                null,
                List.of("Oui carrément !", "Non surtout pas !", "Essaie châtain")
        );

        PromptDto dto = promptService.createPrompt("creator@whispr.com", req);

        assertNotNull(dto);
        assertEquals("Dois-je me teindre en blond ?", dto.question());
        assertEquals(PromptType.POLL, dto.type());
        verify(promptOptionRepository, times(3)).save(any(PromptOption.class));
    }

    @Test
    void createPrompt_Poll_ThrowsWhenLessThanTwoOptions() {
        when(userRepository.findByEmailIgnoreCase("creator@whispr.com")).thenReturn(Optional.of(mockUser));
        when(promptRepository.save(any(Prompt.class))).thenAnswer(inv -> inv.getArgument(0));

        CreatePromptRequest req = new CreatePromptRequest("QCM ?", "POLL", null, List.of("Option 1 seule"));

        assertThrows(ResponseStatusException.class, () ->
                promptService.createPrompt("creator@whispr.com", req)
        );
    }

    @Test
    void respondToPrompt_Poll_ShouldIncrementVoteCount() {
        UUID promptId = UUID.randomUUID();
        Prompt prompt = Prompt.builder()
                .id(promptId)
                .user(mockUser)
                .question("Meilleure couleur ?")
                .type(PromptType.POLL)
                .isActive(true)
                .totalResponses(5)
                .options(new ArrayList<>())
                .build();

        UUID opt1Id = UUID.randomUUID();
        PromptOption opt1 = PromptOption.builder()
                .id(opt1Id)
                .prompt(prompt)
                .optionText("Violet")
                .voteCount(2)
                .displayOrder(0)
                .build();
        prompt.getOptions().add(opt1);

        when(linkRepository.findBySlug("creatorVIP")).thenReturn(Optional.of(mockLink));
        when(promptRepository.findByUserIdAndIsActiveTrue(mockUser.getId())).thenReturn(Optional.of(prompt));
        when(promptAnswerRepository.existsByPromptIdAndHashedIp(promptId, "ip_hash_1")).thenReturn(false);
        when(promptOptionRepository.findById(opt1Id)).thenReturn(Optional.of(opt1));
        when(promptRepository.save(any(Prompt.class))).thenAnswer(inv -> inv.getArgument(0));

        RespondPromptRequest request = new RespondPromptRequest(opt1Id, null, null);
        PromptResponseResultDto result = promptService.respondToPrompt("creatorVIP", request, "ip_hash_1");

        assertTrue(result.success());
        assertEquals(3, opt1.getVoteCount());
        assertEquals(6, prompt.getTotalResponses());
        verify(promptAnswerRepository, times(1)).save(any(PromptAnswer.class));
    }

    @Test
    void respondToPrompt_Rating_ShouldUpdateRatingSumAndCount() {
        UUID promptId = UUID.randomUUID();
        Prompt prompt = Prompt.builder()
                .id(promptId)
                .user(mockUser)
                .question("Note mon style sur 10")
                .type(PromptType.RATING)
                .scaleMax(10)
                .isActive(true)
                .ratingSum(24L)
                .ratingCount(3)
                .totalResponses(3)
                .build();

        when(linkRepository.findBySlug("creatorVIP")).thenReturn(Optional.of(mockLink));
        when(promptRepository.findByUserIdAndIsActiveTrue(mockUser.getId())).thenReturn(Optional.of(prompt));
        when(promptAnswerRepository.existsByPromptIdAndHashedIp(promptId, "ip_hash_2")).thenReturn(false);
        when(promptRepository.save(any(Prompt.class))).thenAnswer(inv -> inv.getArgument(0));

        RespondPromptRequest request = new RespondPromptRequest(null, 8, null);
        PromptResponseResultDto result = promptService.respondToPrompt("creatorVIP", request, "ip_hash_2");

        assertTrue(result.success());
        assertEquals(32L, prompt.getRatingSum());
        assertEquals(4, prompt.getRatingCount());
        assertEquals(4, prompt.getTotalResponses());
        assertEquals(8.0, result.prompt().averageRating());
        verify(promptAnswerRepository, times(1)).save(any(PromptAnswer.class));
    }

    @Test
    void respondToPrompt_Poll_WhenAlreadyVoted_ShouldReturnFriendlyErrorWithoutDuplicate() {
        UUID promptId = UUID.randomUUID();
        Prompt prompt = Prompt.builder()
                .id(promptId)
                .user(mockUser)
                .question("Déjà voté ?")
                .type(PromptType.POLL)
                .isActive(true)
                .build();

        when(linkRepository.findBySlug("creatorVIP")).thenReturn(Optional.of(mockLink));
        when(promptRepository.findByUserIdAndIsActiveTrue(mockUser.getId())).thenReturn(Optional.of(prompt));
        when(promptAnswerRepository.existsByPromptIdAndHashedIp(promptId, "ip_hash_already")).thenReturn(true);

        RespondPromptRequest request = new RespondPromptRequest(UUID.randomUUID(), null, null);
        PromptResponseResultDto result = promptService.respondToPrompt("creatorVIP", request, "ip_hash_already");

        assertFalse(result.success());
        assertTrue(result.message().contains("déjà voté"));
        verify(promptOptionRepository, never()).save(any());
        verify(promptAnswerRepository, never()).save(any());
    }

    @Test
    void deletePrompt_ShouldRemoveWhenOwner() {
        UUID promptId = UUID.randomUUID();
        Prompt prompt = Prompt.builder()
                .id(promptId)
                .user(mockUser)
                .build();

        when(userRepository.findByEmailIgnoreCase("creator@whispr.com")).thenReturn(Optional.of(mockUser));
        when(promptRepository.findById(promptId)).thenReturn(Optional.of(prompt));

        promptService.deletePrompt("creator@whispr.com", promptId);

        verify(promptRepository, times(1)).delete(prompt);
    }
}

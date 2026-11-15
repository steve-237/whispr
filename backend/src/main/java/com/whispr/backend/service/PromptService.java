package com.whispr.backend.service;

import com.whispr.backend.domain.*;
import com.whispr.backend.dto.*;
import com.whispr.backend.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class PromptService {

    private final PromptRepository promptRepository;
    private final PromptOptionRepository promptOptionRepository;
    private final PromptAnswerRepository promptAnswerRepository;
    private final UserRepository userRepository;
    private final LinkRepository linkRepository;
    private final MessageRepository messageRepository;
    private final ModerationService moderationService;

    @Transactional(readOnly = true)
    public List<PromptDto> getUserPrompts(String userEmail) {
        User user = getUserByEmail(userEmail);
        return promptRepository.findByUserIdOrderByCreatedAtDesc(user.getId())
                .stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public Optional<PromptDto> getActivePromptForSlug(String slug) {
        if (slug == null || slug.isBlank()) return Optional.empty();
        String cleanSlug = (slug.startsWith("@") ? slug.substring(1) : slug).trim();
        Link link = linkRepository.findBySlug(cleanSlug)
                .or(() -> linkRepository.findBySlugIgnoreCase(cleanSlug))
                .orElse(null);

        if (link == null || Boolean.FALSE.equals(link.getIsActive())) {
            return Optional.empty();
        }

        return promptRepository.findByUserIdAndIsActiveTrue(link.getUser().getId())
                .map(this::toDto);
    }

    @Transactional
    public PromptDto createPrompt(String userEmail, CreatePromptRequest request) {
        User user = getUserByEmail(userEmail);

        if (request.question() == null || request.question().trim().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "La question du sondage ne peut pas être vide.");
        }

        PromptType type = PromptType.TEXT;
        if (request.type() != null) {
            try {
                type = PromptType.valueOf(request.type().trim().toUpperCase());
            } catch (IllegalArgumentException e) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Type de sondage invalide: " + request.type());
            }
        }

        // Désactiver les anciens prompts actifs pour que le nouveau devienne le prompt courant
        promptRepository.findByUserIdAndIsActiveTrue(user.getId()).ifPresent(active -> {
            active.setIsActive(false);
            promptRepository.save(active);
        });

        int scaleMax = (request.scaleMax() != null && request.scaleMax() > 0) ? request.scaleMax() : 10;

        Prompt prompt = Prompt.builder()
                .user(user)
                .question(request.question().trim())
                .type(type)
                .isActive(true)
                .scaleMax(type == PromptType.RATING ? scaleMax : null)
                .totalResponses(0)
                .ratingSum(0L)
                .ratingCount(0)
                .build();

        prompt = promptRepository.save(prompt);

        if (type == PromptType.POLL) {
            List<String> options = request.options();
            if (options == null || options.stream().filter(o -> o != null && !o.isBlank()).count() < 2) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Un QCM requiert au moins 2 options.");
            }

            int order = 0;
            for (String optText : options) {
                if (optText != null && !optText.isBlank()) {
                    PromptOption option = PromptOption.builder()
                            .prompt(prompt)
                            .optionText(optText.trim())
                            .voteCount(0)
                            .displayOrder(order++)
                            .build();
                    promptOptionRepository.save(option);
                    prompt.getOptions().add(option);
                }
            }
        }

        return toDto(prompt);
    }

    @Transactional
    public PromptDto togglePromptStatus(String userEmail, UUID promptId, boolean active) {
        User user = getUserByEmail(userEmail);
        Prompt prompt = promptRepository.findById(promptId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Sondage introuvable"));

        if (!prompt.getUser().getId().equals(user.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Action non autorisée.");
        }

        if (active) {
            // Désactiver tous les autres
            promptRepository.findByUserIdAndIsActiveTrue(user.getId()).ifPresent(p -> {
                p.setIsActive(false);
                promptRepository.save(p);
            });
            prompt.setIsActive(true);
        } else {
            prompt.setIsActive(false);
        }

        return toDto(promptRepository.save(prompt));
    }

    @Transactional
    public void deletePrompt(String userEmail, UUID promptId) {
        User user = getUserByEmail(userEmail);
        Prompt prompt = promptRepository.findById(promptId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Sondage introuvable"));

        if (!prompt.getUser().getId().equals(user.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Action non autorisée.");
        }

        promptRepository.delete(prompt);
    }

    @Transactional
    public PromptResponseResultDto respondToPrompt(String slug, RespondPromptRequest request, String hashedIp) {
        if (slug == null || slug.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Slug manquant.");
        }
        String cleanSlug = (slug.startsWith("@") ? slug.substring(1) : slug).trim();
        Link link = linkRepository.findBySlug(cleanSlug)
                .or(() -> linkRepository.findBySlugIgnoreCase(cleanSlug))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Profil introuvable"));

        Prompt prompt = promptRepository.findByUserIdAndIsActiveTrue(link.getUser().getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Aucun sondage actif sur ce profil."));

        // Anti-spam vote unique pour POLL et RATING
        if (hashedIp != null && promptAnswerRepository.existsByPromptIdAndHashedIp(prompt.getId(), hashedIp)) {
            if (prompt.getType() == PromptType.POLL || prompt.getType() == PromptType.RATING) {
                return new PromptResponseResultDto(false, "Vous avez déjà voté pour ce sondage !", toDto(prompt));
            }
        }

        PromptOption selectedOption = null;

        if (prompt.getType() == PromptType.POLL) {
            if (request.selectedOptionId() == null) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Veuillez sélectionner une option.");
            }
            selectedOption = promptOptionRepository.findById(request.selectedOptionId())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Option invalide."));

            if (!selectedOption.getPrompt().getId().equals(prompt.getId())) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Option n'appartenant pas à ce sondage.");
            }

            selectedOption.setVoteCount(selectedOption.getVoteCount() + 1);
            promptOptionRepository.save(selectedOption);
            prompt.setTotalResponses(prompt.getTotalResponses() + 1);
        } else if (prompt.getType() == PromptType.RATING) {
            Integer rating = request.ratingValue();
            int max = prompt.getScaleMax() != null ? prompt.getScaleMax() : 10;
            if (rating == null || rating < 1 || rating > max) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Note invalide (doit être entre 1 et " + max + ").");
            }
            prompt.setRatingSum(prompt.getRatingSum() + rating);
            prompt.setRatingCount(prompt.getRatingCount() + 1);
            prompt.setTotalResponses(prompt.getTotalResponses() + 1);
        } else if (prompt.getType() == PromptType.TEXT) {
            String answer = request.textAnswer();
            if (answer == null || answer.trim().isEmpty()) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Votre réponse ne peut pas être vide.");
            }
            if (moderationService != null && moderationService.isToxic(answer)) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Votre réponse enfreint nos règles de bienveillance.");
            }
            prompt.setTotalResponses(prompt.getTotalResponses() + 1);

            // Créer également un message dans l'inbox du créateur
            Message message = Message.builder()
                    .link(link)
                    .content("🎯 [Sondage : " + prompt.getQuestion() + "]\n" + answer.trim())
                    .type("PROMPT_ANSWER")
                    .status("DELIVERED")
                    .isRead(false)
                    .aiCategory("PROMPT")
                    .build();
            messageRepository.save(message);
        }

        // Enregistrer la réponse
        PromptAnswer answerEntity = PromptAnswer.builder()
                .prompt(prompt)
                .selectedOption(selectedOption)
                .ratingValue(request.ratingValue())
                .textAnswer(request.textAnswer() != null ? request.textAnswer().trim() : null)
                .hashedIp(hashedIp)
                .build();
        promptAnswerRepository.save(answerEntity);

        Prompt savedPrompt = promptRepository.save(prompt);

        return new PromptResponseResultDto(true, "Votre réponse a été enregistrée avec succès !", toDto(savedPrompt));
    }

    public PromptDto toDto(Prompt prompt) {
        List<PromptOptionDto> optionDtos = Collections.emptyList();
        int totalVotes = prompt.getOptions().stream().mapToInt(PromptOption::getVoteCount).sum();

        if (prompt.getOptions() != null) {
            optionDtos = prompt.getOptions().stream()
                    .map(opt -> {
                        double pct = (totalVotes > 0) ? Math.round((opt.getVoteCount() * 100.0 / totalVotes) * 10.0) / 10.0 : 0.0;
                        return new PromptOptionDto(
                                opt.getId(),
                                opt.getOptionText(),
                                opt.getVoteCount(),
                                opt.getDisplayOrder(),
                                pct
                        );
                    })
                    .collect(Collectors.toList());
        }

        Double avgRating = null;
        if (prompt.getRatingCount() != null && prompt.getRatingCount() > 0) {
            avgRating = Math.round((prompt.getRatingSum() * 1.0 / prompt.getRatingCount()) * 10.0) / 10.0;
        }

        return new PromptDto(
                prompt.getId(),
                prompt.getQuestion(),
                prompt.getType(),
                prompt.getIsActive(),
                prompt.getScaleMax(),
                prompt.getTotalResponses(),
                avgRating,
                prompt.getRatingCount(),
                optionDtos,
                prompt.getCreatedAt()
        );
    }

    private User getUserByEmail(String email) {
        return userRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Utilisateur introuvable"));
    }
}

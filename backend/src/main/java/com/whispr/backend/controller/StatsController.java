package com.whispr.backend.controller;

import com.whispr.backend.domain.Link;
import com.whispr.backend.domain.Message;
import com.whispr.backend.domain.User;
import com.whispr.backend.dto.StatsDto;
import com.whispr.backend.repository.LinkRepository;
import com.whispr.backend.service.MessageService;
import com.whispr.backend.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/stats")
@RequiredArgsConstructor
public class StatsController {

    private final UserService userService;
    private final LinkRepository linkRepository;
    private final MessageService messageService;

    @GetMapping("/my")
    public ResponseEntity<StatsDto> getMyStats(Authentication authentication) {
        String email = authentication.getName();
        User user = userService.getUserByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

        Link link = linkRepository.findByUserId(user.getId())
                .orElseThrow(() -> new RuntimeException("Link not found"));

        List<Message> messages = messageService.getMessagesForLink(link.getId());

        long total = messages.size();
        long positiveCount = messages.stream().filter(m -> "POSITIVE".equals(m.getAiCategory())).count();
        long negativeCount = messages.stream().filter(m -> "NEGATIVE".equals(m.getAiCategory())).count();
        long neutralCount = total - positiveCount - negativeCount;

        String aiSummary;
        if (total == 0) {
            aiSummary = "Votre boîte de réception est vide. Partagez votre lien !";
        } else if (positiveCount > negativeCount * 2) {
            aiSummary = "L'ambiance est excellente ! Vos amis vous adorent et sont très positifs.";
        } else if (negativeCount > positiveCount) {
            aiSummary = "Il y a un peu de tension... N'hésitez pas à activer la modération stricte si besoin.";
        } else {
            aiSummary = "Beaucoup de messages neutres. Les gens sont curieux d'en savoir plus sur vous !";
        }

        StatsDto stats = new StatsDto(total, positiveCount, neutralCount, negativeCount, aiSummary);
        return ResponseEntity.ok(stats);
    }
}

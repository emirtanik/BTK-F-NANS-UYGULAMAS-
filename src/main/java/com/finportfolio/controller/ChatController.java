package com.finportfolio.controller;

import com.finportfolio.exception.BusinessException;
import org.springframework.http.HttpStatus;
import com.finportfolio.dto.ChatMessageRequest;
import com.finportfolio.dto.ChatResponse;
import com.finportfolio.dto.WelcomeChatResponse;
import com.finportfolio.security.JwtService;
import com.finportfolio.service.ChatService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/chat")
@RequiredArgsConstructor
@io.swagger.v3.oas.annotations.tags.Tag(name = "4. AI Asistan", description = "Gemini destekli portföy asistanı")
public class ChatController {

    private final ChatService chatService;
    private final JwtService jwtService;

    /**
     * Login sonrası gösterilen karşılama özeti
     * (son girişten beri kar/zarar listesi + bot mesajı)
     */
    @GetMapping("/welcome")
    public ResponseEntity<WelcomeChatResponse> welcome(HttpServletRequest request) {
        Long userId = extractUserId(request);
        return ResponseEntity.ok(chatService.welcome(userId));
    }

    /**
     * Serbest sohbet (sohbet asistanı)
     */
    @PostMapping("/message")
    public ResponseEntity<ChatResponse> chat(
            @Valid @RequestBody ChatMessageRequest request,
            HttpServletRequest httpRequest
    ) {
        Long userId = extractUserId(httpRequest);
        return ResponseEntity.ok(chatService.chat(userId, request.message()));
    }

    /**
     * Danışman karşılama özeti
     */
    @GetMapping("/advice/welcome")
    public ResponseEntity<ChatResponse> adviceWelcome(HttpServletRequest request) {
        Long userId = extractUserId(request);
        return ResponseEntity.ok(chatService.adviceWelcome(userId));
    }

    /**
     * Danışmana soru sor (akıl veren asistan)
     */
    @PostMapping("/advice")
    public ResponseEntity<ChatResponse> advise(
            @Valid @RequestBody ChatMessageRequest request,
            HttpServletRequest httpRequest
    ) {
        Long userId = extractUserId(httpRequest);
        return ResponseEntity.ok(chatService.advise(userId, request.message()));
    }

    private Long extractUserId(HttpServletRequest request) {
        String authHeader = request.getHeader("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            return jwtService.extractUserId(authHeader.substring(7));
        }
        throw new BusinessException("Authorization header eksik", HttpStatus.UNAUTHORIZED);
    }
}
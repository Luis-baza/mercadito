package com.uniremington.marketplace.controller.res;

import com.uniremington.marketplace.dto.ChatRequest;
import com.uniremington.marketplace.dto.ChatResponse;
import com.uniremington.marketplace.service.ChatRateLimiter;
import com.uniremington.marketplace.service.GeminiChatResult;
import com.uniremington.marketplace.service.GeminiChatService;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/chat")
public class ChatRestController {

    private static final String INTERACTION_ID_SESSION_KEY =
            ChatRestController.class.getName() + ".interactionId";

    private final GeminiChatService chatService;
    private final ChatRateLimiter rateLimiter;

    public ChatRestController(GeminiChatService chatService, ChatRateLimiter rateLimiter) {
        this.chatService = chatService;
        this.rateLimiter = rateLimiter;
    }

    @PostMapping("/mensaje")
    public ChatResponse mensaje(@Valid @RequestBody ChatRequest request, HttpSession session) {
        rateLimiter.check(session);
        String previousId = (String) session.getAttribute(INTERACTION_ID_SESSION_KEY);
        GeminiChatResult result = chatService.responder(request.mensaje(), previousId);
        session.setAttribute(INTERACTION_ID_SESSION_KEY, result.interactionId());
        return new ChatResponse(result.respuesta());
    }

    @PostMapping("/reiniciar")
    public ResponseEntity<Void> reiniciar(HttpSession session) {
        session.removeAttribute(INTERACTION_ID_SESSION_KEY);
        return ResponseEntity.noContent().build();
    }
}

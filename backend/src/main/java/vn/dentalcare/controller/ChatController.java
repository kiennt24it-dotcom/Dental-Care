package vn.dentalcare.controller;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.web.bind.annotation.*;
import vn.dentalcare.dto.ApiResponse;
import vn.dentalcare.dto.ChatDtos.ContactResponse;
import vn.dentalcare.dto.ChatDtos.MessageResponse;
import vn.dentalcare.dto.ChatDtos.SendMessageRequest;
import vn.dentalcare.repository.UserRepository;
import vn.dentalcare.service.ChatService;

import java.util.List;

@RestController
@RequestMapping("/api/chat")
@PreAuthorize("isAuthenticated()")
public class ChatController {
    private final ChatService chat;
    private final UserRepository users;
    private final SimpMessagingTemplate messaging;

    public ChatController(ChatService chat, UserRepository users, SimpMessagingTemplate messaging) {
        this.chat = chat;
        this.users = users;
        this.messaging = messaging;
    }

    public record MessageRequest(@NotNull Long recipientId, @NotBlank @Size(max = 4000) String content) {}

    @GetMapping("/contacts")
    public ApiResponse<List<ContactResponse>> contacts(
        @AuthenticationPrincipal UserDetails principal,
        @RequestParam(defaultValue = "") String search) {
        return ApiResponse.ok(chat.contacts(principal.getUsername(), search));
    }

    @GetMapping("/conversations/{contactId}")
    public ApiResponse<List<MessageResponse>> conversation(
        @AuthenticationPrincipal UserDetails principal, @PathVariable Long contactId) {
        return ApiResponse.ok(chat.conversation(principal.getUsername(), contactId));
    }

    @PostMapping("/messages")
    public ApiResponse<MessageResponse> send(
        @AuthenticationPrincipal UserDetails principal,
        @Valid @RequestBody MessageRequest request) {
        SendMessageRequest message = new SendMessageRequest(request.recipientId(), request.content());
        MessageResponse sent = chat.send(principal.getUsername(), message.recipientId(), message.content());
        String recipientEmail = users.findById(message.recipientId())
            .orElseThrow(() -> new IllegalStateException("Chat recipient disappeared after message persistence"))
            .getEmail();
        messaging.convertAndSendToUser(recipientEmail, "/queue/messages", sent);
        messaging.convertAndSendToUser(principal.getUsername(), "/queue/messages", sent);
        return ApiResponse.ok(sent);
    }

    @PatchMapping("/conversations/{contactId}/read")
    public ApiResponse<Integer> markRead(
        @AuthenticationPrincipal UserDetails principal, @PathVariable Long contactId) {
        return ApiResponse.ok(chat.markRead(principal.getUsername(), contactId));
    }
}

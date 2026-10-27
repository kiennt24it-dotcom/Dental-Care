package vn.dentalcare.dto;

import vn.dentalcare.entity.Role;

import java.time.Instant;

public final class ChatDtos {
    private ChatDtos() {}

    public record ContactResponse(Long id, String fullName, String avatarUrl, Role role,
                                  String lastMessage, Instant lastMessageAt,
                                  long unreadCount) {}

    public record MessageResponse(Long id, Long senderId, String senderName,
                                  Long recipientId, String recipientName, String content,
                                  Instant createdAt, Instant readAt) {}

    public record SendMessageRequest(Long recipientId, String content) {}
}

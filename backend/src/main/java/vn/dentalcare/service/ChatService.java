package vn.dentalcare.service;

import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.dentalcare.dto.ChatDtos.ContactResponse;
import vn.dentalcare.dto.ChatDtos.MessageResponse;
import vn.dentalcare.entity.ChatMessage;
import vn.dentalcare.entity.Role;
import vn.dentalcare.entity.User;
import vn.dentalcare.exception.ApiException;
import vn.dentalcare.repository.ChatMessageRepository;
import vn.dentalcare.repository.UserRepository;

import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Set;

@Service
public class ChatService {
    private static final int MAX_CONVERSATION_MESSAGES = 100;
    private static final Set<Role> STAFF_ROLES = Set.of(Role.RECEPTIONIST, Role.DOCTOR, Role.ADMIN);

    private final ChatMessageRepository messages;
    private final UserRepository users;
    private final NotificationService notifications;

    public ChatService(ChatMessageRepository messages, UserRepository users, NotificationService notifications) {
        this.messages = messages;
        this.users = users;
        this.notifications = notifications;
    }

    @Transactional(readOnly = true)
    public List<ContactResponse> contacts(String email, String search) {
        User caller = user(email);
        String term = search == null ? "" : search.trim().toLowerCase(java.util.Locale.ROOT);
        return users.findByActiveTrue().stream()
            .filter(contact -> !contact.getId().equals(caller.getId()))
            .filter(contact -> caller.getRole() != Role.PATIENT
                || STAFF_ROLES.contains(contact.getRole()))
            .filter(contact -> term.isEmpty()
                || contact.getFullName().toLowerCase(java.util.Locale.ROOT).contains(term)
                || contact.getEmail().toLowerCase(java.util.Locale.ROOT).contains(term))
            .map(contact -> contactResponse(caller, contact))
            .sorted(Comparator.comparing(ContactResponse::lastMessageAt,
                    Comparator.nullsLast(Comparator.reverseOrder()))
                .thenComparing(ContactResponse::fullName, String.CASE_INSENSITIVE_ORDER))
            .toList();
    }

    @Transactional(readOnly = true)
    public List<MessageResponse> conversation(String email, Long contactId) {
        User caller = user(email);
        User contact = contact(caller, contactId);
        List<ChatMessage> recent = messages.findConversation(
            caller.getId(), contact.getId(), PageRequest.of(0, MAX_CONVERSATION_MESSAGES));
        return recent.stream().sorted(Comparator.comparing(ChatMessage::getCreatedAt))
            .map(ChatService::messageResponse).toList();
    }

    @Transactional
    public MessageResponse send(String email, Long recipientId, String content) {
        User sender = user(email);
        User recipient = contact(sender, recipientId);
        String normalized = content == null ? "" : content.trim();
        if (normalized.isEmpty() || normalized.length() > 4000) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Tin nhắn phải có từ 1 đến 4000 ký tự");
        }
        ChatMessage saved = messages.save(new ChatMessage(sender, recipient, normalized));
        notifications.create(recipient, "Tin nhắn mới",
            sender.getFullName() + " (" + roleLabel(sender.getRole()) + ") đã gửi cho bạn một tin nhắn.",
            "/app/chat?contactId=" + sender.getId());
        return messageResponse(saved);
    }

    @Transactional
    public int markRead(String email, Long contactId) {
        User recipient = user(email);
        User sender = contact(recipient, contactId);
        return messages.markConversationRead(sender.getId(), recipient.getId(), Instant.now());
    }

    private ContactResponse contactResponse(User caller, User contact) {
        List<ChatMessage> latest = messages.findConversation(
            caller.getId(), contact.getId(), PageRequest.of(0, 1));
        ChatMessage last = latest.isEmpty() ? null : latest.get(0);
        return new ContactResponse(contact.getId(), contact.getFullName(), contact.getAvatarUrl(),
            contact.getRole(), last == null ? null : last.getContent(),
            last == null ? null : last.getCreatedAt(),
            messages.countBySender_IdAndRecipient_IdAndReadAtIsNull(contact.getId(), caller.getId()));
    }

    private User contact(User caller, Long contactId) {
        if (contactId == null || caller.getId().equals(contactId)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Người nhận tin nhắn không hợp lệ");
        }
        User contact = users.findById(contactId)
            .filter(User::isActive)
            .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Không tìm thấy người nhận"));
        if (caller.getRole() == Role.PATIENT && !STAFF_ROLES.contains(contact.getRole())) {
            throw new ApiException(HttpStatus.FORBIDDEN, "Bệnh nhân chỉ có thể nhắn tin với đội ngũ phòng khám");
        }
        return contact;
    }

    private User user(String email) {
        return users.findByEmailIgnoreCase(email)
            .filter(User::isActive)
            .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Không tìm thấy tài khoản"));
    }

    private static MessageResponse messageResponse(ChatMessage message) {
        return new MessageResponse(message.getId(), message.getSender().getId(),
            message.getSender().getFullName(), message.getRecipient().getId(),
            message.getRecipient().getFullName(), message.getContent(),
            message.getCreatedAt(), message.getReadAt());
    }

    private static String roleLabel(Role role) {
        return switch (role) {
            case PATIENT -> "Bệnh nhân";
            case RECEPTIONIST -> "Lễ tân";
            case DOCTOR -> "Bác sĩ";
            case ADMIN -> "Quản trị viên";
        };
    }
}

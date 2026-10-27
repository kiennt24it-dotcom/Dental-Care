package vn.dentalcare.entity;

import jakarta.persistence.*;

import java.time.Instant;

@Entity
@Table(name = "chat_messages", indexes = {
    @Index(name = "idx_chat_sender_recipient_created", columnList = "sender_id,recipient_id,created_at"),
    @Index(name = "idx_chat_recipient_read", columnList = "recipient_id,read_at")
})
public class ChatMessage {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "sender_id", nullable = false)
    private User sender;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "recipient_id", nullable = false)
    private User recipient;

    @Column(nullable = false, length = 4000)
    private String content;

    @Column(nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    private Instant readAt;

    protected ChatMessage() {}

    public ChatMessage(User sender, User recipient, String content) {
        this.sender = sender;
        this.recipient = recipient;
        this.content = content;
    }

    public Long getId() { return id; }
    public User getSender() { return sender; }
    public User getRecipient() { return recipient; }
    public String getContent() { return content; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getReadAt() { return readAt; }
    public void markRead(Instant at) { if (readAt == null) readAt = at; }
}

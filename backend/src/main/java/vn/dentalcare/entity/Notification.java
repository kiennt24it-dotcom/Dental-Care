package vn.dentalcare.entity;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "notifications", indexes = @Index(name = "idx_notification_user_read", columnList = "user_id,read_at"))
public class Notification {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "user_id", nullable = false) private User user;
    @Column(nullable = false, length = 160) private String title;
    @Column(nullable = false, length = 1000) private String message;
    @Column(length = 500) private String actionPath;
    @Column(nullable = false, updatable = false) private Instant createdAt = Instant.now();
    private Instant readAt;
    protected Notification() {}
    public Notification(User user, String title, String message) { this(user,title,message,null); }
    public Notification(User user, String title, String message,String actionPath) {
        this.user=user; this.title=title; this.message=message; this.actionPath=actionPath;
    }
    public Long getId() { return id; }
    public User getUser() { return user; }
    public String getTitle() { return title; }
    public String getMessage() { return message; }
    public String getActionPath() { return actionPath; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getReadAt() { return readAt; }
    public void markRead(Instant at) { if (readAt == null) readAt=at; }
}

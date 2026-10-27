package vn.dentalcare.entity;
import jakarta.persistence.*;
import java.time.Instant;
@Entity
@Table(name="password_reset_tokens", indexes=@Index(name="idx_password_reset_token", columnList="token", unique=true))
public class PasswordResetToken {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
    @ManyToOne(fetch=FetchType.LAZY, optional=false) @JoinColumn(name="user_id", nullable=false) private User user;
    @Column(nullable=false, unique=true, length=80) private String token;
    @Column(nullable=false) private Instant expiresAt;
    protected PasswordResetToken() {}
    public PasswordResetToken(User user, String token, Instant expiresAt) { this.user=user; this.token=token; this.expiresAt=expiresAt; }
    public Long getId() { return id; }
    public User getUser() { return user; }
    public Instant getExpiresAt() { return expiresAt; }
}

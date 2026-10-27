package vn.dentalcare.entity;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "users", indexes = @Index(name = "idx_users_email", columnList = "email", unique = true))
public class User {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false, length = 120)
    private String fullName;
    @Column(nullable = false, unique = true, length = 190)
    private String email;
    @Column(nullable = false)
    private String passwordHash;
    @Column(length = 30)
    private String phone;
    @Column(length = 500)
    private String avatarUrl;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 20)
    private Role role;
    @Column(nullable = false)
    private boolean active = true;
    @Column(nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    protected User() {}
    public User(String fullName, String email, String passwordHash, Role role) {
        this.fullName = fullName; this.email = email.toLowerCase(); this.passwordHash = passwordHash; this.role = role;
    }
    public Long getId() { return id; }
    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }
    public String getEmail() { return email; }
    public String getPasswordHash() { return passwordHash; }
    public void setPasswordHash(String passwordHash) { this.passwordHash = passwordHash; }
    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }
    public String getAvatarUrl() { return avatarUrl; }
    public void setAvatarUrl(String avatarUrl) { this.avatarUrl = avatarUrl; }
    public Role getRole() { return role; }
    public void setRole(Role role) { this.role = role; }
    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
    public Instant getCreatedAt() { return createdAt; }
}

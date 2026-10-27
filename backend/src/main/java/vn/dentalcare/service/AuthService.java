package vn.dentalcare.service;

import vn.dentalcare.dto.AuthDtos.*;
import vn.dentalcare.entity.*;
import vn.dentalcare.exception.ApiException;
import vn.dentalcare.repository.*;
import vn.dentalcare.security.JwtService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.Instant;
import java.util.UUID;

@Service
public class AuthService {
    private final UserRepository users;
    private final RefreshTokenRepository refreshTokens;
    private final PasswordResetTokenRepository resetTokens;
    private final PasswordEncoder encoder;
    private final JwtService jwt;
    private final EmailService email;
    private final long refreshMs;
    private final String frontendUrl;
    public AuthService(UserRepository users, RefreshTokenRepository refreshTokens, PasswordResetTokenRepository resetTokens,
                       PasswordEncoder encoder, JwtService jwt, EmailService email,
                       @Value("${app.jwt.refresh-expiration-ms}") long refreshMs, @Value("${app.frontend-url}") String frontendUrl) {
        this.users=users; this.refreshTokens=refreshTokens; this.resetTokens=resetTokens; this.encoder=encoder;
        this.jwt=jwt; this.email=email; this.refreshMs=refreshMs; this.frontendUrl=frontendUrl;
    }
    @Transactional
    public AuthResponse register(RegisterRequest request) {
        if (users.existsByEmailIgnoreCase(request.email())) throw new ApiException(HttpStatus.CONFLICT, "Email đã được sử dụng");
        User user = new User(request.fullName().trim(), request.email().trim(), encoder.encode(request.password()), Role.PATIENT);
        user.setPhone(request.phone());
        users.save(user);
        email.send(user.getEmail(), "Chào mừng đến với DentalCare", "Xin chào " + user.getFullName() + ", tài khoản DentalCare của bạn đã được tạo.");
        return issue(user);
    }
    @Transactional
    public AuthResponse login(LoginRequest request) {
        User user = users.findByEmailIgnoreCase(request.email())
            .filter(User::isActive).orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "Email hoặc mật khẩu không chính xác"));
        if (!encoder.matches(request.password(), user.getPasswordHash())) throw new ApiException(HttpStatus.UNAUTHORIZED, "Email hoặc mật khẩu không chính xác");
        return issue(user);
    }
    @Transactional
    public AuthResponse refresh(String token) {
        RefreshToken old = refreshTokens.findByToken(token).orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "Refresh token không hợp lệ"));
        if (old.getExpiresAt().isBefore(Instant.now()) || !old.getUser().isActive()) {
            refreshTokens.delete(old); throw new ApiException(HttpStatus.UNAUTHORIZED, "Refresh token đã hết hạn");
        }
        User user = old.getUser();
        refreshTokens.delete(old);
        return issue(user);
    }
    @Transactional
    public void logout(String token) { refreshTokens.deleteByToken(token); }
    @Transactional
    public void requestPasswordReset(String emailAddress) {
        users.findByEmailIgnoreCase(emailAddress).filter(User::isActive).ifPresent(user -> {
            resetTokens.deleteByUser_Id(user.getId());
            String token = UUID.randomUUID().toString();
            resetTokens.save(new PasswordResetToken(user, token, Instant.now().plusSeconds(1800)));
            email.send(user.getEmail(), "Đặt lại mật khẩu DentalCare",
                "Mở liên kết sau để đặt lại mật khẩu (có hiệu lực trong 30 phút): " + frontendUrl + "/reset-password?token=" + token);
        });
    }
    @Transactional
    public void resetPassword(ResetPasswordRequest request) {
        PasswordResetToken token = resetTokens.findByToken(request.token())
            .orElseThrow(() -> new ApiException(HttpStatus.BAD_REQUEST, "Liên kết đặt lại mật khẩu không hợp lệ"));
        if (token.getExpiresAt().isBefore(Instant.now())) {
            resetTokens.delete(token); throw new ApiException(HttpStatus.BAD_REQUEST, "Liên kết đặt lại mật khẩu đã hết hạn");
        }
        User user=token.getUser();
        user.setPasswordHash(encoder.encode(request.password()));
        users.save(user);
        resetTokens.delete(token);
        refreshTokens.deleteAll(refreshTokens.findAll().stream().filter(item -> item.getUser().getId().equals(user.getId())).toList());
    }
    @Transactional(readOnly=true)
    public UserResponse currentUser(String emailAddress) {
        return users.findByEmailIgnoreCase(emailAddress).map(AuthService::response)
            .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Không tìm thấy tài khoản"));
    }
    @Transactional
    public AuthResponse oauthLogin(String fullName, String emailAddress, String avatarUrl) {
        User user=users.findByEmailIgnoreCase(emailAddress).orElseGet(() -> {
            User created=new User(fullName,emailAddress,encoder.encode(UUID.randomUUID().toString()),Role.PATIENT);
            created.setAvatarUrl(avatarUrl);
            return users.save(created);
        });
        if (!user.isActive()) throw new ApiException(HttpStatus.FORBIDDEN,"Tài khoản đã bị vô hiệu hóa");
        if (user.getAvatarUrl()==null && avatarUrl!=null) user.setAvatarUrl(avatarUrl);
        return issue(user);
    }
    private AuthResponse issue(User user) {
        String refresh = UUID.randomUUID().toString();
        refreshTokens.save(new RefreshToken(user, refresh, Instant.now().plusMillis(refreshMs)));
        return new AuthResponse(jwt.createAccessToken(user), refresh, response(user));
    }
    public static UserResponse response(User user) {
        return new UserResponse(user.getId(), user.getFullName(), user.getEmail(), user.getPhone(), user.getAvatarUrl(), user.getRole().name());
    }
}

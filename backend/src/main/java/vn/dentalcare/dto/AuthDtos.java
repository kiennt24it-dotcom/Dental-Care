package vn.dentalcare.dto;
import jakarta.validation.constraints.*;
public final class AuthDtos {
    private AuthDtos() {}
    public record RegisterRequest(@NotBlank @Size(max=120) String fullName, @NotBlank @Email @Size(max=190) String email,
                                  @NotBlank @Size(min=8, max=72) String password, @Size(max=30) String phone) {}
    public record LoginRequest(@NotBlank @Email String email, @NotBlank String password) {}
    public record RefreshRequest(@NotBlank String refreshToken) {}
    public record AuthResponse(String accessToken, String refreshToken, UserResponse user) {}
    public record UserResponse(Long id, String fullName, String email, String phone, String avatarUrl, String role) {}
    public record ForgotPasswordRequest(@NotBlank @Email String email) {}
    public record ResetPasswordRequest(@NotBlank String token, @NotBlank @Size(min=8,max=72) String password) {}
}

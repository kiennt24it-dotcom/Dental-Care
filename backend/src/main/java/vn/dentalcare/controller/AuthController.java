package vn.dentalcare.controller;
import vn.dentalcare.dto.*;
import vn.dentalcare.dto.AuthDtos.*;
import vn.dentalcare.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    private final AuthService auth;
    public AuthController(AuthService auth) { this.auth=auth; }
    @PostMapping("/register") public ApiResponse<AuthResponse> register(@Valid @RequestBody RegisterRequest request) { return ApiResponse.ok(auth.register(request)); }
    @PostMapping("/login") public ApiResponse<AuthResponse> login(@Valid @RequestBody LoginRequest request) { return ApiResponse.ok(auth.login(request)); }
    @PostMapping("/refresh") public ApiResponse<AuthResponse> refresh(@Valid @RequestBody RefreshRequest request) { return ApiResponse.ok(auth.refresh(request.refreshToken())); }
    @PostMapping("/logout") public ApiResponse<Void> logout(@Valid @RequestBody RefreshRequest request) { auth.logout(request.refreshToken()); return ApiResponse.ok(null); }
    @PostMapping("/forgot-password") public ApiResponse<Void> forgot(@Valid @RequestBody ForgotPasswordRequest request) { auth.requestPasswordReset(request.email()); return ApiResponse.ok(null); }
    @PostMapping("/reset-password") public ApiResponse<Void> reset(@Valid @RequestBody ResetPasswordRequest request) { auth.resetPassword(request); return ApiResponse.ok(null); }
    @GetMapping("/me") public ApiResponse<UserResponse> me(@AuthenticationPrincipal UserDetails principal) {
        return ApiResponse.ok(auth.currentUser(principal.getUsername()));
    }
}

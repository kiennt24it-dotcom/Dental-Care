package vn.dentalcare.controller;
import vn.dentalcare.dto.*;
import vn.dentalcare.dto.AuthDtos.UserResponse;
import vn.dentalcare.dto.UserDtos.*;
import vn.dentalcare.entity.*;
import vn.dentalcare.exception.ApiException;
import vn.dentalcare.repository.UserRepository;
import vn.dentalcare.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.http.MediaType;
import java.util.List;
@RestController
@RequestMapping("/api/users")
public class UserController {
    private final UserRepository users;
    private final vn.dentalcare.service.AvatarStorageService avatars;
    public UserController(UserRepository users, vn.dentalcare.service.AvatarStorageService avatars) { this.users=users; this.avatars=avatars; }
    @GetMapping("/me") public ApiResponse<UserResponse> me(@AuthenticationPrincipal UserDetails principal) {
        return ApiResponse.ok(users.findByEmailIgnoreCase(principal.getUsername()).map(AuthService::response)
            .orElseThrow(()->new ApiException(HttpStatus.NOT_FOUND,"Không tìm thấy tài khoản")));
    }
    @PutMapping("/me") public ApiResponse<UserResponse> update(@Valid @RequestBody UpdateProfileRequest request,@AuthenticationPrincipal UserDetails principal) {
        User user=users.findByEmailIgnoreCase(principal.getUsername()).orElseThrow(()->new ApiException(HttpStatus.NOT_FOUND,"Không tìm thấy tài khoản"));
        user.setFullName(request.fullName().trim()); user.setPhone(request.phone()); user.setAvatarUrl(request.avatarUrl());
        return ApiResponse.ok(AuthService.response(users.save(user)));
    }
    @PostMapping(value="/me/avatar",consumes=MediaType.MULTIPART_FORM_DATA_VALUE)
    public ApiResponse<UserResponse> uploadAvatar(@RequestParam("file") MultipartFile file,@AuthenticationPrincipal UserDetails principal) {
        User user=users.findByEmailIgnoreCase(principal.getUsername()).orElseThrow(()->new ApiException(HttpStatus.NOT_FOUND,"Không tìm thấy tài khoản"));
        user.setAvatarUrl(avatars.store(user.getId(),file));
        return ApiResponse.ok(AuthService.response(users.save(user)));
    }
    @GetMapping("/patients") public ApiResponse<List<PatientResponse>> patients(@RequestParam(defaultValue="") String search,@AuthenticationPrincipal UserDetails principal) {
        User caller=users.findByEmailIgnoreCase(principal.getUsername()).orElseThrow(()->new ApiException(HttpStatus.NOT_FOUND,"Không tìm thấy tài khoản"));
        if(caller.getRole()!=Role.RECEPTIONIST) throw new ApiException(HttpStatus.FORBIDDEN,"Chỉ lễ tân mới được tìm bệnh nhân để tạo lịch hẹn");
        List<User> patients=search.isBlank()?users.findByRoleAndActiveTrue(Role.PATIENT):users.findByRoleAndActiveTrueAndFullNameContainingIgnoreCase(Role.PATIENT,search);
        return ApiResponse.ok(patients.stream().map(p->new PatientResponse(p.getId(),p.getFullName(),p.getEmail(),p.getPhone())).toList());
    }
    @GetMapping @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<List<AdminDtos.UserAdminResponse>> allUsers(@RequestParam(defaultValue="") String search) {
        List<User> result=search.isBlank()?users.findAll():users.findAll().stream()
            .filter(user->user.getFullName().toLowerCase(java.util.Locale.ROOT).contains(search.toLowerCase(java.util.Locale.ROOT))
                || user.getEmail().toLowerCase(java.util.Locale.ROOT).contains(search.toLowerCase(java.util.Locale.ROOT))).toList();
        return ApiResponse.ok(result.stream().map(user->new AdminDtos.UserAdminResponse(user.getId(),user.getFullName(),user.getEmail(),
            user.getPhone(),user.getRole().name(),user.isActive())).toList());
    }
}

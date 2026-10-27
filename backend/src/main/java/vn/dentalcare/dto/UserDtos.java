package vn.dentalcare.dto;
import jakarta.validation.constraints.*;
public final class UserDtos {
    private UserDtos() {}
    public record UpdateProfileRequest(@NotBlank @Size(max=120) String fullName, @Size(max=30) String phone, @Size(max=500) String avatarUrl) {}
    public record PatientResponse(Long id,String fullName,String email,String phone) {}
}

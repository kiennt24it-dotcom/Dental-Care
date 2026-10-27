package vn.dentalcare.dto;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalTime;
import java.time.Instant;
public final class AdminDtos {
    private AdminDtos() {}
    public record ServiceWriteRequest(@NotBlank @Size(max=120) String name,@Size(max=80) String category,
        @NotNull @DecimalMin("0.01") BigDecimal price,@Min(15) @Max(240) int durationMinutes,@Size(max=2000) String description) {}
    public record SpecialtyWriteRequest(@NotBlank @Size(max=100) String name,@Size(max=500) String description) {}
    public record ScheduleWriteRequest(@NotNull Long doctorId,@NotNull DayOfWeek dayOfWeek,@NotNull LocalTime startTime,
        @NotNull LocalTime endTime,LocalTime breakStart,LocalTime breakEnd,boolean active) {}
    public record ActiveRequest(boolean active) {}
    public record RoleRequest(@NotNull vn.dentalcare.entity.Role role) {}
    public record UserAdminResponse(Long id,String fullName,String email,String phone,String role,boolean active) {}
    public record UnavailabilityRequest(@NotNull Long doctorId,@NotNull Instant startAt,@NotNull Instant endAt,@Size(max=300) String reason) {}
    public record UnavailabilityResponse(Long id,Long doctorId,String doctorName,Instant startAt,Instant endAt,String reason) {}
}

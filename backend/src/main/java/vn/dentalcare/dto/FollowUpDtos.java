package vn.dentalcare.dto;
import jakarta.validation.constraints.*;
import java.time.Instant;
public final class FollowUpDtos {
    private FollowUpDtos() {}
    public record CreateRequest(@NotNull Long previousAppointmentId,@NotNull Instant scheduledAt,
                                @NotBlank @Size(max=300) String reason,@Size(max=1000) String notes) {}
    public record FollowUpResponse(Long id,Long patientId,String patientName,Long doctorId,String doctorName,
                                   Long previousAppointmentId,Instant scheduledAt,String status,String reason,String notes) {}
}

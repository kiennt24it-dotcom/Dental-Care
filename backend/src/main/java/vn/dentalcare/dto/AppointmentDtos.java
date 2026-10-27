package vn.dentalcare.dto;
import jakarta.validation.constraints.*;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
public final class AppointmentDtos {
    private AppointmentDtos() {}
    public record CreateRequest(@NotNull Long doctorId, @NotNull Long serviceId, @NotNull Instant startAt,
                                @Size(max=2000) String symptoms, Long patientId) {}
    public record StatusRequest(@NotNull String status, @Size(max=2000) String internalNote) {}
    public record AppointmentResponse(Long id, Long patientId, String patientName, Long doctorId, String doctorName,
                                      Long serviceId, String serviceName, Instant startAt, Instant endAt,
                                      String status, String symptoms, String internalNote) {}
    public record AvailableSlot(LocalTime start, LocalTime end) {}
    public record SlotsResponse(Long doctorId, LocalDate date, java.util.List<AvailableSlot> slots) {}
}

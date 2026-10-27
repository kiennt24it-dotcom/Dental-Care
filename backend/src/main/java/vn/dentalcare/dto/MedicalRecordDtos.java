package vn.dentalcare.dto;
import jakarta.validation.constraints.*;
import java.time.LocalDate;
public final class MedicalRecordDtos {
    private MedicalRecordDtos() {}
    public record CreateRequest(@NotNull Long appointmentId, @Size(max=2000) String diagnosis,
                                @Size(max=4000) String treatment, @Size(max=3000) String prescription,
                                @Size(max=3000) String notes, LocalDate followUpDate) {}
    public record MedicalRecordResponse(Long id, Long patientId, String patientName, Long doctorId, String doctorName,
                                        Long appointmentId, LocalDate examinationDate, String symptoms, String diagnosis,
                                        String treatment, String prescription, String notes, LocalDate followUpDate) {}
}

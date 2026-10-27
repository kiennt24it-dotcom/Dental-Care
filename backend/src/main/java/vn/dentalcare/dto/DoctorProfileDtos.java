package vn.dentalcare.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import vn.dentalcare.entity.DoctorProfileSubmission;
import java.time.Instant;

public final class DoctorProfileDtos {
    private DoctorProfileDtos() {}

    public record ProfileWriteRequest(
        @NotNull Long specialtyId,
        @Min(0) @Max(70) int experienceYears,
        @Size(max=200) String qualification,
        @Size(max=2000) String description,
        @Size(max=3000) String achievements
    ) {}

    public record ReviewRequest(@NotNull Decision decision, @Size(max=1000) String reviewNote) {}
    public enum Decision { APPROVE, REJECT }

    public record ProfileResponse(
        Long doctorId, String doctorName, String avatarUrl, Long specialtyId, String specialty,
        int experienceYears, String qualification, String description, String achievements,
        DoctorProfileSubmission.Status status, String reviewNote, Instant submittedAt
    ) {}
}

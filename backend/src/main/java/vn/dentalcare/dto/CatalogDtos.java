package vn.dentalcare.dto;
import java.math.BigDecimal;
public final class CatalogDtos {
    private CatalogDtos() {}
    public record ServiceResponse(Long id, String name, String category, BigDecimal price, int durationMinutes, String description, boolean active) {}
    public record DoctorResponse(Long id, String fullName, String avatarUrl, String specialty, int experienceYears,
                                 String qualification, String description, String achievements) {}
    public record SpecialtyResponse(Long id, String name, String description) {}
}

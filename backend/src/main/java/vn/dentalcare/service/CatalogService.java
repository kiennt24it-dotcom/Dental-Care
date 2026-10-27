package vn.dentalcare.service;
import vn.dentalcare.dto.CatalogDtos.*;
import vn.dentalcare.entity.*;
import vn.dentalcare.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
@Service
public class CatalogService {
    private final DentalServiceRepository services;
    private final DoctorProfileRepository doctors;
    private final SpecialtyRepository specialties;
    public CatalogService(DentalServiceRepository services, DoctorProfileRepository doctors, SpecialtyRepository specialties) {
        this.services=services; this.doctors=doctors; this.specialties=specialties;
    }
    @Transactional(readOnly=true)
    public List<ServiceResponse> services() { return services.findByActiveTrueOrderByNameAsc().stream().map(s -> new ServiceResponse(s.getId(),s.getName(),s.getCategory(),s.getPrice(),s.getDurationMinutes(),s.getDescription(),s.isActive())).toList(); }
    @Transactional(readOnly=true)
    public List<DoctorResponse> doctors() { return doctors.findAllByUser_ActiveTrueAndUser_Role(Role.DOCTOR).stream().map(d -> new DoctorResponse(d.getId(),d.getUser().getFullName(),d.getUser().getAvatarUrl(),d.getSpecialty()==null ? null : d.getSpecialty().getName(),d.getExperienceYears(),d.getQualification(),d.getDescription(),d.getAchievements())).toList(); }
    @Transactional(readOnly=true)
    public List<SpecialtyResponse> specialties() { return specialties.findAll().stream().map(s -> new SpecialtyResponse(s.getId(),s.getName(),s.getDescription())).toList(); }
}

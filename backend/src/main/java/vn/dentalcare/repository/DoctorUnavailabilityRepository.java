package vn.dentalcare.repository;
import vn.dentalcare.entity.DoctorUnavailability;
import org.springframework.data.jpa.repository.JpaRepository;
public interface DoctorUnavailabilityRepository extends JpaRepository<DoctorUnavailability,Long> {
    boolean existsByDoctor_IdAndStartAtLessThanAndEndAtGreaterThanAndActiveTrue(Long doctorId,java.time.Instant end,java.time.Instant start);
    java.util.List<DoctorUnavailability> findByDoctor_IdAndEndAtGreaterThanOrderByStartAtAsc(Long doctorId,java.time.Instant now);
}

package vn.dentalcare.repository;
import vn.dentalcare.entity.*;
import org.springframework.data.jpa.repository.JpaRepository;
import java.time.Instant;
import java.util.List;
public interface AppointmentRepository extends JpaRepository<Appointment, Long>, org.springframework.data.jpa.repository.JpaSpecificationExecutor<Appointment> {
    List<Appointment> findByPatient_IdOrderByStartAtDesc(Long patientId);
    List<Appointment> findByPatient_IdAndStartAtGreaterThanEqualAndStartAtLessThanOrderByStartAt(Long patientId, Instant from, Instant to);
    List<Appointment> findByDoctor_IdAndStartAtBetweenOrderByStartAt(Long doctorId, Instant from, Instant to);
    List<Appointment> findByDoctor_IdAndStartAtGreaterThanEqualAndStartAtLessThanOrderByStartAt(Long doctorId, Instant from, Instant to);
    List<Appointment> findByStartAtBetweenOrderByStartAt(Instant from, Instant to);
    List<Appointment> findByStartAtGreaterThanEqualAndStartAtLessThanOrderByStartAt(Instant from, Instant to);
    List<Appointment> findByStatusAndStartAtBetween(AppointmentStatus status, Instant from, Instant to);
    List<Appointment> findByStatusAndStartAtBetweenAndReminderSentFalse(AppointmentStatus status,Instant from,Instant to);
    boolean existsByDoctor_IdAndStatusInAndStartAtLessThanAndEndAtGreaterThan(Long doctorId, List<AppointmentStatus> statuses, Instant end, Instant start);
    boolean existsByPatient_IdAndStatusInAndStartAtLessThanAndEndAtGreaterThan(Long patientId, List<AppointmentStatus> statuses, Instant end, Instant start);
    long countByStatus(AppointmentStatus status);
    long countByStartAtBetween(Instant from, Instant to);
}

package vn.dentalcare.repository;
import vn.dentalcare.entity.FollowUp;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.time.Instant;
public interface FollowUpRepository extends JpaRepository<FollowUp, Long> {
    List<FollowUp> findByPatient_IdOrderByScheduledAtDesc(Long patientId);
    List<FollowUp> findByDoctor_IdOrderByScheduledAtAsc(Long doctorId);
    List<FollowUp> findByScheduledAtBetweenAndStatusAndReminderSentFalse(Instant from,Instant to,String status);
}

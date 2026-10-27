package vn.dentalcare.repository;

import vn.dentalcare.entity.DoctorProfileSubmission;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface DoctorProfileSubmissionRepository extends JpaRepository<DoctorProfileSubmission, Long> {
    Optional<DoctorProfileSubmission> findByDoctor_Id(Long doctorId);
    List<DoctorProfileSubmission> findByStatusAndDoctor_RoleOrderBySubmittedAtAsc(
        DoctorProfileSubmission.Status status, vn.dentalcare.entity.Role role);
}

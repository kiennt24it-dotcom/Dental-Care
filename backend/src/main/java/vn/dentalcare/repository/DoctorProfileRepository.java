package vn.dentalcare.repository;
import vn.dentalcare.entity.DoctorProfile;
import vn.dentalcare.entity.Role;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
public interface DoctorProfileRepository extends JpaRepository<DoctorProfile, Long> {
    List<DoctorProfile> findAllByUser_ActiveTrueAndUser_Role(Role role);
}

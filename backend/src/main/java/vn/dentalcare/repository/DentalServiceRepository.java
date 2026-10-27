package vn.dentalcare.repository;
import vn.dentalcare.entity.DentalService;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
public interface DentalServiceRepository extends JpaRepository<DentalService, Long> {
    List<DentalService> findByActiveTrueOrderByNameAsc();
}

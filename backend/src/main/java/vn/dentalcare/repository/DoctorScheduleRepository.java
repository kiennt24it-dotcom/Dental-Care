package vn.dentalcare.repository;
import vn.dentalcare.entity.DoctorSchedule;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import java.time.DayOfWeek;
import java.util.Optional;
public interface DoctorScheduleRepository extends JpaRepository<DoctorSchedule, Long> {
    Optional<DoctorSchedule> findByDoctor_IdAndDayOfWeek(Long doctorId, DayOfWeek dayOfWeek);
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select s from DoctorSchedule s where s.doctor.id = :doctorId and s.dayOfWeek = :day")
    Optional<DoctorSchedule> lockByDoctorAndDay(@Param("doctorId") Long doctorId, @Param("day") DayOfWeek day);
}

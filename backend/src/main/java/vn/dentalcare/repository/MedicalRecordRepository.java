package vn.dentalcare.repository;
import vn.dentalcare.entity.MedicalRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
public interface MedicalRecordRepository extends JpaRepository<MedicalRecord, Long> {
    List<MedicalRecord> findByPatient_IdOrderByExaminationDateDesc(Long patientId);
    List<MedicalRecord> findByDoctor_IdOrderByExaminationDateDesc(Long doctorId);
    boolean existsByAppointment_Id(Long appointmentId);
}

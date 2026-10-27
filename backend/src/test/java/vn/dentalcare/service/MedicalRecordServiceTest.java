package vn.dentalcare.service;

import vn.dentalcare.dto.MedicalRecordDtos.CreateRequest;
import vn.dentalcare.entity.Role;
import vn.dentalcare.exception.ApiException;
import vn.dentalcare.repository.*;
import org.junit.jupiter.api.Test;
import java.time.LocalDate;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class MedicalRecordServiceTest {
    @Test
    void patientCannotCreateMedicalRecord() {
        MedicalRecordService service=new MedicalRecordService(mock(MedicalRecordRepository.class),
            mock(AppointmentRepository.class),mock(UserRepository.class),mock(FollowUpRepository.class),mock(EmailService.class),
            mock(NotificationService.class),"Asia/Ho_Chi_Minh");
        assertThrows(ApiException.class,()->service.create(new CreateRequest(1L,"Diagnosis","Treatment",null,null,null),
            "patient@example.com",Role.PATIENT));
    }
}

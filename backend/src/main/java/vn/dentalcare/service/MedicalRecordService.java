package vn.dentalcare.service;
import vn.dentalcare.dto.MedicalRecordDtos.*;
import vn.dentalcare.entity.*;
import vn.dentalcare.exception.ApiException;
import vn.dentalcare.repository.*;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.beans.factory.annotation.Value;
import java.time.LocalDate;
import java.time.Instant;
import java.time.ZoneId;
import java.util.List;
@Service
public class MedicalRecordService {
    private final MedicalRecordRepository records;
    private final AppointmentRepository appointments;
    private final UserRepository users;
    private final FollowUpRepository followUps;
    private final EmailService email;
    private final NotificationService notifications;
    private final ZoneId clinicZone;
    public MedicalRecordService(MedicalRecordRepository records, AppointmentRepository appointments, UserRepository users,
                                FollowUpRepository followUps,EmailService email,NotificationService notifications,
                                @Value("${app.clinic-timezone:Asia/Ho_Chi_Minh}") String timezone) {
        this.records=records; this.appointments=appointments; this.users=users;this.followUps=followUps;this.email=email;
        this.notifications=notifications;this.clinicZone=ZoneId.of(timezone);
    }
    @Transactional(readOnly=true)
    public List<MedicalRecordResponse> list(String email, Role role) {
        User user=users.findByEmailIgnoreCase(email).orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND,"Không tìm thấy tài khoản"));
        List<MedicalRecord> result=switch(role) {
            case PATIENT -> records.findByPatient_IdOrderByExaminationDateDesc(user.getId());
            case DOCTOR -> records.findByDoctor_IdOrderByExaminationDateDesc(user.getId());
            default -> throw new ApiException(HttpStatus.FORBIDDEN,"Bạn không có quyền xem hồ sơ y tế");
        };
        return result.stream().map(MedicalRecordService::response).toList();
    }
    @Transactional
    public MedicalRecordResponse create(CreateRequest request, String doctorEmail, Role role) {
        if (role!=Role.DOCTOR) throw new ApiException(HttpStatus.FORBIDDEN,"Chỉ bác sĩ mới được tạo hồ sơ y tế");
        Appointment appointment=appointments.findById(request.appointmentId()).orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND,"Không tìm thấy lịch hẹn"));
        User doctor=users.findByEmailIgnoreCase(doctorEmail).orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND,"Không tìm thấy bác sĩ"));
        if (!appointment.getDoctor().getId().equals(doctor.getId()) || appointment.getStatus()!=AppointmentStatus.COMPLETED)
            throw new ApiException(HttpStatus.FORBIDDEN,"Chỉ bác sĩ phụ trách mới được ghi hồ sơ cho lịch đã hoàn tất");
        if (records.existsByAppointment_Id(appointment.getId())) throw new ApiException(HttpStatus.CONFLICT,"Lịch hẹn đã có hồ sơ y tế");
        if(request.followUpDate()!=null && !request.followUpDate().isAfter(LocalDate.now(clinicZone)))
            throw new ApiException(HttpStatus.BAD_REQUEST,"Ngày tái khám phải ở tương lai");
        MedicalRecord record=records.save(new MedicalRecord(appointment.getPatient(),doctor,appointment,LocalDate.now(clinicZone),
            appointment.getSymptoms(),request.diagnosis(),request.treatment(),request.prescription(),request.notes(),request.followUpDate()));
        notifications.create(appointment.getPatient(),"Hồ sơ điều trị đã được cập nhật",
            "Bác sĩ " + doctor.getFullName() + " đã cập nhật hồ sơ điều trị cho lịch hẹn #" + appointment.getId() + ".");
        if(request.followUpDate()!=null) {
            Instant followUpAt=request.followUpDate().atTime(9,0).atZone(clinicZone).toInstant();
            followUps.save(new FollowUp(appointment.getPatient(),doctor,appointment,followUpAt,"Tái khám theo chỉ định",request.notes()));
            notifications.create(appointment.getPatient(),"Bác sĩ đề xuất lịch tái khám",
                "Bác sĩ " + doctor.getFullName() + " đề nghị bạn tái khám vào ngày " + request.followUpDate() + ".");
            email.send(appointment.getPatient().getEmail(),"Nhắc lịch tái khám DentalCare",
                "Bác sĩ " + doctor.getFullName() + " đề nghị bạn tái khám vào ngày " + request.followUpDate() + ".");
        }
        return response(record);
    }
    private static MedicalRecordResponse response(MedicalRecord r) {
        return new MedicalRecordResponse(r.getId(),r.getPatient().getId(),r.getPatient().getFullName(),r.getDoctor().getId(),
            r.getDoctor().getFullName(),r.getAppointment().getId(),r.getExaminationDate(),r.getSymptoms(),r.getDiagnosis(),
            r.getTreatment(),r.getPrescription(),r.getNotes(),r.getFollowUpDate());
    }
}

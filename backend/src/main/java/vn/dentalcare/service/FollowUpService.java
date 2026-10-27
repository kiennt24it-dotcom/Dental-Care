package vn.dentalcare.service;
import vn.dentalcare.dto.FollowUpDtos.*;
import vn.dentalcare.entity.*;
import vn.dentalcare.exception.ApiException;
import vn.dentalcare.repository.*;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.Instant;
import java.util.List;
@Service
public class FollowUpService {
    private final FollowUpRepository followUps;
    private final AppointmentRepository appointments;
    private final UserRepository users;
    private final EmailService email;
    private final NotificationService notifications;
    public FollowUpService(FollowUpRepository followUps,AppointmentRepository appointments,UserRepository users,EmailService email,
                           NotificationService notifications) {
        this.followUps=followUps;this.appointments=appointments;this.users=users;this.email=email;this.notifications=notifications;
    }
    @Transactional(readOnly=true)
    public List<FollowUpResponse> list(String emailAddress,Role role) {
        User user=user(emailAddress);
        List<FollowUp> rows=switch(role) {
            case PATIENT -> followUps.findByPatient_IdOrderByScheduledAtDesc(user.getId());
            case DOCTOR -> followUps.findByDoctor_IdOrderByScheduledAtAsc(user.getId());
            default -> throw new ApiException(HttpStatus.FORBIDDEN,"Bạn không có quyền xem lịch tái khám");
        };
        return rows.stream().map(FollowUpService::response).toList();
    }
    @Transactional
    public FollowUpResponse create(CreateRequest request,String emailAddress,Role role) {
        if(role!=Role.DOCTOR) throw new ApiException(HttpStatus.FORBIDDEN,"Chỉ bác sĩ mới được tạo lịch tái khám");
        User doctor=user(emailAddress);
        Appointment previous=appointments.findById(request.previousAppointmentId())
            .orElseThrow(()->new ApiException(HttpStatus.NOT_FOUND,"Không tìm thấy lịch hẹn trước đó"));
        if(!previous.getDoctor().getId().equals(doctor.getId()) || previous.getStatus()!=AppointmentStatus.COMPLETED)
            throw new ApiException(HttpStatus.FORBIDDEN,"Chỉ bác sĩ phụ trách mới được tạo lịch tái khám cho lịch đã hoàn tất");
        if(!request.scheduledAt().isAfter(Instant.now())) throw new ApiException(HttpStatus.BAD_REQUEST,"Thời gian tái khám phải ở tương lai");
        FollowUp result=followUps.save(new FollowUp(previous.getPatient(),doctor,previous,request.scheduledAt(),request.reason(),request.notes()));
        notifications.create(previous.getPatient(),"Đề xuất lịch tái khám DentalCare",
            "Bác sĩ " + doctor.getFullName() + " đề xuất lịch tái khám vào " + request.scheduledAt() + ".");
        email.send(previous.getPatient().getEmail(),"Đề xuất lịch tái khám DentalCare",
            "Bác sĩ " + doctor.getFullName() + " đề xuất lịch tái khám vào " + request.scheduledAt() + ". Lý do: " + request.reason());
        return response(result);
    }
    private User user(String email) { return users.findByEmailIgnoreCase(email).orElseThrow(()->new ApiException(HttpStatus.NOT_FOUND,"Không tìm thấy tài khoản")); }
    private static FollowUpResponse response(FollowUp f) {
        return new FollowUpResponse(f.getId(),f.getPatient().getId(),f.getPatient().getFullName(),f.getDoctor().getId(),
            f.getDoctor().getFullName(),f.getPreviousAppointment().getId(),f.getScheduledAt(),f.getStatus(),f.getReason(),f.getNotes());
    }
}

package vn.dentalcare.service;

import vn.dentalcare.dto.AppointmentDtos.*;
import vn.dentalcare.entity.*;
import vn.dentalcare.exception.ApiException;
import vn.dentalcare.repository.*;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.transaction.annotation.Isolation;
import jakarta.persistence.criteria.Predicate;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Service
public class AppointmentService {
    private static final List<AppointmentStatus> BLOCKING = List.of(AppointmentStatus.PENDING, AppointmentStatus.CONFIRMED);
    private final AppointmentRepository appointments;
    private final DoctorScheduleRepository schedules;
    private final UserRepository users;
    private final DentalServiceRepository services;
    private final EmailService email;
    private final NotificationService notifications;
    private final DoctorUnavailabilityRepository unavailabilities;
    private final ZoneId clinicZone;
    public AppointmentService(AppointmentRepository appointments, DoctorScheduleRepository schedules, UserRepository users,
                              DentalServiceRepository services, EmailService email,NotificationService notifications,
                              DoctorUnavailabilityRepository unavailabilities,
                              @Value("${app.clinic-timezone:Asia/Ho_Chi_Minh}") String clinicTimezone) {
        this.appointments=appointments; this.schedules=schedules; this.users=users; this.services=services; this.email=email;
        this.notifications=notifications;this.unavailabilities=unavailabilities;this.clinicZone=ZoneId.of(clinicTimezone);
    }
    @Transactional(readOnly=true)
    public SlotsResponse slots(Long doctorId, LocalDate date, Long serviceId) {
        DoctorSchedule schedule=schedules.findByDoctor_IdAndDayOfWeek(doctorId, date.getDayOfWeek())
            .filter(DoctorSchedule::isActive).orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Bác sĩ không làm việc vào ngày này"));
        DentalService service=services.findById(serviceId).filter(DentalService::isActive)
            .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Không tìm thấy dịch vụ"));
        LocalDateTime cursor=date.atTime(schedule.getStartTime());
        LocalDateTime closing=date.atTime(schedule.getEndTime());
        List<AvailableSlot> result=new ArrayList<>();
        while (!cursor.plusMinutes(service.getDurationMinutes()).isAfter(closing)) {
            LocalDateTime end=cursor.plusMinutes(service.getDurationMinutes());
            boolean inBreak=schedule.getBreakStart()!=null && schedule.getBreakEnd()!=null
                && cursor.toLocalTime().isBefore(schedule.getBreakEnd()) && end.toLocalTime().isAfter(schedule.getBreakStart());
            Instant startInstant=cursor.atZone(clinicZone).toInstant();
            Instant endInstant=end.atZone(clinicZone).toInstant();
            boolean booked=appointments.existsByDoctor_IdAndStatusInAndStartAtLessThanAndEndAtGreaterThan(
                doctorId, BLOCKING, endInstant, startInstant);
            boolean unavailable=unavailabilities.existsByDoctor_IdAndStartAtLessThanAndEndAtGreaterThanAndActiveTrue(doctorId,endInstant,startInstant);
            if (!inBreak && !booked && !unavailable && startInstant.isAfter(Instant.now())) result.add(new AvailableSlot(cursor.toLocalTime(), end.toLocalTime()));
            int nextStep=!inBreak && !booked && !unavailable ? ((service.getDurationMinutes()+29)/30)*30 : 30;
            cursor=cursor.plusMinutes(nextStep);
        }
        return new SlotsResponse(doctorId, date, result);
    }
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public AppointmentResponse create(CreateRequest request, String emailAddress, Role actorRole) {
        User actor=findUser(emailAddress);
        User patient;
        if (actorRole == Role.PATIENT) patient=actor;
        else if (actorRole == Role.RECEPTIONIST) {
            if (request.patientId()==null) throw new ApiException(HttpStatus.BAD_REQUEST, "Cần chọn bệnh nhân");
            patient=users.findById(request.patientId()).filter(u -> u.getRole()==Role.PATIENT)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Không tìm thấy bệnh nhân"));
        } else throw new ApiException(HttpStatus.FORBIDDEN, "Bạn không có quyền tạo lịch hẹn");
        patient=users.lockById(patient.getId()).orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Không tìm thấy bệnh nhân"));
        User doctor=users.findById(request.doctorId()).filter(u -> u.isActive() && u.getRole()==Role.DOCTOR)
            .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Không tìm thấy bác sĩ"));
        DentalService service=services.findById(request.serviceId()).filter(DentalService::isActive)
            .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Không tìm thấy dịch vụ"));
        if (!request.startAt().isAfter(Instant.now())) throw new ApiException(HttpStatus.BAD_REQUEST, "Thời gian hẹn phải ở tương lai");
        LocalDateTime localStart=LocalDateTime.ofInstant(request.startAt(), clinicZone);
        Instant end=request.startAt().plusSeconds(service.getDurationMinutes()*60L);
        DoctorSchedule schedule=schedules.lockByDoctorAndDay(doctor.getId(), localStart.getDayOfWeek())
            .filter(DoctorSchedule::isActive).orElseThrow(() -> new ApiException(HttpStatus.CONFLICT, "Bác sĩ không làm việc vào thời gian đã chọn"));
        LocalTime startTime=localStart.toLocalTime();
        LocalTime endTime=LocalDateTime.ofInstant(end, clinicZone).toLocalTime();
        if (startTime.isBefore(schedule.getStartTime()) || endTime.isAfter(schedule.getEndTime())
            || startTime.getSecond()!=0 || startTime.getNano()!=0
            || java.time.Duration.between(schedule.getStartTime(),startTime).toMinutes()%30!=0)
            throw new ApiException(HttpStatus.CONFLICT, "Thời gian nằm ngoài ca làm việc hoặc không đúng khung lịch");
        if (schedule.getBreakStart()!=null && schedule.getBreakEnd()!=null
            && startTime.isBefore(schedule.getBreakEnd()) && endTime.isAfter(schedule.getBreakStart()))
            throw new ApiException(HttpStatus.CONFLICT, "Thời gian đã chọn trùng giờ nghỉ của bác sĩ");
        if(unavailabilities.existsByDoctor_IdAndStartAtLessThanAndEndAtGreaterThanAndActiveTrue(doctor.getId(),end,request.startAt()))
            throw new ApiException(HttpStatus.CONFLICT,"Bác sĩ không thể nhận lịch trong thời gian đã chọn");
        if (appointments.existsByDoctor_IdAndStatusInAndStartAtLessThanAndEndAtGreaterThan(doctor.getId(), BLOCKING, end, request.startAt()))
            throw new ApiException(HttpStatus.CONFLICT, "Khung giờ vừa được đặt, vui lòng chọn khung giờ khác");
        if (appointments.existsByPatient_IdAndStatusInAndStartAtLessThanAndEndAtGreaterThan(patient.getId(), BLOCKING, end, request.startAt()))
            throw new ApiException(HttpStatus.CONFLICT, "Bệnh nhân đã có lịch hẹn khác trong khung giờ này");
        Appointment appointment=appointments.save(new Appointment(patient, doctor, service, request.startAt(), end, request.symptoms()));
        notifications.create(patient,"Đã nhận yêu cầu đặt lịch",
            "Yêu cầu khám với bác sĩ " + doctor.getFullName() + " đã được ghi nhận và đang chờ xác nhận.",
            "/app/appointments");
        notifications.createForActiveReceptionists("Yêu cầu đặt lịch mới",
            patient.getFullName() + " vừa gửi yêu cầu khám với bác sĩ " + doctor.getFullName() + " lúc "
                + DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm").withZone(clinicZone).format(request.startAt()) + ".",
            "/app/appointments?status=PENDING");
        email.send(patient.getEmail(), "Đã nhận yêu cầu đặt lịch", "Yêu cầu khám lúc " + request.startAt() + " đã được ghi nhận và đang chờ xác nhận.");
        return response(appointment,actorRole);
    }
    @Transactional
    public AppointmentResponse changeStatus(Long id, StatusRequest request, String emailAddress, Role role) {
        Appointment appointment=appointments.findById(id).orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Không tìm thấy lịch hẹn"));
        AppointmentStatus next;
        try { next=AppointmentStatus.valueOf(request.status().toUpperCase(Locale.ROOT)); }
        catch (IllegalArgumentException ex) { throw new ApiException(HttpStatus.BAD_REQUEST, "Trạng thái không hợp lệ"); }
        boolean permitted;
        if (role==Role.PATIENT) permitted=appointment.getPatient().getEmail().equalsIgnoreCase(emailAddress)
            && next==AppointmentStatus.CANCELLED && appointment.getStartAt().isAfter(Instant.now().plus(Duration.ofHours(24)))
            && (appointment.getStatus()==AppointmentStatus.PENDING || appointment.getStatus()==AppointmentStatus.CONFIRMED);
        else if (role==Role.DOCTOR) permitted=appointment.getDoctor().getEmail().equalsIgnoreCase(emailAddress)
            && appointment.getStatus()==AppointmentStatus.CONFIRMED
                && List.of(AppointmentStatus.COMPLETED, AppointmentStatus.NO_SHOW).contains(next);
        else permitted=role==Role.RECEPTIONIST
            && ((appointment.getStatus()==AppointmentStatus.PENDING && List.of(AppointmentStatus.CONFIRMED,AppointmentStatus.REJECTED,AppointmentStatus.CANCELLED).contains(next))
                || (appointment.getStatus()==AppointmentStatus.CONFIRMED && next==AppointmentStatus.CANCELLED));
        if (!permitted) throw new ApiException(HttpStatus.FORBIDDEN, "Không thể chuyển lịch hẹn sang trạng thái này");
        appointment.setStatus(next);
        if (request.internalNote()!=null && (role==Role.DOCTOR || role==Role.RECEPTIONIST || role==Role.ADMIN))
            appointment.setInternalNote(request.internalNote());
        appointments.save(appointment);
        String subject=switch(next) {
            case CONFIRMED -> "Lịch hẹn đã được xác nhận";
            case REJECTED -> "Lịch hẹn chưa thể được tiếp nhận";
            case CANCELLED -> "Lịch hẹn đã được hủy";
            case COMPLETED -> "Cảm ơn bạn đã đến DentalCare";
            case NO_SHOW -> "Cập nhật lịch hẹn DentalCare";
            default -> "Cập nhật lịch hẹn DentalCare";
        };
        notifications.create(appointment.getPatient(),subject,"Trạng thái lịch hẹn #" + id + ": " + next.name(),
            "/app/appointments");
        email.send(appointment.getPatient().getEmail(), subject, "Trạng thái lịch hẹn #" + id + ": " + next.name());
        return response(appointment,role);
    }
    @Transactional(readOnly=true)
    public List<AppointmentResponse> list(String emailAddress, Role role, LocalDate date, String status) {
        List<Appointment> found;
        if (role==Role.PATIENT) found=appointments.findByPatient_IdOrderByStartAtDesc(findUser(emailAddress).getId());
        else if (role==Role.DOCTOR) {
            LocalDate d=date==null ? LocalDate.now(clinicZone) : date;
            found=appointments.findByDoctor_IdAndStartAtBetweenOrderByStartAt(findUser(emailAddress).getId(),
                d.atStartOfDay(clinicZone).toInstant(), d.plusDays(1).atStartOfDay(clinicZone).toInstant());
        } else if (role==Role.RECEPTIONIST) {
            if (date==null) found=appointments.findAll().stream().sorted(Comparator.comparing(Appointment::getStartAt)).toList();
            else found=appointments.findByStartAtBetweenOrderByStartAt(date.atStartOfDay(clinicZone).toInstant(), date.plusDays(1).atStartOfDay(clinicZone).toInstant());
        } else throw new ApiException(HttpStatus.FORBIDDEN, "Không có quyền xem lịch hẹn");
        return filterStatus(found, status).stream().map(appointment -> response(appointment,role)).toList();
    }
    @Transactional(readOnly=true)
    public List<AppointmentResponse> listBetween(String emailAddress, Role role, LocalDate from, LocalDate to, String status) {
        if (from==null || to==null || !to.isAfter(from) || Duration.between(from.atStartOfDay(),to.atStartOfDay()).toDays()>31)
            throw new ApiException(HttpStatus.BAD_REQUEST,"Khoảng ngày không hợp lệ (tối đa 31 ngày)");
        Instant start=from.atStartOfDay(clinicZone).toInstant();
        Instant end=to.atStartOfDay(clinicZone).toInstant();
        User user=findUser(emailAddress);
        List<Appointment> found=switch(role) {
            case PATIENT -> appointments.findByPatient_IdAndStartAtGreaterThanEqualAndStartAtLessThanOrderByStartAt(user.getId(),start,end);
            case DOCTOR -> appointments.findByDoctor_IdAndStartAtGreaterThanEqualAndStartAtLessThanOrderByStartAt(user.getId(),start,end);
            case RECEPTIONIST -> appointments.findByStartAtGreaterThanEqualAndStartAtLessThanOrderByStartAt(start,end);
            case ADMIN -> throw new ApiException(HttpStatus.FORBIDDEN,"Quản trị viên không xử lý lịch hẹn");
        };
        return filterStatus(found,status).stream().map(appointment -> response(appointment,role)).toList();
    }
    @Transactional(readOnly=true)
    public org.springframework.data.domain.Page<AppointmentResponse> page(String emailAddress,Role role,LocalDate date,
        LocalDate from,LocalDate to,String status,String search,int page,int size) {
        if(page<0||size<1||size>100) throw new ApiException(HttpStatus.BAD_REQUEST,"Thông số phân trang không hợp lệ");
        if((from==null)!=(to==null)||date!=null&&from!=null)
            throw new ApiException(HttpStatus.BAD_REQUEST,"Chỉ chọn một ngày hoặc một khoảng ngày đầy đủ");
        if(from!=null&&(!to.isAfter(from)||Duration.between(from.atStartOfDay(),to.atStartOfDay()).toDays()>31))
            throw new ApiException(HttpStatus.BAD_REQUEST,"Khoảng ngày không hợp lệ (tối đa 31 ngày)");
        User user=findUser(emailAddress);
        AppointmentStatus requestedStatus=parseStatus(status);
        LocalDate effectiveDate=date;
        if(effectiveDate==null&&from==null&&role==Role.DOCTOR) effectiveDate=LocalDate.now(clinicZone);
        LocalDate finalDate=effectiveDate;
        String query=search==null?"":search.trim().toLowerCase(Locale.ROOT);
        Specification<Appointment> criteria=(root,queryContext,builder)->{
            List<Predicate> predicates=new ArrayList<>();
            if(role==Role.PATIENT) predicates.add(builder.equal(root.get("patient").get("id"),user.getId()));
            else if(role==Role.DOCTOR) predicates.add(builder.equal(root.get("doctor").get("id"),user.getId()));
            else if(role!=Role.RECEPTIONIST)
                throw new ApiException(HttpStatus.FORBIDDEN,"Không có quyền xem lịch hẹn");
            if(finalDate!=null) {
                Instant start=finalDate.atStartOfDay(clinicZone).toInstant();
                Instant end=finalDate.plusDays(1).atStartOfDay(clinicZone).toInstant();
                predicates.add(builder.greaterThanOrEqualTo(root.get("startAt"),start));
                predicates.add(builder.lessThan(root.get("startAt"),end));
            } else if(from!=null) {
                predicates.add(builder.greaterThanOrEqualTo(root.get("startAt"),from.atStartOfDay(clinicZone).toInstant()));
                predicates.add(builder.lessThan(root.get("startAt"),to.atStartOfDay(clinicZone).toInstant()));
            }
            if(requestedStatus!=null) predicates.add(builder.equal(root.get("status"),requestedStatus));
            if(!query.isBlank()) {
                String pattern="%"+query+"%";
                predicates.add(builder.or(
                    builder.like(builder.lower(root.join("patient").get("fullName")),pattern),
                    builder.like(builder.lower(root.join("doctor").get("fullName")),pattern),
                    builder.like(builder.lower(root.join("service").get("name")),pattern)));
            }
            return builder.and(predicates.toArray(Predicate[]::new));
        };
        Sort.Direction direction=role==Role.PATIENT?Sort.Direction.DESC:Sort.Direction.ASC;
        Page<Appointment> result=appointments.findAll(criteria,PageRequest.of(page,size,Sort.by(direction,"startAt")));
        return result.map(appointment -> response(appointment,role));
    }
    private static List<Appointment> filterStatus(List<Appointment> found,String status) {
        if(status==null||status.isBlank()) return found;
        AppointmentStatus filter=parseStatus(status);
        return found.stream().filter(a->a.getStatus()==filter).toList();
    }
    private static AppointmentStatus parseStatus(String status) {
        if(status==null||status.isBlank()) return null;
        try { return AppointmentStatus.valueOf(status.toUpperCase(Locale.ROOT)); }
        catch(IllegalArgumentException ex) { throw new ApiException(HttpStatus.BAD_REQUEST,"Trạng thái lọc không hợp lệ"); }
    }
    static AppointmentResponse response(Appointment a,Role role) {
        return new AppointmentResponse(a.getId(), a.getPatient().getId(), a.getPatient().getFullName(), a.getDoctor().getId(),
            a.getDoctor().getFullName(), a.getService().getId(), a.getService().getName(), a.getStartAt(), a.getEndAt(),
            a.getStatus().name(), a.getSymptoms(), role==Role.PATIENT?null:a.getInternalNote());
    }
    private User findUser(String emailAddress) { return users.findByEmailIgnoreCase(emailAddress).orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Không tìm thấy tài khoản")); }
}

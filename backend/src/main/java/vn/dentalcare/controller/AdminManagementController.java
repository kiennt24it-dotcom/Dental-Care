package vn.dentalcare.controller;
import vn.dentalcare.dto.*;
import vn.dentalcare.dto.AdminDtos.*;
import vn.dentalcare.dto.CatalogDtos.ServiceResponse;
import vn.dentalcare.entity.*;
import vn.dentalcare.exception.ApiException;
import vn.dentalcare.repository.*;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.transaction.annotation.Transactional;
import java.time.DayOfWeek;
import java.util.List;
@RestController
@PreAuthorize("hasRole('ADMIN')")
public class AdminManagementController {
    private final DentalServiceRepository services;
    private final SpecialtyRepository specialties;
    private final DoctorScheduleRepository schedules;
    private final UserRepository users;
    private final DoctorUnavailabilityRepository unavailabilities;
    public AdminManagementController(DentalServiceRepository services,SpecialtyRepository specialties,
        DoctorScheduleRepository schedules,UserRepository users,DoctorUnavailabilityRepository unavailabilities) {
        this.services=services;this.specialties=specialties;this.schedules=schedules;this.users=users;this.unavailabilities=unavailabilities;
    }
    @PostMapping("/api/services")
    public ApiResponse<ServiceResponse> createService(@Valid @RequestBody ServiceWriteRequest request) {
        DentalService service=services.save(new DentalService(request.name(),request.category(),request.price(),request.durationMinutes(),request.description()));
        return ApiResponse.ok(serviceDto(service));
    }
    @GetMapping("/api/services")
    public ApiResponse<List<ServiceResponse>> allServices() {
        return ApiResponse.ok(services.findAll().stream().map(this::serviceDto).toList());
    }
    @PutMapping("/api/services/{id}")
    public ApiResponse<ServiceResponse> updateService(@PathVariable Long id,@Valid @RequestBody ServiceWriteRequest request) {
        DentalService service=services.findById(id).orElseThrow(()->new ApiException(HttpStatus.NOT_FOUND,"Không tìm thấy dịch vụ"));
        service.update(request.name(),request.category(),request.price(),request.durationMinutes(),request.description());
        return ApiResponse.ok(serviceDto(services.save(service)));
    }
    @PatchMapping("/api/services/{id}/active")
    public ApiResponse<ServiceResponse> activateService(@PathVariable Long id,@Valid @RequestBody ActiveRequest request) {
        DentalService service=services.findById(id).orElseThrow(()->new ApiException(HttpStatus.NOT_FOUND,"Không tìm thấy dịch vụ"));
        service.setActive(request.active());
        return ApiResponse.ok(serviceDto(services.save(service)));
    }
    @PostMapping("/api/specialties")
    public ApiResponse<CatalogDtos.SpecialtyResponse> createSpecialty(@Valid @RequestBody SpecialtyWriteRequest request) {
        Specialty item=specialties.save(new Specialty(request.name(),request.description()));
        return ApiResponse.ok(new CatalogDtos.SpecialtyResponse(item.getId(),item.getName(),item.getDescription()));
    }
    @PostMapping("/api/schedules") @Transactional
    public ApiResponse<String> upsertSchedule(@Valid @RequestBody ScheduleWriteRequest request) {
        User doctor=users.findById(request.doctorId()).filter(u->u.getRole()==Role.DOCTOR).orElseThrow(()->new ApiException(HttpStatus.NOT_FOUND,"Không tìm thấy bác sĩ"));
        if(!request.endTime().isAfter(request.startTime())) throw new ApiException(HttpStatus.BAD_REQUEST,"Giờ kết thúc phải sau giờ bắt đầu");
        if((request.breakStart()==null)!=(request.breakEnd()==null) || request.breakStart()!=null &&
            (!request.breakEnd().isAfter(request.breakStart()) || request.breakStart().isBefore(request.startTime()) || request.breakEnd().isAfter(request.endTime())))
            throw new ApiException(HttpStatus.BAD_REQUEST,"Khung nghỉ không hợp lệ");
        DoctorSchedule schedule=schedules.lockByDoctorAndDay(doctor.getId(),request.dayOfWeek())
            .or(()->schedules.findByDoctor_IdAndDayOfWeek(doctor.getId(),request.dayOfWeek()))
            .orElseGet(()->new DoctorSchedule(doctor,request.dayOfWeek(),request.startTime(),request.endTime(),request.breakStart(),request.breakEnd()));
        schedule.update(request.startTime(),request.endTime(),request.breakStart(),request.breakEnd(),request.active());
        schedules.save(schedule);
        return ApiResponse.ok("Lịch làm việc đã được lưu");
    }
    @GetMapping("/api/unavailabilities")
    public ApiResponse<List<UnavailabilityResponse>> getUnavailabilities(@RequestParam Long doctorId) {
        return ApiResponse.ok(unavailabilities.findByDoctor_IdAndEndAtGreaterThanOrderByStartAtAsc(doctorId,java.time.Instant.now())
            .stream().map(item->new UnavailabilityResponse(item.getId(),item.getDoctor().getId(),item.getDoctor().getFullName(),item.getStartAt(),item.getEndAt(),item.getReason())).toList());
    }
    @PostMapping("/api/unavailabilities") @Transactional
    public ApiResponse<UnavailabilityResponse> createUnavailability(@Valid @RequestBody UnavailabilityRequest request) {
        User doctor=users.findById(request.doctorId()).filter(u->u.getRole()==Role.DOCTOR)
            .orElseThrow(()->new ApiException(HttpStatus.NOT_FOUND,"Không tìm thấy bác sĩ"));
        if(!request.endAt().isAfter(request.startAt()) || !request.endAt().isAfter(java.time.Instant.now()))
            throw new ApiException(HttpStatus.BAD_REQUEST,"Khoảng thời gian không hợp lệ hoặc đã kết thúc");
        for(DayOfWeek day:DayOfWeek.values()) schedules.lockByDoctorAndDay(doctor.getId(),day);
        DoctorUnavailability saved=unavailabilities.save(new DoctorUnavailability(doctor,request.startAt(),request.endAt(),request.reason()));
        return ApiResponse.ok(new UnavailabilityResponse(saved.getId(),doctor.getId(),doctor.getFullName(),saved.getStartAt(),saved.getEndAt(),saved.getReason()));
    }
    @PatchMapping("/api/users/{id}/role")
    public ApiResponse<UserAdminResponse> changeRole(@PathVariable Long id,@Valid @RequestBody RoleRequest request) {
        User user=users.findById(id).orElseThrow(()->new ApiException(HttpStatus.NOT_FOUND,"Không tìm thấy tài khoản"));
        if(user.getRole()==Role.ADMIN && request.role()!=Role.ADMIN && users.countByRoleAndActiveTrue(Role.ADMIN)<=1)
            throw new ApiException(HttpStatus.CONFLICT,"Không thể hạ quyền quản trị viên đang hoạt động cuối cùng");
        user.setRole(request.role());
        return ApiResponse.ok(userDto(users.save(user)));
    }
    @PatchMapping("/api/users/{id}/active")
    public ApiResponse<UserAdminResponse> setUserActive(@PathVariable Long id,@Valid @RequestBody ActiveRequest request) {
        User user=users.findById(id).orElseThrow(()->new ApiException(HttpStatus.NOT_FOUND,"Không tìm thấy tài khoản"));
        if(user.getRole()==Role.ADMIN && user.isActive() && !request.active() && users.countByRoleAndActiveTrue(Role.ADMIN)<=1)
            throw new ApiException(HttpStatus.CONFLICT,"Không thể vô hiệu hóa quản trị viên đang hoạt động cuối cùng");
        user.setActive(request.active());
        return ApiResponse.ok(userDto(users.save(user)));
    }
    private ServiceResponse serviceDto(DentalService service) {
        return new ServiceResponse(service.getId(),service.getName(),service.getCategory(),service.getPrice(),service.getDurationMinutes(),service.getDescription(),service.isActive());
    }
    private UserAdminResponse userDto(User user) {
        return new UserAdminResponse(user.getId(),user.getFullName(),user.getEmail(),user.getPhone(),user.getRole().name(),user.isActive());
    }
}

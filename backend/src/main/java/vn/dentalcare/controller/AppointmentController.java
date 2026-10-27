package vn.dentalcare.controller;

import vn.dentalcare.dto.*;
import vn.dentalcare.dto.AppointmentDtos.*;
import vn.dentalcare.dto.PageResponse;
import vn.dentalcare.entity.Role;
import vn.dentalcare.service.AppointmentService;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDate;

@RestController
@RequestMapping("/api/appointments")
public class AppointmentController {
    private final AppointmentService appointments;
    public AppointmentController(AppointmentService appointments) { this.appointments=appointments; }
    @GetMapping("/available-slots")
    public ApiResponse<SlotsResponse> slots(@RequestParam Long doctorId,
        @RequestParam @DateTimeFormat(iso=DateTimeFormat.ISO.DATE) LocalDate date, @RequestParam Long serviceId) {
        return ApiResponse.ok(appointments.slots(doctorId, date, serviceId));
    }
    @PostMapping @PreAuthorize("hasAnyRole('PATIENT','RECEPTIONIST')")
    public ApiResponse<AppointmentResponse> create(@Valid @RequestBody CreateRequest request,
        @AuthenticationPrincipal UserDetails principal) {
        return ApiResponse.ok(appointments.create(request, principal.getUsername(), role(principal)));
    }
    @GetMapping @PreAuthorize("hasAnyRole('PATIENT','RECEPTIONIST','DOCTOR')")
    public ApiResponse<java.util.List<AppointmentResponse>> list(@AuthenticationPrincipal UserDetails principal,
        @RequestParam(required=false) @DateTimeFormat(iso=DateTimeFormat.ISO.DATE) LocalDate date,
        @RequestParam(required=false) @DateTimeFormat(iso=DateTimeFormat.ISO.DATE) LocalDate from,
        @RequestParam(required=false) @DateTimeFormat(iso=DateTimeFormat.ISO.DATE) LocalDate to,
        @RequestParam(required=false) String status) {
        if(from!=null||to!=null) return ApiResponse.ok(appointments.listBetween(principal.getUsername(),role(principal),from,to,status));
        return ApiResponse.ok(appointments.list(principal.getUsername(), role(principal), date, status));
    }
    @GetMapping("/page") @PreAuthorize("hasAnyRole('PATIENT','RECEPTIONIST','DOCTOR')")
    public ApiResponse<PageResponse<AppointmentResponse>> page(@AuthenticationPrincipal UserDetails principal,
        @RequestParam(defaultValue="0") int page,@RequestParam(defaultValue="20") int size,
        @RequestParam(required=false) @DateTimeFormat(iso=DateTimeFormat.ISO.DATE) LocalDate date,
        @RequestParam(required=false) @DateTimeFormat(iso=DateTimeFormat.ISO.DATE) LocalDate from,
        @RequestParam(required=false) @DateTimeFormat(iso=DateTimeFormat.ISO.DATE) LocalDate to,
        @RequestParam(required=false) String status,@RequestParam(required=false) String search) {
        return ApiResponse.ok(PageResponse.from(appointments.page(principal.getUsername(),role(principal),date,from,to,status,search,page,size)));
    }
    @PatchMapping("/{id}/status") @PreAuthorize("hasAnyRole('PATIENT','RECEPTIONIST','DOCTOR')")
    public ApiResponse<AppointmentResponse> status(@PathVariable Long id,
        @Valid @RequestBody StatusRequest request, @AuthenticationPrincipal UserDetails principal) {
        return ApiResponse.ok(appointments.changeStatus(id, request, principal.getUsername(), role(principal)));
    }
    private Role role(UserDetails principal) { return Role.valueOf(principal.getAuthorities().iterator().next().getAuthority().substring(5)); }
}

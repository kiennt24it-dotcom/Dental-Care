package vn.dentalcare.controller;
import vn.dentalcare.dto.*;
import vn.dentalcare.dto.MedicalRecordDtos.*;
import vn.dentalcare.entity.Role;
import vn.dentalcare.service.MedicalRecordService;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.util.List;
@RestController
@RequestMapping("/api/medical-records")
public class MedicalRecordController {
    private final MedicalRecordService records;
    public MedicalRecordController(MedicalRecordService records) { this.records=records; }
    @GetMapping @PreAuthorize("hasAnyRole('PATIENT','DOCTOR')")
    public ApiResponse<List<MedicalRecordResponse>> list(@AuthenticationPrincipal UserDetails principal) {
        return ApiResponse.ok(records.list(principal.getUsername(),role(principal)));
    }
    @PostMapping @PreAuthorize("hasRole('DOCTOR')")
    public ApiResponse<MedicalRecordResponse> create(@Valid @RequestBody CreateRequest request,@AuthenticationPrincipal UserDetails principal) {
        return ApiResponse.ok(records.create(request,principal.getUsername(),role(principal)));
    }
    private Role role(UserDetails principal) { return Role.valueOf(principal.getAuthorities().iterator().next().getAuthority().substring(5)); }
}

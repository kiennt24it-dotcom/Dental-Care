package vn.dentalcare.controller;

import vn.dentalcare.dto.ApiResponse;
import vn.dentalcare.dto.DoctorProfileDtos.*;
import vn.dentalcare.service.DoctorProfileService;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/doctor-profiles")
public class DoctorProfileController {
    private final DoctorProfileService profiles;
    public DoctorProfileController(DoctorProfileService profiles) { this.profiles=profiles; }

    @GetMapping("/me") @PreAuthorize("hasRole('DOCTOR')")
    public ApiResponse<ProfileResponse> mine(@AuthenticationPrincipal UserDetails principal) {
        return ApiResponse.ok(profiles.mine(principal.getUsername()));
    }

    @PutMapping("/me") @PreAuthorize("hasRole('DOCTOR')")
    public ApiResponse<ProfileResponse> submit(@Valid @RequestBody ProfileWriteRequest request,
        @AuthenticationPrincipal UserDetails principal) {
        return ApiResponse.ok(profiles.submit(principal.getUsername(),request));
    }

    @GetMapping("/pending") @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<List<ProfileResponse>> pending() {
        return ApiResponse.ok(profiles.pending());
    }

    @PatchMapping("/{doctorId}/review") @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<ProfileResponse> review(@PathVariable Long doctorId,@Valid @RequestBody ReviewRequest request) {
        return ApiResponse.ok(profiles.review(doctorId,request));
    }
}

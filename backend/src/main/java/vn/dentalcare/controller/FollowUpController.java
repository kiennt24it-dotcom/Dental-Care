package vn.dentalcare.controller;
import vn.dentalcare.dto.*;
import vn.dentalcare.dto.FollowUpDtos.*;
import vn.dentalcare.entity.Role;
import vn.dentalcare.service.FollowUpService;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.util.List;
@RestController
@RequestMapping("/api/follow-ups")
public class FollowUpController {
    private final FollowUpService service;
    public FollowUpController(FollowUpService service){this.service=service;}
    @GetMapping @PreAuthorize("hasAnyRole('PATIENT','DOCTOR')")
    public ApiResponse<List<FollowUpResponse>> list(@AuthenticationPrincipal UserDetails user){
        return ApiResponse.ok(service.list(user.getUsername(),role(user)));
    }
    @PostMapping @PreAuthorize("hasRole('DOCTOR')")
    public ApiResponse<FollowUpResponse> create(@Valid @RequestBody CreateRequest request,@AuthenticationPrincipal UserDetails user){
        return ApiResponse.ok(service.create(request,user.getUsername(),role(user)));
    }
    private Role role(UserDetails user){return Role.valueOf(user.getAuthorities().iterator().next().getAuthority().substring(5));}
}

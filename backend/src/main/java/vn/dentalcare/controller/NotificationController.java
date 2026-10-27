package vn.dentalcare.controller;

import vn.dentalcare.dto.ApiResponse;
import vn.dentalcare.dto.NotificationDtos.NotificationListResponse;
import vn.dentalcare.service.NotificationService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/notifications")
public class NotificationController {
    private final NotificationService notifications;
    public NotificationController(NotificationService notifications) { this.notifications=notifications; }

    @GetMapping
    public ApiResponse<NotificationListResponse> list(@AuthenticationPrincipal UserDetails principal) {
        return ApiResponse.ok(notifications.list(principal.getUsername()));
    }

    @PatchMapping("/{id}/read")
    public ApiResponse<Void> markRead(@PathVariable Long id,@AuthenticationPrincipal UserDetails principal) {
        notifications.markRead(id,principal.getUsername());
        return ApiResponse.ok(null);
    }

    @PatchMapping("/read-all")
    public ApiResponse<Void> markAllRead(@AuthenticationPrincipal UserDetails principal) {
        notifications.markAllRead(principal.getUsername());
        return ApiResponse.ok(null);
    }
}

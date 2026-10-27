package vn.dentalcare.dto;

import java.time.Instant;
import java.util.List;

public final class NotificationDtos {
    private NotificationDtos() {}
    public record NotificationResponse(Long id,String title,String message,String actionPath,Instant createdAt,Instant readAt) {}
    public record NotificationListResponse(List<NotificationResponse> items,long unreadCount) {}
}

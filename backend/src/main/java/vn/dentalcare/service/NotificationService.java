package vn.dentalcare.service;

import vn.dentalcare.dto.NotificationDtos.*;
import vn.dentalcare.entity.Notification;
import vn.dentalcare.entity.Role;
import vn.dentalcare.entity.User;
import vn.dentalcare.exception.ApiException;
import vn.dentalcare.repository.NotificationRepository;
import vn.dentalcare.repository.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.Instant;

@Service
public class NotificationService {
    private final NotificationRepository notifications;
    private final UserRepository users;
    public NotificationService(NotificationRepository notifications,UserRepository users) {
        this.notifications=notifications;
        this.users=users;
    }

    @Transactional(readOnly=true)
    public NotificationListResponse list(String email) {
        User user=user(email);
        return new NotificationListResponse(
            notifications.findTop50ByUser_IdOrderByCreatedAtDesc(user.getId()).stream()
                .map(NotificationService::response).toList(),
            notifications.countByUser_IdAndReadAtIsNull(user.getId()));
    }

    @Transactional
    public void markRead(Long id,String email) {
        User user=user(email);
        Notification notification=notifications.findById(id)
            .orElseThrow(()->new ApiException(HttpStatus.NOT_FOUND,"Không tìm thấy thông báo"));
        if (!notification.getUser().getId().equals(user.getId()))
            throw new ApiException(HttpStatus.FORBIDDEN,"Bạn không có quyền cập nhật thông báo này");
        notification.markRead(Instant.now());
        notifications.save(notification);
    }

    @Transactional
    public void markAllRead(String email) {
        User user=user(email);
        notifications.markAllUnreadAsRead(user.getId(),Instant.now());
    }

    @Transactional
    public void create(User recipient,String title,String message) {
        create(recipient,title,message,null);
    }

    @Transactional
    public void create(User recipient,String title,String message,String actionPath) {
        notifications.save(new Notification(recipient,title,message,actionPath));
    }

    @Transactional
    public void createForActiveReceptionists(String title,String message,String actionPath) {
        users.findByRoleAndActiveTrue(Role.RECEPTIONIST).forEach(
            receptionist -> create(receptionist,title,message,actionPath));
    }

    private User user(String email) {
        return users.findByEmailIgnoreCase(email)
            .orElseThrow(()->new ApiException(HttpStatus.NOT_FOUND,"Không tìm thấy tài khoản"));
    }

    private static NotificationResponse response(Notification notification) {
        return new NotificationResponse(notification.getId(),notification.getTitle(),notification.getMessage(),
            notification.getActionPath(),notification.getCreatedAt(),notification.getReadAt());
    }
}

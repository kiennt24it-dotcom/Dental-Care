package vn.dentalcare.service;

import vn.dentalcare.entity.Notification;
import vn.dentalcare.entity.Role;
import vn.dentalcare.entity.User;
import vn.dentalcare.exception.ApiException;
import vn.dentalcare.repository.NotificationRepository;
import vn.dentalcare.repository.UserRepository;
import org.junit.jupiter.api.Test;
import java.util.Optional;
import java.util.List;
import org.mockito.ArgumentCaptor;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class NotificationServiceTest {
    @Test
    void sendsActionableBookingNotificationsToEveryActiveReceptionist() {
        NotificationRepository notifications=mock(NotificationRepository.class);
        UserRepository users=mock(UserRepository.class);
        User first=mock(User.class);
        User second=mock(User.class);
        when(users.findByRoleAndActiveTrue(Role.RECEPTIONIST)).thenReturn(List.of(first,second));
        NotificationService service=new NotificationService(notifications,users);

        service.createForActiveReceptionists("Yêu cầu đặt lịch mới","Bệnh nhân vừa gửi yêu cầu.",
            "/app/appointments?status=PENDING");

        ArgumentCaptor<Notification> saved=ArgumentCaptor.forClass(Notification.class);
        verify(notifications,times(2)).save(saved.capture());
        assertEquals(List.of(first,second),saved.getAllValues().stream().map(Notification::getUser).toList());
        assertTrue(saved.getAllValues().stream().allMatch(
            notification -> "/app/appointments?status=PENDING".equals(notification.getActionPath())));
    }

    @Test
    void onlyTheOwningUserCanMarkANotificationRead() {
        NotificationRepository notifications=mock(NotificationRepository.class);
        UserRepository users=mock(UserRepository.class);
        User owner=mock(User.class);
        User other=mock(User.class);
        Notification notification=mock(Notification.class);
        when(users.findByEmailIgnoreCase("owner@example.com")).thenReturn(Optional.of(owner));
        when(notifications.findById(12L)).thenReturn(Optional.of(notification));
        when(notification.getUser()).thenReturn(other);
        when(owner.getId()).thenReturn(1L);
        when(other.getId()).thenReturn(2L);
        NotificationService service=new NotificationService(notifications,users);

        assertThrows(ApiException.class,()->service.markRead(12L,"owner@example.com"));
        verify(notifications,never()).save(notification);
        verify(notification,never()).markRead(any());
    }

    @Test
    void marksOwnedNotificationReadIdempotently() {
        NotificationRepository notifications=mock(NotificationRepository.class);
        UserRepository users=mock(UserRepository.class);
        User owner=mock(User.class);
        Notification notification=mock(Notification.class);
        when(users.findByEmailIgnoreCase("owner@example.com")).thenReturn(Optional.of(owner));
        when(owner.getId()).thenReturn(1L);
        when(notifications.findById(12L)).thenReturn(Optional.of(notification));
        when(notification.getUser()).thenReturn(owner);
        NotificationService service=new NotificationService(notifications,users);

        assertDoesNotThrow(()->service.markRead(12L,"owner@example.com"));
        verify(notification).markRead(any());
        verify(notifications).save(notification);
    }
}

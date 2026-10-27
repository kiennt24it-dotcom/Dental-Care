package vn.dentalcare.repository;

import vn.dentalcare.entity.Notification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;
import java.time.Instant;

public interface NotificationRepository extends JpaRepository<Notification,Long> {
    List<Notification> findTop50ByUser_IdOrderByCreatedAtDesc(Long userId);
    long countByUser_IdAndReadAtIsNull(Long userId);
    @Modifying
    @Query("update Notification n set n.readAt=:readAt where n.user.id=:userId and n.readAt is null")
    int markAllUnreadAsRead(@Param("userId") Long userId,@Param("readAt") Instant readAt);
}

package vn.dentalcare.repository;

import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import vn.dentalcare.entity.ChatMessage;

import java.time.Instant;
import java.util.List;

public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long> {
    @Query("""
        select message from ChatMessage message
        where (message.sender.id = :userId and message.recipient.id = :contactId)
           or (message.sender.id = :contactId and message.recipient.id = :userId)
        order by message.createdAt desc
        """)
    List<ChatMessage> findConversation(@Param("userId") Long userId,
                                      @Param("contactId") Long contactId,
                                      Pageable pageable);

    long countBySender_IdAndRecipient_IdAndReadAtIsNull(Long senderId, Long recipientId);

    @Modifying
    @Query("""
        update ChatMessage message set message.readAt = :readAt
        where message.sender.id = :senderId and message.recipient.id = :recipientId
          and message.readAt is null
        """)
    int markConversationRead(@Param("senderId") Long senderId,
                            @Param("recipientId") Long recipientId,
                            @Param("readAt") Instant readAt);
}

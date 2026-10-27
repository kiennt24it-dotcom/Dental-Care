package vn.dentalcare.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import vn.dentalcare.entity.ChatMessage;
import vn.dentalcare.entity.Role;
import vn.dentalcare.entity.User;
import vn.dentalcare.exception.ApiException;
import vn.dentalcare.repository.ChatMessageRepository;
import vn.dentalcare.repository.UserRepository;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ChatServiceTest {
    @Mock ChatMessageRepository messages;
    @Mock UserRepository users;
    @Mock NotificationService notifications;

    private ChatService chat;

    @BeforeEach
    void setUp() {
        chat = new ChatService(messages, users, notifications);
    }

    @Test
    void patientsCannotMessageOtherPatients() {
        User patient = user(1L, Role.PATIENT, "patient@example.test");
        User anotherPatient = user(2L, Role.PATIENT, "other@example.test");
        when(users.findByEmailIgnoreCase(patient.getEmail())).thenReturn(Optional.of(patient));
        when(users.findById(anotherPatient.getId())).thenReturn(Optional.of(anotherPatient));

        assertThrows(ApiException.class,
            () -> chat.send(patient.getEmail(), anotherPatient.getId(), "Hello"));
        verifyNoInteractions(messages, notifications);
    }

    @Test
    void staffAndPatientCanExchangePersistedMessagesAndNotifyRecipient() {
        User patient = user(1L, Role.PATIENT, "patient@example.test");
        User doctor = user(2L, Role.DOCTOR, "doctor@example.test");
        when(users.findByEmailIgnoreCase(patient.getEmail())).thenReturn(Optional.of(patient));
        when(users.findById(doctor.getId())).thenReturn(Optional.of(doctor));
        when(messages.save(any(ChatMessage.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var sent = chat.send(patient.getEmail(), doctor.getId(), "  Tôi bị đau răng  ");

        assertEquals(patient.getId(), sent.senderId());
        assertEquals(doctor.getId(), sent.recipientId());
        assertEquals("Tôi bị đau răng", sent.content());
        verify(notifications).create(doctor, "Tin nhắn mới",
            "Test user (Bệnh nhân) đã gửi cho bạn một tin nhắn.",
            "/app/chat?contactId=1");
    }

    private static User user(Long id, Role role, String email) {
        User user = new User("Test user", email, "hashed-password", role);
        ReflectionTestUtils.setField(user, "id", id);
        return user;
    }
}

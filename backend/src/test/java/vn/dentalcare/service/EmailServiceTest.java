package vn.dentalcare.service;

import org.junit.jupiter.api.Test;
import org.springframework.mail.MailSendException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class EmailServiceTest {
    @Test
    void sendsOnlyAfterTheDatabaseTransactionCommits() {
        JavaMailSender sender=mock(JavaMailSender.class);
        EmailService email=new EmailService(sender,"noreply@example.com");
        TransactionSynchronizationManager.initSynchronization();
        try {
            email.send("patient@example.com","Appointment confirmed","Your appointment is confirmed.");
            verifyNoInteractions(sender);
            TransactionSynchronizationManager.getSynchronizations().get(0).afterCommit();
            verify(sender).send(any(SimpleMailMessage.class));
        } finally {
            TransactionSynchronizationManager.clearSynchronization();
        }
    }

    @Test
    void mailServerFailureDoesNotUndoCommittedClinicWork() {
        JavaMailSender sender=mock(JavaMailSender.class);
        doThrow(new MailSendException("SMTP unavailable")).when(sender).send(any(SimpleMailMessage.class));
        EmailService email=new EmailService(sender,"noreply@example.com");
        TransactionSynchronizationManager.initSynchronization();
        try {
            email.send("patient@example.com","Appointment confirmed","Your appointment is confirmed.");
            assertDoesNotThrow(() -> TransactionSynchronizationManager.getSynchronizations().get(0).afterCommit());
        } finally {
            TransactionSynchronizationManager.clearSynchronization();
        }
    }
}

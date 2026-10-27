package vn.dentalcare.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@Service
public class EmailService {
    private static final Logger log = LoggerFactory.getLogger(EmailService.class);
    private final JavaMailSender sender;
    private final String from;
    public EmailService(JavaMailSender sender, @Value("${MAIL_FROM:noreply@dentalcare.local}") String from) {
        this.sender=sender; this.from=from;
    }
    public void send(String to, String subject, String text) {
        Runnable delivery=() -> deliver(to,subject,text);
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            delivery.run();
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override public void afterCommit() { delivery.run(); }
        });
    }
    private void deliver(String to,String subject,String text) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(from); message.setTo(to); message.setSubject(subject); message.setText(text); sender.send(message);
        } catch (MailException ex) {
            log.error("Email delivery failed for recipient {}. Configure MAIL_HOST and SMTP credentials to enable delivery.", to, ex);
        }
    }
}

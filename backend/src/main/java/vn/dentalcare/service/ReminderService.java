package vn.dentalcare.service;
import vn.dentalcare.entity.*;
import vn.dentalcare.repository.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.*;
@Service
public class ReminderService {
    private static final Logger log=LoggerFactory.getLogger(ReminderService.class);
    private final AppointmentRepository appointments;
    private final FollowUpRepository followUps;
    private final EmailService email;
    public ReminderService(AppointmentRepository appointments,FollowUpRepository followUps,EmailService email) {
        this.appointments=appointments;this.followUps=followUps;this.email=email;
    }
    @Scheduled(cron="0 0 * * * *")
    @Transactional
    public void sendUpcomingReminders() {
        Instant now=Instant.now();
        Instant from=now.plus(Duration.ofHours(23)),to=now.plus(Duration.ofHours(24));
        for(Appointment appointment:appointments.findByStatusAndStartAtBetweenAndReminderSentFalse(AppointmentStatus.CONFIRMED,from,to)) {
            email.send(appointment.getPatient().getEmail(),"Nhắc lịch hẹn DentalCare",
                "Bạn có lịch khám với " + appointment.getDoctor().getFullName() + " sau khoảng 24 giờ.");
            appointment.setReminderSent(true);
        }
        for(FollowUp followUp:followUps.findByScheduledAtBetweenAndStatusAndReminderSentFalse(from,to,"PLANNED")) {
            email.send(followUp.getPatient().getEmail(),"Nhắc lịch tái khám DentalCare",
                "Bạn có lịch tái khám với " + followUp.getDoctor().getFullName() + " sau khoảng 24 giờ.");
            followUp.setReminderSent(true);
        }
    }
}

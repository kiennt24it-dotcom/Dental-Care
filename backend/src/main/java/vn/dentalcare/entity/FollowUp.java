package vn.dentalcare.entity;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "follow_ups")
public class FollowUp {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "patient_id", nullable = false) private User patient;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "doctor_id", nullable = false) private User doctor;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "previous_appointment_id", nullable = false) private Appointment previousAppointment;
    @Column(nullable = false) private Instant scheduledAt;
    @Column(nullable = false, length = 20) private String status = "PLANNED";
    @Column(length = 300) private String reason;
    @Column(length = 1000) private String notes;
    @Column(nullable = false) private boolean reminderSent;
    protected FollowUp() {}
    public FollowUp(User patient, User doctor, Appointment previousAppointment, Instant scheduledAt, String reason, String notes) {
        this.patient=patient; this.doctor=doctor; this.previousAppointment=previousAppointment; this.scheduledAt=scheduledAt; this.reason=reason; this.notes=notes;
    }
    public Long getId() { return id; }
    public User getPatient() { return patient; }
    public User getDoctor() { return doctor; }
    public Appointment getPreviousAppointment() { return previousAppointment; }
    public Instant getScheduledAt() { return scheduledAt; }
    public String getStatus() { return status; }
    public String getReason() { return reason; }
    public String getNotes() { return notes; }
    public boolean isReminderSent() { return reminderSent; }
    public void setReminderSent(boolean reminderSent) { this.reminderSent=reminderSent; }
}

package vn.dentalcare.entity;

import jakarta.persistence.*;
import java.time.Instant;
import java.math.BigDecimal;

@Entity
@Table(name = "appointments", indexes = {
    @Index(name = "idx_appointment_doctor_start", columnList = "doctor_id,start_at"),
    @Index(name = "idx_appointment_patient_start", columnList = "patient_id,start_at")
})
public class Appointment {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "patient_id", nullable = false) private User patient;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "doctor_id", nullable = false) private User doctor;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "service_id", nullable = false) private DentalService service;
    @Column(name="price_at_booking",nullable=false,precision=12,scale=2) private BigDecimal priceAtBooking;
    @Column(name = "start_at", nullable = false) private Instant startAt;
    @Column(name = "end_at", nullable = false) private Instant endAt;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 20) private AppointmentStatus status = AppointmentStatus.PENDING;
    @Column(length = 2000) private String symptoms;
    @Column(length = 2000) private String internalNote;
    @Column(nullable = false) private boolean followUpRequested;
    @Column(nullable = false) private boolean reminderSent;
    @Column(nullable = false, updatable = false) private Instant createdAt = Instant.now();
    protected Appointment() {}
    public Appointment(User patient, User doctor, DentalService service, Instant startAt, Instant endAt, String symptoms) {
        this.patient=patient; this.doctor=doctor; this.service=service; this.priceAtBooking=service.getPrice(); this.startAt=startAt; this.endAt=endAt; this.symptoms=symptoms;
    }
    public Long getId() { return id; }
    public User getPatient() { return patient; }
    public User getDoctor() { return doctor; }
    public DentalService getService() { return service; }
    public BigDecimal getPriceAtBooking() { return priceAtBooking; }
    public Instant getStartAt() { return startAt; }
    public Instant getEndAt() { return endAt; }
    public AppointmentStatus getStatus() { return status; }
    public void setStatus(AppointmentStatus status) { this.status = status; }
    public String getSymptoms() { return symptoms; }
    public String getInternalNote() { return internalNote; }
    public void setInternalNote(String internalNote) { this.internalNote = internalNote; }
    public boolean isFollowUpRequested() { return followUpRequested; }
    public boolean isReminderSent() { return reminderSent; }
    public void setReminderSent(boolean reminderSent) { this.reminderSent=reminderSent; }
    public Instant getCreatedAt() { return createdAt; }
}

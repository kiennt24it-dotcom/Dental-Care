package vn.dentalcare.entity;

import jakarta.persistence.*;
import java.time.Instant;
import java.time.LocalDate;

@Entity
@Table(name = "medical_records", uniqueConstraints = @UniqueConstraint(columnNames = "appointment_id"))
public class MedicalRecord {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "patient_id", nullable = false) private User patient;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "doctor_id", nullable = false) private User doctor;
    @OneToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "appointment_id", nullable = false) private Appointment appointment;
    @Column(nullable = false) private LocalDate examinationDate;
    @Column(length = 2000) private String symptoms;
    @Column(length = 2000) private String diagnosis;
    @Column(length = 4000) private String treatment;
    @Column(length = 3000) private String prescription;
    @Column(length = 3000) private String notes;
    private LocalDate followUpDate;
    @Column(nullable = false, updatable = false) private Instant createdAt = Instant.now();
    protected MedicalRecord() {}
    public MedicalRecord(User patient, User doctor, Appointment appointment, LocalDate date, String symptoms, String diagnosis, String treatment, String prescription, String notes, LocalDate followUpDate) {
        this.patient=patient; this.doctor=doctor; this.appointment=appointment; this.examinationDate=date; this.symptoms=symptoms; this.diagnosis=diagnosis; this.treatment=treatment; this.prescription=prescription; this.notes=notes; this.followUpDate=followUpDate;
    }
    public Long getId() { return id; }
    public User getPatient() { return patient; }
    public User getDoctor() { return doctor; }
    public Appointment getAppointment() { return appointment; }
    public LocalDate getExaminationDate() { return examinationDate; }
    public String getSymptoms() { return symptoms; }
    public String getDiagnosis() { return diagnosis; }
    public String getTreatment() { return treatment; }
    public String getPrescription() { return prescription; }
    public String getNotes() { return notes; }
    public LocalDate getFollowUpDate() { return followUpDate; }
}

package vn.dentalcare.entity;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "doctor_profile_submissions", uniqueConstraints = @UniqueConstraint(
    name = "uk_doctor_profile_submission_doctor", columnNames = "doctor_id"))
public class DoctorProfileSubmission {
    public enum Status { NOT_SUBMITTED, PENDING, APPROVED, REJECTED }

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @OneToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "doctor_id", nullable = false, unique = true)
    private User doctor;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "specialty_id", nullable = false)
    private Specialty specialty;
    @Column(nullable = false) private int experienceYears;
    @Column(length = 200) private String qualification;
    @Column(length = 2000) private String description;
    @Column(length = 3000) private String achievements;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 20)
    private Status status = Status.PENDING;
    @Column(length = 1000) private String reviewNote;
    @Column(nullable = false) private Instant submittedAt = Instant.now();

    protected DoctorProfileSubmission() {}

    public DoctorProfileSubmission(User doctor, Specialty specialty, int experienceYears,
        String qualification, String description, String achievements) {
        this.doctor=doctor;this.specialty=specialty;this.experienceYears=experienceYears;
        this.qualification=qualification;this.description=description;this.achievements=achievements;
    }

    public Long getId() { return id; }
    public User getDoctor() { return doctor; }
    public Specialty getSpecialty() { return specialty; }
    public int getExperienceYears() { return experienceYears; }
    public String getQualification() { return qualification; }
    public String getDescription() { return description; }
    public String getAchievements() { return achievements; }
    public Status getStatus() { return status; }
    public String getReviewNote() { return reviewNote; }
    public Instant getSubmittedAt() { return submittedAt; }

    public void submit(Specialty specialty, int experienceYears, String qualification, String description, String achievements) {
        this.specialty=specialty;this.experienceYears=experienceYears;this.qualification=qualification;
        this.description=description;this.achievements=achievements;this.status=Status.PENDING;
        this.reviewNote=null;this.submittedAt=Instant.now();
    }

    public void review(Status status, String reviewNote) {
        this.status=status;this.reviewNote=reviewNote;
    }
}

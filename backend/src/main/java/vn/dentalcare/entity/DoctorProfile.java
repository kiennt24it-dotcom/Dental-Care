package vn.dentalcare.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "doctor_profiles")
public class DoctorProfile {
    @Id @Column(name = "user_id") private Long id;
    @OneToOne(fetch = FetchType.LAZY, optional = false) @MapsId @JoinColumn(name = "user_id") private User user;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "specialty_id") private Specialty specialty;
    @Column(nullable = false) private int experienceYears;
    @Column(length = 200) private String qualification;
    @Column(length = 2000) private String description;
    @Column(length = 3000) private String achievements;
    protected DoctorProfile() {}
    public DoctorProfile(User user, Specialty specialty, int experienceYears, String qualification, String description) {
        this.user=user; this.specialty=specialty; this.experienceYears=experienceYears; this.qualification=qualification; this.description=description;
    }
    public DoctorProfile(User user, Specialty specialty, int experienceYears, String qualification, String description, String achievements) {
        this(user,specialty,experienceYears,qualification,description);this.achievements=achievements;
    }
    public Long getId() { return id; }
    public User getUser() { return user; }
    public Specialty getSpecialty() { return specialty; }
    public int getExperienceYears() { return experienceYears; }
    public String getQualification() { return qualification; }
    public String getDescription() { return description; }
    public String getAchievements() { return achievements; }
    public void update(Specialty specialty,int experienceYears,String qualification,String description,String achievements) {
        this.specialty=specialty;this.experienceYears=experienceYears;this.qualification=qualification;this.description=description;this.achievements=achievements;
    }
}

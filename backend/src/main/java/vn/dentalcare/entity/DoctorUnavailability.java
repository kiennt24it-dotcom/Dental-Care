package vn.dentalcare.entity;
import jakarta.persistence.*;
import java.time.Instant;
@Entity
@Table(name="doctor_unavailabilities",indexes=@Index(name="idx_unavailability_doctor_start",columnList="doctor_id,start_at"))
public class DoctorUnavailability {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
    @ManyToOne(fetch=FetchType.LAZY,optional=false) @JoinColumn(name="doctor_id",nullable=false) private User doctor;
    @Column(name="start_at",nullable=false) private Instant startAt;
    @Column(name="end_at",nullable=false) private Instant endAt;
    @Column(length=300) private String reason;
    @Column(nullable=false) private boolean active=true;
    protected DoctorUnavailability() {}
    public DoctorUnavailability(User doctor,Instant startAt,Instant endAt,String reason) {
        this.doctor=doctor;this.startAt=startAt;this.endAt=endAt;this.reason=reason;
    }
    public Long getId(){return id;}
    public User getDoctor(){return doctor;}
    public Instant getStartAt(){return startAt;}
    public Instant getEndAt(){return endAt;}
    public String getReason(){return reason;}
    public boolean isActive(){return active;}
}

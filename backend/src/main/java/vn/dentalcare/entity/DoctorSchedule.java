package vn.dentalcare.entity;

import jakarta.persistence.*;
import java.time.DayOfWeek;
import java.time.LocalTime;

@Entity
@Table(name = "doctor_schedules", uniqueConstraints = @UniqueConstraint(columnNames = {"doctor_id", "day_of_week"}))
public class DoctorSchedule {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "doctor_id", nullable = false) private User doctor;
    @Enumerated(EnumType.STRING) @Column(name = "day_of_week", nullable = false, length = 12) private DayOfWeek dayOfWeek;
    @Column(nullable = false) private LocalTime startTime;
    @Column(nullable = false) private LocalTime endTime;
    private LocalTime breakStart;
    private LocalTime breakEnd;
    @Column(nullable = false) private boolean active = true;
    protected DoctorSchedule() {}
    public DoctorSchedule(User doctor, DayOfWeek day, LocalTime start, LocalTime end, LocalTime breakStart, LocalTime breakEnd) {
        this.doctor=doctor; this.dayOfWeek=day; this.startTime=start; this.endTime=end; this.breakStart=breakStart; this.breakEnd=breakEnd;
    }
    public Long getId() { return id; }
    public User getDoctor() { return doctor; }
    public DayOfWeek getDayOfWeek() { return dayOfWeek; }
    public LocalTime getStartTime() { return startTime; }
    public LocalTime getEndTime() { return endTime; }
    public LocalTime getBreakStart() { return breakStart; }
    public LocalTime getBreakEnd() { return breakEnd; }
    public boolean isActive() { return active; }
    public void update(LocalTime start,LocalTime end,LocalTime breakStart,LocalTime breakEnd,boolean active) {
        this.startTime=start;this.endTime=end;this.breakStart=breakStart;this.breakEnd=breakEnd;this.active=active;
    }
}

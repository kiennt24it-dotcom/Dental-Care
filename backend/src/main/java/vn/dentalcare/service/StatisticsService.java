package vn.dentalcare.service;
import vn.dentalcare.dto.StatisticsDtos.*;
import vn.dentalcare.entity.*;
import vn.dentalcare.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.beans.factory.annotation.Value;
import java.math.BigDecimal;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
@Service
public class StatisticsService {
    private final UserRepository users;
    private final AppointmentRepository appointments;
    private final ZoneId clinicZone;
    public StatisticsService(UserRepository users,AppointmentRepository appointments,@Value("${app.clinic-timezone:Asia/Ho_Chi_Minh}") String timezone) { this.users=users;this.appointments=appointments;this.clinicZone=ZoneId.of(timezone); }
    @Transactional(readOnly=true)
    public Overview overview() {
        LocalDate today=LocalDate.now(clinicZone);
        Instant dayStart=today.atStartOfDay(clinicZone).toInstant(),dayEnd=today.plusDays(1).atStartOfDay(clinicZone).toInstant();
        long patients=users.countByRoleAndActiveTrue(Role.PATIENT),doctors=users.countByRoleAndActiveTrue(Role.DOCTOR);
        long todayCount=appointments.countByStartAtBetween(dayStart,dayEnd);
        long completed=appointments.countByStatus(AppointmentStatus.COMPLETED),cancelled=appointments.countByStatus(AppointmentStatus.CANCELLED),noShow=appointments.countByStatus(AppointmentStatus.NO_SHOW);
        long total=completed+noShow;
        List<Appointment> recent=appointments.findByStartAtBetweenOrderByStartAt(today.minusDays(6).atStartOfDay(clinicZone).toInstant(),dayEnd);
        DateTimeFormatter format=DateTimeFormatter.ofPattern("EEE d",Locale.forLanguageTag("vi-VN"));
        Map<LocalDate,Long> byDate=recent.stream().collect(Collectors.groupingBy(a->a.getStartAt().atZone(clinicZone).toLocalDate(),Collectors.counting()));
        List<DayMetric> daily=new ArrayList<>();
        for(int i=6;i>=0;i--){LocalDate d=today.minusDays(i);daily.add(new DayMetric(d.format(format),byDate.getOrDefault(d,0L)));}
        Map<String,Long> services=recent.stream().collect(Collectors.groupingBy(a->a.getService().getName(),Collectors.counting()));
        List<ServiceMetric> popular=services.entrySet().stream().sorted(Map.Entry.<String,Long>comparingByValue().reversed()).limit(5).map(e->new ServiceMetric(e.getKey(),e.getValue())).toList();
        BigDecimal revenue=appointments.findByStatusAndStartAtBetween(AppointmentStatus.COMPLETED,Instant.EPOCH,Instant.now())
            .stream().map(Appointment::getPriceAtBooking).reduce(BigDecimal.ZERO,BigDecimal::add);
        return new Overview(patients,doctors,todayCount,completed,cancelled,noShow,total==0?0.0:(double)noShow/total,revenue,daily,popular);
    }
}

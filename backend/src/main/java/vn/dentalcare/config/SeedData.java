package vn.dentalcare.config;

import vn.dentalcare.entity.*;
import vn.dentalcare.repository.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;

@Configuration
public class SeedData {
    private static final String DEMO_PASSWORD = "DentalCare@2025";

    @Bean
    CommandLineRunner seedDentalCare(@Value("${app.seed.enabled:true}") boolean enabled,
        @Value("${app.clinic-timezone:Asia/Ho_Chi_Minh}") String timezone,
        UserRepository users, SpecialtyRepository specialties, DentalServiceRepository services,
        DoctorProfileRepository profiles, DoctorScheduleRepository schedules, AppointmentRepository appointments,
        MedicalRecordRepository records, FollowUpRepository followUps, DoctorUnavailabilityRepository unavailabilities,
        NotificationRepository notifications, PasswordEncoder encoder, PlatformTransactionManager transactionManager) {
        return args -> {
            if (!enabled) return;
            TransactionTemplate transaction = new TransactionTemplate(transactionManager);
            transaction.executeWithoutResult(status -> {
                if (users.existsByEmailIgnoreCase("demo.patient01@dentalcare.local")) return;

                User admin = user(users, encoder, "Quản trị DentalCare", "admin@dentalcare.vn", Role.ADMIN);
                admin.setPhone("0900000001");
                users.save(admin);
                user(users, encoder, "Nguyễn Thu Hà", "le.tan@dentalcare.vn", Role.RECEPTIONIST);
                User originalDoctor = user(users, encoder, "Nguyễn Minh Tuấn", "bacsi.tuan@dentalcare.vn", Role.DOCTOR);
                User originalDoctorTwo = user(users, encoder, "Trần Ngọc Mai", "bacsi.mai@dentalcare.vn", Role.DOCTOR);
                User originalPatient = user(users, encoder, "Lê Minh Anh", "benhnhan@dentalcare.vn", Role.PATIENT);

                List<Specialty> specialtyRows = ensureSpecialties(specialties);
                List<DentalService> serviceRows = ensureServices(services);
                List<User> doctors = new ArrayList<>(List.of(originalDoctor, originalDoctorTwo));
                String[][] doctorData = {
                    {"Nguyễn Hoàng Long", "demo.doctor01@dentalcare.local", "Nha khoa phục hình", "Bác sĩ CKI Phục hình", "8"},
                    {"Phạm Thu Trang", "demo.doctor02@dentalcare.local", "Nội nha", "Thạc sĩ Răng Hàm Mặt", "11"},
                    {"Võ Quốc Bảo", "demo.doctor03@dentalcare.local", "Nha khoa trẻ em", "Bác sĩ Răng Hàm Mặt", "6"}
                };
                for (String[] data : doctorData)
                    doctors.add(user(users, encoder, data[0], data[1], Role.DOCTOR));

                List<User> patients = new ArrayList<>();
                patients.add(originalPatient);
                String[][] patientData = {
                    {"Nguyễn Gia Hân", "demo.patient01@dentalcare.local"},
                    {"Trần Minh Khoa", "demo.patient02@dentalcare.local"},
                    {"Lê Ngọc Bảo Châu", "demo.patient03@dentalcare.local"},
                    {"Phạm Đức Anh", "demo.patient04@dentalcare.local"},
                    {"Đặng Khánh Linh", "demo.patient05@dentalcare.local"},
                    {"Bùi Nhật Nam", "demo.patient06@dentalcare.local"},
                    {"Hoàng Thảo Vy", "demo.patient07@dentalcare.local"},
                    {"Đỗ Quang Huy", "demo.patient08@dentalcare.local"}
                };
                for (String[] data : patientData)
                    patients.add(user(users, encoder, data[0], data[1], Role.PATIENT));

                for (int i = 0; i < doctors.size(); i++) {
                    User doctor = doctors.get(i);
                    Specialty specialty = specialtyRows.get(i % specialtyRows.size());
                    if (!profiles.existsById(doctor.getId())) {
                        String qualification = i < 2 ? "Bác sĩ Răng Hàm Mặt" : doctorData[i - 2][3];
                        int experience = i < 2 ? (i == 0 ? 12 : 9) : Integer.parseInt(doctorData[i - 2][4]);
                        profiles.save(new DoctorProfile(doctor, specialty, experience, qualification,
                            "Tư vấn và điều trị tận tâm theo phác đồ phù hợp với từng bệnh nhân."));
                    }
                    for (DayOfWeek day : List.of(DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY,
                        DayOfWeek.THURSDAY, DayOfWeek.FRIDAY, DayOfWeek.SATURDAY)) {
                        if (schedules.findByDoctor_IdAndDayOfWeek(doctor.getId(), day).isEmpty())
                            schedules.save(new DoctorSchedule(doctor, day, LocalTime.of(8, 30), LocalTime.of(17, 0),
                                LocalTime.of(12, 0), LocalTime.of(13, 0)));
                    }
                }

                ZoneId zone = ZoneId.of(timezone);
                List<Appointment> completedAppointments = new ArrayList<>();
                for (int i = 0; i < 8; i++) {
                    User patient = patients.get(i);
                    User doctor = doctors.get(i % doctors.size());
                    DentalService service = serviceRows.get(i % serviceRows.size());
                    LocalDate visitDate = workingDate(LocalDate.now(zone).minusDays(i + 1L));
                    Instant start = visitDate.atTime(9 + (i % 3), 0).atZone(zone).toInstant();
                    Appointment appointment = new Appointment(patient, doctor, service, start,
                        start.plusSeconds(service.getDurationMinutes() * 60L),
                        List.of("Ê buốt nhẹ khi uống nước lạnh", "Đau răng hàm khi nhai", "Khám răng định kỳ",
                            "Nướu hơi nhạy cảm").get(i % 4));
                    appointment.setStatus(AppointmentStatus.COMPLETED);
                    completedAppointments.add(appointments.save(appointment));
                }

                LocalDate nextWorkingDay = workingDate(LocalDate.now(zone).plusDays(1));
                Instant firstUpcoming = nextWorkingDay.atTime(9, 0).atZone(zone).toInstant();
                if (appointments.findByPatient_IdOrderByStartAtDesc(originalPatient.getId()).stream()
                    .noneMatch(appointment -> appointment.getStatus() == AppointmentStatus.PENDING
                        && appointment.getStartAt().equals(firstUpcoming))) {
                    DentalService examination = serviceRows.get(0);
                    appointments.save(new Appointment(originalPatient, originalDoctor, examination, firstUpcoming,
                        firstUpcoming.plusSeconds(examination.getDurationMinutes() * 60L),
                        "Ê buốt nhẹ khi uống nước lạnh"));
                }

                LocalDate today = LocalDate.now(zone);
                for (int i = 0; i < completedAppointments.size(); i++) {
                    Appointment appointment = completedAppointments.get(i);
                    LocalDate examDate = LocalDate.ofInstant(appointment.getStartAt(), zone);
                    LocalDate followUpDate = today.plusDays(30L + i);
                    if (!records.existsByAppointment_Id(appointment.getId())) {
                        records.save(new MedicalRecord(appointment.getPatient(), appointment.getDoctor(), appointment,
                            examDate, appointment.getSymptoms(),
                            List.of("Răng nhạy cảm mức độ nhẹ", "Sâu răng mặt nhai", "Chưa ghi nhận tổn thương").get(i % 3),
                            appointment.getService().getName() + " và hướng dẫn chăm sóc tại nhà",
                            "Chưa cần sử dụng thuốc", "Tái khám nếu triệu chứng kéo dài.", followUpDate));
                    }
                    if (i < 5) {
                        Instant scheduled = followUpDate.atTime(9, 0).atZone(zone).toInstant();
                        followUps.save(new FollowUp(appointment.getPatient(), appointment.getDoctor(), appointment,
                            scheduled, "Tái khám định kỳ", "Mang theo hồ sơ điều trị lần trước."));
                    }
                }

                for (int i = 0; i < doctors.size(); i++) {
                    User doctor = doctors.get(i);
                    LocalDate unavailableDate = workingDate(today.plusDays(20L + i));
                    Instant start = unavailableDate.atTime(14, 0).atZone(zone).toInstant();
                    unavailabilities.save(new DoctorUnavailability(doctor, start, start.plus(Duration.ofHours(1)),
                        "Lịch nghỉ/bận mẫu — dữ liệu demo"));
                }

                for (int i = 0; i < patients.size(); i++) {
                    User patient = patients.get(i);
                    notifications.save(new Notification(patient, "Chào mừng đến với DentalCare",
                        "Đây là thông báo mẫu. Bạn có thể đặt lịch khám hoặc cập nhật hồ sơ cá nhân."));
                }
                for (Appointment appointment : completedAppointments)
                    notifications.save(new Notification(appointment.getPatient(), "Hồ sơ điều trị đã được cập nhật",
                        "Bác sĩ " + appointment.getDoctor().getFullName() + " đã hoàn tất hồ sơ khám của bạn."));
            });
        };
    }

    private static User user(UserRepository users, PasswordEncoder encoder, String name, String email, Role role) {
        return users.findByEmailIgnoreCase(email).orElseGet(() -> {
            User created = new User(name, email, encoder.encode(DEMO_PASSWORD), role);
            return users.save(created);
        });
    }

    private static List<Specialty> ensureSpecialties(SpecialtyRepository repository) {
        List<Specialty> result = new ArrayList<>(repository.findAll());
        String[][] data = {
            {"Nha khoa phục hình", "Phục hồi chức năng và thẩm mỹ răng."},
            {"Nội nha", "Điều trị tủy và bảo tồn răng tự nhiên."},
            {"Nha khoa trẻ em", "Chăm sóc sức khỏe răng miệng cho trẻ."}
        };
        for (String[] item : data) {
            Specialty specialty = result.stream().filter(row -> row.getName().equals(item[0])).findFirst()
                .orElseGet(() -> repository.save(new Specialty(item[0], item[1])));
            if (!result.contains(specialty)) result.add(specialty);
        }
        return result;
    }

    private static List<DentalService> ensureServices(DentalServiceRepository repository) {
        List<DentalService> result = new ArrayList<>(repository.findAll());
        Object[][] data = {
            {"Nhổ răng khôn", "Tiểu phẫu", "1500000", 60, "Thăm khám và nhổ răng khôn theo chỉ định."},
            {"Điều trị tủy", "Điều trị", "1800000", 90, "Điều trị tủy răng và phục hồi theo phác đồ."},
            {"Tư vấn chỉnh nha", "Chỉnh nha", "300000", 45, "Khám và tư vấn kế hoạch chỉnh nha."},
            {"Trám bít hố rãnh", "Nha khoa trẻ em", "250000", 30, "Hỗ trợ bảo vệ mặt nhai khỏi nguy cơ sâu răng."}
        };
        for (Object[] item : data) {
            String name = (String) item[0];
            DentalService service = result.stream().filter(row -> row.getName().equals(name)).findFirst()
                .orElseGet(() -> repository.save(new DentalService(name, (String) item[1],
                    new BigDecimal((String) item[2]), (Integer) item[3], (String) item[4])));
            if (!result.contains(service)) result.add(service);
        }
        return result;
    }

    private static LocalDate workingDate(LocalDate date) {
        while (date.getDayOfWeek() == DayOfWeek.SUNDAY) date = date.plusDays(1);
        return date;
    }
}

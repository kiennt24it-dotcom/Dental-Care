package vn.dentalcare.service;

import vn.dentalcare.dto.AppointmentDtos.CreateRequest;
import vn.dentalcare.dto.AppointmentDtos.StatusRequest;
import vn.dentalcare.entity.*;
import vn.dentalcare.exception.ApiException;
import vn.dentalcare.repository.*;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class AppointmentServiceTest {
    @Test
    void bookingReadsCommittedAppointmentsAfterSerializingOnTheDoctorSchedule() throws Exception {
        Transactional transaction = AppointmentService.class
            .getMethod("create", CreateRequest.class, String.class, Role.class)
            .getAnnotation(Transactional.class);

        assertNotNull(transaction);
        assertEquals(Isolation.READ_COMMITTED, transaction.isolation());
    }

    @Test
    void loadsAppointmentsForRequestedClinicDateRange() {
        AppointmentRepository appointments=mock(AppointmentRepository.class);
        UserRepository users=mock(UserRepository.class);
        User receptionist=mock(User.class);
        when(receptionist.getId()).thenReturn(9L);
        when(users.findByEmailIgnoreCase("staff@example.com")).thenReturn(Optional.of(receptionist));
        AppointmentService service=new AppointmentService(appointments,mock(DoctorScheduleRepository.class),users,
            mock(DentalServiceRepository.class),mock(EmailService.class),mock(NotificationService.class),
            mock(DoctorUnavailabilityRepository.class),"Asia/Ho_Chi_Minh");
        LocalDate from=LocalDate.of(2030,1,7);
        LocalDate to=from.plusDays(7);

        assertTrue(service.listBetween("staff@example.com",Role.RECEPTIONIST,from,to,null).isEmpty());

        verify(appointments).findByStartAtGreaterThanEqualAndStartAtLessThanOrderByStartAt(
            from.atStartOfDay(ZoneId.of("Asia/Ho_Chi_Minh")).toInstant(),
            to.atStartOfDay(ZoneId.of("Asia/Ho_Chi_Minh")).toInstant());
    }

    @Test
    void rejectsUnboundedOrReversedAppointmentRanges() {
        AppointmentService service=new AppointmentService(mock(AppointmentRepository.class),mock(DoctorScheduleRepository.class),
            mock(UserRepository.class),mock(DentalServiceRepository.class),mock(EmailService.class),mock(NotificationService.class),
            mock(DoctorUnavailabilityRepository.class),"Asia/Ho_Chi_Minh");

        assertThrows(ApiException.class,()->service.listBetween("staff@example.com",Role.ADMIN,null,LocalDate.now(),null));
        assertThrows(ApiException.class,()->service.listBetween("staff@example.com",Role.ADMIN,LocalDate.now(),LocalDate.now(),null));
        assertThrows(ApiException.class,()->service.listBetween("staff@example.com",Role.ADMIN,LocalDate.now(),LocalDate.now().plusDays(32),null));
        assertThrows(ApiException.class,()->service.page("staff@example.com",Role.ADMIN,null,null,null,null,null,-1,20));
        assertThrows(ApiException.class,()->service.page("staff@example.com",Role.ADMIN,null,null,null,null,null,0,101));
    }

    @Test
    void returnsPageMetadataForAppointmentSearch() {
        AppointmentRepository appointments=mock(AppointmentRepository.class);
        UserRepository users=mock(UserRepository.class);
        when(users.findByEmailIgnoreCase("admin@example.com")).thenReturn(Optional.of(mock(User.class)));
        when(appointments.findAll(org.mockito.ArgumentMatchers.<Specification<Appointment>>any(),
            org.mockito.ArgumentMatchers.<Pageable>any()))
            .thenReturn(Page.empty(org.springframework.data.domain.PageRequest.of(1,10,
                org.springframework.data.domain.Sort.by(org.springframework.data.domain.Sort.Direction.ASC,"startAt"))));
        AppointmentService service=new AppointmentService(appointments,mock(DoctorScheduleRepository.class),users,
            mock(DentalServiceRepository.class),mock(EmailService.class),mock(NotificationService.class),
            mock(DoctorUnavailabilityRepository.class),"Asia/Ho_Chi_Minh");

        Page<vn.dentalcare.dto.AppointmentDtos.AppointmentResponse> result=service.page(
            "admin@example.com",Role.ADMIN,LocalDate.of(2030,1,1),null,null,"CONFIRMED","Nguyen",1,10);

        assertEquals(1,result.getNumber());
        assertEquals(10,result.getSize());
        verify(appointments).findAll(org.mockito.ArgumentMatchers.<Specification<Appointment>>any(),
            eq(org.springframework.data.domain.PageRequest.of(1,10,
                org.springframework.data.domain.Sort.by(org.springframework.data.domain.Sort.Direction.ASC,"startAt"))));
    }

    @Test
    void hidesInternalAppointmentNotesFromPatients() {
        User patient=new User("Patient","patient@example.com","hash",Role.PATIENT);
        User doctor=new User("Doctor","doctor@example.com","hash",Role.DOCTOR);
        DentalService treatment=new DentalService("Exam","General",new BigDecimal("200000"),30,"Exam");
        Appointment appointment=new Appointment(patient,doctor,treatment,Instant.parse("2030-01-01T09:00:00Z"),
            Instant.parse("2030-01-01T09:30:00Z"),"Tooth sensitivity");
        appointment.setInternalNote("Private clinic note");

        assertNull(AppointmentService.response(appointment,Role.PATIENT).internalNote());
        assertEquals("Private clinic note",AppointmentService.response(appointment,Role.DOCTOR).internalNote());
    }

    @Test
    void refusesOverlappingDoctorBooking() {
        AppointmentRepository appointments=mock(AppointmentRepository.class);
        DoctorScheduleRepository schedules=mock(DoctorScheduleRepository.class);
        UserRepository users=mock(UserRepository.class);
        DentalServiceRepository services=mock(DentalServiceRepository.class);
        DoctorUnavailabilityRepository unavailabilities=mock(DoctorUnavailabilityRepository.class);
        EmailService email=mock(EmailService.class);
        AppointmentService service=new AppointmentService(appointments,schedules,users,services,email,
            mock(NotificationService.class),unavailabilities,"Asia/Ho_Chi_Minh");

        User patient=new User("Patient","patient@example.com","hash",Role.PATIENT);
        User doctor=new User("Doctor","doctor@example.com","hash",Role.DOCTOR);
        DentalService treatment=new DentalService("Exam","General",new BigDecimal("200000"),30,"Exam");
        DoctorSchedule schedule=new DoctorSchedule(doctor,DayOfWeek.TUESDAY,LocalTime.of(8,0),LocalTime.of(17,0),
            LocalTime.of(12,0),LocalTime.of(13,0));
        when(users.findByEmailIgnoreCase(patient.getEmail())).thenReturn(Optional.of(patient));
        when(users.lockById(any())).thenReturn(Optional.of(patient));
        when(users.findById(10L)).thenReturn(Optional.of(doctor));
        when(services.findById(20L)).thenReturn(Optional.of(treatment));
        when(schedules.lockByDoctorAndDay(any(),eq(DayOfWeek.TUESDAY))).thenReturn(Optional.of(schedule));
        when(appointments.existsByDoctor_IdAndStatusInAndStartAtLessThanAndEndAtGreaterThan(any(),anyList(),any(),any())).thenReturn(true);

        Instant start=LocalDate.of(2030,1,1).atTime(9,0).toInstant(ZoneOffset.UTC);
        ApiException error=assertThrows(ApiException.class,()->service.create(
            new CreateRequest(10L,20L,start,"Check",null),patient.getEmail(),Role.PATIENT));
        assertEquals("Khung giờ vừa được đặt, vui lòng chọn khung giờ khác",error.getMessage());
        verify(appointments,never()).save(any());
    }

    @Test
    void notifiesActiveReceptionistsWhenPatientRequestsABooking() {
        AppointmentRepository appointments=mock(AppointmentRepository.class);
        DoctorScheduleRepository schedules=mock(DoctorScheduleRepository.class);
        UserRepository users=mock(UserRepository.class);
        DentalServiceRepository services=mock(DentalServiceRepository.class);
        NotificationService notifications=mock(NotificationService.class);
        User patient=mock(User.class);
        User doctor=mock(User.class);
        DentalService treatment=new DentalService("Exam","General",new BigDecimal("200000"),30,"Exam");
        DoctorSchedule schedule=new DoctorSchedule(doctor,DayOfWeek.TUESDAY,LocalTime.of(8,0),LocalTime.of(17,0),
            LocalTime.of(12,0),LocalTime.of(13,0));
        AppointmentService service=new AppointmentService(appointments,schedules,users,services,mock(EmailService.class),
            notifications,mock(DoctorUnavailabilityRepository.class),"Asia/Ho_Chi_Minh");
        when(patient.getId()).thenReturn(1L);
        when(patient.getEmail()).thenReturn("patient@example.com");
        when(patient.getFullName()).thenReturn("Patient Name");
        when(doctor.getId()).thenReturn(10L);
        when(doctor.getRole()).thenReturn(Role.DOCTOR);
        when(doctor.isActive()).thenReturn(true);
        when(doctor.getFullName()).thenReturn("Doctor Name");
        when(users.findByEmailIgnoreCase(patient.getEmail())).thenReturn(Optional.of(patient));
        when(users.lockById(1L)).thenReturn(Optional.of(patient));
        when(users.findById(10L)).thenReturn(Optional.of(doctor));
        when(services.findById(20L)).thenReturn(Optional.of(treatment));
        when(schedules.lockByDoctorAndDay(10L,DayOfWeek.TUESDAY)).thenReturn(Optional.of(schedule));
        when(appointments.save(any(Appointment.class))).thenAnswer(invocation->invocation.getArgument(0));
        Instant start=Instant.parse("2030-01-01T02:00:00Z");

        service.create(new CreateRequest(10L,20L,start,"Check",null),patient.getEmail(),Role.PATIENT);

        verify(notifications).create(eq(patient),eq("Đã nhận yêu cầu đặt lịch"),anyString(),eq("/app/appointments"));
        verify(notifications).createForActiveReceptionists(eq("Yêu cầu đặt lịch mới"),contains("Patient Name"),
            eq("/app/appointments?status=PENDING"));
    }

    @Test
    void receptionistOwnsBookingApprovalAndDoctorCannotConfirmPendingRequests() {
        AppointmentRepository appointments=mock(AppointmentRepository.class);
        NotificationService notifications=mock(NotificationService.class);
        EmailService email=mock(EmailService.class);
        AppointmentService service=new AppointmentService(appointments,mock(DoctorScheduleRepository.class),
            mock(UserRepository.class),mock(DentalServiceRepository.class),email,notifications,
            mock(DoctorUnavailabilityRepository.class),"Asia/Ho_Chi_Minh");
        User patient=new User("Patient","patient@example.com","hash",Role.PATIENT);
        User doctor=new User("Doctor","doctor@example.com","hash",Role.DOCTOR);
        DentalService treatment=new DentalService("Exam","General",new BigDecimal("200000"),30,"Exam");
        Appointment appointment=new Appointment(patient,doctor,treatment,Instant.parse("2030-01-01T09:00:00Z"),
            Instant.parse("2030-01-01T09:30:00Z"),"Check");
        when(appointments.findById(1L)).thenReturn(Optional.of(appointment));

        assertThrows(ApiException.class,()->service.changeStatus(1L,new StatusRequest("CONFIRMED",null),
            doctor.getEmail(),Role.DOCTOR));
        assertThrows(ApiException.class,()->service.changeStatus(1L,new StatusRequest("CONFIRMED",null),
            "admin@example.com",Role.ADMIN));

        assertEquals("CONFIRMED",service.changeStatus(1L,new StatusRequest("CONFIRMED",null),
            "reception@example.com",Role.RECEPTIONIST).status());
        verify(notifications).create(eq(patient),anyString(),anyString(),eq("/app/appointments"));
    }

    @Test
    void administratorCannotCreateOrManageAppointments() {
        AppointmentService service=new AppointmentService(mock(AppointmentRepository.class),mock(DoctorScheduleRepository.class),
            mock(UserRepository.class),mock(DentalServiceRepository.class),mock(EmailService.class),
            mock(NotificationService.class),mock(DoctorUnavailabilityRepository.class),"Asia/Ho_Chi_Minh");

        assertThrows(ApiException.class,()->service.list("admin@example.com",Role.ADMIN,null,null));
        assertThrows(ApiException.class,()->service.changeStatus(1L,new StatusRequest("CONFIRMED",null),
            "admin@example.com",Role.ADMIN));
    }
}

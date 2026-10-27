package vn.dentalcare.service;

import vn.dentalcare.dto.DoctorProfileDtos.*;
import vn.dentalcare.entity.*;
import vn.dentalcare.exception.ApiException;
import vn.dentalcare.repository.*;
import org.junit.jupiter.api.Test;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class DoctorProfileServiceTest {
    @Test
    void doctorProfileChangesAreKeptAsUnpublishedDraftUntilApproval() {
        DoctorProfileRepository profiles=mock(DoctorProfileRepository.class);
        DoctorProfileSubmissionRepository submissions=mock(DoctorProfileSubmissionRepository.class);
        UserRepository users=mock(UserRepository.class);
        SpecialtyRepository specialties=mock(SpecialtyRepository.class);
        User doctor=mock(User.class);
        Specialty specialty=mock(Specialty.class);
        when(doctor.getId()).thenReturn(17L);
        when(doctor.getRole()).thenReturn(Role.DOCTOR);
        when(doctor.getFullName()).thenReturn("Dr. Example");
        when(users.findByEmailIgnoreCase("doctor@example.com")).thenReturn(Optional.of(doctor));
        when(specialties.findById(4L)).thenReturn(Optional.of(specialty));
        when(specialty.getId()).thenReturn(4L);
        when(specialty.getName()).thenReturn("Nha khoa tổng quát");
        when(submissions.findByDoctor_Id(17L)).thenReturn(Optional.empty());
        when(submissions.save(any(DoctorProfileSubmission.class))).thenAnswer(invocation->invocation.getArgument(0));
        DoctorProfileService service=new DoctorProfileService(profiles,submissions,users,specialties);

        ProfileResponse result=service.submit("doctor@example.com",
            new ProfileWriteRequest(4L,8,"Bác sĩ Răng Hàm Mặt","Giới thiệu","Giải thưởng chuyên môn"));

        assertEquals(DoctorProfileSubmission.Status.PENDING,result.status());
        assertEquals("Giải thưởng chuyên môn",result.achievements());
        verify(profiles,never()).save(any());
    }

    @Test
    void approvingSubmissionPublishesProfileAndRecordsReviewStatus() {
        DoctorProfileRepository profiles=mock(DoctorProfileRepository.class);
        DoctorProfileSubmissionRepository submissions=mock(DoctorProfileSubmissionRepository.class);
        UserRepository users=mock(UserRepository.class);
        SpecialtyRepository specialties=mock(SpecialtyRepository.class);
        User doctor=mock(User.class);
        Specialty specialty=mock(Specialty.class);
        when(doctor.getId()).thenReturn(17L);
        when(doctor.getFullName()).thenReturn("Dr. Example");
        when(doctor.getRole()).thenReturn(Role.DOCTOR);
        when(specialty.getId()).thenReturn(4L);
        when(specialty.getName()).thenReturn("Nha khoa tổng quát");
        when(profiles.findById(17L)).thenReturn(Optional.empty());
        DoctorProfileSubmission submission=new DoctorProfileSubmission(doctor,specialty,8,
            "Bác sĩ Răng Hàm Mặt","Giới thiệu","Giải thưởng chuyên môn");
        when(submissions.findByDoctor_Id(17L)).thenReturn(Optional.of(submission));
        when(profiles.save(any(DoctorProfile.class))).thenAnswer(invocation->invocation.getArgument(0));
        when(submissions.save(any(DoctorProfileSubmission.class))).thenAnswer(invocation->invocation.getArgument(0));
        DoctorProfileService service=new DoctorProfileService(profiles,submissions,users,specialties);

        ProfileResponse result=service.review(17L,new ReviewRequest(Decision.APPROVE,null));

        assertEquals(DoctorProfileSubmission.Status.APPROVED,result.status());
        assertEquals("Giải thưởng chuyên môn",result.achievements());
        verify(profiles).save(argThat(profile->profile.getAchievements().equals("Giải thưởng chuyên môn")));
    }

    @Test
    void rejectingSubmissionRequiresActionableReviewNote() {
        DoctorProfileSubmissionRepository submissions=mock(DoctorProfileSubmissionRepository.class);
        User doctor=mock(User.class);
        Specialty specialty=mock(Specialty.class);
        when(doctor.getId()).thenReturn(17L);
        when(doctor.getFullName()).thenReturn("Dr. Example");
        when(specialty.getId()).thenReturn(4L);
        when(specialty.getName()).thenReturn("Nha khoa tổng quát");
        when(submissions.findByDoctor_Id(17L)).thenReturn(Optional.of(
            new DoctorProfileSubmission(doctor,specialty,8,null,null,null)));
        DoctorProfileService service=new DoctorProfileService(mock(DoctorProfileRepository.class),submissions,
            mock(UserRepository.class),mock(SpecialtyRepository.class));

        assertThrows(ApiException.class,()->service.review(17L,new ReviewRequest(Decision.REJECT," ")));
    }
}

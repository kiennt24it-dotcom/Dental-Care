package vn.dentalcare.service;

import vn.dentalcare.dto.DoctorProfileDtos.*;
import vn.dentalcare.entity.*;
import vn.dentalcare.exception.ApiException;
import vn.dentalcare.repository.*;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

@Service
public class DoctorProfileService {
    private final DoctorProfileRepository profiles;
    private final DoctorProfileSubmissionRepository submissions;
    private final UserRepository users;
    private final SpecialtyRepository specialties;

    public DoctorProfileService(DoctorProfileRepository profiles, DoctorProfileSubmissionRepository submissions,
        UserRepository users, SpecialtyRepository specialties) {
        this.profiles=profiles;this.submissions=submissions;this.users=users;this.specialties=specialties;
    }

    @Transactional(readOnly=true)
    public ProfileResponse mine(String email) {
        User doctor=findDoctor(email);
        DoctorProfile profile=profiles.findById(doctor.getId()).orElse(null);
        DoctorProfileSubmission submission=submissions.findByDoctor_Id(doctor.getId()).orElse(null);
        if(submission!=null && submission.getStatus()!=DoctorProfileSubmission.Status.APPROVED)
            return response(doctor,submission);
        return profile==null
            ?new ProfileResponse(doctor.getId(),doctor.getFullName(),doctor.getAvatarUrl(),null,null,0,null,null,null,
                DoctorProfileSubmission.Status.NOT_SUBMITTED,null,null)
            :response(doctor,profile,submission);
    }

    @Transactional
    public ProfileResponse submit(String email,ProfileWriteRequest request) {
        User doctor=findDoctor(email);
        Specialty specialty=specialties.findById(request.specialtyId())
            .orElseThrow(()->new ApiException(HttpStatus.NOT_FOUND,"Không tìm thấy chuyên khoa"));
        DoctorProfileSubmission submission=submissions.findByDoctor_Id(doctor.getId())
            .orElseGet(()->new DoctorProfileSubmission(doctor,specialty,request.experienceYears(),
                clean(request.qualification()),clean(request.description()),clean(request.achievements())));
        if(submission.getId()!=null)
            submission.submit(specialty,request.experienceYears(),clean(request.qualification()),
                clean(request.description()),clean(request.achievements()));
        return response(doctor,submissions.save(submission));
    }

    @Transactional(readOnly=true)
    public List<ProfileResponse> pending() {
        return submissions.findByStatusAndDoctor_RoleOrderBySubmittedAtAsc(DoctorProfileSubmission.Status.PENDING,Role.DOCTOR)
            .stream().map(submission->response(submission.getDoctor(),submission)).toList();
    }

    @Transactional
    public ProfileResponse review(Long doctorId,ReviewRequest request) {
        DoctorProfileSubmission submission=submissions.findByDoctor_Id(doctorId)
            .orElseThrow(()->new ApiException(HttpStatus.NOT_FOUND,"Không tìm thấy hồ sơ chờ duyệt"));
        if(submission.getStatus()!=DoctorProfileSubmission.Status.PENDING)
            throw new ApiException(HttpStatus.CONFLICT,"Hồ sơ này đã được xử lý");
        if(submission.getDoctor().getRole()!=Role.DOCTOR)
            throw new ApiException(HttpStatus.CONFLICT,"Tài khoản không còn thuộc vai trò bác sĩ");
        if(request.decision()==Decision.REJECT && (request.reviewNote()==null||request.reviewNote().isBlank()))
            throw new ApiException(HttpStatus.BAD_REQUEST,"Vui lòng ghi lý do từ chối để bác sĩ chỉnh sửa hồ sơ");
        if(request.decision()==Decision.APPROVE) {
            User doctor=submission.getDoctor();
            DoctorProfile profile=profiles.findById(doctorId)
                .orElseGet(()->new DoctorProfile(doctor,submission.getSpecialty(),submission.getExperienceYears(),
                    submission.getQualification(),submission.getDescription(),submission.getAchievements()));
            profile.update(submission.getSpecialty(),submission.getExperienceYears(),submission.getQualification(),
                submission.getDescription(),submission.getAchievements());
            profiles.save(profile);
            submission.review(DoctorProfileSubmission.Status.APPROVED,clean(request.reviewNote()));
        } else {
            submission.review(DoctorProfileSubmission.Status.REJECTED,clean(request.reviewNote()));
        }
        return response(submission.getDoctor(),submissions.save(submission));
    }

    private User findDoctor(String email) {
        return users.findByEmailIgnoreCase(email).filter(user->user.getRole()==Role.DOCTOR)
            .orElseThrow(()->new ApiException(HttpStatus.FORBIDDEN,"Chức năng này chỉ dành cho bác sĩ"));
    }

    private static ProfileResponse response(User doctor,DoctorProfileSubmission submission) {
        return new ProfileResponse(doctor.getId(),doctor.getFullName(),doctor.getAvatarUrl(),
            submission.getSpecialty().getId(),submission.getSpecialty().getName(),submission.getExperienceYears(),
            submission.getQualification(),submission.getDescription(),submission.getAchievements(),
            submission.getStatus(),submission.getReviewNote(),submission.getSubmittedAt());
    }

    private static ProfileResponse response(User doctor,DoctorProfile profile,DoctorProfileSubmission submission) {
        return new ProfileResponse(doctor.getId(),doctor.getFullName(),doctor.getAvatarUrl(),
            profile.getSpecialty()==null?null:profile.getSpecialty().getId(),
            profile.getSpecialty()==null?null:profile.getSpecialty().getName(),profile.getExperienceYears(),
            profile.getQualification(),profile.getDescription(),profile.getAchievements(),
            submission==null?DoctorProfileSubmission.Status.APPROVED:submission.getStatus(),
            submission==null?null:submission.getReviewNote(),submission==null?null:submission.getSubmittedAt());
    }

    private static String clean(String value) { return value==null||value.isBlank()?null:value.trim(); }
}

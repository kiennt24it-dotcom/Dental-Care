package vn.dentalcare;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.springframework.http.MediaType.APPLICATION_JSON;

@SpringBootTest
@AutoConfigureMockMvc
class ApiSecurityIntegrationTest {
    @Autowired MockMvc mvc;
    @Test void publicCatalogIsAvailableWithoutLogin() throws Exception {
        mvc.perform(get("/api/public/services"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true));
    }
    @Test @WithMockUser(roles="PATIENT")
    void patientCannotReadClinicStatistics() throws Exception {
        mvc.perform(get("/api/statistics/overview")).andExpect(status().isForbidden());
    }
    @Test
    void unauthenticatedApiRequestsReturnUnauthorizedWithoutOAuthRedirect() throws Exception {
        mvc.perform(get("/api/appointments"))
            .andExpect(status().isUnauthorized())
            .andExpect(header().doesNotExist("Location"));
    }
    @Test
    void newRegistrationsArePatientsByDefault() throws Exception {
        mvc.perform(post("/api/auth/register").contentType(APPLICATION_JSON)
            .content("{\"fullName\":\"Test Patient\",\"email\":\"registration-default-role@example.com\","
                + "\"password\":\"StrongPassword@2025\",\"phone\":\"0900000000\"}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.user.role").value("PATIENT"));
    }
    @Test @WithMockUser(roles="ADMIN")
    void adminCannotUsePatientSymptomAssistant() throws Exception {
        mvc.perform(post("/api/ai/symptoms").contentType("application/json")
            .content("{\"symptoms\":\"Sensitive tooth\",\"consentToExternalProcessing\":true}"))
            .andExpect(status().isForbidden());
    }
    @Test @WithMockUser(roles="PATIENT")
    void symptomAssistantRequiresConsentForExternalAiProcessing() throws Exception {
        mvc.perform(post("/api/ai/symptoms").contentType(APPLICATION_JSON)
            .content("{\"symptoms\":\"Sensitive tooth\"}"))
            .andExpect(status().isBadRequest());
    }

    @Test @WithMockUser(roles="ADMIN")
    void administratorsCannotReadAppointmentOrClinicalRecordWorkspaces() throws Exception {
        mvc.perform(get("/api/appointments")).andExpect(status().isForbidden());
        mvc.perform(get("/api/medical-records")).andExpect(status().isForbidden());
        mvc.perform(get("/api/follow-ups")).andExpect(status().isForbidden());
    }

    @Test @WithMockUser(roles="DOCTOR")
    void onlyAdministratorsCanReadPendingDoctorProfiles() throws Exception {
        mvc.perform(get("/api/doctor-profiles/pending")).andExpect(status().isForbidden());
    }

    @Test @WithMockUser(roles="PATIENT")
    void patientsCannotEditDoctorProfiles() throws Exception {
        mvc.perform(get("/api/doctor-profiles/me")).andExpect(status().isForbidden());
    }

    @Test @WithMockUser(roles="ADMIN")
    void administratorsCannotSubmitDoctorProfilesAsDoctors() throws Exception {
        mvc.perform(put("/api/doctor-profiles/me").contentType(APPLICATION_JSON)
            .content("{\"specialtyId\":1,\"experienceYears\":5,\"qualification\":\"Board certified\","
                + "\"description\":\"Profile\",\"achievements\":\"Award\"}"))
            .andExpect(status().isForbidden());
    }
}

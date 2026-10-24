package com.project.course_enrollment.api;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import com.project.course_enrollment.store.RegistrationStore;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class EnrollmentApiIntegrationTest {

    private static final String URL = "/api/offerings/CS101/enrollments";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private RegistrationStore store;

    @BeforeEach
    void resetState() {
        store.clear();
    }

    private String body(String learnerId, boolean eligible) {
        return "{\"learnerId\":\"" + learnerId + "\",\"eligible\":" + eligible + "}";
    }

    @Test
    void fullEnrollWaitlistDropPromoteFlow() throws Exception {
        // Fill capacity (2).
        mockMvc.perform(post(URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body("L1", true)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.outcome").value("ENROLLED"));

        mockMvc.perform(post(URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body("L2", true)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.outcome").value("ENROLLED"));

        // Third eligible learner is waitlisted.
        mockMvc.perform(post(URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body("L3", true)))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.outcome").value("WAITLISTED"));

        // Duplicate enroll is idempotent.
        mockMvc.perform(post(URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body("L1", true)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.outcome").value("ALREADY_ENROLLED"));

        // Ineligible learner is rejected.
        mockMvc.perform(post(URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body("X", false)))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.outcome").value("NOT_ELIGIBLE"));

        // Drop an enrolled learner -> promotes L3 (earliest eligible waitlisted).
        mockMvc.perform(delete(URL + "/L1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.outcome").value("DROPPED"));

        // L3 is now enrolled (promoted).
        mockMvc.perform(post(URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body("L3", true)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.outcome").value("ALREADY_ENROLLED"));

        // Repeated drop of L1 does not free an extra seat.
        mockMvc.perform(delete(URL + "/L1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.outcome").value("NOT_ENROLLED"));
    }

    @Test
    void validationErrorReturns400() throws Exception {
        mockMvc.perform(post(URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"learnerId\":\" \",\"eligible\":true}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.learnerId").exists());
    }

    @Test
    void unknownOfferingReturns404() throws Exception {
        mockMvc.perform(delete("/api/offerings/NOPE/enrollments/L1"))
                .andExpect(status().isNotFound());
    }
}

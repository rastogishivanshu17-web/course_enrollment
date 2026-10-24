package com.project.course_enrollment.api;

import com.project.course_enrollment.config.CourseOfferingConfig;
import com.project.course_enrollment.domain.EnrollmentOutcome;
import com.project.course_enrollment.domain.EnrollmentResult;
import com.project.course_enrollment.domain.Learner;
import com.project.course_enrollment.service.EnrollmentService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * MockMvc controller tests. The EnrollmentService is mocked (Mockito) to isolate the web
 * layer; the real CourseOfferingConfig supplies the CourseOffering bean (id=CS101).
 */
@WebMvcTest(EnrollmentController.class)
@Import(CourseOfferingConfig.class)
class EnrollmentControllerTest {

    private static final String ENROLL_URL = "/api/offerings/CS101/enrollments";

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private EnrollmentService enrollmentService;

    @Test
    void enrollReturns201WhenEnrolled() throws Exception {
        when(enrollmentService.enroll(any(Learner.class)))
                .thenReturn(EnrollmentResult.of(
                        EnrollmentOutcome.ENROLLED, "L1", "Learner enrolled"));

        mockMvc.perform(post(ENROLL_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"learnerId\":\"L1\",\"eligible\":true}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.outcome").value("ENROLLED"))
                .andExpect(jsonPath("$.learnerId").value("L1"));
    }

    @Test
    void enrollReturns202WhenWaitlisted() throws Exception {
        when(enrollmentService.enroll(any(Learner.class)))
                .thenReturn(EnrollmentResult.of(
                        EnrollmentOutcome.WAITLISTED, "L1", "Waitlisted"));

        mockMvc.perform(post(ENROLL_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"learnerId\":\"L1\",\"eligible\":true}"))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.outcome").value("WAITLISTED"));
    }

    @Test
    void enrollReturns200WhenAlreadyEnrolled() throws Exception {
        when(enrollmentService.enroll(any(Learner.class)))
                .thenReturn(EnrollmentResult.of(
                        EnrollmentOutcome.ALREADY_ENROLLED, "L1", "dup"));

        mockMvc.perform(post(ENROLL_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"learnerId\":\"L1\",\"eligible\":true}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.outcome").value("ALREADY_ENROLLED"));
    }

    @Test
    void enrollReturns422WhenNotEligible() throws Exception {
        when(enrollmentService.enroll(any(Learner.class)))
                .thenReturn(EnrollmentResult.of(
                        EnrollmentOutcome.NOT_ELIGIBLE, "L1", "prereq"));

        mockMvc.perform(post(ENROLL_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"learnerId\":\"L1\",\"eligible\":false}"))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.outcome").value("NOT_ELIGIBLE"));
    }

    @Test
    void enrollReturns400AndSkipsServiceWhenBodyInvalid() throws Exception {
        // Blank learnerId and missing eligible both violate validation.
        mockMvc.perform(post(ENROLL_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"learnerId\":\" \"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.fieldErrors.learnerId").exists())
                .andExpect(jsonPath("$.fieldErrors.eligible").exists());

        verifyNoInteractions(enrollmentService);
    }

    @Test
    void enrollReturns404AndSkipsServiceForUnknownOffering() throws Exception {
        mockMvc.perform(post("/api/offerings/UNKNOWN/enrollments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"learnerId\":\"L1\",\"eligible\":true}"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));

        verifyNoInteractions(enrollmentService);
    }

    @Test
    void dropReturns200WhenDropped() throws Exception {
        when(enrollmentService.drop("L1"))
                .thenReturn(EnrollmentResult.of(
                        EnrollmentOutcome.DROPPED, "L1", "dropped"));

        mockMvc.perform(delete(ENROLL_URL + "/L1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.outcome").value("DROPPED"));

        verify(enrollmentService).drop(eq("L1"));
    }

    @Test
    void dropReturns200WhenNotEnrolled() throws Exception {
        when(enrollmentService.drop("ghost"))
                .thenReturn(EnrollmentResult.of(
                        EnrollmentOutcome.NOT_ENROLLED, "ghost", "not enrolled"));

        mockMvc.perform(delete(ENROLL_URL + "/ghost"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.outcome").value("NOT_ENROLLED"));
    }

    @Test
    void dropReturns404AndSkipsServiceForUnknownOffering() throws Exception {
        mockMvc.perform(delete("/api/offerings/UNKNOWN/enrollments/L1"))
                .andExpect(status().isNotFound());

        verify(enrollmentService, never()).drop(any(String.class));
    }
}

package gr.alexc.acodelearn.course;

import com.fasterxml.jackson.databind.ObjectMapper;
import gr.alexc.acodelearn.course.command.CreateCourseCommand;
import gr.alexc.acodelearn.course.command.EnrollByEmailCommand;
import gr.alexc.acodelearn.course.command.UpdateCourseCommand;
import gr.alexc.acodelearn.course.query.projections.CourseSummaryView;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Verifies the {@link org.springframework.security.access.prepost.PreAuthorize}
 * rules wired on {@link CourseController}: endpoint-level role checks and
 * instance-level checks delegated to {@link CourseAccessChecker}
 * (bean {@code @courseAccess}).
 *
 * <p>The {@link CourseAccessChecker} bean is mocked, so each test stubs
 * the desired allow/deny outcome and asserts the resulting HTTP status.
 */
@SpringBootTest
@AutoConfigureMockMvc
class CourseControllerSecurityTest {

    @Autowired MockMvc mockMvc;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @MockitoBean CourseService courseService;
    @MockitoBean EnrollmentService enrollmentService;
    @MockitoBean CourseAccessChecker courseAccess;

    private static org.springframework.test.web.servlet.request.RequestPostProcessor jwtWithRoles(String... roles) {
        SimpleGrantedAuthority[] auths = java.util.Arrays.stream(roles)
                .map(SimpleGrantedAuthority::new)
                .toArray(SimpleGrantedAuthority[]::new);
        return jwt()
                .jwt(j -> j.subject("sub").claim("preferred_username", "u"))
                .authorities(auths);
    }

    // --- POST /course (hasAnyRole TEACHER/ADMIN) ---

    @Test
    void createCourse_unauthenticated_returns401() throws Exception {
        mockMvc.perform(post("/course")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CreateCourseCommand("t", "d", 1))))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void createCourse_studentRole_returns403() throws Exception {
        mockMvc.perform(post("/course")
                        .with(jwtWithRoles("ROLE_STUDENT"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CreateCourseCommand("t", "d", 1))))
                .andExpect(status().isForbidden());
        verify(courseService, never()).createCourse(any(), anyString());
    }

    @Test
    void createCourse_teacherRole_isAllowed() throws Exception {
        when(courseService.createCourse(any(), anyString()))
                .thenReturn(new CourseSummaryView(1L, "t", "d", 1));

        mockMvc.perform(post("/course")
                        .with(jwtWithRoles("ROLE_TEACHER"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CreateCourseCommand("t", "d", 1))))
                .andExpect(status().isOk());
        verify(courseService).createCourse(any(), anyString());
    }

    @Test
    void createCourse_adminRole_isAllowed() throws Exception {
        when(courseService.createCourse(any(), anyString()))
                .thenReturn(new CourseSummaryView(1L, "t", "d", 1));

        mockMvc.perform(post("/course")
                        .with(jwtWithRoles("ROLE_ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CreateCourseCommand("t", "d", 1))))
                .andExpect(status().isOk());
    }

    // --- PUT /course/{id} (canEdit) ---

    @Test
    void updateCourse_canEditTrue_isAllowed() throws Exception {
        when(courseAccess.canEdit(any(), any(Long.class))).thenReturn(true);
        when(courseService.updateCourse(any()))
                .thenReturn(new CourseSummaryView(7L, "t", "d", 1));

        mockMvc.perform(put("/course/7")
                        .with(jwtWithRoles("ROLE_TEACHER"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new UpdateCourseCommand(7L, "t", "d", 1))))
                .andExpect(status().isOk());
    }

    @Test
    void updateCourse_canEditFalse_returns403() throws Exception {
        when(courseAccess.canEdit(any(), any(Long.class))).thenReturn(false);

        mockMvc.perform(put("/course/7")
                        .with(jwtWithRoles("ROLE_TEACHER"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new UpdateCourseCommand(7L, "t", "d", 1))))
                .andExpect(status().isForbidden());
        verify(courseService, never()).updateCourse(any());
    }

    // --- POST /course/{id}/instructors/{userId} (canManageInstructors) ---

    @Test
    void addInstructor_canManageInstructorsTrue_isAllowed() throws Exception {
        when(courseAccess.canManageInstructors(any(), any(Long.class))).thenReturn(true);

        mockMvc.perform(post("/course/7/instructors/5")
                        .with(jwtWithRoles("ROLE_TEACHER")))
                .andExpect(status().isNoContent());
        verify(enrollmentService).addInstructor(7L, 5L);
    }

    @Test
    void addInstructor_canManageInstructorsFalse_returns403() throws Exception {
        when(courseAccess.canManageInstructors(any(), any(Long.class))).thenReturn(false);

        mockMvc.perform(post("/course/7/instructors/5")
                        .with(jwtWithRoles("ROLE_TEACHER")))
                .andExpect(status().isForbidden());
        verify(enrollmentService, never()).addInstructor(anyLong(), anyLong());
    }

    @Test
    void removeInstructor_canManageInstructorsFalse_returns403() throws Exception {
        when(courseAccess.canManageInstructors(any(), any(Long.class))).thenReturn(false);

        mockMvc.perform(delete("/course/7/instructors/5")
                        .with(jwtWithRoles("ROLE_TEACHER")))
                .andExpect(status().isForbidden());
        verify(enrollmentService, never()).removeInstructor(anyLong(), anyLong());
    }

    // --- POST /course/{id}/enrollments/{userId} (canManageEnrollments) ---

    @Test
    void enrollStudent_canManageEnrollmentsFalse_returns403() throws Exception {
        when(courseAccess.canManageEnrollments(any(), any(Long.class))).thenReturn(false);

        mockMvc.perform(post("/course/7/enrollments/3")
                        .with(jwtWithRoles("ROLE_STUDENT")))
                .andExpect(status().isForbidden());
        verify(enrollmentService, never()).enrollStudent(anyLong(), anyLong());
    }

    @Test
    void enrollStudent_canManageEnrollmentsTrue_isAllowed() throws Exception {
        when(courseAccess.canManageEnrollments(any(), any(Long.class))).thenReturn(true);
        when(enrollmentService.enrollStudent(7L, 3L))
                .thenReturn(new gr.alexc.acodelearn.course.query.projections.CourseEnrollmentView(
                        1L, 7L, 3L, "x@x.com", CourseEnrollment.Status.ACTIVE, java.time.Instant.now()));

        mockMvc.perform(post("/course/7/enrollments/3")
                        .with(jwtWithRoles("ROLE_TEACHER")))
                .andExpect(status().isOk());
        verify(enrollmentService).enrollStudent(7L, 3L);
    }

    @Test
    void enrollByEmail_canManageEnrollmentsFalse_returns403() throws Exception {
        when(courseAccess.canManageEnrollments(any(), any(Long.class))).thenReturn(false);

        mockMvc.perform(post("/course/7/enrollments/by-email")
                        .with(jwtWithRoles("ROLE_TEACHER"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new EnrollByEmailCommand("a@b.com"))))
                .andExpect(status().isForbidden());
        verify(enrollmentService, never()).enrollByEmail(anyLong(), any());
    }

    @Test
    void removeEnrollment_unauthenticated_returns401() throws Exception {
        mockMvc.perform(delete("/course/7/enrollments/3"))
                .andExpect(status().isUnauthorized());
    }
}

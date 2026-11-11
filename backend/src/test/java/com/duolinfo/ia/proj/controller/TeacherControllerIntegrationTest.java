package com.duolinfo.ia.proj.controller;

import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import tools.jackson.databind.ObjectMapper;
import com.duolinfo.ia.proj.dto.request.TeacherApprovalRequestDTO;
import com.duolinfo.ia.proj.entity.Teacher;
import com.duolinfo.ia.proj.exception.GlobalExceptionHandler;
import com.duolinfo.ia.proj.exception.ResourceNotFoundException;
import com.duolinfo.ia.proj.service.AccessControlService;
import com.duolinfo.ia.proj.service.TeacherService;

class TeacherControllerIntegrationTest {

    private MockMvc mockMvc;
    private TeacherService teacherService;
    private AccessControlService accessControlService;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        teacherService = mock(TeacherService.class);
        accessControlService = mock(AccessControlService.class);
        objectMapper = new ObjectMapper();

        TeacherController controller = new TeacherController(teacherService);
        ReflectionTestUtils.setField(controller, "accessControlService", accessControlService);

        mockMvc = MockMvcBuilders.standaloneSetup(controller)
            .setControllerAdvice(new GlobalExceptionHandler())
            .build();
    }

    @Test
    void getMe_withAuthenticatedTeacher_returnsProfile() throws Exception {
        Teacher teacher = new Teacher();
        teacher.setId(12L);
        teacher.setEmail("prof@example.com");
        teacher.setName("Professor Silva");
        teacher.setApproved(true);

        when(teacherService.findByEmail("prof@example.com")).thenReturn(teacher);

        UsernamePasswordAuthenticationToken principal = new UsernamePasswordAuthenticationToken(
            "prof@example.com",
            null,
            List.of(new SimpleGrantedAuthority("ROLE_TEACHER"))
        );

        mockMvc.perform(get("/api/teachers/me").principal(principal))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id", is(12)))
            .andExpect(jsonPath("$.email", is("prof@example.com")))
            .andExpect(jsonPath("$.name", is("Professor Silva")));
    }

    @Test
    void getById_whenFound_returnsTeacher() throws Exception {
        Teacher teacher = new Teacher();
        teacher.setId(12L);
        teacher.setEmail("prof@example.com");
        teacher.setName("Professor Silva");

        when(teacherService.findById(12L)).thenReturn(teacher);

        UsernamePasswordAuthenticationToken principal = new UsernamePasswordAuthenticationToken(
            "admin@example.com",
            null,
            List.of(new SimpleGrantedAuthority("ROLE_ADMIN"))
        );

        mockMvc.perform(get("/api/teachers/12").principal(principal))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id", is(12)))
            .andExpect(jsonPath("$.email", is("prof@example.com")));
    }

    @Test
    void getById_whenNotFound_returns404() throws Exception {
        when(teacherService.findById(999L)).thenThrow(new ResourceNotFoundException("Professor não encontrado."));

        UsernamePasswordAuthenticationToken principal = new UsernamePasswordAuthenticationToken(
            "admin@example.com",
            null,
            List.of(new SimpleGrantedAuthority("ROLE_ADMIN"))
        );

        mockMvc.perform(get("/api/teachers/999").principal(principal))
            .andExpect(status().isNotFound());
    }

    @Test
    void updateApproval_asAdmin_updatesApprovalStatus() throws Exception {
        Teacher teacher = new Teacher();
        teacher.setId(12L);
        teacher.setEmail("prof@example.com");
        teacher.setApproved(true);
        teacher.setApprovalStatus("APPROVED");

        when(teacherService.updateApproval(12L, true)).thenReturn(teacher);

        TeacherApprovalRequestDTO req = new TeacherApprovalRequestDTO(true);

        UsernamePasswordAuthenticationToken principal = new UsernamePasswordAuthenticationToken(
            "admin@example.com",
            null,
            List.of(new SimpleGrantedAuthority("ROLE_ADMIN"))
        );

        mockMvc.perform(patch("/api/teachers/12/approval")
                .principal(principal)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.approved", is(true)))
            .andExpect(jsonPath("$.approvalStatus", is("APPROVED")));

        verify(teacherService).updateApproval(12L, true);
    }
}

package com.duolinfo.ia.proj.controller;

import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
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
import com.duolinfo.ia.proj.dto.request.StudentRequestDTO;
import com.duolinfo.ia.proj.entity.Student;
import com.duolinfo.ia.proj.entity.User;
import com.duolinfo.ia.proj.exception.BusinessException;
import com.duolinfo.ia.proj.exception.GlobalExceptionHandler;
import com.duolinfo.ia.proj.exception.ResourceNotFoundException;
import com.duolinfo.ia.proj.service.AccessControlService;
import com.duolinfo.ia.proj.service.EnrollmentService;
import com.duolinfo.ia.proj.service.SchoolClassService;
import com.duolinfo.ia.proj.service.StudentService;

class StudentControllerIntegrationTest {

    private MockMvc mockMvc;
    private StudentService studentService;
    private AccessControlService accessControlService;
    private EnrollmentService enrollmentService;
    private SchoolClassService schoolClassService;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        studentService = mock(StudentService.class);
        accessControlService = mock(AccessControlService.class);
        enrollmentService = mock(EnrollmentService.class);
        schoolClassService = mock(SchoolClassService.class);
        objectMapper = new ObjectMapper();

        StudentController controller = new StudentController(studentService);
        ReflectionTestUtils.setField(controller, "accessControlService", accessControlService);
        ReflectionTestUtils.setField(controller, "enrollmentService", enrollmentService);
        ReflectionTestUtils.setField(controller, "schoolClassService", schoolClassService);

        mockMvc = MockMvcBuilders.standaloneSetup(controller)
            .setControllerAdvice(new GlobalExceptionHandler())
            .build();
    }

    @Test
    void getMe_withAuthenticatedStudent_returnsProfile() throws Exception {
        Student student = new Student();
        student.setId(15L);
        student.setEmail("student15@example.com");
        student.setName("Student Fifteen");

        when(studentService.findByEmail("student15@example.com")).thenReturn(student);

        UsernamePasswordAuthenticationToken principal = new UsernamePasswordAuthenticationToken(
            "student15@example.com",
            null,
            List.of(new SimpleGrantedAuthority("ROLE_STUDENT"))
        );

        mockMvc.perform(get("/api/students/me").principal(principal))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id", is(15)))
            .andExpect(jsonPath("$.email", is("student15@example.com")))
            .andExpect(jsonPath("$.name", is("Student Fifteen")));
    }

    @Test
    void getById_whenFound_returnsStudent() throws Exception {
        Student student = new Student();
        student.setId(25L);
        student.setEmail("student25@example.com");
        student.setName("Student Twenty Five");

        when(studentService.findById(25L)).thenReturn(student);

        UsernamePasswordAuthenticationToken principal = new UsernamePasswordAuthenticationToken(
            "admin@example.com",
            null,
            List.of(new SimpleGrantedAuthority("ROLE_ADMIN"))
        );

        mockMvc.perform(get("/api/students/25").principal(principal))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id", is(25)))
            .andExpect(jsonPath("$.email", is("student25@example.com")));
    }

    @Test
    void getById_whenNotFound_returns404() throws Exception {
        when(studentService.findById(999L)).thenThrow(new ResourceNotFoundException("Estudante não encontrado."));

        UsernamePasswordAuthenticationToken principal = new UsernamePasswordAuthenticationToken(
            "admin@example.com",
            null,
            List.of(new SimpleGrantedAuthority("ROLE_ADMIN"))
        );

        mockMvc.perform(get("/api/students/999").principal(principal))
            .andExpect(status().isNotFound());
    }

    @Test
    void delete_callsStudentServiceDelete() throws Exception {
        UsernamePasswordAuthenticationToken principal = new UsernamePasswordAuthenticationToken(
            "admin@example.com",
            null,
            List.of(new SimpleGrantedAuthority("ROLE_ADMIN"))
        );

        mockMvc.perform(delete("/api/students/30").principal(principal))
            .andExpect(status().isNoContent());

        verify(studentService).deleteById(30L);
    }
}

package com.duolinfo.ia.proj.controller;

import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import com.duolinfo.ia.proj.config.UserAuthCache;
import com.duolinfo.ia.proj.entity.ClassTeacherAssignment;
import com.duolinfo.ia.proj.entity.Institution;
import com.duolinfo.ia.proj.entity.LearningExercise;
import com.duolinfo.ia.proj.entity.School;
import com.duolinfo.ia.proj.entity.SchoolClass;
import com.duolinfo.ia.proj.entity.Student;
import com.duolinfo.ia.proj.entity.StudentLanguage;
import com.duolinfo.ia.proj.entity.Teacher;
import com.duolinfo.ia.proj.repository.ClassTeacherAssignmentRepository;
import com.duolinfo.ia.proj.repository.InstitutionRepository;
import com.duolinfo.ia.proj.repository.LearningExerciseRepository;
import com.duolinfo.ia.proj.repository.SchoolClassRepository;
import com.duolinfo.ia.proj.repository.SchoolRepository;
import com.duolinfo.ia.proj.repository.StudentRepository;
import com.duolinfo.ia.proj.repository.TeacherRepository;
import com.duolinfo.ia.proj.service.EnrollmentService;
import com.duolinfo.ia.proj.service.JwtService;

import tools.jackson.databind.ObjectMapper;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class MascotSecurityAndExerciseUpdateIntegrationTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private JwtService jwtService;
    @Autowired private PasswordEncoder passwordEncoder;
    @Autowired private UserAuthCache userAuthCache;
    @Autowired private InstitutionRepository institutionRepository;
    @Autowired private SchoolRepository schoolRepository;
    @Autowired private SchoolClassRepository schoolClassRepository;
    @Autowired private StudentRepository studentRepository;
    @Autowired private TeacherRepository teacherRepository;
    @Autowired private ClassTeacherAssignmentRepository assignmentRepository;
    @Autowired private LearningExerciseRepository exerciseRepository;
    @Autowired private EnrollmentService enrollmentService;

    @BeforeEach
    void clearAuthCache() {
        userAuthCache.evictAll();
    }

    @Test
    void mascotChatRequiresAuthenticationAndUpdatesExerciseHelpMessageEndToEnd() throws Exception {
        String sfx = String.valueOf(System.nanoTime());

        mockMvc.perform(post("/api/ai/mascot/chat")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of("message", "Como ganho XP?"))))
            .andExpect(status().isUnauthorized());

        Institution inst = new Institution();
        inst.setName("Inst Mascot " + sfx);
        inst.setCode("MSC_" + sfx);
        inst.setEmail("msc_" + sfx + "@test.local");
        inst = institutionRepository.save(inst);

        School school = new School();
        school.setName("School Mascot " + sfx);
        school.setInstitution(inst);
        school = schoolRepository.save(school);

        SchoolClass sc = new SchoolClass();
        sc.setName("Inglês A " + sfx);
        sc.setSchool(school);
        sc.setInstitution(inst);
        sc.setLanguageName("Inglês");
        sc.setSchoolYear("2026");
        sc.setShift("MANHÃ");
        sc.setGradeLevel("Básico");
        sc.setActive(true);
        sc = schoolClassRepository.save(sc);

        Teacher teacher = new Teacher();
        teacher.setEmail("prof_msc_" + sfx + "@test.local");
        teacher.setName("Prof Mascote");
        teacher.setPasswordHash(passwordEncoder.encode("secret123"));
        teacher.setInstitution(inst);
        teacher.setApproved(true);
        teacher.setApprovalStatus("APPROVED");
        teacher.setTaughtLanguages(List.of("Inglês"));
        teacher = teacherRepository.save(teacher);

        Student student = new Student();
        student.setEmail("stud_msc_" + sfx + "@test.local");
        student.setName("Aluno Mascote");
        student.setPasswordHash(passwordEncoder.encode("secret123"));
        student.setInstitution(inst);
        student.addLanguage(StudentLanguage.builder()
            .languageName("Inglês")
            .level("Básico")
            .score(0)
            .build());
        student = studentRepository.save(student);

        ClassTeacherAssignment cta = new ClassTeacherAssignment();
        cta.setSchoolClass(sc);
        cta.setTeacher(teacher);
        cta.setStartDate(LocalDate.now());
        assignmentRepository.save(cta);

        enrollmentService.enrollStudent(student.getId(), sc.getId(), LocalDate.now(), "Matrícula teste");

        String teacherToken = jwtService.generateToken(teacher);
        String studentToken = jwtService.generateToken(student);

        // Authenticated student can reach /api/ai/mascot/chat and off-topic is rejected in-scope=false
        mockMvc.perform(post("/api/ai/mascot/chat")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + studentToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of(
                    "message", "Como funciona o sistema de XP na plataforma?",
                    "activeSection", "home",
                    "userRole", "student"
                ))))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.inScope", is(true)));

        // Teacher creates exercise with mascotHelpMessage
        Map<String, Object> createBody = Map.ofEntries(
            Map.entry("title", "Verb To Be"),
            Map.entry("category", "Gramática"),
            Map.entry("theme", "Apresentação"),
            Map.entry("languageName", "Inglês"),
            Map.entry("proficiencyLevel", "Básico"),
            Map.entry("description", "Pratique o verbo to be."),
            Map.entry("contentBody", "Am / Is / Are"),
            Map.entry("mascotHelpMessage", "Observe o sujeito da frase antes de escolher am, is ou are."),
            Map.entry("schoolClassId", sc.getId()),
            Map.entry("questions", List.of(Map.of(
                "style", "Múltipla Escolha",
                "question", "She ___ happy.",
                "options", List.of("am", "is", "are"),
                "correctAnswer", "is"
            )))
        );

        String createdJson = mockMvc.perform(post("/api/exercises")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + teacherToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(createBody)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.mascotHelpMessage", is("Observe o sujeito da frase antes de escolher am, is ou are.")))
            .andReturn()
            .getResponse()
            .getContentAsString();

        Long exerciseId = objectMapper.readTree(createdJson).get("id").asLong();

        // Student cannot PUT /api/exercises/{id}
        Map<String, Object> updateBody = Map.ofEntries(
            Map.entry("title", "Verb To Be Atualizado"),
            Map.entry("category", "Gramática"),
            Map.entry("theme", "Apresentação"),
            Map.entry("languageName", "Inglês"),
            Map.entry("proficiencyLevel", "Básico"),
            Map.entry("description", "Nova descrição."),
            Map.entry("contentBody", "Am / Is / Are"),
            Map.entry("mascotHelpMessage", "Nova dica: I usa am, He/She/It usam is."),
            Map.entry("schoolClassId", sc.getId()),
            Map.entry("questions", List.of(Map.of(
                "style", "Múltipla Escolha",
                "question", "She ___ happy.",
                "options", List.of("am", "is", "are"),
                "correctAnswer", "is"
            )))
        );

        mockMvc.perform(put("/api/exercises/" + exerciseId)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + studentToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updateBody)))
            .andExpect(status().isForbidden());

        // Teacher updates exercise help message
        mockMvc.perform(put("/api/exercises/" + exerciseId)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + teacherToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updateBody)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.title", is("Verb To Be Atualizado")))
            .andExpect(jsonPath("$.mascotHelpMessage", is("Nova dica: I usa am, He/She/It usam is.")));

        LearningExercise persisted = exerciseRepository.findById(exerciseId).orElseThrow();
        assertEquals("Nova dica: I usa am, He/She/It usam is.", persisted.getMascotHelpMessage());

        // Student fetches activity and receives updated mascotHelpMessage
        mockMvc.perform(get("/api/exercises/" + exerciseId)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + studentToken))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.mascotHelpMessage", is("Nova dica: I usa am, He/She/It usam is.")));
    }
}

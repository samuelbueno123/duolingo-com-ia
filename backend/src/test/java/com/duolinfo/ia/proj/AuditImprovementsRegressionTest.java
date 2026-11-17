package com.duolinfo.ia.proj;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import com.duolinfo.ia.proj.entity.ClassTeacherAssignment;
import com.duolinfo.ia.proj.entity.EnrollmentStatus;
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

import jakarta.persistence.EntityManager;
import tools.jackson.databind.ObjectMapper;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class AuditImprovementsRegressionTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private JwtService jwtService;
    @Autowired private PasswordEncoder passwordEncoder;
    @Autowired private EntityManager entityManager;
    @Autowired private InstitutionRepository institutionRepository;
    @Autowired private SchoolRepository schoolRepository;
    @Autowired private SchoolClassRepository schoolClassRepository;
    @Autowired private StudentRepository studentRepository;
    @Autowired private TeacherRepository teacherRepository;
    @Autowired private ClassTeacherAssignmentRepository assignmentRepository;
    @Autowired private LearningExerciseRepository exerciseRepository;
    @Autowired private EnrollmentService enrollmentService;
    @Autowired private com.duolinfo.ia.proj.service.LearningPathService learningPathService;
    @Autowired private com.duolinfo.ia.proj.config.UserAuthCache userAuthCache;

    @org.junit.jupiter.api.BeforeEach
    void clearCaches() {
        userAuthCache.evictAll();
    }

    private Institution createInst(String suffix) {
        Institution inst = new Institution();
        inst.setName("Inst Test " + suffix);
        inst.setCode("INST_" + suffix);
        inst.setEmail("inst_" + suffix + "@test.local");
        return institutionRepository.save(inst);
    }

    private School createSchool(Institution inst, String suffix) {
        School school = new School();
        school.setName("School " + suffix);
        school.setInstitution(inst);
        return schoolRepository.save(school);
    }

    private SchoolClass createClass(School school, String name, String lang) {
        SchoolClass sc = new SchoolClass();
        sc.setName(name);
        sc.setSchool(school);
        sc.setInstitution(school.getInstitution());
        sc.setLanguageName(lang);
        sc.setSchoolYear("2026");
        sc.setShift("MANHÃ");
        sc.setGradeLevel("Básico");
        sc.setActive(true);
        return schoolClassRepository.save(sc);
    }

    private Student createStudent(String email, String name, Institution inst, String lang, String level) {
        Student s = new Student();
        s.setEmail(email);
        s.setName(name);
        s.setPasswordHash(passwordEncoder.encode("secret123"));
        s.setInstitution(inst);
        s.addLanguage(StudentLanguage.builder()
            .languageName(lang)
            .level(level)
            .score(0)
            .build());
        return studentRepository.save(s);
    }

    private Teacher createTeacher(String email, String name, Institution inst, List<String> langs) {
        Teacher t = new Teacher();
        t.setEmail(email);
        t.setName(name);
        t.setPasswordHash(passwordEncoder.encode("secret123"));
        t.setInstitution(inst);
        t.setApproved(true);
        t.setApprovalStatus("APPROVED");
        t.setTaughtLanguages(langs);
        return teacherRepository.save(t);
    }

    @Test
    @DisplayName("Aluno pode adicionar 2o idioma sem erro de constraint (uk_student_language_name)")
    void studentCanAddSecondLanguageAfterProfileCreation() throws Exception {
        String sfx = String.valueOf(System.nanoTime());
        Institution inst = createInst("L2_" + sfx);
        Student student = createStudent("lang2_" + sfx + "@test.local", "Ana Paula", inst, "Inglês", "Básico");
        entityManager.flush();
        String token = jwtService.generateToken(student);

        Map<String, Object> body = Map.of(
            "name", "Ana Paula",
            "email", student.getEmail(),
            "institutionId", inst.getId(),
            "languages", List.of(
                Map.of("languageName", "Inglês", "level", "Básico", "score", 0),
                Map.of("languageName", "Francês", "level", "Intermediário", "score", 0)
            )
        );

        mockMvc.perform(put("/api/students/" + student.getId())
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(body)))
            .andExpect(status().isNoContent());

        entityManager.flush();
        Student reloaded = studentRepository.findById(student.getId()).orElseThrow();
        assertEquals(2, reloaded.getLanguages().size());
    }

    @Test
    @DisplayName("Membros da instituicao: aluno ve 'Eu' para si e nenhum email de aluno e exposto")
    void institutionMembersMasksStudentEmailsAndMarksSelfAsEu() throws Exception {
        String sfx = String.valueOf(System.nanoTime());
        Institution inst = createInst("MEM_" + sfx);
        Student self = createStudent("self_" + sfx + "@test.local", "Lucas Me", inst, "Inglês", "Básico");
        Student peer = createStudent("peer_" + sfx + "@test.local", "Colega Peer", inst, "Inglês", "Básico");
        Teacher prof = createTeacher("prof_" + sfx + "@test.local", "Prof. Helena", inst, List.of("Inglês"));
        String token = jwtService.generateToken(self);

        mockMvc.perform(get("/api/students/me/institution-members")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.teachers[*].email", hasItem(prof.getEmail())))
            .andExpect(jsonPath("$.students[*].name", hasItem("Eu")))
            .andExpect(jsonPath("$.students[*].name", hasItem(peer.getName())))
            .andExpect(jsonPath("$.students[*].email", everyItem(nullValue())));
    }

    @Test
    @DisplayName("Convite de turma exclui caller e admins externos e envia nota/aviso para aluno")
    void invitableUsersExcludesSelfAndExternalAdminsAndNotesDeliverToStudent() throws Exception {
        String sfx = String.valueOf(System.nanoTime());
        Institution inst1 = createInst("I1_" + sfx);
        Institution inst2 = createInst("I2_" + sfx);
        School school1 = createSchool(inst1, "S1_" + sfx);
        SchoolClass sc = createClass(school1, "Inglês Manhã " + sfx, "Inglês");

        Teacher callerProf = createTeacher("caller_" + sfx + "@test.local", "Prof Caller", inst1, List.of("Inglês"));
        Teacher colleagueProf = createTeacher("colleague_" + sfx + "@test.local", "Prof Colega", inst1, List.of("Inglês"));
        Teacher externalAdmin = createTeacher("extadm_" + sfx + "@test.local", "Admin Externo", inst2, List.of("Inglês"));
        externalAdmin.setInstitutionAdmin(true);
        teacherRepository.save(externalAdmin);

        Student candidate = createStudent("cand_" + sfx + "@test.local", "Aluno Convite", inst1, "Inglês", "Básico");

        ClassTeacherAssignment cta = new ClassTeacherAssignment();
        cta.setSchoolClass(sc);
        cta.setTeacher(callerProf);
        cta.setStartDate(LocalDate.now());
        assignmentRepository.save(cta);

        String profToken = jwtService.generateToken(callerProf);
        String studentToken = jwtService.generateToken(candidate);

        mockMvc.perform(get("/api/classes/" + sc.getId() + "/invitable-users")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + profToken))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[*].userId", not(hasItem(callerProf.getId().intValue()))))
            .andExpect(jsonPath("$[*].userId", not(hasItem(externalAdmin.getId().intValue()))))
            .andExpect(jsonPath("$[*].userId", hasItem(candidate.getId().intValue())))
            .andExpect(jsonPath("$[*].userId", hasItem(colleagueProf.getId().intValue())));

        mockMvc.perform(post("/api/classes/" + sc.getId() + "/invite")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + profToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of(
                    "userId", candidate.getId(),
                    "role", "STUDENT"
                ))))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("INVITED"));

        // Envia anotação privada e confirma que aluno lê em /api/students/me/teacher-notes
        mockMvc.perform(post("/api/teachers/me/notes")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + profToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of(
                    "studentId", candidate.getId(),
                    "schoolClassId", sc.getId(),
                    "note", "Excelente pronúncia hoje!"
                ))))
            .andExpect(status().isCreated());

        mockMvc.perform(get("/api/students/me/teacher-notes")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + studentToken))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].note").value("Excelente pronúncia hoje!"));

        // Mural da turma publica comunicado normalmente
        mockMvc.perform(post("/api/classes/" + sc.getId() + "/announcements")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + profToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of(
                    "title", "Prova sexta",
                    "content", "Revisem módulo 1!",
                    "priority", "URGENT"
                ))))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.title").value("Prova sexta"));
    }

    @Test
    @DisplayName("Trilha de aprendizagem suporta 6 idiomas, inicia com 0% e exige quiz >= 60% para concluir modulo")
    void learningPathSupportsSixLanguagesStartsAtZeroAndEnforcesModuleQuiz() throws Exception {
        String sfx = String.valueOf(System.nanoTime());
        Institution inst = createInst("LP_" + sfx);
        Student student = createStudent("lp_" + sfx + "@test.local", "Estudante LP", inst, "Japonês", "Básico");
        String token = jwtService.generateToken(student);

        // Teste diagnóstico para Japonês
        mockMvc.perform(get("/api/learning-path/diagnostic")
                .param("language", "Japonês")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(greaterThanOrEqualTo(6)));

        // Trilha nova inicia com 0% concluído
        String pathJson = mockMvc.perform(get("/api/learning-path/me")
                .param("language", "Japonês")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.overallProgressPercent").value(0.0))
            .andExpect(jsonPath("$.modules.length()").value(greaterThanOrEqualTo(4)))
            .andReturn().getResponse().getContentAsString();

        Map<?, ?> pathMap = objectMapper.readValue(pathJson, Map.class);
        List<?> mods = (List<?>) pathMap.get("modules");
        Number firstModId = (Number) ((Map<?, ?>) mods.get(0)).get("moduleId");
        assertNotNull(firstModId);

        // Quiz do módulo retornando perguntas reais
        mockMvc.perform(get("/api/learning-path/modules/" + firstModId.longValue() + "/quiz")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(greaterThanOrEqualTo(3)));

        var quizQuestions = learningPathService.getModuleQuiz(firstModId.longValue());
        List<Integer> wrongIndices = quizQuestions.stream()
            .map(q -> (q.correctIndex() + 1) % Math.max(2, q.options().size()))
            .toList();
        List<Integer> rightIndices = quizQuestions.stream()
            .map(q -> q.correctIndex())
            .toList();

        // Reprovação com 0 acertos não conclui módulo
        mockMvc.perform(post("/api/learning-path/modules/" + firstModId.longValue() + "/quiz-submit")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of(
                    "selectedIndices", wrongIndices
                ))))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.passed").value(false))
            .andExpect(jsonPath("$.xpEarned").value(0));

        // Aprovação com respostas corretas conclui módulo e atribui XP
        mockMvc.perform(post("/api/learning-path/modules/" + firstModId.longValue() + "/quiz-submit")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of(
                    "selectedIndices", rightIndices
                ))))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.passed").value(true))
            .andExpect(jsonPath("$.xpEarned").value(greaterThanOrEqualTo(1)))
            .andExpect(jsonPath("$.path.overallProgressPercent").value(org.hamcrest.Matchers.greaterThan(0.0)));

        // Reingresso na turma funciona ao reativar matrícula cancelada e correção normaliza acentos
        School school = createSchool(inst, "Rejoin_" + sfx);
        SchoolClass sc = createClass(school, "Japonês N5 " + sfx, "Japonês");
        var enr = enrollmentService.enrollStudent(student.getId(), sc.getId(), LocalDate.now(), "Ini");
        enrollmentService.updateStatus(enr.getId(), EnrollmentStatus.CANCELLED, "Cancel");
        var reactivated = enrollmentService.enrollStudent(student.getId(), sc.getId(), LocalDate.now(), "Rejoin");
        assertEquals(EnrollmentStatus.ACTIVE, reactivated.getStatus());

        // Conversação IA funciona com outros idiomas além do Inglês (ex: Francês / Japonês)
        mockMvc.perform(post("/api/ai/conversations/start")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of(
                    "scenarioCode", "COFFEE_SHOP",
                    "targetLanguage", "Francês",
                    "cefrLevel", "A1"
                ))))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.messages[0].content", containsString("bienvenue")));

        // Normalização de acentos / pontuação em exercício
        Teacher prof = createTeacher("prof_ex_" + sfx + "@test.local", "Prof Ex", inst, List.of("Francês"));
        LearningExercise ex = new LearningExercise();
        ex.setTitle("Saudações");
        ex.setLanguageName("Francês");
        ex.setProficiencyLevel("Básico");
        ex.setSchoolClass(sc);
        ex.setTeacher(prof);
        ex.setQuestionsJson(objectMapper.writeValueAsString(List.of(
            Map.of(
                "prompt", "Traduza café:",
                "style", "Completar",
                "expectedAnswer", "Café!",
                "options", List.of()
            )
        )));
        ex = exerciseRepository.save(ex);

        String submitResp = mockMvc.perform(post("/api/exercises/" + ex.getId() + "/submissions")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of(
                    "answers", List.of("cafe")
                ))))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.correctCount").value(1))
            .andExpect(jsonPath("$.questionFeedback[0].isCorrect").value(true))
            .andReturn().getResponse().getContentAsString();
        assertTrue(submitResp.contains("\"correctCount\":1"));
    }
}

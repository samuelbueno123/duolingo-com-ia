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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
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
import com.duolinfo.ia.proj.entity.ExerciseSubmission;
import com.duolinfo.ia.proj.entity.Institution;
import com.duolinfo.ia.proj.entity.LearningExercise;
import com.duolinfo.ia.proj.entity.School;
import com.duolinfo.ia.proj.entity.SchoolClass;
import com.duolinfo.ia.proj.entity.Student;
import com.duolinfo.ia.proj.entity.StudentLanguage;
import com.duolinfo.ia.proj.entity.Teacher;
import com.duolinfo.ia.proj.repository.ClassTeacherAssignmentRepository;
import com.duolinfo.ia.proj.repository.ExerciseSubmissionRepository;
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
    @Autowired private ExerciseSubmissionRepository submissionRepository;
    @Autowired private EnrollmentService enrollmentService;
    @Autowired private com.duolinfo.ia.proj.service.LearningPathService learningPathService;
    @Autowired private com.duolinfo.ia.proj.config.UserAuthCache userAuthCache;
    @Autowired private com.duolinfo.ia.proj.repository.SpacedReviewItemRepository spacedReviewItemRepository;
    @Autowired private com.duolinfo.ia.proj.service.DefaultThreeInfoSeedService defaultThreeInfoSeedService;

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

    @Test
    @DisplayName("Admins adicionam co-docentes na turma e dashboard da turma retorna foto de perfil dos alunos")
    void adminsCanAddCoTeachersWhileTeachersCannotAndClassDashboardIncludesProfilePicture() throws Exception {
        String sfx = String.valueOf(System.nanoTime());
        Institution inst = createInst("CO_" + sfx);
        School school = createSchool(inst, "S_CO_" + sfx);
        SchoolClass sc = createClass(school, "Turma Co " + sfx, "Inglês");

        Teacher instAdmin = createTeacher("instadm_" + sfx + "@test.local", "Admin Inst", inst, List.of("Inglês"));
        instAdmin.setInstitutionAdmin(true);
        instAdmin.setApproved(true);
        teacherRepository.save(instAdmin);

        Teacher prof1 = createTeacher("prof1_" + sfx + "@test.local", "Prof Um", inst, List.of("Inglês"));
        Teacher prof2 = createTeacher("prof2_" + sfx + "@test.local", "Prof Dois", inst, List.of("Inglês"));
        Teacher prof3 = createTeacher("prof3_" + sfx + "@test.local", "Prof Tres", inst, List.of("Inglês"));

        Student aluno = createStudent("foto_" + sfx + "@test.local", "Aluno Foto", inst, "Inglês", "Básico");
        aluno.setProfilePicture("https://cdn.example.com/avatars/aluno.png");
        studentRepository.save(aluno);
        enrollmentService.enrollStudent(aluno.getId(), sc.getId(), LocalDate.now(), "Teste foto");

        String instAdminToken = jwtService.generateToken(instAdmin);
        String prof1Token = jwtService.generateToken(prof1);

        // Regular non-admin teacher cannot add a teacher via /api/class-teacher-assignments/additional or /invite?role=TEACHER
        mockMvc.perform(post("/api/class-teacher-assignments/additional")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + prof1Token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of(
                    "schoolClassId", sc.getId(),
                    "teacherId", prof2.getId(),
                    "reason", "Tentativa não autorizada"
                ))))
            .andExpect(status().isForbidden());

        mockMvc.perform(post("/api/classes/" + sc.getId() + "/invite")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + prof1Token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of(
                    "userId", prof2.getId(),
                    "role", "TEACHER"
                ))))
            .andExpect(status().isForbidden());

        // Institution admin assigns prof1 and then adds prof2 + prof3 without replacing previous ones
        mockMvc.perform(post("/api/class-teacher-assignments")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + instAdminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of(
                    "schoolClassId", sc.getId(),
                    "teacherId", prof1.getId(),
                    "reason", "Professor Titular"
                ))))
            .andExpect(status().isCreated());

        mockMvc.perform(post("/api/class-teacher-assignments/additional")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + instAdminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of(
                    "schoolClassId", sc.getId(),
                    "teacherId", prof2.getId(),
                    "reason", "Co-docente"
                ))))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.teacherName").value("Prof Dois"));

        mockMvc.perform(post("/api/classes/" + sc.getId() + "/invite")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + instAdminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of(
                    "userId", prof3.getId(),
                    "role", "TEACHER"
                ))))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.role").value("TEACHER"));

        mockMvc.perform(get("/api/class-teacher-assignments/class/" + sc.getId() + "/active")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + instAdminToken))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(3));

        // Class dashboard returns student's profilePicture
        mockMvc.perform(get("/api/classes/" + sc.getId() + "/dashboard")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + jwtService.generateToken(aluno)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.students[0].profilePicture").value("https://cdn.example.com/avatars/aluno.png"));
    }

    @Test
    @DisplayName("Dashboard da turma retorna serie temporal multi-metrica e matricula do aluno devolve DTO ativo")
    void studentJoinReturnsActiveEnrollmentAndClassDashboardExposesPerformanceSeries() throws Exception {
        String sfx = String.valueOf(System.nanoTime());
        Institution inst = createInst("PERF_" + sfx);
        School school = createSchool(inst, "PERF_" + sfx);
        SchoolClass sc = createClass(school, "Turma Grafico " + sfx, "Inglês");
        Teacher prof = createTeacher("perfprof_" + sfx + "@test.local", "Prof Grafico", inst, List.of("Inglês"));

        ClassTeacherAssignment cta = new ClassTeacherAssignment();
        cta.setSchoolClass(sc);
        cta.setTeacher(prof);
        cta.setStartDate(LocalDate.now());
        assignmentRepository.save(cta);

        Student s1 = createStudent("perfs1_" + sfx + "@test.local", "Aluno Um", inst, "Inglês", "Básico");
        Student s2 = createStudent("perfs2_" + sfx + "@test.local", "Aluno Dois", inst, "Inglês", "Básico");
        enrollmentService.enrollStudent(s1.getId(), sc.getId(), LocalDate.now(), "Inicial");

        String s2Token = jwtService.generateToken(s2);
        String profToken = jwtService.generateToken(prof);

        // Aluno ingressa na turma via endpoint oficial e recebe a matricula ACTIVE no payload
        mockMvc.perform(post("/api/students/me/enrollments")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + s2Token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of("schoolClassId", sc.getId()))))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.schoolClassId").value(sc.getId().intValue()))
            .andExpect(jsonPath("$.schoolClassName").value(sc.getName()))
            .andExpect(jsonPath("$.status").value("ACTIVE"));

        LearningExercise ex = new LearningExercise();
        ex.setTitle("Atividade Evolucao " + sfx);
        ex.setLanguageName("Inglês");
        ex.setProficiencyLevel("Básico");
        ex.setQuestionsJson("[]");
        ex.setTeacher(prof);
        ex.setSchoolClass(sc);
        ex = exerciseRepository.save(ex);

        ExerciseSubmission sub1 = new ExerciseSubmission();
        sub1.setExercise(ex);
        sub1.setStudent(s1);
        sub1.setAnswersJson("[]");
        sub1.setCorrectCount(2);
        sub1.setQuestionCount(4);
        sub1.setXpEarned(15);
        sub1.setSubmittedAt(Instant.now().minusSeconds(7200));
        submissionRepository.save(sub1);

        ExerciseSubmission sub2 = new ExerciseSubmission();
        sub2.setExercise(ex);
        sub2.setStudent(s2);
        sub2.setAnswersJson("[]");
        sub2.setCorrectCount(4);
        sub2.setQuestionCount(4);
        sub2.setXpEarned(20);
        sub2.setSubmittedAt(Instant.now().minusSeconds(3600));
        submissionRepository.save(sub2);

        entityManager.flush();

        // Tanto aluno quanto professor visualizam a serie temporal multi-metrica na turma
        for (String viewerToken : List.of(s2Token, profToken)) {
            mockMvc.perform(get("/api/classes/" + sc.getId() + "/dashboard")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + viewerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.performanceSeries.length()").value(2))
                .andExpect(jsonPath("$.performanceSeries[0].submissionsCount").value(1))
                .andExpect(jsonPath("$.performanceSeries[1].submissionsCount").value(2))
                .andExpect(jsonPath("$.performanceSeries[1].cumulativeXp").value(35))
                .andExpect(jsonPath("$.performanceSeries[0].accuracyPercent").value(50.0))
                .andExpect(jsonPath("$.performanceSeries[1].overallScorePercent").value(greaterThanOrEqualTo(1.0)));
        }
    }

    @Test
    @DisplayName("Todos os 6 idiomas possuem desafios de pronuncia, sessao SRS e 8 modulos de trilha com quiz completo")
    void allSixLanguagesHavePronunciationSrsStarterItemsAndEightLearningPathModules() throws Exception {
        String sfx = String.valueOf(System.nanoTime());
        Institution inst = createInst("SIX_" + sfx);
        Student student = createStudent("six_" + sfx + "@test.local", "Poliglota Teste", inst, "Japonês", "Básico");
        String token = jwtService.generateToken(student);

        List<String> languages = List.of("Inglês", "Espanhol", "Francês", "Alemão", "Italiano", "Japonês");

        for (String lang : languages) {
            // 1. Prática oral & pronúncia disponível para todas as 6 línguas
            mockMvc.perform(get("/api/pronunciation/challenges")
                    .param("language", lang)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(greaterThanOrEqualTo(4)));

            // 2. Revisão espaçada (SRS) gera cards iniciais por idioma automaticamente
            mockMvc.perform(get("/api/srs/session")
                    .param("language", lang)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(greaterThanOrEqualTo(5)));

            mockMvc.perform(get("/api/srs/stats")
                    .param("language", lang)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalCards").value(greaterThanOrEqualTo(5)))
                .andExpect(jsonPath("$.dueToday").value(greaterThanOrEqualTo(5)));

            // 3. Trilha personalizada possui 8 módulos progressivos A1..B2 para cada idioma
            String pathJson = mockMvc.perform(get("/api/learning-path/me")
                    .param("language", lang)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.modules.length()").value(8))
                .andReturn().getResponse().getContentAsString();

            Map<?, ?> pathMap = objectMapper.readValue(pathJson, Map.class);
            List<?> mods = (List<?>) pathMap.get("modules");
            Number firstModuleId = (Number) ((Map<?, ?>) mods.get(0)).get("moduleId");

            mockMvc.perform(get("/api/learning-path/modules/" + firstModuleId.longValue() + "/quiz")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(greaterThanOrEqualTo(3)))
                .andExpect(jsonPath("$[0].mascotHelpMessage").isNotEmpty());
        }

        // Avaliação de fala em Japonês preserva caracteres japoneses sem limpar a frase
        String jpJson = mockMvc.perform(get("/api/pronunciation/challenges")
                .param("language", "Japonês")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
            .andExpect(status().isOk())
            .andReturn().getResponse().getContentAsString();
        List<?> jpList = objectMapper.readValue(jpJson, List.class);
        Map<?, ?> firstJp = (Map<?, ?>) jpList.get(0);
        Number jpId = (Number) firstJp.get("id");
        String jpPhrase = firstJp.get("phraseText").toString();

        mockMvc.perform(post("/api/pronunciation/evaluate")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of(
                    "challengeId", jpId.longValue(),
                    "spokenText", jpPhrase
                ))))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.finalScore").value(100.0))
            .andExpect(jsonPath("$.xpEarned").value(15));
    }

    @Test
    @DisplayName("Ranking individual retorna profilePicture do estudante para exibicao no podio e na lista")
    void individualRankingIncludesStudentProfilePicture() throws Exception {
        String sfx = String.valueOf(System.nanoTime());
        Institution inst = createInst("RNK_" + sfx);
        School school = createSchool(inst, "RNK_" + sfx);
        SchoolClass sc = createClass(school, "Turma Rank " + sfx, "Inglês");
        Teacher prof = createTeacher("rnkprof_" + sfx + "@test.local", "Prof Rank", inst, List.of("Inglês"));

        Student aluno = createStudent("rnkaluno_" + sfx + "@test.local", "Foto Rank", inst, "Inglês", "Básico");
        aluno.setProfilePicture("https://cdn.example.com/avatars/rank.png");
        studentRepository.save(aluno);
        enrollmentService.enrollStudent(aluno.getId(), sc.getId(), LocalDate.now(), "Rank");

        LearningExercise ex = new LearningExercise();
        ex.setTitle("Exercicio Rank " + sfx);
        ex.setLanguageName("Inglês");
        ex.setProficiencyLevel("Básico");
        ex.setQuestionsJson("[]");
        ex.setTeacher(prof);
        ex.setSchoolClass(sc);
        ex = exerciseRepository.save(ex);

        ExerciseSubmission sub = new ExerciseSubmission();
        sub.setExercise(ex);
        sub.setStudent(aluno);
        sub.setAnswersJson("[]");
        sub.setCorrectCount(5);
        sub.setQuestionCount(5);
        sub.setXpEarned(9999);
        sub.setSubmittedAt(Instant.now());
        submissionRepository.save(sub);
        entityManager.flush();

        mockMvc.perform(get("/api/exercises/rankings/individual")
                .param("period", "week")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + jwtService.generateToken(aluno)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[?(@.studentName == 'Foto Rank')].profilePicture")
                .value(hasItem("https://cdn.example.com/avatars/rank.png")));
    }

    @Test
    @DisplayName("Todos os 9 escopos de ranking filtram por turma/escola/app e ranqueiam professores por desempenho da turma")
    void allNineRankingScopesFilterCorrectlyAndRankTeachersByClassPerformance() throws Exception {
        String sfx = String.valueOf(System.nanoTime());
        Institution instA = createInst("R9A_" + sfx);
        instA.setCity("Nova Iguaçu");
        institutionRepository.save(instA);
        Institution instB = createInst("R9B_" + sfx);
        instB.setCity("Rio de Janeiro");
        institutionRepository.save(instB);

        School schoolA = createSchool(instA, "R9A_" + sfx);
        School schoolB = createSchool(instB, "R9B_" + sfx);

        SchoolClass scA1 = createClass(schoolA, "Turma A1 " + sfx, "Inglês");
        SchoolClass scA2 = createClass(schoolA, "Turma A2 " + sfx, "Espanhol");
        SchoolClass scB1 = createClass(schoolB, "Turma B1 " + sfx, "Francês");

        Teacher profA1 = createTeacher("pa1_" + sfx + "@test.local", "Prof Alpha 1", instA, List.of("Inglês"));
        Teacher profA2 = createTeacher("pa2_" + sfx + "@test.local", "Prof Alpha 2", instA, List.of("Espanhol"));
        Teacher profB1 = createTeacher("pb1_" + sfx + "@test.local", "Prof Beta 1", instB, List.of("Francês"));

        assignmentRepository.save(ClassTeacherAssignment.builder()
            .schoolClass(scA1).teacher(profA1).startDate(LocalDate.now()).reason("Titular").build());
        assignmentRepository.save(ClassTeacherAssignment.builder()
            .schoolClass(scA2).teacher(profA2).startDate(LocalDate.now()).reason("Titular").build());
        assignmentRepository.save(ClassTeacherAssignment.builder()
            .schoolClass(scB1).teacher(profB1).startDate(LocalDate.now()).reason("Titular").build());

        Student aliceA1 = createStudent("alice_" + sfx + "@test.local", "Alice A1", instA, "Inglês", "Básico");
        Student brunoA1 = createStudent("bruno_" + sfx + "@test.local", "Bruno A1", instA, "Inglês", "Básico");
        Student carlaA2 = createStudent("carla_" + sfx + "@test.local", "Carla A2", instA, "Espanhol", "Básico");
        Student danielB1 = createStudent("daniel_" + sfx + "@test.local", "Daniel B1", instB, "Francês", "Básico");

        enrollmentService.enrollStudent(aliceA1.getId(), scA1.getId(), LocalDate.now(), "A1");
        enrollmentService.enrollStudent(brunoA1.getId(), scA1.getId(), LocalDate.now(), "A1");
        enrollmentService.enrollStudent(carlaA2.getId(), scA2.getId(), LocalDate.now(), "A2");
        enrollmentService.enrollStudent(danielB1.getId(), scB1.getId(), LocalDate.now(), "B1");

        LearningExercise exA1 = new LearningExercise();
        exA1.setTitle("Ex A1 " + sfx);
        exA1.setLanguageName("Inglês");
        exA1.setProficiencyLevel("Básico");
        exA1.setQuestionsJson("[]");
        exA1.setTeacher(profA1);
        exA1.setSchoolClass(scA1);
        exA1 = exerciseRepository.save(exA1);

        LearningExercise exA2 = new LearningExercise();
        exA2.setTitle("Ex A2 " + sfx);
        exA2.setLanguageName("Espanhol");
        exA2.setProficiencyLevel("Básico");
        exA2.setQuestionsJson("[]");
        exA2.setTeacher(profA2);
        exA2.setSchoolClass(scA2);
        exA2 = exerciseRepository.save(exA2);

        LearningExercise exB1 = new LearningExercise();
        exB1.setTitle("Ex B1 " + sfx);
        exB1.setLanguageName("Francês");
        exB1.setProficiencyLevel("Básico");
        exB1.setQuestionsJson("[]");
        exB1.setTeacher(profB1);
        exB1.setSchoolClass(scB1);
        exB1 = exerciseRepository.save(exB1);

        // scA1 / profA1: 90/100 acertos (90.0%) e 900 XP total -> teacher score 1800
        ExerciseSubmission s1 = new ExerciseSubmission();
        s1.setExercise(exA1);
        s1.setStudent(aliceA1);
        s1.setAnswersJson("[]");
        s1.setCorrectCount(50);
        s1.setQuestionCount(50);
        s1.setXpEarned(500);
        s1.setSubmittedAt(Instant.now());
        submissionRepository.save(s1);

        ExerciseSubmission s2 = new ExerciseSubmission();
        s2.setExercise(exA1);
        s2.setStudent(brunoA1);
        s2.setAnswersJson("[]");
        s2.setCorrectCount(40);
        s2.setQuestionCount(50);
        s2.setXpEarned(400);
        s2.setSubmittedAt(Instant.now());
        submissionRepository.save(s2);

        // scA2 / profA2: 50/100 acertos (50.0%) e 950 XP total -> maior XP, menor desempenho (score 1450)
        ExerciseSubmission s3 = new ExerciseSubmission();
        s3.setExercise(exA2);
        s3.setStudent(carlaA2);
        s3.setAnswersJson("[]");
        s3.setCorrectCount(50);
        s3.setQuestionCount(100);
        s3.setXpEarned(950);
        s3.setSubmittedAt(Instant.now());
        submissionRepository.save(s3);

        // scB1 / profB1: outra escola (30/50 acertos, 300 XP)
        ExerciseSubmission s4 = new ExerciseSubmission();
        s4.setExercise(exB1);
        s4.setStudent(danielB1);
        s4.setAnswersJson("[]");
        s4.setCorrectCount(30);
        s4.setQuestionCount(50);
        s4.setXpEarned(300);
        s4.setSubmittedAt(Instant.now());
        submissionRepository.save(s4);
        entityManager.flush();

        String studentToken = jwtService.generateToken(aliceA1);
        String teacherToken = jwtService.generateToken(profA1);

        // 1. Indivíduo da turma (scA1)
        mockMvc.perform(get("/api/exercises/rankings")
                .param("scope", "class_students")
                .param("schoolClassId", String.valueOf(scA1.getId()))
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + studentToken))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(2))
            .andExpect(jsonPath("$[0].studentName").value("Alice A1"))
            .andExpect(jsonPath("$[1].studentName").value("Bruno A1"));

        // 2. Indivíduo geral da escola (instA)
        mockMvc.perform(get("/api/exercises/rankings")
                .param("scope", "school_students")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + studentToken))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(3))
            .andExpect(jsonPath("$[0].studentName").value("Carla A2"))
            .andExpect(jsonPath("$[1].studentName").value("Alice A1"))
            .andExpect(jsonPath("$[2].studentName").value("Bruno A1"));

        // 3. Indivíduo geral do aplicativo
        mockMvc.perform(get("/api/exercises/rankings")
                .param("scope", "app_students")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + studentToken))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[*].studentName", hasItem("Alice A1")))
            .andExpect(jsonPath("$[*].studentName", hasItem("Daniel B1")));

        // 4. Ranking de turma na escola (instA: scA2 950 XP > scA1 900 XP)
        mockMvc.perform(get("/api/exercises/rankings")
                .param("scope", "school_classes")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + studentToken))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(2))
            .andExpect(jsonPath("$[0].className").value(scA2.getName()))
            .andExpect(jsonPath("$[1].className").value(scA1.getName()));

        // 5. Ranking de turma no aplicativo
        mockMvc.perform(get("/api/exercises/rankings")
                .param("scope", "app_classes")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + studentToken))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[*].className", hasItem(scA1.getName())))
            .andExpect(jsonPath("$[*].className", hasItem(scB1.getName())));

        // 6. Ranking de escolas
        mockMvc.perform(get("/api/exercises/rankings")
                .param("scope", "schools")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + studentToken))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[*].schoolName", hasItem(instA.getName())))
            .andExpect(jsonPath("$[*].schoolName", hasItem(instB.getName())));

        // 7. Melhor professor na turma (scA1)
        mockMvc.perform(get("/api/exercises/rankings")
                .param("scope", "class_teachers")
                .param("schoolClassId", String.valueOf(scA1.getId()))
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + teacherToken))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(1))
            .andExpect(jsonPath("$[0].studentName").value("Prof Alpha 1"))
            .andExpect(jsonPath("$[0].accuracyPercent").value(90.0));

        // 8. Melhor professor geral da escola: Prof Alpha 1 (90% / 1800) > Prof Alpha 2 (50% / 1450)
        mockMvc.perform(get("/api/exercises/rankings")
                .param("scope", "school_teachers")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + teacherToken))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(2))
            .andExpect(jsonPath("$[0].studentName").value("Prof Alpha 1"))
            .andExpect(jsonPath("$[0].accuracyPercent").value(90.0))
            .andExpect(jsonPath("$[1].studentName").value("Prof Alpha 2"))
            .andExpect(jsonPath("$[1].accuracyPercent").value(50.0));

        // 9. Melhor professor geral do aplicativo
        mockMvc.perform(get("/api/exercises/rankings")
                .param("scope", "app_teachers")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + teacherToken))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[*].studentName", hasItem("Prof Alpha 1")))
            .andExpect(jsonPath("$[*].studentName", hasItem("Prof Beta 1")));
    }

    @Test
    @DisplayName("Verdadeiro ou Falso, Correspondência (com embaralhamento) e Áudio (escuta) criam, exibem e corrigem corretamente")
    void trueFalseMatchingAndAudioQuestionsWorkEndToEnd() throws Exception {
        String sfx = String.valueOf(System.nanoTime());
        Institution inst = createInst("QST_" + sfx);
        School school = createSchool(inst, "QST_" + sfx);
        SchoolClass sc = createClass(school, "Turma Questoes " + sfx, "Inglês");
        Teacher prof = createTeacher("qprof_" + sfx + "@test.local", "Prof Questoes", inst, List.of("Inglês"));
        assignmentRepository.save(ClassTeacherAssignment.builder()
            .schoolClass(sc).teacher(prof).startDate(LocalDate.now()).reason("Titular").build());

        Student aluno = createStudent("qaluno_" + sfx + "@test.local", "Aluno Questoes", inst, "Inglês", "Básico");
        enrollmentService.enrollStudent(aluno.getId(), sc.getId(), LocalDate.now(), "Turma");
        entityManager.flush();

        String teacherToken = jwtService.generateToken(prof);
        String studentToken = jwtService.generateToken(aluno);

        Map<String, Object> payload = Map.of(
            "title", "Tipos Especiais " + sfx,
            "category", "Vocabulário",
            "theme", "Saudações",
            "languageName", "Inglês",
            "proficiencyLevel", "Básico",
            "schoolClassId", sc.getId(),
            "questions", List.of(
                Map.of(
                    "style", "Verdadeiro ou Falso",
                    "question", "O verbo 'to be' significa ser ou estar.",
                    "options", List.of(),
                    "correctAnswer", "Verdadeiro"
                ),
                Map.of(
                    "style", "Correspondência",
                    "question", "Associe as palavras às traduções",
                    "options", List.of("Apple => Maçã", "Book => Livro", "Water => Água"),
                    "correctAnswer", "Apple => Maçã | Book => Livro | Water => Água"
                ),
                Map.of(
                    "style", "Áudio",
                    "question", "Ouça o áudio e escreva a frase",
                    "options", List.of(),
                    "audioText", "Good morning",
                    "expectedAnswer", "Good morning"
                )
            )
        );

        String createdJson = mockMvc.perform(post("/api/exercises")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + teacherToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(payload)))
            .andExpect(status().isOk())
            .andReturn().getResponse().getContentAsString();
        Long exerciseId = ((Number) objectMapper.readValue(createdJson, Map.class).get("id")).longValue();

        // Estudante lê atividade sem gabarito: V/F recebe opções padrão, Correspondência recebe LEFT:: / RIGHT:: sem expor pares corretos, Áudio recebe audioText
        mockMvc.perform(get("/api/exercises/" + exerciseId)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + studentToken))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.questions[0].style").value("Verdadeiro ou Falso"))
            .andExpect(jsonPath("$.questions[0].options[0]").value("Verdadeiro"))
            .andExpect(jsonPath("$.questions[0].options[1]").value("Falso"))
            .andExpect(jsonPath("$.questions[0].correctAnswer").value(nullValue()))
            .andExpect(jsonPath("$.questions[1].style").value("Correspondência"))
            .andExpect(jsonPath("$.questions[1].options", hasItem("LEFT::Apple")))
            .andExpect(jsonPath("$.questions[1].options", hasItem("RIGHT::Maçã")))
            .andExpect(jsonPath("$.questions[1].correctAnswer").value(nullValue()))
            .andExpect(jsonPath("$.questions[2].style").value("Áudio"))
            .andExpect(jsonPath("$.questions[2].audioText").value("Good morning"))
            .andExpect(jsonPath("$.questions[2].expectedAnswer").value(nullValue()));

        // Estudante envia respostas com pares em ordem diferente -> deve corrigir 3/3!
        mockMvc.perform(post("/api/exercises/" + exerciseId + "/submissions")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + studentToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of(
                    "answers", List.of(
                        "Verdadeiro",
                        "Water => Água | Apple => Maçã | Book => Livro",
                        "good morning!"
                    )
                ))))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.correctCount").value(3))
            .andExpect(jsonPath("$.questionCount").value(3))
            .andExpect(jsonPath("$.questionFeedback[0].status").value("CORRECT"))
            .andExpect(jsonPath("$.questionFeedback[1].status").value("CORRECT"))
            .andExpect(jsonPath("$.questionFeedback[2].status").value("CORRECT"));
    }

    @Test
    @DisplayName("Resposta que precisa de correção do professor (PENDING_REVIEW) cria aviso nas notificações do professor e do co-professor")
    void pendingReviewSubmissionNotifiesTeacherInPersistentNotificationsAndMarksRead() throws Exception {
        String sfx = String.valueOf(System.nanoTime());
        Institution inst = createInst("TNOT_" + sfx);
        School school = createSchool(inst, "TNOT_" + sfx);
        SchoolClass sc = createClass(school, "Turma Redacao " + sfx, "Inglês");

        Teacher profAutor = createTeacher("tautor_" + sfx + "@test.local", "Prof Autor", inst, List.of("Inglês"));
        Teacher coProf = createTeacher("tco_" + sfx + "@test.local", "Co Prof", inst, List.of("Inglês"));
        assignmentRepository.save(ClassTeacherAssignment.builder()
            .schoolClass(sc).teacher(profAutor).startDate(LocalDate.now()).reason("Titular").build());
        assignmentRepository.save(ClassTeacherAssignment.builder()
            .schoolClass(sc).teacher(coProf).startDate(LocalDate.now()).reason("Co-docente").build());

        Student aluno = createStudent("maluno_" + sfx + "@test.local", "Maria Silva", inst, "Inglês", "Básico");
        enrollmentService.enrollStudent(aluno.getId(), sc.getId(), LocalDate.now(), "Turma");
        entityManager.flush();

        String teacherToken = jwtService.generateToken(profAutor);
        String coTeacherToken = jwtService.generateToken(coProf);
        String studentToken = jwtService.generateToken(aluno);

        // 1. Atividade com questão discursiva (requer correção do professor -> PENDING_REVIEW)
        Map<String, Object> openPayload = Map.of(
            "title", "Redação Semanal " + sfx,
            "category", "Escrita",
            "theme", "Rotina",
            "languageName", "Inglês",
            "proficiencyLevel", "Básico",
            "schoolClassId", sc.getId(),
            "questions", List.of(
                Map.of(
                    "style", "Discursiva",
                    "question", "Write a short paragraph about your morning routine.",
                    "options", List.of(),
                    "expectedAnswer", "Use present simple verbs cleanly"
                )
            )
        );
        String createdOpenJson = mockMvc.perform(post("/api/exercises")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + teacherToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(openPayload)))
            .andExpect(status().isOk())
            .andReturn().getResponse().getContentAsString();
        Long openExId = ((Number) objectMapper.readValue(createdOpenJson, Map.class).get("id")).longValue();

        // 2. Atividade apenas objetiva (AUTO_GRADED -> NÃO deve gerar aviso de correção pendente)
        Map<String, Object> mcPayload = Map.of(
            "title", "Quiz Rapido " + sfx,
            "category", "Vocabulário",
            "theme", "Cores",
            "languageName", "Inglês",
            "proficiencyLevel", "Básico",
            "schoolClassId", sc.getId(),
            "questions", List.of(
                Map.of(
                    "style", "Múltipla Escolha",
                    "question", "Blue = ?",
                    "options", List.of("Azul", "Verde"),
                    "correctAnswer", "Azul"
                )
            )
        );
        String createdMcJson = mockMvc.perform(post("/api/exercises")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + teacherToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(mcPayload)))
            .andExpect(status().isOk())
            .andReturn().getResponse().getContentAsString();
        Long mcExId = ((Number) objectMapper.readValue(createdMcJson, Map.class).get("id")).longValue();

        // Submete quiz objetivo primeiro -> 0 avisos pendentes
        mockMvc.perform(post("/api/exercises/" + mcExId + "/submissions")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + studentToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of("answers", List.of("Azul")))))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.reviewStatus").value("AUTO_GRADED"));

        mockMvc.perform(get("/api/teachers/me/notifications/unread-count")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + teacherToken))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.unreadCount").value(0));

        // Submete questão discursiva -> gera aviso "Correção pendente" para ambos professores da turma
        mockMvc.perform(post("/api/exercises/" + openExId + "/submissions")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + studentToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of(
                    "answers", List.of("I wake up at seven every day and study English.")
                ))))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.reviewStatus").value("PENDING_REVIEW"));

        entityManager.flush();

        mockMvc.perform(get("/api/teachers/me/notifications/unread-count")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + teacherToken))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.unreadCount").value(1));

        mockMvc.perform(get("/api/teachers/me/notifications/unread-count")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + coTeacherToken))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.unreadCount").value(1));

        String notifsJson = mockMvc.perform(get("/api/teachers/me/notifications")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + teacherToken))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(1))
            .andExpect(jsonPath("$[0].title").value("Correção pendente"))
            .andExpect(jsonPath("$[0].type").value("CORRECTION_PENDING"))
            .andExpect(jsonPath("$[0].actionUrl").value("/review-exercises"))
            .andExpect(jsonPath("$[0].read").value(false))
            .andExpect(jsonPath("$[0].message", containsString("Maria Silva")))
            .andExpect(jsonPath("$[0].message", containsString("Redação Semanal")))
            .andReturn().getResponse().getContentAsString();

        List<?> notifList = objectMapper.readValue(notifsJson, List.class);
        Long notifId = ((Number) ((Map<?, ?>) notifList.get(0)).get("id")).longValue();

        // Marcar notificação como lida reduz contagem não lida para 0
        mockMvc.perform(patch("/api/teachers/me/notifications/" + notifId + "/read")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + teacherToken))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.read").value(true));

        mockMvc.perform(get("/api/teachers/me/notifications/unread-count")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + teacherToken))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.unreadCount").value(0));
    }

    @Test
    @DisplayName("Regressão: SuperAdmin acessa arquivos de log separados com retenção de 30 dias e segurança")
    void superAdminShouldInspectDownloadAndCleanSeparatedSystemLogFiles() throws Exception {
        com.duolinfo.ia.proj.entity.User adminUser = entityManager
            .createQuery("SELECT u FROM User u WHERE lower(u.email) = 'admin@duolingo.com'", com.duolinfo.ia.proj.entity.User.class)
            .getSingleResult();
        String adminToken = jwtService.generateToken(adminUser);

        Student regularStudent = createStudent("logperm@test.local", "Aluno Log", createInst("LOGPERM"), "en", "A1");
        String studentToken = jwtService.generateToken(regularStudent);

        // Estudante comum não pode acessar endpoints de log de servidor
        mockMvc.perform(get("/api/admin/logs/files")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + studentToken))
            .andExpect(status().isForbidden());

        // SuperAdmin lista os arquivos separados (application.log, errors.log, security-audit.log)
        mockMvc.perform(get("/api/admin/logs/files")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.retentionDays").value(30))
            .andExpect(jsonPath("$.files[*].fileName", hasItem("security-audit.log")))
            .andExpect(jsonPath("$.files[*].fileName", hasItem("application.log")));

        // SuperAdmin lê linhas do canal security-audit.log com contagem de severidades
        mockMvc.perform(get("/api/admin/logs/files/security-audit.log")
                .param("limit", "25")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.fileName").value("security-audit.log"))
            .andExpect(jsonPath("$.channel").value("security-audit"))
            .andExpect(jsonPath("$.retentionDays").value(30))
            .andExpect(jsonPath("$.entries").isArray());

        // Download seguro retorna anexo com Content-Disposition
        mockMvc.perform(get("/api/admin/logs/files/security-audit.log/download")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken))
            .andExpect(status().isOk())
            .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.header()
                .string(HttpHeaders.CONTENT_DISPOSITION, containsString("security-audit.log")));

        // Tentativa de path traversal bloqueada (400)
        mockMvc.perform(get("/api/admin/logs/files/not-a-log.txt")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken))
            .andExpect(status().isBadRequest());

        // Disparo manual de política de retenção de 30 dias
        mockMvc.perform(post("/api/admin/logs/files/cleanup")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.retentionDays").value(30));
    }

    @Test
    @DisplayName("Regressão: Professor aprovado e Admin podem alterar fotos da instituição e turmas sem risco de IDOR")
    void teachersAndAdminsCanUpdateInstitutionAndClassPicturesWithIsolation() throws Exception {
        String sfx = String.valueOf(System.nanoTime());
        Institution instA = createInst("PICA_" + sfx);
        Institution instB = createInst("PICB_" + sfx);
        School schoolA = createSchool(instA, "PICA_" + sfx);
        SchoolClass classA = createClass(schoolA, "Turma Foto " + sfx, "Inglês");

        Teacher teacherA = createTeacher("teachA_" + sfx + "@test.local", "Prof A", instA, List.of("Inglês"));
        Teacher teacherB = createTeacher("teachB_" + sfx + "@test.local", "Prof B", instB, List.of("Espanhol"));
        Student studentA = createStudent("studA_" + sfx + "@test.local", "Aluno A", instA, "Inglês", "A1");
        com.duolinfo.ia.proj.entity.User adminUser = entityManager
            .createQuery("SELECT u FROM User u WHERE lower(u.email) = 'admin@duolingo.com'", com.duolinfo.ia.proj.entity.User.class)
            .getSingleResult();

        String teacherAToken = jwtService.generateToken(teacherA);
        String teacherBToken = jwtService.generateToken(teacherB);
        String studentToken = jwtService.generateToken(studentA);
        String adminToken = jwtService.generateToken(adminUser);

        java.io.ByteArrayOutputStream pngBaos = new java.io.ByteArrayOutputStream();
        javax.imageio.ImageIO.write(
            new java.awt.image.BufferedImage(16, 16, java.awt.image.BufferedImage.TYPE_INT_ARGB),
            "png",
            pngBaos
        );
        String validDataPng = "data:image/png;base64,"
            + java.util.Base64.getEncoder().encodeToString(pngBaos.toByteArray());

        // Estudante e professor de outra instituição não podem alterar fotos
        mockMvc.perform(put("/api/institutions/" + instA.getId() + "/picture")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + studentToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of("pictureUrl", validDataPng))))
            .andExpect(status().isForbidden());

        mockMvc.perform(put("/api/institutions/" + instA.getId() + "/picture")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + teacherBToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of("pictureUrl", validDataPng))))
            .andExpect(status().isForbidden());

        mockMvc.perform(put("/api/classes/" + classA.getId() + "/picture")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + teacherBToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of("pictureUrl", validDataPng))))
            .andExpect(status().isForbidden());

        // Professor aprovado da instituição A atualiza foto da instituição A e da turma A
        String instPicJson = mockMvc.perform(put("/api/institutions/" + instA.getId() + "/picture")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + teacherAToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of("pictureUrl", validDataPng))))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.pictureUrl", containsString("/media/profile-pictures/")))
            .andReturn().getResponse().getContentAsString();
        String instPicPath = objectMapper.readTree(instPicJson).get("pictureUrl").asText();

        String classPicJson = mockMvc.perform(put("/api/classes/" + classA.getId() + "/picture")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + teacherAToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of("pictureUrl", validDataPng))))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.pictureUrl", containsString("/media/profile-pictures/")))
            .andReturn().getResponse().getContentAsString();
        String classPicPath = objectMapper.readTree(classPicJson).get("pictureUrl").asText();

        // Verifica persistência nos GETs de instituição e dashboard da turma
        mockMvc.perform(get("/api/institutions/" + instA.getId())
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + studentToken))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.pictureUrl").value(instPicPath));

        mockMvc.perform(get("/api/classes/" + classA.getId() + "/dashboard")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + teacherAToken))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.pictureUrl").value(classPicPath))
            .andExpect(jsonPath("$.institutionPictureUrl").value(instPicPath));

        // Admin atualiza via PATCH e depois remove a foto da instituição
        mockMvc.perform(patch("/api/institutions/" + instA.getId() + "/picture")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of("pictureUrl", validDataPng))))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.pictureUrl", containsString("/media/profile-pictures/")));

        mockMvc.perform(put("/api/institutions/" + instA.getId() + "/picture")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"pictureUrl\":\"\"}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.pictureUrl").value(nullValue()));

        mockMvc.perform(put("/api/classes/" + classA.getId() + "/picture")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + teacherAToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"pictureUrl\":\"\"}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.pictureUrl").value(nullValue()));
    }

    @Test
    @DisplayName("Regressão: Turma padrão 3 INFO é criada com admin@duolingo.com e 7 atividades (3 questões variadas cada)")
    void defaultThreeInfoClassAndSevenVariedExercisesSeededForAdminTeacher() throws Exception {
        Teacher adminTeacher = teacherRepository.findByEmailIgnoreCase("admin@duolingo.com")
            .orElseThrow();
        assertTrue(Boolean.TRUE.equals(adminTeacher.getApproved()));

        SchoolClass threeInfo = schoolClassRepository.findAll().stream()
            .filter(sc -> "3 INFO".equalsIgnoreCase(sc.getName()))
            .findFirst()
            .orElseThrow();
        assertTrue(Boolean.TRUE.equals(threeInfo.getActive()));

        boolean assignedToAdmin = assignmentRepository.findByTeacherId(adminTeacher.getId()).stream()
            .anyMatch(a -> a.getSchoolClass() != null
                && a.getSchoolClass().getId().equals(threeInfo.getId())
                && a.getEndDate() == null);
        assertTrue(assignedToAdmin, "admin@duolingo.com deve estar atribuído como professor da turma padrão 3 INFO");

        String adminToken = jwtService.generateToken(adminTeacher);
        String mineJson = mockMvc.perform(get("/api/exercises")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken))
            .andExpect(status().isOk())
            .andReturn().getResponse().getContentAsString();

        List<?> allMine = objectMapper.readValue(mineJson, List.class);
        var threeInfoExercises = allMine.stream()
            .filter(Map.class::isInstance)
            .map(m -> (Map<?, ?>) m)
            .filter(m -> threeInfo.getId().equals(((Number) m.get("schoolClassId")).longValue()))
            .toList();

        assertEquals(7, threeInfoExercises.size(), "Turma padrão 3 INFO deve possuir exatamente 7 atividades iniciais");

        java.util.Set<String> observedStyles = new java.util.HashSet<>();
        for (Map<?, ?> exMap : threeInfoExercises) {
            List<?> qs = (List<?>) exMap.get("questions");
            assertNotNull(qs);
            assertEquals(3, qs.size(), "Cada atividade da turma 3 INFO deve conter exatamente 3 questões");
            for (Object qObj : qs) {
                Map<?, ?> q = (Map<?, ?>) qObj;
                if (q.get("style") != null) {
                    observedStyles.add(q.get("style").toString());
                }
            }
        }
        assertTrue(observedStyles.size() >= 6,
            "As questões devem conter ampla variação de estilos, encontrados: " + observedStyles);
        assertTrue(observedStyles.contains("Múltipla Escolha"));
        assertTrue(observedStyles.contains("Verdadeiro ou Falso"));
        assertTrue(observedStyles.contains("Correspondência"));
        assertTrue(observedStyles.contains("Áudio"));
        assertTrue(observedStyles.contains("Completar"));
        assertTrue(observedStyles.contains("Discursiva"));

        // Execução repetida do seed deve permanecer idempotente (continua com 7 atividades e 1 vínculo)
        defaultThreeInfoSeedService.ensureDefaultThreeInfoSetup(threeInfo.getInstitution().getId());
        long countAfter = exerciseRepository.findBySchoolClass_IdOrderByCreatedAtDesc(threeInfo.getId()).size();
        assertEquals(7L, countAfter);
    }

    @Test
    @DisplayName("Regressão: Trilha exibe apenas idiomas do aluno e calcula competências reais por idioma selecionado")
    void learningPathRestrictsToEnrolledLanguagesAndComputesRealDynamicCompetences() throws Exception {
        String sfx = String.valueOf(System.nanoTime());
        Institution inst = createInst("LPDYN_" + sfx);
        School school = createSchool(inst, "LPDYN_" + sfx);
        SchoolClass jpClass = createClass(school, "Turma JP " + sfx, "Japonês");
        Teacher teacher = createTeacher("t_dyn_" + sfx + "@test.local", "Prof JP", inst, List.of("Japonês"));

        Student student = createStudent("s_dyn_" + sfx + "@test.local", "Aluno Poliglota", inst, "Japonês", "Básico");
        student.addLanguage(StudentLanguage.builder()
            .languageName("Espanhol")
            .level("Básico")
            .score(0)
            .build());
        studentRepository.save(student);
        String token = jwtService.generateToken(student);

        // Sem passar ?language=, seleciona o primeiro idioma aprendido pelo estudante ("Japonês")
        // e retorna estritamente availableLanguages = ["Japonês", "Espanhol"]
        String initialJson = mockMvc.perform(get("/api/learning-path/me")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.language").value("Japonês"))
            .andExpect(jsonPath("$.availableLanguages.length()").value(2))
            .andExpect(jsonPath("$.availableLanguages[0]").value("Japonês"))
            .andExpect(jsonPath("$.availableLanguages[1]").value("Espanhol"))
            .andExpect(jsonPath("$.grammarCompetence").value(0.0))
            .andReturn().getResponse().getContentAsString();

        Map<?, ?> initialMap = objectMapper.readValue(initialJson, Map.class);
        List<?> mods = (List<?>) initialMap.get("modules");
        Number firstJpModuleId = (Number) ((Map<?, ?>) mods.get(0)).get("moduleId");

        // Registra atividades reais SOMENTE em Japonês:
        // 1) Exercício de turma de gramática/vocabulário respondido com 100%
        LearningExercise ex = new LearningExercise();
        ex.setTitle("Gramática e Vocabulário Japonês N5 " + sfx);
        ex.setCategory("Gramática");
        ex.setLanguageName("Japonês");
        ex.setProficiencyLevel("A1");
        ex.setQuestionsJson("[]");
        ex.setTeacher(teacher);
        ex.setSchoolClass(jpClass);
        ex = exerciseRepository.save(ex);

        ExerciseSubmission sub = new ExerciseSubmission();
        sub.setExercise(ex);
        sub.setStudent(student);
        sub.setAnswersJson("[]");
        sub.setCorrectCount(5);
        sub.setQuestionCount(5);
        sub.setXpEarned(30);
        sub.setReviewStatus("AUTO_GRADED");
        submissionRepository.save(sub);

        // 2) Dois flashcards SRS revisados em Japonês
        for (int i = 0; i < 2; i++) {
            com.duolinfo.ia.proj.entity.SpacedReviewItem item = new com.duolinfo.ia.proj.entity.SpacedReviewItem();
            item.setStudent(student);
            item.setLanguageName("Japonês");
            item.setTerm("term_" + i);
            item.setTranslation("trad_" + i);
            item.setRepetitionNumber(2);
            item.setDueDate(LocalDate.now().plusDays(3));
            spacedReviewItemRepository.save(item);
        }

        // 3) Conclusão real do primeiro módulo de Japonês via quiz
        var quizQuestions = learningPathService.getModuleQuiz(firstJpModuleId.longValue());
        List<Integer> rightIndices = quizQuestions.stream().map(q -> q.correctIndex()).toList();
        mockMvc.perform(post("/api/learning-path/modules/" + firstJpModuleId.longValue() + "/quiz-submit")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of("selectedIndices", rightIndices))))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.passed").value(true));

        entityManager.flush();
        entityManager.clear();

        // Verifica crescimento real em Japonês vs Espanhol intacto
        mockMvc.perform(get("/api/learning-path/me")
                .param("language", "Japonês")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.grammarCompetence").value(org.hamcrest.Matchers.greaterThan(15.0)))
            .andExpect(jsonPath("$.vocabularyCompetence").value(org.hamcrest.Matchers.greaterThan(18.0)))
            .andExpect(jsonPath("$.readingCompetence").value(org.hamcrest.Matchers.greaterThan(12.0)))
            .andExpect(jsonPath("$.activityMetrics.modulesCompleted").value(1))
            .andExpect(jsonPath("$.activityMetrics.exerciseSubmissions").value(1))
            .andExpect(jsonPath("$.activityMetrics.srsReviewed").value(2));

        mockMvc.perform(get("/api/learning-path/me")
                .param("language", "Espanhol")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.language").value("Espanhol"))
            .andExpect(jsonPath("$.grammarCompetence").value(0.0))
            .andExpect(jsonPath("$.vocabularyCompetence").value(0.0))
            .andExpect(jsonPath("$.activityMetrics.modulesCompleted").value(0))
            .andExpect(jsonPath("$.activityMetrics.exerciseSubmissions").value(0))
            .andExpect(jsonPath("$.activityMetrics.srsReviewed").value(0));
    }
}

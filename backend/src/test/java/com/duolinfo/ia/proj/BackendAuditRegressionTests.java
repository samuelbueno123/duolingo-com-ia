package com.duolinfo.ia.proj;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.not;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import com.duolinfo.ia.proj.config.Utf8MojibakeSanitizer;
import com.duolinfo.ia.proj.dto.request.StudentLanguageRequestDTO;
import com.duolinfo.ia.proj.entity.CatalogOption;
import com.duolinfo.ia.proj.entity.ClassTeacherAssignment;
import com.duolinfo.ia.proj.entity.CommunitySubmission;
import com.duolinfo.ia.proj.entity.Enrollment;
import com.duolinfo.ia.proj.entity.EnrollmentStatus;
import com.duolinfo.ia.proj.entity.Institution;
import com.duolinfo.ia.proj.entity.LearningExercise;
import com.duolinfo.ia.proj.entity.LearningPathModule;
import com.duolinfo.ia.proj.entity.School;
import com.duolinfo.ia.proj.entity.SchoolClass;
import com.duolinfo.ia.proj.entity.SpacedReviewItem;
import com.duolinfo.ia.proj.entity.Student;
import com.duolinfo.ia.proj.entity.StudentLanguage;
import com.duolinfo.ia.proj.entity.Teacher;
import com.duolinfo.ia.proj.entity.TeacherDocument;
import com.duolinfo.ia.proj.repository.CatalogOptionRepository;
import com.duolinfo.ia.proj.repository.ClassTeacherAssignmentRepository;
import com.duolinfo.ia.proj.repository.CommunitySubmissionRepository;
import com.duolinfo.ia.proj.repository.EnrollmentRepository;
import com.duolinfo.ia.proj.repository.InstitutionRepository;
import com.duolinfo.ia.proj.repository.LearningExerciseRepository;
import com.duolinfo.ia.proj.repository.LearningPathModuleRepository;
import com.duolinfo.ia.proj.repository.SchoolClassRepository;
import com.duolinfo.ia.proj.repository.SchoolRepository;
import com.duolinfo.ia.proj.repository.SpacedReviewItemRepository;
import com.duolinfo.ia.proj.repository.StudentLanguageRepository;
import com.duolinfo.ia.proj.repository.StudentRepository;
import com.duolinfo.ia.proj.repository.TeacherDocumentRepository;
import com.duolinfo.ia.proj.repository.TeacherRepository;
import com.duolinfo.ia.proj.service.AdminService;
import com.duolinfo.ia.proj.service.JwtService;
import com.duolinfo.ia.proj.service.LearningExerciseService;
import com.duolinfo.ia.proj.service.StudentStreakService;

import tools.jackson.databind.ObjectMapper;

@SpringBootTest
@AutoConfigureMockMvc
class BackendAuditRegressionTests {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private JwtService jwtService;
    @Autowired private StudentRepository studentRepository;
    @Autowired private TeacherRepository teacherRepository;
    @Autowired private InstitutionRepository institutionRepository;
    @Autowired private SchoolRepository schoolRepository;
    @Autowired private SchoolClassRepository schoolClassRepository;
    @Autowired private TeacherDocumentRepository teacherDocumentRepository;
    @Autowired private CatalogOptionRepository catalogOptionRepository;
    @Autowired private CommunitySubmissionRepository communitySubmissionRepository;
    @Autowired private LearningPathModuleRepository learningPathModuleRepository;
    @Autowired private SpacedReviewItemRepository spacedReviewItemRepository;
    @Autowired private LearningExerciseRepository learningExerciseRepository;
    @Autowired private StudentLanguageRepository studentLanguageRepository;
    @Autowired private EnrollmentRepository enrollmentRepository;
    @Autowired private ClassTeacherAssignmentRepository classTeacherAssignmentRepository;
    @Autowired private AdminService adminService;
    @Autowired private LearningExerciseService learningExerciseService;
    @Autowired private StudentStreakService studentStreakService;
    @Autowired(required = false) private CacheManager cacheManager;

    private String uniq() {
        return String.valueOf(System.nanoTime());
    }

    private Institution createInstitution(String suffix) {
        return institutionRepository.saveAndFlush(Institution.builder()
            .name("Inst Audit " + suffix)
            .code("AUD" + suffix)
            .email("inst_" + suffix + "@audit.test")
            .active(true)
            .build());
    }

    private Student createStudent(String suffix, Institution inst, boolean active) {
        Student s = new Student();
        s.setName("Aluno Audit " + suffix);
        s.setEmail("student_" + suffix + "@audit.test");
        s.setActive(active);
        s.setInstitution(inst);
        return studentRepository.saveAndFlush(s);
    }

    private Teacher createTeacher(String suffix, Institution inst, boolean approved, boolean instAdmin, boolean admin) {
        Teacher t = new Teacher();
        t.setName("Prof Audit " + suffix);
        t.setEmail("teacher_" + suffix + "@audit.test");
        t.setInstitution(inst);
        t.setApproved(approved);
        t.setApprovalStatus(approved ? "APPROVED" : "PENDING");
        t.setInstitutionAdmin(instAdmin);
        t.setAdmin(admin);
        t.setActive(true);
        return teacherRepository.saveAndFlush(t);
    }

    @Test
    @DisplayName("Usuario desativado nao consegue autenticar com JWT previamente emitido e cache e invalidado")
    void deactivatedUserJwtIsRejectedAndCacheEvicted() throws Exception {
        String sfx = uniq();
        Student student = createStudent(sfx, null, true);
        String token = jwtService.generateToken(student, "STUDENT");

        // Popula UserAuthCache
        mockMvc.perform(get("/api/students/me")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
            .andExpect(status().isOk());

        // Super-admin suspende usuario e evicta cache
        adminService.toggleUserActive(student.getId(), "admin@duolingo.com");

        mockMvc.perform(get("/api/students/me")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
            .andExpect(status().isUnauthorized());

        mockMvc.perform(get("/api/home")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
            .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Professor pendente acessa GET /api/teachers/me e professor aprovado acessa POST /api/ai/explain-mistake")
    void pendingTeacherMeAndAiExplainMistakeAccessibleToTeachers() throws Exception {
        String sfx = uniq();
        Institution inst = createInstitution(sfx);
        Teacher pending = createTeacher("p_" + sfx, inst, false, false, false);
        String pendingToken = jwtService.generateToken(pending, "USER");

        mockMvc.perform(get("/api/teachers/me")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + pendingToken))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.email").value(pending.getEmail()));

        Teacher approved = createTeacher("a_" + sfx, inst, true, false, false);
        String approvedToken = jwtService.generateToken(approved, "TEACHER");

        mockMvc.perform(post("/api/ai/explain-mistake")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + approvedToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of(
                    "question", "Complete: I ____ happy yesterday.",
                    "wrongAnswer", "am",
                    "correctAnswer", "was",
                    "language", "Inglês"
                ))))
            .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Catalogo de idiomas aceita catalogType 'LANGUAGE' case-insensitive e filtra estritamente os 6 idiomas base")
    void catalogLanguagesIsCaseInsensitive() throws Exception {
        CatalogOption invalidOpt = catalogOptionRepository.saveAndFlush(new CatalogOption("LANGUAGE", "RussoInvalido" + uniq()));
        CatalogOption aliasOpt = catalogOptionRepository.saveAndFlush(new CatalogOption("LANGUAGE", "alemao"));
        try {
            if (cacheManager != null) {
                Cache cache = cacheManager.getCache("catalogLanguages");
                if (cache != null) {
                    cache.clear();
                }
            }

            mockMvc.perform(get("/api/catalog/languages"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(6))
                .andExpect(jsonPath("$", hasItem("Alemão")))
                .andExpect(jsonPath("$", hasItem("Japonês")))
                .andExpect(jsonPath("$", not(hasItem(invalidOpt.getValue()))));
        } finally {
            catalogOptionRepository.delete(invalidOpt);
            catalogOptionRepository.delete(aliasOpt);
            catalogOptionRepository.flush();
            if (cacheManager != null) {
                Cache cache = cacheManager.getCache("catalogLanguages");
                if (cache != null) {
                    cache.clear();
                }
            }
        }
    }

    @Test
    @DisplayName("Documento docente bloqueia admin de outra instituicao e sanitiza Content-Disposition para o dono/super-admin")
    void teacherDocumentCrossInstitutionIdorBlockedAndFilenameSanitized() throws Exception {
        String sfx = uniq();
        Institution instA = createInstitution("A_" + sfx);
        Institution instB = createInstitution("B_" + sfx);

        Teacher teacherA = createTeacher("ta_" + sfx, instA, true, false, false);
        Teacher instAdminB = createTeacher("ib_" + sfx, instB, true, true, false);
        Teacher instAdminA = createTeacher("ia_" + sfx, instA, true, true, false);

        TeacherDocument doc = new TeacherDocument();
        doc.setTeacher(teacherA);
        doc.setFileName("comprovante\"\r\nSet-Cookie:evil.pdf");
        doc.setContentType("application/pdf");
        doc.setFileSize(4L);
        doc.setFileData(new byte[] { 1, 2, 3, 4 });
        doc = teacherDocumentRepository.saveAndFlush(doc);

        String tokenAdminB = jwtService.generateToken(instAdminB, "ADMIN");
        mockMvc.perform(get("/api/admin/documents/" + doc.getId() + "/download")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenAdminB))
            .andExpect(status().isForbidden());

        String tokenAdminA = jwtService.generateToken(instAdminA, "ADMIN");
        mockMvc.perform(get("/api/admin/documents/" + doc.getId() + "/download")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenAdminA))
            .andExpect(status().isOk())
            .andExpect(header().string(HttpHeaders.CONTENT_DISPOSITION, not(containsString("\r"))))
            .andExpect(header().string(HttpHeaders.CONTENT_DISPOSITION, not(containsString("\n"))));
    }

    @Test
    @DisplayName("Submissao privada da comunidade bloqueia leitura e avaliacao de terceiros com 403")
    void privateCommunitySubmissionBlocksReadAndReviewByOtherStudent() throws Exception {
        String sfx = uniq();
        Student author = createStudent("aut_" + sfx, null, true);
        Student stranger = createStudent("str_" + sfx, null, true);

        CommunitySubmission priv = communitySubmissionRepository.saveAndFlush(CommunitySubmission.builder()
            .authorStudentId(author.getId())
            .authorName(author.getName())
            .language("Inglês")
            .promptTitle("Rascunho Privado")
            .content("My private text")
            .targetLevel("A2")
            .isPublic(false)
            .status("PENDING_REVIEW")
            .reviewCount(0)
            .createdAt(LocalDateTime.now())
            .build());

        String strangerToken = jwtService.generateToken(stranger, "STUDENT");

        mockMvc.perform(get("/api/community/submissions/" + priv.getId())
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + strangerToken))
            .andExpect(status().isForbidden());

        mockMvc.perform(post("/api/community/reviews")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + strangerToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of(
                    "submissionId", priv.getId(),
                    "overallFeedback", "Tentativa indevida",
                    "correctedContent", "Corrected",
                    "rating", 4
                ))))
            .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("GET /api/home prioriza ADMIN sobre TEACHER e nao expoe passwordHash no payload JSON")
    void homePrioritizesAdminOverTeacherAndOmitsPasswordHash() throws Exception {
        String sfx = uniq();
        Institution inst = createInstitution(sfx);
        Teacher adminTeacher = createTeacher(sfx, inst, true, false, true);
        adminTeacher.setPasswordHash("$2a$10$NeverLeakThisHashValue");
        adminTeacher = teacherRepository.saveAndFlush(adminTeacher);

        String token = jwtService.generateToken(adminTeacher, "ADMIN");
        String body = mockMvc.perform(get("/api/home")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.profileType").value("ADMIN"))
            .andExpect(jsonPath("$.user.passwordHash").doesNotExist())
            .andReturn()
            .getResponse()
            .getContentAsString();

        assertTrue(!body.contains("NeverLeakThisHashValue"));
        assertTrue(!body.contains("passwordHash"));
    }

    @Test
    @DisplayName("getStudentProgressSummary preserva XP acumulado fora de exercicios (SRS/comunidade/trilha)")
    void progressSummaryPreservesNonExerciseXp() {
        String sfx = uniq();
        Institution inst = createInstitution(sfx);
        School school = schoolRepository.saveAndFlush(School.builder().name("Esc " + sfx).institution(inst).build());
        SchoolClass sc = schoolClassRepository.saveAndFlush(SchoolClass.builder().name("Turma " + sfx).school(school).institution(inst).active(true).build());
        Teacher teacher = createTeacher(sfx, inst, true, false, false);
        Student student = createStudent(sfx, inst, true);

        StudentLanguage lang = studentLanguageRepository.saveAndFlush(StudentLanguage.builder()
            .student(student)
            .languageName("Inglês")
            .level("Básico")
            .score(250)
            .build());
        assertNotNull(lang.getId());

        enrollmentRepository.saveAndFlush(Enrollment.builder()
            .student(student)
            .schoolClass(sc)
            .enrollmentDate(LocalDate.now())
            .status(EnrollmentStatus.ACTIVE)
            .build());

        LearningExercise exercise = new LearningExercise();
        exercise.setTitle("Ex " + sfx);
        exercise.setLanguageName("Inglês");
        exercise.setProficiencyLevel("A1");
        exercise.setTeacher(teacher);
        exercise.setSchoolClass(sc);
        exercise.setActive(true);
        exercise.setQuestionsJson("[{\"style\":\"Objetiva\",\"question\":\"Hi?\",\"options\":[\"Hi\",\"Bye\"],\"correctAnswer\":\"Hi\"}]");
        learningExerciseRepository.saveAndFlush(exercise);

        learningExerciseService.getStudentProgressSummary(student.getEmail(), "Inglês");
        int scoreAfter = studentLanguageRepository
            .findByStudentAndLanguageNameIgnoreCase(student, "Inglês")
            .orElseThrow()
            .getScore();
        assertEquals(250, scoreAfter);
    }

    @Test
    @DisplayName("Endpoints restritos negam acesso de STUDENT com 403 (generate-ai, /api/users, join-class cross-institution)")
    void restrictedEndpointsReturn403ForUnauthorizedRolesAndCrossInstitution() throws Exception {
        String sfx = uniq();
        Institution instA = createInstitution("JA_" + sfx);
        Institution instB = createInstitution("JB_" + sfx);
        School schoolB = schoolRepository.saveAndFlush(School.builder().name("Esc B " + sfx).institution(instB).build());
        SchoolClass classB = schoolClassRepository.saveAndFlush(SchoolClass.builder().name("Turma B " + sfx).school(schoolB).institution(instB).active(true).build());

        Student studentA = createStudent(sfx, instA, true);
        String studentToken = jwtService.generateToken(studentA, "STUDENT");

        mockMvc.perform(post("/api/exercises/generate-ai")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + studentToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"language\":\"Inglês\",\"proficiencyLevel\":\"A1\",\"topic\":\"Verb to be\",\"questionCount\":2}"))
            .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/users")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + studentToken))
            .andExpect(status().isForbidden());

        mockMvc.perform(post("/api/students/me/enrollments")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + studentToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of("schoolClassId", classB.getId()))))
            .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Trilha de aprendizado nao expoe correctIndex no diagnostico e evita XP duplicado ao reconcluir modulo")
    void learningPathHidesCorrectIndexAndPreventsDuplicateXpOnRecompletion() throws Exception {
        String sfx = uniq();
        Student student = createStudent(sfx, null, true);
        studentLanguageRepository.saveAndFlush(StudentLanguage.builder()
            .student(student)
            .languageName("Inglês")
            .level("Básico")
            .score(100)
            .build());

        LearningPathModule module = learningPathModuleRepository.saveAndFlush(LearningPathModule.builder()
            .language("Inglês")
            .cefrLevel("A1")
            .moduleOrder(99)
            .title("Módulo Audit " + sfx)
            .description("Descrição")
            .category("Fundamentos")
            .requiredXp(40)
            .build());

        String token = jwtService.generateToken(student, "STUDENT");

        mockMvc.perform(get("/api/learning-path/diagnostic")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].correctIndex").doesNotExist());

        mockMvc.perform(post("/api/learning-path/modules/" + module.getId() + "/complete")
                .param("scorePercent", "250.0")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
            .andExpect(status().isOk());

        int scoreAfterFirst = studentLanguageRepository
            .findByStudentAndLanguageNameIgnoreCase(student, "Inglês")
            .orElseThrow()
            .getScore();
        assertEquals(140, scoreAfterFirst);

        // Segunda conclusao do mesmo modulo nao deve somar XP novamente
        mockMvc.perform(post("/api/learning-path/modules/" + module.getId() + "/complete")
                .param("scorePercent", "100.0")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
            .andExpect(status().isOk());

        int scoreAfterSecond = studentLanguageRepository
            .findByStudentAndLanguageNameIgnoreCase(student, "Inglês")
            .orElseThrow()
            .getScore();
        assertEquals(140, scoreAfterSecond);
    }

    @Test
    @DisplayName("SRS aceita rating 0..5, rejeita rating 6 com 400, e rotas inexistentes retornam 404")
    void srsRatingValidationAnd404HandlerWorkProperly() throws Exception {
        String sfx = uniq();
        Student student = createStudent(sfx, null, true);
        SpacedReviewItem item = spacedReviewItemRepository.saveAndFlush(SpacedReviewItem.builder()
            .student(student)
            .languageName("Inglês")
            .term("Resilient_" + sfx)
            .translation("Resiliente")
            .dueDate(LocalDate.now())
            .build());

        String studentToken = jwtService.generateToken(student, "STUDENT");

        mockMvc.perform(post("/api/srs/review/" + item.getId())
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + studentToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"rating\":5}"))
            .andExpect(status().isOk());

        mockMvc.perform(post("/api/srs/review/" + item.getId())
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + studentToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"rating\":6}"))
            .andExpect(status().isBadRequest());

        Teacher admin = createTeacher("adm_" + sfx, createInstitution("404_" + sfx), true, false, true);
        String adminToken = jwtService.generateToken(admin, "ADMIN");
        mockMvc.perform(get("/api/rota-inexistente-auditoria")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken))
            .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("StudentStreakService protege XP negativo e meta diaria fora de 5..500; DTO de idioma aceita score String")
    void streakGuardsAndStudentLanguageDtoDeserialization() throws Exception {
        String sfx = uniq();
        Student student = createStudent(sfx, null, true);

        studentStreakService.recordActivity(student.getId(), -25);
        Student reloaded = studentRepository.findById(student.getId()).orElseThrow();
        assertTrue(reloaded.getTodayXpEarned() == null || reloaded.getTodayXpEarned() >= 0);

        assertThrows(Exception.class, () -> studentStreakService.setDailyGoal(student.getId(), 4));
        assertThrows(Exception.class, () -> studentStreakService.setDailyGoal(student.getId(), 501));

        StudentLanguageRequestDTO dto = objectMapper.readValue(
            "{\"languageName\":\"Espanhol\",\"proficiencyLevel\":\"Intermediário\",\"score\":\"120\"}",
            StudentLanguageRequestDTO.class
        );
        assertEquals("Espanhol", dto.languageName());
        assertEquals("Intermediário", dto.level());
        assertEquals(120, dto.score());
    }

    @Test
    @DisplayName("Estudante so ve/ingressa em turmas dos idiomas escolhidos e plataforma bloqueia idiomas fora dos 6 oficiais")
    void strictSixLanguagesAndStudentClassLanguageMatchingEnforced() throws Exception {
        String sfx = uniq();
        Institution inst = createInstitution("LC_" + sfx);
        School school = schoolRepository.saveAndFlush(School.builder().name("Esc LC " + sfx).institution(inst).build());

        SchoolClass spanishClass = schoolClassRepository.saveAndFlush(SchoolClass.builder()
            .name("Turma Espanhol " + sfx)
            .languageName("Espanhol")
            .school(school)
            .institution(inst)
            .active(true)
            .build());

        SchoolClass japaneseClass = schoolClassRepository.saveAndFlush(SchoolClass.builder()
            .name("Turma Japones " + sfx)
            .languageName("Japonês")
            .school(school)
            .institution(inst)
            .active(true)
            .build());

        Student student = createStudent("lc_" + sfx, inst, true);
        studentLanguageRepository.saveAndFlush(StudentLanguage.builder()
            .student(student)
            .languageName("Espanhol")
            .level("Básico")
            .score(0)
            .build());

        String studentToken = jwtService.generateToken(student, "STUDENT");

        // Por padrão /available-classes mostra apenas turmas compatíveis com os idiomas do aluno
        mockMvc.perform(get("/api/students/me/available-classes")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + studentToken))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[*].name", hasItem(spanishClass.getName())))
            .andExpect(jsonPath("$[*].name", not(hasItem(japaneseClass.getName()))));

        // Com includeIneligible=true mostra ambas (para o app exibir aviso/cadeado visual)
        mockMvc.perform(get("/api/students/me/available-classes")
                .param("includeIneligible", "true")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + studentToken))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[*].name", hasItem(spanishClass.getName())))
            .andExpect(jsonPath("$[*].name", hasItem(japaneseClass.getName())));

        // Tentar ingressar em turma de Japonês sem ter Japonês no perfil é bloqueado (409 BusinessException)
        mockMvc.perform(post("/api/students/me/enrollments")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + studentToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of("schoolClassId", japaneseClass.getId()))))
            .andExpect(status().isConflict());

        // Ingressar em turma de Espanhol compatível funciona normalmente (201)
        mockMvc.perform(post("/api/students/me/enrollments")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + studentToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of("schoolClassId", spanishClass.getId()))))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.schoolClassLanguage").value("Espanhol"));

        // Tentar cadastrar estudante com idioma inexistente (ex: Esperanto) falha com erro de negócio (409)
        mockMvc.perform(post("/api/students")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of(
                    "name", "Aluno Invalido",
                    "email", "inv_" + sfx + "@audit.test",
                    "password", "SenhaForte@123",
                    "languages", List.of(Map.of("languageName", "Esperanto", "level", "Básico"))
                ))))
            .andExpect(status().isConflict());
    }

    @Test
    @DisplayName("Endpoints de matriculas, turmas, atribuicoes docentes e documentos nao falham com LazyInitializationException ou SQL bytea quando open-in-view=false")
    void noLazyInitializationOrByteaSearchFailuresAcrossControllers() throws Exception {
        String sfx = uniq();
        Institution inst = createInstitution("LAZY_" + sfx);
        School school = schoolRepository.saveAndFlush(School.builder()
            .name("Escola Lazy " + sfx)
            .institution(inst)
            .build());
        SchoolClass schoolClass = schoolClassRepository.saveAndFlush(SchoolClass.builder()
            .name("Turma Inglês " + sfx)
            .languageName("Inglês")
            .school(school)
            .institution(null) // proposital: força fallback para school.getInstitution()
            .active(true)
            .build());
        Teacher teacher = createTeacher("lazy_" + sfx, inst, true, true, true);
        Student student = createStudent("lazy_" + sfx, null, true); // institution null força lookup por enrollments
        studentLanguageRepository.saveAndFlush(StudentLanguage.builder()
            .student(student)
            .languageName("Inglês")
            .level("Básico")
            .score(0)
            .build());

        Enrollment enrollment = enrollmentRepository.saveAndFlush(Enrollment.builder()
            .student(student)
            .schoolClass(schoolClass)
            .enrollmentDate(LocalDate.now())
            .status(EnrollmentStatus.ACTIVE)
            .notes("Matrícula de teste")
            .build());

        ClassTeacherAssignment assignment = classTeacherAssignmentRepository.saveAndFlush(ClassTeacherAssignment.builder()
            .schoolClass(schoolClass)
            .teacher(teacher)
            .startDate(LocalDate.now())
            .reason("Titular")
            .build());

        TeacherDocument doc = new TeacherDocument();
        doc.setTeacher(teacher);
        doc.setFileName("certificado_ingles_" + sfx + ".pdf");
        doc.setContentType("application/pdf");
        doc.setFileSize(4L);
        doc.setFileData(new byte[] {1, 2, 3, 4});
        teacherDocumentRepository.saveAndFlush(doc);

        String studentToken = jwtService.generateToken(student, "STUDENT");
        String teacherToken = jwtService.generateToken(teacher, "TEACHER");
        String adminToken = jwtService.generateToken(teacher, "ADMIN");

        mockMvc.perform(get("/api/students/" + student.getId() + "/enrollments")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + studentToken))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].id").value(enrollment.getId()))
            .andExpect(jsonPath("$[0].studentName").value(student.getName()))
            .andExpect(jsonPath("$[0].schoolClassName").value(schoolClass.getName()));

        mockMvc.perform(get("/api/students/" + student.getId() + "/enrollments/active")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + studentToken))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].schoolClassLanguage").value("Inglês"));

        mockMvc.perform(get("/api/enrollments/student/" + student.getId())
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + teacherToken))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].studentName").value(student.getName()));

        mockMvc.perform(get("/api/enrollments/class/" + schoolClass.getId() + "/active")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + teacherToken))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].schoolClassName").value(schoolClass.getName()));

        mockMvc.perform(get("/api/classes")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + studentToken))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[*].name", hasItem(schoolClass.getName())));

        mockMvc.perform(get("/api/classes/" + schoolClass.getId())
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + teacherToken))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.name").value(schoolClass.getName()));

        mockMvc.perform(get("/api/classes/" + schoolClass.getId() + "/dashboard")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + teacherToken))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.name").value(schoolClass.getName()));

        mockMvc.perform(get("/api/teachers/" + teacher.getId() + "/assignments")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + teacherToken))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].id").value(assignment.getId()))
            .andExpect(jsonPath("$[0].schoolClassName").value(schoolClass.getName()));

        mockMvc.perform(get("/api/class-teacher-assignments/class/" + schoolClass.getId() + "/current")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + teacherToken))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.teacherName").value(teacher.getName()));

        // /api/admin/documents sem e com search (evita lower(bytea) no PostgreSQL)
        mockMvc.perform(get("/api/admin/documents")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.content[*].fileName", hasItem(doc.getFileName())));

        mockMvc.perform(get("/api/admin/documents")
                .param("search", "certificado_ingles_" + sfx)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.content[*].fileName", hasItem(doc.getFileName())));
    }

    @Test
    @DisplayName("Utf8MojibakeSanitizer recupera strings duplamente codificadas sem alterar acentos validos")
    void utf8MojibakeSanitizerRepairsAndPreservesValidText() {
        assertEquals("Turma de Inglês Básico A1", Utf8MojibakeSanitizer.repairIfMojibake("Turma de InglÃªs BÃ¡sico A1"));
        assertEquals("Español Práctico: En el Restaurante", Utf8MojibakeSanitizer.repairIfMojibake("EspaÃ±ol PrÃ¡ctico: En el Restaurante"));
        assertEquals("A prática diária leva à fluência.", Utf8MojibakeSanitizer.repairIfMojibake("A prÃ¡tica diÃ¡ria leva Ã\u00a0 fluÃªncia."));
        assertEquals("Conquista / Realização", Utf8MojibakeSanitizer.repairIfMojibake("Conquista / RealizaÃ§Ã£o"));

        // Textos já válidos em UTF-8 permanecem idênticos
        assertEquals("MANHÃ", Utf8MojibakeSanitizer.repairIfMojibake("MANHÃ"));
        assertEquals("Inglês", Utf8MojibakeSanitizer.repairIfMojibake("Inglês"));
        assertEquals("Avançado", Utf8MojibakeSanitizer.repairIfMojibake("Avançado"));
        assertEquals("Professor Responsável", Utf8MojibakeSanitizer.repairIfMojibake("Professor Responsável"));
    }
}

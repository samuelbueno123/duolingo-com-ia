package com.duolinfo.ia.proj.config;

import java.util.List;

import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.duolinfo.ia.proj.entity.Achievement;
import com.duolinfo.ia.proj.entity.CatalogOption;
import com.duolinfo.ia.proj.entity.Institution;
import com.duolinfo.ia.proj.entity.User;
import com.duolinfo.ia.proj.repository.AchievementRepository;
import com.duolinfo.ia.proj.repository.CatalogOptionRepository;
import com.duolinfo.ia.proj.repository.InstitutionRepository;
import com.duolinfo.ia.proj.repository.UserRepository;
import com.duolinfo.ia.proj.service.InstitutionService;

@Configuration
public class CatalogDataInitializer {

    @Bean
    ApplicationRunner initializeReferenceCatalog(
            CatalogOptionRepository repository,
            InstitutionRepository institutionRepository,
            UserRepository userRepository,
            AchievementRepository achievementRepository,
            PasswordEncoder passwordEncoder,
            JdbcTemplate jdbcTemplate) {
        return args -> {
            Institution defaultInst = ensureDefaultInstitution(institutionRepository);
            ensureDefaultAdmin(userRepository, passwordEncoder);
            addMissing(repository, "language", List.of(
                "Alemão",
                "Espanhol",
                "Francês",
                "Inglês",
                "Italiano",
                "Japonês"
            ));
            syncProficiencyLevels(repository);
            seedDefaultAchievements(achievementRepository);
            ensureIndexesAndBackfill(jdbcTemplate, defaultInst.getId());
        };
    }

    private static void syncProficiencyLevels(CatalogOptionRepository repository) {
        List<String> allowed = List.of("Fácil", "Médio", "Difícil");
        List<CatalogOption> existing = repository.findByCatalogType("proficiency");
        for (CatalogOption opt : existing) {
            if (!allowed.contains(opt.getValue())) {
                repository.delete(opt);
            }
        }
        addMissing(repository, "proficiency", allowed);
    }

    private static void seedDefaultAchievements(AchievementRepository repository) {
        List<Achievement> defaults = List.of(
            Achievement.builder()
                .code("FIRST_SUBMISSION")
                .title("Primeiro Exercício")
                .description("Conclua sua primeira atividade na plataforma.")
                .category("Progresso")
                .iconName("flag_rounded")
                .displayOrder(1)
                .build(),
            Achievement.builder()
                .code("FIVE_SUBMISSIONS")
                .title("Estudante Dedicado")
                .description("Conclua 5 exercícios enviados por professores.")
                .category("Progresso")
                .iconName("local_fire_department_rounded")
                .displayOrder(2)
                .build(),
            Achievement.builder()
                .code("TEN_SUBMISSIONS")
                .title("Maratonista de Idiomas")
                .description("Conclua 10 exercícios com foco nos estudos.")
                .category("Progresso")
                .iconName("directions_run_rounded")
                .displayOrder(3)
                .build(),
            Achievement.builder()
                .code("PERFECT_SCORE")
                .title("Pontuação Perfeita")
                .description("Gabarite um exercício acertando 100% das questões.")
                .category("Excelência")
                .iconName("workspace_premium_rounded")
                .displayOrder(4)
                .build(),
            Achievement.builder()
                .code("FIRST_100_XP")
                .title("Centurião de XP")
                .description("Acumule pelo menos 100 XP somando seus idiomas.")
                .category("XP")
                .iconName("bolt_rounded")
                .displayOrder(5)
                .build(),
            Achievement.builder()
                .code("FIRST_500_XP")
                .title("Mestre do XP")
                .description("Acumule pelo menos 500 XP na plataforma.")
                .category("XP")
                .iconName("military_tech_rounded")
                .displayOrder(6)
                .build(),
            Achievement.builder()
                .code("POLYGLOT")
                .title("Poliglota em Formação")
                .description("Estude 2 ou mais idiomas simultaneamente.")
                .category("Idiomas")
                .iconName("translate_rounded")
                .displayOrder(7)
                .build(),
            Achievement.builder()
                .code("CLASS_MEMBER")
                .title("Membro da Turma")
                .description("Esteja matriculado ativamente em uma turma.")
                .category("Comunidade")
                .iconName("groups_rounded")
                .displayOrder(8)
                .build(),
            Achievement.builder()
                .code("TEACHER_FEEDBACK")
                .title("Feedback Recebido")
                .description("Receba uma avaliação ou feedback de um professor.")
                .category("Comunidade")
                .iconName("rate_review_rounded")
                .displayOrder(9)
                .build(),
            Achievement.builder()
                .code("HONOR_STUDENT")
                .title("Destaque Acadêmico")
                .description("Conquista especial concedida pelo desempenho exemplar.")
                .category("Especial")
                .iconName("emoji_events_rounded")
                .displayOrder(10)
                .build()
        );

        for (Achievement item : defaults) {
            if (!repository.existsByCode(item.getCode())) {
                repository.save(item);
            }
        }
    }

    private static void ensureIndexesAndBackfill(JdbcTemplate jdbcTemplate, Long defaultInstitutionId) {
        try {
            jdbcTemplate.execute(
                "UPDATE classes c SET institution_id = s.institution_id "
                + "FROM schools s WHERE c.school_id = s.id AND c.institution_id IS NULL");
            if (defaultInstitutionId != null) {
                jdbcTemplate.update(
                    "UPDATE students SET institution_id = ? WHERE institution_id IS NULL",
                    defaultInstitutionId);
            }
            jdbcTemplate.execute(
                "UPDATE teachers SET approval_status = CASE WHEN approved = true THEN 'APPROVED' ELSE 'PENDING' END "
                + "WHERE approval_status IS NULL");
            jdbcTemplate.execute(
                "CREATE INDEX IF NOT EXISTS idx_instituicao_turma ON classes (institution_id, school_id)");
            jdbcTemplate.execute(
                "CREATE INDEX IF NOT EXISTS idx_school_institution ON schools (institution_id)");
            jdbcTemplate.execute(
                "CREATE INDEX IF NOT EXISTS idx_instituicao_aluno ON students (institution_id)");
            jdbcTemplate.execute(
                "CREATE INDEX IF NOT EXISTS idx_instituicao_professor ON teachers (institution_id, approved)");
            jdbcTemplate.execute(
                "CREATE INDEX IF NOT EXISTS idx_instituicao_administradores ON teachers (institution_id, institution_admin, primary_institution_admin)");
            jdbcTemplate.execute(
                "CREATE INDEX IF NOT EXISTS idx_professores_exercicios ON learning_exercises (teacher_id, school_class_id, language_name)");
            jdbcTemplate.execute(
                "CREATE INDEX IF NOT EXISTS idx_idaluno_conquista ON student_achievements (student_id, achievement_id)");
        } catch (Exception ignored) {
            // Indexes or tables will be created by Hibernate DDL and retried on next startup if needed
        }
    }

    private static void ensureDefaultAdmin(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder) {
        User admin = userRepository.findByEmailIgnoreCase("admin@duolingo.com")
            .orElseGet(User::new);
        admin.setName("Administrador");
        admin.setEmail("admin@duolingo.com");
        admin.setAdmin(true);
        if (admin.getGender() == null || admin.getGender().isBlank()) {
            admin.setGender("MASCULINO");
        }
        if (admin.getPasswordHash() == null
                || admin.getPasswordHash().isBlank()
                || !passwordEncoder.matches("duadmin", admin.getPasswordHash())) {
            admin.setPasswordHash(passwordEncoder.encode("duadmin"));
        }
        userRepository.save(admin);
    }

    private static Institution ensureDefaultInstitution(InstitutionRepository repository) {
        return repository.findByCode(InstitutionService.DEFAULT_INSTITUTION_CODE)
            .orElseGet(() -> repository.save(Institution.builder()
                .name("CEFET/RJ - Campus Nova Iguaçu")
                .code(InstitutionService.DEFAULT_INSTITUTION_CODE)
                .email("cefet.novaiguacu@duolinfo.local")
                .city("Nova Iguaçu")
                .state("RJ")
                .country("Brasil")
                .active(true)
                .build()));
    }

    private static void addMissing(
            CatalogOptionRepository repository,
            String type,
            List<String> values) {
        values.stream()
            .filter(value -> !repository.existsByCatalogTypeAndValue(type, value))
            .map(value -> new CatalogOption(type, value))
            .forEach(repository::save);
    }
}


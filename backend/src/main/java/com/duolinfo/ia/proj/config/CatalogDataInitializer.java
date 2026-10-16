package com.duolinfo.ia.proj.config;

import java.util.List;

import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.duolinfo.ia.proj.entity.CatalogOption;
import com.duolinfo.ia.proj.entity.Institution;
import com.duolinfo.ia.proj.repository.CatalogOptionRepository;
import com.duolinfo.ia.proj.repository.InstitutionRepository;
import com.duolinfo.ia.proj.service.InstitutionService;

@Configuration
public class CatalogDataInitializer {

    @Bean
    ApplicationRunner initializeReferenceCatalog(
            CatalogOptionRepository repository,
            InstitutionRepository institutionRepository) {
        return args -> {
            ensureDefaultInstitution(institutionRepository);
            addMissing(repository, "language", List.of(
                "Alemão",
                "Espanhol",
                "Francês",
                "Inglês",
                "Italiano",
                "Japonês"
            ));
            addMissing(repository, "proficiency", List.of(
                "Iniciante",
                "Básico",
                "Intermediário",
                "Avançado",
                "Fluente"
            ));
        };
    }

    private static void ensureDefaultInstitution(InstitutionRepository repository) {
        if (repository.existsByCode(InstitutionService.DEFAULT_INSTITUTION_CODE)) {
            return;
        }

        repository.save(Institution.builder()
            .name("CEFET/RJ - Campus Nova Iguaçu")
            .code(InstitutionService.DEFAULT_INSTITUTION_CODE)
            .email("cefet.novaiguacu@duolinfo.local")
            .city("Nova Iguaçu")
            .state("RJ")
            .country("Brasil")
            .active(true)
            .build());
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

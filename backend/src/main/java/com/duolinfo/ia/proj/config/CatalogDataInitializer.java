package com.duolinfo.ia.proj.config;

import java.util.List;

import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.duolinfo.ia.proj.entity.CatalogOption;
import com.duolinfo.ia.proj.repository.CatalogOptionRepository;

@Configuration
public class CatalogDataInitializer {

    @Bean
    ApplicationRunner initializeReferenceCatalog(
            CatalogOptionRepository repository) {
        return args -> {
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

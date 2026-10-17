package com.duolinfo.ia.proj.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.duolinfo.ia.proj.entity.CatalogOption;

public interface CatalogOptionRepository
        extends JpaRepository<CatalogOption, Long> {

    List<CatalogOption> findByCatalogTypeAndActiveTrueOrderByValueAsc(
        String catalogType
    );

    boolean existsByCatalogTypeAndValue(String catalogType, String value);

    List<CatalogOption> findByCatalogType(String catalogType);
}

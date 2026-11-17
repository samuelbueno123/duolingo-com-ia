package com.duolinfo.ia.proj.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.duolinfo.ia.proj.entity.School;

@Repository
public interface SchoolRepository extends JpaRepository<School, Long> {

    @Override
    @EntityGraph(attributePaths = {"institution"})
    List<School> findAll();

    @Override
    @EntityGraph(attributePaths = {"institution"})
    Optional<School> findById(Long id);

    @EntityGraph(attributePaths = {"institution"})
    Optional<School> findByInepCode(String inepCode);

    boolean existsByInepCode(String inepCode);

    @EntityGraph(attributePaths = {"institution"})
    List<School> findByNameContainingIgnoreCase(String name);

    @EntityGraph(attributePaths = {"institution"})
    List<School> findByInstitutionId(Long institutionId);

    @EntityGraph(attributePaths = {"institution"})
    List<School> findByInstitutionIdAndNameContainingIgnoreCase(
        Long institutionId,
        String name
    );

    @EntityGraph(attributePaths = {"institution"})
    List<School> findByInstitutionIdAndInepCode(
        Long institutionId,
        String inepCode
    );
}
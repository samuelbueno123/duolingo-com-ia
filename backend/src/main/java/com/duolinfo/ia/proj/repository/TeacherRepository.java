package com.duolinfo.ia.proj.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.duolinfo.ia.proj.entity.Teacher;

@Repository
public interface TeacherRepository extends JpaRepository<Teacher, Long> {

    Optional<Teacher> findByGoogleId(String googleId);

    Optional<Teacher> findByEmail(String email);

    List<Teacher> findByNameContainingIgnoreCase(String name);
    boolean existsByNameIgnoreCase(String name);

    Optional<Teacher> findByInstitutionIdAndEmail(
        Long institutionId,
        String email
    );

    List<Teacher> findByInstitutionId(Long institutionId);

    List<Teacher> findByInstitutionIdAndTaughtLanguagesContaining(
        Long institutionId,
        String language
    );

    List<Teacher> findByInstitutionIdAndSpecializationAreasContaining(
        Long institutionId,
        String specializationArea
    );

    List<Teacher> findByInstitution_NameContainingIgnoreCase(
        String institutionName
    );
}
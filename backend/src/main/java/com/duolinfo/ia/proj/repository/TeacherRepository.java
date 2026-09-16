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

    List<Teacher> findByInstitutionContainingIgnoreCase(String institution);

    List<Teacher> findByTaughtLanguagesContainingIgnoreCase(String language);

    List<Teacher> findBySpecializationAreasContainingIgnoreCase(String specializationArea);
}

package com.duolinfo.ia.proj.repository;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.duolinfo.ia.proj.entity.TeacherDocument;

@Repository
public interface TeacherDocumentRepository extends JpaRepository<TeacherDocument, Long> {

    List<TeacherDocument> findByTeacherId(Long teacherId);

    @Query(
        value = "SELECT d FROM TeacherDocument d JOIN FETCH d.teacher t LEFT JOIN FETCH t.institution i " +
                "WHERE (:institutionId IS NULL OR i.id = :institutionId) " +
                "AND (:search IS NULL OR LOWER(d.fileName) LIKE LOWER(CONCAT('%', :search, '%')) " +
                "OR LOWER(t.name) LIKE LOWER(CONCAT('%', :search, '%')) " +
                "OR LOWER(t.email) LIKE LOWER(CONCAT('%', :search, '%')))",
        countQuery = "SELECT COUNT(d) FROM TeacherDocument d JOIN d.teacher t LEFT JOIN t.institution i " +
                     "WHERE (:institutionId IS NULL OR i.id = :institutionId) " +
                     "AND (:search IS NULL OR LOWER(d.fileName) LIKE LOWER(CONCAT('%', :search, '%')) " +
                     "OR LOWER(t.name) LIKE LOWER(CONCAT('%', :search, '%')) " +
                     "OR LOWER(t.email) LIKE LOWER(CONCAT('%', :search, '%')))"
    )
    Page<TeacherDocument> searchDocuments(
        @Param("institutionId") Long institutionId,
        @Param("search") String search,
        Pageable pageable
    );
}

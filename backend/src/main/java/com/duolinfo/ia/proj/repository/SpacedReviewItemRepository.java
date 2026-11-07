package com.duolinfo.ia.proj.repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.duolinfo.ia.proj.entity.SpacedReviewItem;

public interface SpacedReviewItemRepository extends JpaRepository<SpacedReviewItem, Long> {

    List<SpacedReviewItem> findByStudent_IdOrderByDueDateAsc(Long studentId);

    List<SpacedReviewItem> findByStudent_IdAndLanguageNameIgnoreCaseOrderByDueDateAsc(
        Long studentId, String languageName);

    @Query("""
        SELECT i FROM SpacedReviewItem i
        WHERE i.student.id = :studentId
          AND i.dueDate <= :date
        ORDER BY i.dueDate ASC, i.easeFactor ASC
    """)
    List<SpacedReviewItem> findDueItems(
        @Param("studentId") Long studentId,
        @Param("date") LocalDate date
    );

    @Query("""
        SELECT i FROM SpacedReviewItem i
        WHERE i.student.id = :studentId
          AND LOWER(i.languageName) = LOWER(:languageName)
          AND i.dueDate <= :date
        ORDER BY i.dueDate ASC, i.easeFactor ASC
    """)
    List<SpacedReviewItem> findDueItemsByLanguage(
        @Param("studentId") Long studentId,
        @Param("languageName") String languageName,
        @Param("date") LocalDate date
    );

    Optional<SpacedReviewItem> findByIdAndStudent_Id(Long id, Long studentId);

    long countByStudent_IdAndDueDateLessThanEqual(Long studentId, LocalDate date);

    long countByStudent_Id(Long studentId);
}

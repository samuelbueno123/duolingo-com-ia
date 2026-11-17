package com.duolinfo.ia.proj.repository;

import java.util.List;
import java.util.Locale;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.duolinfo.ia.proj.entity.TeacherDocument;

@Repository
public interface TeacherDocumentRepository extends JpaRepository<TeacherDocument, Long> {

    @Override
    @EntityGraph(attributePaths = {"teacher", "teacher.institution"})
    Optional<TeacherDocument> findById(Long id);

    List<TeacherDocument> findByTeacherId(Long teacherId);

    @Query(
        value = "SELECT d FROM TeacherDocument d JOIN FETCH d.teacher t LEFT JOIN FETCH t.institution i",
        countQuery = "SELECT COUNT(d) FROM TeacherDocument d JOIN d.teacher t LEFT JOIN t.institution i"
    )
    Page<TeacherDocument> findAllWithTeacher(Pageable pageable);

    @Query(
        value = "SELECT d FROM TeacherDocument d JOIN FETCH d.teacher t JOIN FETCH t.institution i " +
                "WHERE i.id = :institutionId",
        countQuery = "SELECT COUNT(d) FROM TeacherDocument d JOIN d.teacher t JOIN t.institution i " +
                     "WHERE i.id = :institutionId"
    )
    Page<TeacherDocument> findAllByInstitutionId(
        @Param("institutionId") Long institutionId,
        Pageable pageable
    );

    @Query(
        value = "SELECT d FROM TeacherDocument d JOIN FETCH d.teacher t LEFT JOIN FETCH t.institution i " +
                "WHERE LOWER(d.fileName) LIKE :pattern " +
                "OR LOWER(t.name) LIKE :pattern " +
                "OR LOWER(t.email) LIKE :pattern",
        countQuery = "SELECT COUNT(d) FROM TeacherDocument d JOIN d.teacher t LEFT JOIN t.institution i " +
                     "WHERE LOWER(d.fileName) LIKE :pattern " +
                     "OR LOWER(t.name) LIKE :pattern " +
                     "OR LOWER(t.email) LIKE :pattern"
    )
    Page<TeacherDocument> searchAllByPattern(
        @Param("pattern") String pattern,
        Pageable pageable
    );

    @Query(
        value = "SELECT d FROM TeacherDocument d JOIN FETCH d.teacher t JOIN FETCH t.institution i " +
                "WHERE i.id = :institutionId " +
                "AND (LOWER(d.fileName) LIKE :pattern " +
                "OR LOWER(t.name) LIKE :pattern " +
                "OR LOWER(t.email) LIKE :pattern)",
        countQuery = "SELECT COUNT(d) FROM TeacherDocument d JOIN d.teacher t JOIN t.institution i " +
                     "WHERE i.id = :institutionId " +
                     "AND (LOWER(d.fileName) LIKE :pattern " +
                     "OR LOWER(t.name) LIKE :pattern " +
                     "OR LOWER(t.email) LIKE :pattern)"
    )
    Page<TeacherDocument> searchByInstitutionAndPattern(
        @Param("institutionId") Long institutionId,
        @Param("pattern") String pattern,
        Pageable pageable
    );

    default Page<TeacherDocument> searchDocuments(
            Long institutionId,
            String search,
            Pageable pageable) {
        boolean hasSearch = search != null && !search.isBlank();
        if (!hasSearch) {
            return institutionId == null
                ? findAllWithTeacher(pageable)
                : findAllByInstitutionId(institutionId, pageable);
        }
        String pattern = "%" + search.trim().toLowerCase(Locale.ROOT) + "%";
        return institutionId == null
            ? searchAllByPattern(pattern, pageable)
            : searchByInstitutionAndPattern(institutionId, pattern, pageable);
    }
}

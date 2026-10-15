package com.duolinfo.ia.proj.repository;

import com.duolinfo.ia.proj.JDBC.DataBaseConnection;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.stereotype.Repository;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

@Repository
public class RankSqlRepository {

    private static final String STUDENT_SCORE_QUERY = """
            SELECT s.id AS student_id,
                   u.name AS student_name,
                   COALESCE(SUM(sl.score), 0)::BIGINT AS total_score
            FROM students s
            JOIN users u ON u.id = s.id
            LEFT JOIN student_languages sl ON sl.student_id = s.id
            GROUP BY s.id, u.name
            """;

    private static final String LANGUAGE_SCORE_QUERY = """
            SELECT s.id AS student_id,
                   u.name AS student_name,
                   SUM(sl.score)::BIGINT AS total_score
            FROM student_languages sl
            JOIN students s ON s.id = sl.student_id
            JOIN users u ON u.id = s.id
            WHERE LOWER(sl.language_name) = LOWER(?)
            GROUP BY s.id, u.name
            """;

    private static final String CLASS_SCORE_QUERY = """
            SELECT s.id AS student_id,
                   u.name AS student_name,
                   COALESCE(SUM(sl.score), 0)::BIGINT AS total_score
            FROM (
                SELECT DISTINCT e.student_id
                FROM enrollments e
                WHERE e.class_id = ?
                  AND e.status = 'ACTIVE'
            ) enrolled_students
            JOIN students s ON s.id = enrolled_students.student_id
            JOIN users u ON u.id = s.id
            LEFT JOIN student_languages sl ON sl.student_id = s.id
            GROUP BY s.id, u.name
            """;

    private final DataBaseConnection dataBaseConnection;

    public RankSqlRepository(DataBaseConnection dataBaseConnection) {
        this.dataBaseConnection = dataBaseConnection;
    }

    public List<RankRow> findStudentRanking(
            boolean ascending,
            int start,
            int endExclusive) {
        return findRanking(STUDENT_SCORE_QUERY, null, ascending, start, endExclusive);
    }

    public List<RankRow> findLanguageRanking(
            String language,
            boolean ascending,
            int start,
            int endExclusive) {
        return findRanking(LANGUAGE_SCORE_QUERY, language, ascending, start, endExclusive);
    }

    public List<RankRow> findClassRanking(
            Long classId,
            boolean ascending,
            int start,
            int endExclusive) {
        return findRanking(CLASS_SCORE_QUERY, classId, ascending, start, endExclusive);
    }

    private List<RankRow> findRanking(
            String scoreQuery,
            Object filter,
            boolean ascending,
            int start,
            int endExclusive) {
        String direction = ascending ? "ASC" : "DESC";
        String sql = """
                WITH student_scores AS (
                %s
                ), ranked_students AS (
                    SELECT ROW_NUMBER() OVER (
                               ORDER BY total_score %s,
                                        LOWER(student_name) ASC,
                                        student_id ASC
                           ) - 1 AS ranking_position,
                           student_id,
                           student_name,
                           total_score
                    FROM student_scores
                )
                SELECT ranking_position, student_id, student_name, total_score
                FROM ranked_students
                WHERE ranking_position >= ?
                  AND ranking_position < ?
                ORDER BY ranking_position
                """.formatted(scoreQuery, direction);

        try (Connection connection = dataBaseConnection.connection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            int parameterIndex = 1;
            if (filter instanceof String language) {
                statement.setString(parameterIndex++, language.trim());
            } else if (filter instanceof Long classId) {
                statement.setLong(parameterIndex++, classId);
            }

            statement.setInt(parameterIndex++, start);
            statement.setInt(parameterIndex, endExclusive);

            try (ResultSet result = statement.executeQuery()) {
                List<RankRow> rows = new ArrayList<>();
                while (result.next()) {
                    rows.add(new RankRow(
                            result.getLong("ranking_position"),
                            result.getLong("student_id"),
                            result.getString("student_name"),
                            result.getLong("total_score")
                    ));
                }
                return rows;
            }
        } catch (SQLException exception) {
            throw new DataAccessResourceFailureException(
                    "Não foi possível consultar o ranking de pontuação.",
                    exception
            );
        }
    }

    public record RankRow(
            Long position,
            Long studentId,
            String studentName,
            Long score) {
    }
}



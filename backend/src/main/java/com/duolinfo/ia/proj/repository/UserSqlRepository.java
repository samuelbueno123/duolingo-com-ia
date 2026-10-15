package com.duolinfo.ia.proj.repository;

import com.duolinfo.ia.proj.JDBC.DataBaseConnection;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.stereotype.Repository;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Optional;

@Repository
public class UserSqlRepository {
    private final DataBaseConnection dataBaseConnection;

    public UserSqlRepository(DataBaseConnection dataBaseConnection) {
        this.dataBaseConnection = dataBaseConnection;
    }

    public Optional<UserRow> findByEmail(String email) {
        String sql = """
                SELECT id, name, email
                FROM users
                WHERE email = ?
                """;

        try (Connection connection = dataBaseConnection.connection();
            PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, email);

            try (ResultSet result = statement.executeQuery()) {
                if (!result.next()) {
                    return Optional.empty();
                }

                UserRow user = new UserRow(
                        result.getLong("id"),
                        result.getString("name"),
                        result.getString("email")
                );
                return Optional.of(user);
            }
        } catch (SQLException exception) {
            throw new DataAccessResourceFailureException(
                    "Não foi possível consultar a tabela users.",
                    exception
            );
        }
    }

    public record UserRow(Long id, String name, String email) {
    }
}

package com.duolinfo.ia.proj.JDBC;

import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;

@Component
public class DataBaseConnection {
    private final DataSource dataSource;

    public DataBaseConnection(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    public Connection connection() throws SQLException {
        return dataSource.getConnection();
    }
}

package model;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

public class DatabaseConnection {

    // SQLite database file
    private static final String URL = "jdbc:sqlite:Mydatabase.db";

    // so that we can connect to our database from other classes
    public static Connection connect() throws SQLException {
        Connection conn = DriverManager.getConnection(URL);

        // turning on foreign keys so that cascade delete works properly
        try (Statement stmt = conn.createStatement()) {
            stmt.execute("PRAGMA foreign_keys = ON;");
        }

        return conn;
    }
}
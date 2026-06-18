package com.textadventure.editor.db;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

public class DatabaseConnection {
    private static final String DB_URL = "jdbc:sqlite:./story_editor.db";
    private static Connection instance;

    private DatabaseConnection() {}

    public static Connection getInstance() throws SQLException {
        if (instance == null || instance.isClosed()) {
            instance = DriverManager.getConnection(DB_URL);
            initializeDatabase();
        }
        return instance;
    }

    private static void initializeDatabase() throws SQLException {
        try (Statement stmt = instance.createStatement()) {
            stmt.execute("CREATE TABLE IF NOT EXISTS story_nodes (" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    "parent_id INTEGER, " +
                    "title TEXT NOT NULL, " +
                    "content TEXT, " +
                    "node_type TEXT DEFAULT 'normal', " +
                    "position INTEGER DEFAULT 0, " +
                    "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP, " +
                    "updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP, " +
                    "FOREIGN KEY (parent_id) REFERENCES story_nodes(id) ON DELETE CASCADE)");

            stmt.execute("CREATE TABLE IF NOT EXISTS conditions (" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    "node_id INTEGER NOT NULL, " +
                    "condition_type TEXT DEFAULT 'prerequisite', " +
                    "target_node_id INTEGER, " +
                    "condition_key TEXT, " +
                    "condition_value TEXT, " +
                    "operator TEXT DEFAULT 'equals', " +
                    "description TEXT, " +
                    "FOREIGN KEY (node_id) REFERENCES story_nodes(id) ON DELETE CASCADE, " +
                    "FOREIGN KEY (target_node_id) REFERENCES story_nodes(id) ON DELETE SET NULL)");

            stmt.execute("PRAGMA foreign_keys = ON;");
        }
    }

    public static void close() throws SQLException {
        if (instance != null && !instance.isClosed()) {
            instance.close();
        }
    }
}

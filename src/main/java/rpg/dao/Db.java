package rpg.dao;

import java.sql.*;

public final class Db {
    // crée/ouvre un fichier SQLite à la racine du projet
    private static final String URL = "jdbc:sqlite:test.db";

    private Db() {}

    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(URL);
    }

    // à appeler une fois au démarrage
    public static void initSchema() throws SQLException {
        String sql = """
            CREATE TABLE IF NOT EXISTS character (
              name        TEXT PRIMARY KEY,         -- clé métier unique (tu peux passer à INTEGER + AUTOINCREMENT si tu veux)
              strength    INTEGER NOT NULL,
              agility     INTEGER NOT NULL,
              intelligence INTEGER NOT NULL,
              decorators  TEXT                      -- ex: "Invisibility,FireResistance"
            );
            """;
        try (Connection c = getConnection(); Statement st = c.createStatement()) {
            st.execute(sql);
        }
    }
}

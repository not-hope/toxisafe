package com.toxisafe.util;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.stream.Collectors;

/**
 * Gestiona la conexión SQLite y asegura que el esquema (schema.sql) se ejecute una vez.
 */
public final class DBConnection {

    // Crea el archivo toxisafe.db en el directorio de trabajo
    private static final String URL = "jdbc:sqlite:toxisafe.db";

    private static Connection singleton;
    private static volatile boolean initialized = false;

    private DBConnection() {}

    public static synchronized Connection getConnection() throws SQLException {
        if (singleton == null || singleton.isClosed()) {
            singleton = DriverManager.getConnection(URL);

            // ✅ Activar claves foráneas en SQLite
            try (Statement stmt = singleton.createStatement()) {
                stmt.execute("PRAGMA foreign_keys = ON");
            }
        }
        ensureInitialized();
        return singleton;
    }

    private static void ensureInitialized() {
        if (initialized) return;
        synchronized (DBConnection.class) {
            if (initialized) return;
            try (Statement st = singleton.createStatement()) {
                String sql = readSchemaSQL();
                // Ejecuta el schema completo (contiene múltiples CREATE TABLE IF NOT EXISTS)
                st.executeUpdate(sql);
                initialized = true;
                System.out.println("[DB] Esquema aplicado correctamente.");
                System.out.println("[DB] Ruta BD: " + new java.io.File("toxisafe.db").getAbsolutePath());
            } catch (Exception e) {
                System.err.println("[DB] Error aplicando schema.sql: " + e.getMessage());
                e.printStackTrace();
            }
        }
    }

    private static String readSchemaSQL() throws Exception {
        // Debe existir en: src/main/resources/com/toxisafe/db/schema.sql
        String resourcePath = "com/toxisafe/db/schema.sql";
        InputStream is = DBConnection.class.getClassLoader().getResourceAsStream(resourcePath);
        if (is == null) {
            throw new IllegalStateException("No se encontró " + resourcePath + " en resources.");
        }
        try (BufferedReader br = new BufferedReader(new InputStreamReader(is))) {
            return br.lines().collect(Collectors.joining("\n"));
        }
    }

    public static synchronized void close() {
        if (singleton != null) {
            try { singleton.close(); } catch (java.sql.SQLException ignore) {}
            singleton = null;
        }
    }
}

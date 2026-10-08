package com.szymc.voxel_engine;
import java.sql.*;

public class DatabaseManager {
    private final static String URL = "jdbc:sqlite:my_database.db";

    public static void initializeSchema() {
        String sql = """
                
                CREATE TABLE IF NOT EXISTS chunks (
                  x INTEGER,
                  z INTEGER,
                  data BLOB,
                  PRIMARY KEY (x, z)
                );
                """;

        try (Connection conn = DriverManager.getConnection(URL); Statement statement = conn.createStatement()) {
            statement.execute(sql);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public static void uploadChunk(int x, int z, byte[] blob) {
        String sql = "INSERT INTO chunks (x, z, data) VALUES (?, ?, ?) ON CONFLICT(x, z) DO UPDATE set data = excluded.data";

        try (Connection conn = DriverManager.getConnection(URL); PreparedStatement statement = conn.prepareStatement(sql)) {
            statement.setInt(1, x);
            statement.setInt(2, z);
            statement.setBytes(3, blob);

            statement.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public static byte[] selectChunk(int x, int z) {
        String sql = "SELECT data FROM chunks WHERE x = ? AND z = ?";

        try (Connection conn = DriverManager.getConnection(URL); PreparedStatement statement = conn.prepareStatement(sql)) {
            statement.setInt(1, x);
            statement.setInt(2, z);

            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return resultSet.getBytes("data");
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return null;
    }
}

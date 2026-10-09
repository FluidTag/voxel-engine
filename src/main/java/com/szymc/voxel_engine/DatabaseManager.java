package com.szymc.voxel_engine;
import java.sql.*;

public class DatabaseManager {
    private final static String URL = "jdbc:sqlite:my_database.db";

    public static void initializeSchema() {
        try (Connection conn = DriverManager.getConnection(URL); Statement statement = conn.createStatement()) {
           statement.execute("""
                CREATE TABLE IF NOT EXISTS worlds (
                    world_id INTEGER PRIMARY KEY AUTOINCREMENT,
                    world_name TEXT NOT NULL,
                    seed TEXT NOT NULL
                );
            """);

            statement.execute("""
                CREATE TABLE IF NOT EXISTS players (
                    world_id INTEGER,
                    player_id TEXT DEFAULT 'singleplayer',
                    cameraX REAL,
                    cameraY REAL,
                    cameraZ REAL,
                    cameraYaw REAL,
                    cameraPitch REAL,
                    inventory BLOB,
                    inventoryAmounts BLOB,
                    PRIMARY KEY (world_id, player_id),
                    FOREIGN KEY (world_id) REFERENCES worlds(world_id) ON DELETE CASCADE
                );
            """);

            statement.execute("""
                CREATE TABLE IF NOT EXISTS chunks (
                    world_id INTEGER,
                    x INTEGER,
                    z INTEGER,
                    data BLOB,
                    PRIMARY KEY (world_id, x, z),
                    FOREIGN KEY (world_id) REFERENCES worlds(world_id) ON DELETE CASCADE
                ) WITHOUT ROWID;
            """);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public static void uploadPlayer(int worldId, PlayerCharacter character) {
        String sql = "INSERT OR REPLACE INTO players (world_id, cameraX, cameraY, cameraZ, cameraYaw, cameraPitch, inventory, inventoryAmounts) VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
        Camera cam = character.getPlayerCamera();

        try (Connection conn = DriverManager.getConnection(URL); PreparedStatement statement = conn.prepareStatement(sql)) {
            statement.setInt(1, worldId);
            statement.setFloat(2, cam.cameraPos.x);
            statement.setFloat(3, cam.cameraPos.y);
            statement.setFloat(4, cam.cameraPos.z);

            statement.setFloat(5, cam.getYaw());
            statement.setFloat(6, cam.getPitch());
            statement.setBytes(7, character.getInventory());
            statement.setBytes(8, character.getInventoryAmounts());

            statement.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public static PlayerCharacter getPlayer(int worldId, World worldReference, Window windowReference, Engine engineReference, Camera useCamera) {
        String sql = "SELECT * FROM players WHERE world_id = ?";
        try (Connection conn = DriverManager.getConnection(URL); PreparedStatement statement = conn.prepareStatement(sql)) {
            statement.setInt(1, worldId);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    useCamera.cameraPos.set(resultSet.getFloat("cameraX"), resultSet.getFloat("cameraY"), resultSet.getFloat("cameraZ"));
                    useCamera.setOrientation(resultSet.getFloat("cameraYaw"), resultSet.getFloat("cameraPitch"));

                    byte[] inventory = resultSet.getBytes("inventory");
                    byte[] inventoryAmounts = resultSet.getBytes("inventoryAmounts");
                    PlayerCharacter character = new PlayerCharacter(useCamera, worldReference, windowReference, engineReference);
                    character.setInventoryStruct(inventory, inventoryAmounts);

                    return character;
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return null;
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

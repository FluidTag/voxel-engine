package com.szymc.voxel_engine;
import java.sql.*;
import java.util.ArrayList;
import java.util.UUID;

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
            //statement.execute("DELETE FROM chunks");
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
            //statement.execute("DROP TABLE entities");
            statement.execute("""
                CREATE TABLE IF NOT EXISTS entities (
                    world_id INTEGER,
                    entity_id_high INTEGER,
                    entity_id_low INTEGER,
                    item INTEGER,
                    x REAL,
                    y REAL,
                    z REAL,
                    xVel REAL,
                    yVel REAL,
                    zVel REAL,
                    PRIMARY KEY (world_id, entity_id_high, entity_id_low),
                    FOREIGN KEY (world_id) REFERENCES worlds(world_id) ON DELETE CASCADE
                )
            """);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public static void clearEntites(int worldId) {
        String sql = "DELETE from entities WHERE world_id = ?";

        try (Connection conn = DriverManager.getConnection(URL); PreparedStatement statement = conn.prepareStatement(sql)) {
            statement.setInt(1, worldId);
            statement.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public static void uploadEntity(int worldId, Entity entity) {
        String sql = "INSERT OR REPLACE INTO entities (world_id, entity_id_high, entity_id_low, item, x, y, z, xVel, yVel, zVel) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        if (entity instanceof EntityItem item) {
            try (Connection conn = DriverManager.getConnection(URL); PreparedStatement statement = conn.prepareStatement(sql)) {
                statement.setInt(1, worldId);
                statement.setLong(2, item.entityId.getMostSignificantBits());
                statement.setLong(3, item.entityId.getLeastSignificantBits());
                statement.setInt(4, item.item);
                statement.setFloat(5, item.position.x);
                statement.setFloat(6, item.position.y);
                statement.setFloat(7, item.position.z);
                statement.setFloat(8, item.velocity.x);
                statement.setFloat(9, item.velocity.y);
                statement.setFloat(10, item.velocity.z);

                System.out.println(item.entityId + ": " + item.position.x + ", " + item.position.y + ", " + item.position.z);
                statement.executeUpdate();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }

    public static ArrayList<Entity> getEntities(int worldId) {
        String sql = "SELECT * FROM entities WHERE world_id = ?";

        ArrayList<Entity> entities = new ArrayList<>();
        try (Connection conn = DriverManager.getConnection(URL); PreparedStatement statement = conn.prepareStatement(sql)) {
            statement.setInt(1, worldId);
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    long idHigh = resultSet.getLong("entity_id_high");
                    long idLow = resultSet.getLong("entity_id_low");
                    byte item = resultSet.getByte("item");
                    float x = resultSet.getFloat("x");
                    float y = resultSet.getFloat("y");
                    float z = resultSet.getFloat("z");
                    float xVel = resultSet.getFloat("xVel");
                    float yVel = resultSet.getFloat("yVel");
                    float zVel = resultSet.getFloat("zVel");

                    EntityItem createdItem = new EntityItem(item, 0, true);
                    createdItem.position.set(x, y, z);
                    createdItem.velocity.set(xVel, yVel, zVel);
                    createdItem.renderPosition.set(x, y, z);
                    createdItem.entityId = new UUID(idHigh, idLow);
                    // Have to change the entiy id system bro just gonna do ts tomorrow

                    System.out.println(createdItem.entityId);
                    entities.add(createdItem);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return entities;
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

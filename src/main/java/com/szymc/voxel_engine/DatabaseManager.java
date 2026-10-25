package com.szymc.voxel_engine;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.nio.ShortBuffer;
import java.sql.*;
import java.util.ArrayList;
import java.util.UUID;

public class DatabaseManager {
    private final static String URL = "jdbc:sqlite:my_database.db";

    public static void initializeSchema() {
        try (Connection conn = DriverManager.getConnection(URL); Statement statement = conn.createStatement()) {
            //statement.execute("DROP TABLE worlds");
           statement.execute("""
                CREATE TABLE IF NOT EXISTS worlds (
                    world_id INTEGER PRIMARY KEY AUTOINCREMENT,
                    world_name TEXT NOT NULL,
                    seed TEXT NOT NULL
                );
            """);

            //statement.execute("DROP TABLE players");
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
            //statement.execute("DROP TABLE chunks");
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
                    type INTEGER NOT NULL CHECK (type in (0, 1, 2)),
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

    public static void uploadEntity(int worldId, Entity e) {
        String sql = "INSERT OR REPLACE INTO entities (world_id, entity_id_high, entity_id_low, item, type, x, y, z, xVel, yVel, zVel) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (Connection conn = DriverManager.getConnection(URL); PreparedStatement statement = conn.prepareStatement(sql)) {
            statement.setInt(1, worldId);
            statement.setLong(2, e.entityId.getMostSignificantBits());
            statement.setLong(3, e.entityId.getLeastSignificantBits());
            if (e instanceof EntityItem itemEntity) {
                statement.setInt(4, itemEntity.item);
                statement.setInt(5, 0); // Entity Type 0: Item
            } else if (e instanceof BlockEntity blockEntity) {
                statement.setInt(4, (blockEntity.blockType & 0xFFFF) | ((blockEntity.blockModifiers & 0xFFFF) << 16));
                System.out.println("Saved modifiers: " + Integer.toBinaryString((blockEntity.blockModifiers & 0xFFFF)));
                statement.setInt(5, 1); // Entity Type 1: Block
            }

            statement.setFloat(6, e.position.x);
            statement.setFloat(7, e.position.y);
            statement.setFloat(8, e.position.z);
            statement.setFloat(9, e.velocity.x);
            statement.setFloat(10, e.velocity.y);
            statement.setFloat(11, e.velocity.z);

            System.out.println(e.entityId + ": " + e.position.x + ", " + e.position.y + ", " + e.position.z);
            statement.executeUpdate();
        } catch (SQLException err) {
            err.printStackTrace();
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
                    int item = resultSet.getInt("item");
                    int type = resultSet.getInt("type");
                    float x = resultSet.getFloat("x");
                    float y = resultSet.getFloat("y");
                    float z = resultSet.getFloat("z");
                    float xVel = resultSet.getFloat("xVel");
                    float yVel = resultSet.getFloat("yVel");
                    float zVel = resultSet.getFloat("zVel");

                    Entity createdEntity = null;
                    if (type == 0) {
                        createdEntity = new EntityItem((short) (item&0xFFFF), 0, true);
                    } else if (type == 1) {
                        System.out.println("Got modifiers: " + Integer.toBinaryString((item >>> 16) & 0xFFFF));
                        createdEntity = new BlockEntity((short) (item&0xFFFF), (short) ((item >>> 16) & 0xFFFF), 0);
                    }

                    assert createdEntity != null;
                    createdEntity.position.set(x, y, z);
                    createdEntity.velocity.set(xVel, yVel, zVel);
                    createdEntity.renderPosition.set(x, y, z);
                    createdEntity.entityId = new UUID(idHigh, idLow);

                    entities.add(createdEntity);
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
            ByteBuffer invBuffer = ByteBuffer.allocate(character.getInventory().length * 2);
            invBuffer.asShortBuffer().put(character.getInventory());

            statement.setBytes(7, invBuffer.array());
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

                    ShortBuffer invBuffer = ByteBuffer.wrap(resultSet.getBytes("inventory")).asShortBuffer();
                    short[] invPrim = new short[invBuffer.remaining()];
                    invBuffer.get(invPrim);

                    byte[] inventoryAmounts = resultSet.getBytes("inventoryAmounts");
                    PlayerCharacter character = new PlayerCharacter(useCamera, worldReference, windowReference, engineReference);
                    character.setInventoryStruct(invPrim, inventoryAmounts);

                    return character;
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return null;
    }

    public static void uploadChunk(int worldId, int x, int z, byte[] blob) {
        String sql = "INSERT INTO chunks (world_id, x, z, data) VALUES (?, ?, ?, ?) ON CONFLICT(world_id, x, z) DO UPDATE set data = excluded.data";

        try (Connection conn = DriverManager.getConnection(URL); PreparedStatement statement = conn.prepareStatement(sql)) {
            statement.setInt(1, worldId);
            statement.setInt(2, x);
            statement.setInt(3, z);
            statement.setBytes(4, blob);

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

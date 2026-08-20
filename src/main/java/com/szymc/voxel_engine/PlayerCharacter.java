package com.szymc.voxel_engine;

import org.joml.Vector3f;

import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.glfw.GLFW.GLFW_MOUSE_BUTTON_RIGHT;
import static org.lwjgl.glfw.GLFW.GLFW_PRESS;

record RaycastResult(String face, int x, int y, int z) {};
public class PlayerCharacter {
    private Camera playerCamera;
    private World worldReference;
    private Window windowReference;
    private Engine engineAttachment;

    private double velocityY = 0;
    private boolean isGrounded = false;
    private static final float PLAYER_HEIGHT = 1.8f;
    private static final float PLAYER_RADIUS = 0.3f;
    private boolean isSprinting = false;
    private final boolean[] keysPressed = new boolean[GLFW_KEY_LAST + 1];

    private boolean spectatorMode = true;

    private byte[] inventoryAmounts = new byte[36];
    private byte[] inventory = new byte[36];

    public void setInventorySlot(byte index, byte type, byte amount) {
        this.inventory[index] = type;
        this.inventoryAmounts[index] = amount;
        if (inventoryAmounts[index] == 0) inventory[index] = 0;
    }

    public byte readInventoryType(byte index) {
        return this.inventory[index];
    }

    public byte readInventoryAmount(byte index) {
        return this.inventoryAmounts[index];
    }

    public int currentHotbarSlot = 0;
    private static boolean blockAt(World world, int x, int y, int z) {
        ChunkColumn chunk = world.getLoadedChunkAtPos(x>>5, z>>5);
        if (chunk == null || !chunk.state.isAtleast(ChunkColumn.ChunkState.TERRAIN)) return false;

        if (y < 0 && y > -50) return false;
        if (y <= -50) return true;

        byte block = chunk.getBlockInChunk(x&31, y, z&31);
        return block != Blocks.AIR;
    }

    private static boolean isColliding(World world, float x, float y, float z) {
        int minX = (int)Math.floor(x-PLAYER_RADIUS);
        int maxX = (int)Math.floor(x+PLAYER_RADIUS);

        int minY = (int)Math.floor(y-PLAYER_HEIGHT);
        int maxY = (int)Math.floor(y);

        int minZ = (int)Math.floor(z-PLAYER_RADIUS);
        int maxZ = (int)Math.floor(z+PLAYER_RADIUS);

        for (int bx = minX; bx <= maxX; bx++) {
            for (int by = minY; by <= maxY; by++) {
                for (int bz = minZ; bz <= maxZ; bz++) {
                    if (blockAt(world, bx, by, bz)) return true;
                }
            }
        }

        return false;
    }

    private static RaycastResult getRaycastResult(World world, Camera camera) {
        Vector3f rayOrigin = new Vector3f(camera.cameraPos);
        Vector3f rayDir = camera.getLookUnitNormal();
        float maxDistance = 4.5f;

        int x = (int)Math.floor(rayOrigin.x);
        int y = (int)Math.floor(rayOrigin.y);
        int z = (int)Math.floor(rayOrigin.z);

        int stepX = rayDir.x > 0 ? 1 : -1;
        int stepY = rayDir.y > 0 ? 1 : -1;
        int stepZ = rayDir.z > 0 ? 1 : -1;

        float tDeltaX = (rayDir.x != 0) ? Math.abs(1.0f/rayDir.x) : Float.MAX_VALUE;
        float tDeltaY = (rayDir.y != 0) ? Math.abs(1.0f/rayDir.y) : Float.MAX_VALUE;
        float tDeltaZ = (rayDir.z != 0) ? Math.abs(1.0f/rayDir.z) : Float.MAX_VALUE;

        float tMaxX = (rayDir.x > 0) ? (float)(Math.floor(rayOrigin.x) + 1.0f - rayOrigin.x) * tDeltaX : (float)(rayOrigin.x - Math.floor(rayOrigin.x)) * tDeltaX;
        float tMaxY = (rayDir.y > 0) ? (float)(Math.floor(rayOrigin.y) + 1.0f - rayOrigin.y) * tDeltaY : (float)(rayOrigin.y - Math.floor(rayOrigin.y)) * tDeltaY;
        float tMaxZ = (rayDir.z > 0) ? (float)(Math.floor(rayOrigin.z) + 1.0f - rayOrigin.z) * tDeltaZ : (float)(rayOrigin.z - Math.floor(rayOrigin.z)) * tDeltaZ;

        String hitFace = "NONE";
        boolean hit = false;
        float t = 0;

        while (t <= maxDistance) {
            if (blockAt(world, x, y, z)) {
                hit = true;
                break;
            }

            if (tMaxX < tMaxY) {
                if (tMaxX < tMaxZ) {
                    t = tMaxX;
                    tMaxX += tDeltaX;
                    x += stepX;
                    hitFace = (stepX > 0) ? "WEST" : "EAST";
                } else {
                    t = tMaxZ;
                    tMaxZ += tDeltaZ;
                    z += stepZ;
                    hitFace = (stepZ > 0) ? "NORTH" : "SOUTH";
                }
            } else {
                if (tMaxY < tMaxZ) {
                    t = tMaxY;
                    tMaxY += tDeltaY;
                    y += stepY;
                    hitFace = (stepY > 0) ? "DOWN" : "UP";
                } else {
                    t = tMaxZ;
                    tMaxZ += tDeltaZ;
                    z += stepZ;
                    hitFace = (stepZ > 0) ? "NORTH" : "SOUTH";
                }
            }
        }

        if (hit) return new RaycastResult(hitFace, x, y, z);
        return null;
    }

    public PlayerCharacter(Camera playerCamera, World worldReference, Window windowReference, Engine engineAttachment) {
        this.playerCamera = playerCamera;
        this.worldReference = worldReference;
        this.windowReference = windowReference;
        this.engineAttachment = engineAttachment;
        inventory[0] = Blocks.TORCH;
        inventoryAmounts[0] = 64;

        glfwSetScrollCallback(windowReference.getWindowId(), (windowHandle, xOffset, yOffset) -> {
            if (yOffset < 0) {
                currentHotbarSlot++;
            } else {
                currentHotbarSlot--;
            }

            if (currentHotbarSlot > 8) currentHotbarSlot = 0;
            if (currentHotbarSlot < 0) currentHotbarSlot = 8;
        });

        glfwSetKeyCallback(windowReference.getWindowId(), (windowHandle, key, scancode, action, mods) -> {
            if (key >= GLFW_KEY_1 && key <= GLFW_KEY_9) {
                currentHotbarSlot = key - GLFW_KEY_0 - 1;
            }
        });

        glfwSetMouseButtonCallback(windowReference.getWindowId(), (windowHandle, button, action, mods) -> {
            if ((button == GLFW_MOUSE_BUTTON_LEFT || button == GLFW_MOUSE_BUTTON_RIGHT) && action == GLFW_PRESS) {
                if (button == GLFW_MOUSE_BUTTON_RIGHT && inventory[currentHotbarSlot] == 0) return;

                RaycastResult result = getRaycastResult(worldReference, playerCamera);

                if (result != null) {
                    int x = result.x();
                    int y = result.y();
                    int z = result.z();
                    String hitFace = result.face();

                    if (button == GLFW_MOUSE_BUTTON_RIGHT) {
                        switch (hitFace) {
                            case "WEST":  x -= 1; break;
                            case "EAST":  x += 1; break;
                            case "DOWN":  y -= 1; break;
                            case "UP":    y += 1; break;
                            case "NORTH": z -= 1; break;
                            case "SOUTH": z += 1; break;
                        }
                    }

                    int cx = x >> 5;
                    int cz = z >> 5;
                    ChunkColumn chunk = worldReference.getLoadedChunkAtPos(cx, cz);
                    if (chunk == null) return;

                    if (button == GLFW_MOUSE_BUTTON_LEFT) {
                        engineAttachment.removeOutlineLoc();
                        byte block = chunk.getBlockInChunk(x & 31, y, z & 31);
                        if (block == Blocks.AIR) return;

                        chunk.setBlockInChunk(x & 31, y, z & 31, Blocks.AIR);
                        worldReference.spawnNewItemEntity(block, x, y, z);
                        chunk.setSectionDirty(y >> 4);

                        worldReference.updateChunk(cx, y, cz);
                    } else if (inventory[currentHotbarSlot] != 0 && inventoryAmounts[currentHotbarSlot] > 0) {
                        chunk.setBlockInChunk(x & 31, y, z & 31, inventory[currentHotbarSlot]);
                        inventoryAmounts[currentHotbarSlot]--;
                        if (inventoryAmounts[currentHotbarSlot] == 0) inventory[currentHotbarSlot] = 0;
                        chunk.setSectionDirty(y >> 4);

                        worldReference.updateChunk(cx, y, cz);
                    }
                }
            }
        });
    }

    public byte[] getInventory() {
        return inventory;
    }

    public void poll(float deltaTime) {
        RaycastResult result = getRaycastResult(worldReference, playerCamera);
        if (result != null) {
            engineAttachment.setOutlineLoc(result.x(), result.y(), result.z());
        } else {
            engineAttachment.removeOutlineLoc();
        }

        if (!keysPressed[GLFW_KEY_PAGE_UP] && glfwGetKey(windowReference.getWindowId(), GLFW_KEY_PAGE_UP) == GLFW_PRESS) {
            spectatorMode = !spectatorMode;
        }
        if (!keysPressed[GLFW_KEY_LEFT_CONTROL] && glfwGetKey(windowReference.getWindowId(), GLFW_KEY_LEFT_CONTROL) == GLFW_PRESS) {
            isSprinting = !isSprinting;
        }
        keysPressed[GLFW_KEY_LEFT_CONTROL] = glfwGetKey(windowReference.getWindowId(), GLFW_KEY_LEFT_CONTROL) == GLFW_PRESS;
        keysPressed[GLFW_KEY_PAGE_UP] = glfwGetKey(windowReference.getWindowId(), GLFW_KEY_PAGE_UP) == GLFW_PRESS;

        float newCamSpeed = (!spectatorMode ? (isSprinting ? 6 : 4) : 126) * deltaTime;
        Vector3f playerMoveIntent = spectatorMode ? playerCamera.pollCreativeCameraMovements(windowReference.getWindowId(), newCamSpeed) :
                playerCamera.pollSurvivalCameraMovements(windowReference.getWindowId(), newCamSpeed);
        boolean jumpPressed = glfwGetKey(windowReference.getWindowId(), GLFW_KEY_SPACE) == GLFW_PRESS;

        playerCamera.cameraPos.y += playerMoveIntent.y;
        if (!spectatorMode && isColliding(worldReference, playerCamera.cameraPos.x + playerMoveIntent.x, playerCamera.cameraPos.y, playerCamera.cameraPos.z)) {
            playerMoveIntent.x = 0;
        }
        playerCamera.cameraPos.x += playerMoveIntent.x;

        if (!spectatorMode && isColliding(worldReference, playerCamera.cameraPos.x, playerCamera.cameraPos.y, playerCamera.cameraPos.z + playerMoveIntent.z)) {
            playerMoveIntent.z = 0;
        }

        playerCamera.cameraPos.z += playerMoveIntent.z;

        // Gravity
        if (spectatorMode) return;

        velocityY -= 13*deltaTime;
        float deltaY = (float) (velocityY*deltaTime);

        if (isColliding(worldReference, playerCamera.cameraPos.x, playerCamera.cameraPos.y + deltaY, playerCamera.cameraPos.z)) {
            if (velocityY < 0) {
                float feetY = playerCamera.cameraPos.y - PLAYER_HEIGHT + deltaY;
                playerCamera.cameraPos.y = (float) (Math.floor(feetY) + 1.0 + PLAYER_HEIGHT);
                isGrounded = true;
            } else {
                playerCamera.cameraPos.y = (float)Math.floor(playerCamera.cameraPos.y + deltaY) - 0.001f;
                isGrounded = false;
            }
            velocityY = 0;
        } else {
            playerCamera.cameraPos.y += deltaY;
            isGrounded = false;
        }

        if (isGrounded && jumpPressed) {
            isGrounded = false;
            velocityY = 6.4;
        }
    }

    public Camera getPlayerCamera() {
        return this.playerCamera;
    }
}

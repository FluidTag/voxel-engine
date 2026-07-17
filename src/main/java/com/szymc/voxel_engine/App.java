package com.szymc.voxel_engine;
import org.joml.Vector3f;

import static org.lwjgl.glfw.GLFW.*;
public class App {
	private static double velocityY = 0;
	private static boolean isGrounded = false;
	static final float PLAYER_HEIGHT = 1.8f;
	static final float PLAYER_RADIUS = 0.3f;
	static boolean isSprinting = false;
	static final boolean[] keysPressed = new boolean[GLFW_KEY_LAST + 1];

	private static boolean blockAt(World world, int x, int y, int z) {
		ChunkColumn chunk = world.getLoadedChunkAtPos(x>>5, z>>5);
		if (chunk == null || !chunk.state.isAtleast(ChunkColumn.ChunkState.TERRAIN)) return false;
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

	public static void main(String[] args) {
		Camera camera = new Camera();
		Window window = new Window(1600, 900, "Voxel-Engine");

		World mainWorld = new World(window.getWindowId());
		TerrainTask.initNoise();

		window.attachCamera(camera);
		Engine engine = new Engine(mainWorld, camera);

		BiomeRegistry.init();

		glfwSetMouseButtonCallback(window.getWindowId(), (windowHandle, button, action, mods) -> {
			if ((button == GLFW_MOUSE_BUTTON_LEFT || button == GLFW_MOUSE_BUTTON_RIGHT) && action == GLFW_PRESS) {
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
					if (blockAt(mainWorld, x, y, z)) {
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

				if (hit) {
					if (button == GLFW_MOUSE_BUTTON_RIGHT) {
						switch (hitFace) {
							case "WEST":  x -= 1; break;
							case "EAST":  x += 1; break;
							case "DOWN":  y -= 1; break;
							case "UP":    y += 1; break;
							case "NORTH": z -= 1; break;
							case "SOUTH": z += 1; break;
						}
					} else {
						engine.removeOutlineLoc();
					}

					int cx = x >> 5;
					int cz = z >> 5;
					ChunkColumn chunk = mainWorld.getLoadedChunkAtPos(cx, cz);
					chunk.setBlockInChunk(x & 31, y, z & 31, button == GLFW_MOUSE_BUTTON_RIGHT ? Blocks.DIRT : Blocks.AIR);
					chunk.dirtyCount++;
					chunk.setSectionDirty(y >> 4);
					mainWorld.updateChunk(cx, cz);
				}
			}
		});

		double lastFrameTime = 0.0;
		while (!window.shouldClose()) {
			double currentFrameTime = window.getFrameTime();
			float deltaTime = (float)(currentFrameTime - lastFrameTime);
			lastFrameTime = currentFrameTime;

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
				if (blockAt(mainWorld, x, y, z)) {
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

			if (hit) {
				engine.setOutlineLoc(x, y, z);
			} else {
				engine.removeOutlineLoc();
			}

			if (!keysPressed[GLFW_KEY_LEFT_CONTROL] && glfwGetKey(window.getWindowId(), GLFW_KEY_LEFT_CONTROL) == GLFW_PRESS) {
				isSprinting = !isSprinting;
			}
			keysPressed[GLFW_KEY_LEFT_CONTROL] = glfwGetKey(window.getWindowId(), GLFW_KEY_LEFT_CONTROL) == GLFW_PRESS;

			float newCamSpeed = (isSprinting ? 6 : 4) * deltaTime;
			Vector3f playerMoveIntent = camera.pollCameraMovements(window.getWindowId(), newCamSpeed);
			boolean jumpPressed = glfwGetKey(window.getWindowId(), GLFW_KEY_SPACE) == GLFW_PRESS;

			if (isColliding(mainWorld, camera.cameraPos.x + playerMoveIntent.x, camera.cameraPos.y, camera.cameraPos.z)) {
				playerMoveIntent.x = 0;
			}
			camera.cameraPos.x += playerMoveIntent.x;

			if (isColliding(mainWorld, camera.cameraPos.x, camera.cameraPos.y, camera.cameraPos.z + playerMoveIntent.z)) {
				playerMoveIntent.z = 0;
			}
			camera.cameraPos.z += playerMoveIntent.z;

			// Gravity
			velocityY -= 13*deltaTime;
			float deltaY = (float) (velocityY*deltaTime);

			if (isColliding(mainWorld, camera.cameraPos.x, camera.cameraPos.y + deltaY, camera.cameraPos.z)) {
				if (velocityY < 0) {
					float feetY = camera.cameraPos.y - PLAYER_HEIGHT + deltaY;
					camera.cameraPos.y = (float) (Math.floor(feetY) + 1.0 + PLAYER_HEIGHT);
					isGrounded = true;
				} else {
					camera.cameraPos.y = (float)Math.floor(camera.cameraPos.y + deltaY) - 0.001f;
					isGrounded = false;
				}
				velocityY = 0;
			} else {
				camera.cameraPos.y += deltaY;
				isGrounded = false;
			}

			if (isGrounded && jumpPressed) {
				isGrounded = false;
				velocityY = 6.4;
			}

			mainWorld.pollGenerationThreads();
			mainWorld.update(camera.cameraPos);
			engine.render();

			if (glfwGetKey(window.getWindowId(), GLFW_KEY_T) == GLFW_PRESS) {
				int wx = camera.getWorldX();
				int wy = camera.getWorldY();
				int wz = camera.getWorldZ();

				System.out.println(wx + ", " + wy + ", " + wz);
			}

			window.swapBuffers();
			glfwPollEvents();
		}


		glfwTerminate();
	}
}
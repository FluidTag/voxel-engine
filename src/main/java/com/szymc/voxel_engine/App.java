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

		return chunk.getBlockInChunk(x&31, y, z&31) != 0;
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

		double lastFrameTime = 0.0;

		while (!window.shouldClose()) {
			double currentFrameTime = window.getFrameTime();
			float deltaTime = (float)(currentFrameTime - lastFrameTime);
			lastFrameTime = currentFrameTime;
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
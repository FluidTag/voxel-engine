package com.szymc.voxel_engine;
import org.joml.Vector3f;

import static org.lwjgl.glfw.GLFW.*;
public class App {
	private static boolean blockAt(World world, int x, int y, int z) {
		ChunkColumn chunk = world.getLoadedChunkAtPos(x>>5, z>>5);
		if (chunk == null || !chunk.state.isAtleast(ChunkColumn.ChunkState.TERRAIN)) return false;

		if (y < 0 && y > -50) return false;
		if (y <= -50) return true;

		byte block = chunk.getBlockInChunk(x&31, y, z&31);
		return block != Blocks.AIR;
	}

	private static boolean isCubeColliding(World world, float wx, float wy, float wz, float cubeSize) {
		int minX = (int)Math.floor(wx);
		int maxX = (int)Math.floor(wx+cubeSize);

		int minY = (int)Math.floor(wy);
		int maxY = (int)Math.floor(wy+cubeSize);

		int minZ = (int)Math.floor(wz);
		int maxZ = (int)Math.floor(wz+cubeSize);

		for (int x = minX; x <= maxX; x++) {
			for (int y = minY; y <= maxY; y++) {
				for (int z = minZ; z <= maxZ; z++) {
					if (blockAt(world, x, y, z)) return true;
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
		PlayerCharacter character = new PlayerCharacter(camera, mainWorld, window, engine);
		engine.setPlayer(character);

		BiomeRegistry.init();

		double lastFrameTime = 0.0;
		double tIncrement = 0;
		int ticks = 0;
		while (!window.shouldClose()) {
			double currentFrameTime = window.getFrameTime();
			float deltaTime = (float)(currentFrameTime - lastFrameTime);
			lastFrameTime = currentFrameTime;
			tIncrement += 1*deltaTime;
			if (tIncrement >= 0.10f) {
				ticks++;
				tIncrement = 0;

				// Physics Update
				for (Entity entity : mainWorld.getEntities().values()) {
					if (entity.getClass() == EntityItem.class) {
						EntityItem item = (EntityItem) entity;
						item.velocity.y += -0.1f;
						if (isCubeColliding(mainWorld, item.position.x, item.position.y + item.velocity.y, item.position.z, 0.3f)) {
							item.velocity.y = 0;
							item.onGround = true;
						}

						float distance = (item.position.x - character.getPlayerCamera().cameraPos.x) * (item.position.x - character.getPlayerCamera().cameraPos.x)
											+ (item.position.y - character.getPlayerCamera().cameraPos.y + 0.9f) * (item.position.y - character.getPlayerCamera().cameraPos.y + 0.9f)
											+ (item.position.z - character.getPlayerCamera().cameraPos.z) * (item.position.z - character.getPlayerCamera().cameraPos.z);
						System.out.println(distance);
						if (distance <= 2.4) {
							// Locate empty inventory slot
							byte slot = -1;
							for (byte i = 0; i < 36; i++) {
								byte inventoryType = character.readInventoryType(i);
								if (inventoryType == 0 || (inventoryType == item.item && character.readInventoryAmount(i) <= 63)) {
									slot = i;
									break;
								}
							}

							if (slot != -1) {
								character.setInventorySlot(slot, item.item, (byte)(character.readInventoryAmount(slot)+1));
								mainWorld.destroyItemEntity(item.entityId);
							} else System.out.println("Inventory full");
						}

						item.position.y += item.velocity.y;
					}
				}
			}

			character.poll(deltaTime);

			mainWorld.pollGenerationThreads();
			mainWorld.update(camera.cameraPos);
			engine.render();

			if (glfwGetKey(window.getWindowId(), GLFW_KEY_T) == GLFW_PRESS) {
				int wx = camera.getWorldX();
				int wy = camera.getWorldY();
				int wz = camera.getWorldZ();
				int surfaceHeight = TerrainTask.getNoiseHeight(wx, wz);
				float temp = TerrainTask.getTemp(wx, wz);
				float moist = TerrainTask.getMoist(wx, wz);
				float erosion = TerrainTask.getErosion(wx, wz);

				BiomeType surfaceBiome = TerrainTask.getBiomeType(surfaceHeight, temp, moist, TerrainTask.getContinental(wx, wz), erosion, TerrainTask.getWeirdness(wx, wz));
				//BiomeType biome = TerrainTask.getBiomeType(wy, temp, moist, TerrainTask.getContinental(wx, wz), erosion, TerrainTask.getWeirdness(wx, wz));

				System.out.println(wx + ", " + wy + ", " + wz + " | Surface Biome ("+surfaceHeight+"): " + surfaceBiome + " [T "+Math.round(temp*100f)/100f+", M "+Math.round(moist*100f)/100f+", E "+Math.round(erosion*100f)/100f + "]");
			}

			window.swapBuffers();
			glfwPollEvents();
		}


		glfwTerminate();
	}
}
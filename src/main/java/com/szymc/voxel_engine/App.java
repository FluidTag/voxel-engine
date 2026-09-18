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

	public static final int WINDOW_WIDTH = 1600;
	public static final int WINDOW_HEIGHT = 900;

	public static void main(String[] args) {
		Camera camera = new Camera();
		Window window = new Window(App.WINDOW_WIDTH, App.WINDOW_HEIGHT, "Voxel-Engine");

		World mainWorld = new World(window.getWindowId());
		TerrainTask.initNoise();

		window.attachCamera(camera);
		Engine engine = new Engine(mainWorld, camera);
		PlayerCharacter character = new PlayerCharacter(camera, mainWorld, window, engine);
		engine.setPlayer(character);

		BiomeRegistry.init();

		double lastFrameTime = 0.0;
		double tIncrement = 0;
		while (!window.shouldClose()) {
			double currentFrameTime = window.getFrameTime();
			float deltaTime = (float)(currentFrameTime - lastFrameTime);
			lastFrameTime = currentFrameTime;

			tIncrement += 1*deltaTime;
			float currentInterp = (float) (tIncrement/0.05f);
			for (Entity entity : mainWorld.getEntities().values()) {
				if (entity.getClass() == EntityItem.class) {
					EntityItem item = (EntityItem) entity;
					item.previousPosition.lerp(item.position, currentInterp, item.renderPosition);
				}
			}

			if (tIncrement >= 0.05f) {
				mainWorld.incrementTick();
				tIncrement -= 0.05f;

				if (glfwGetMouseButton(window.getWindowId(), GLFW_MOUSE_BUTTON_LEFT) == GLFW_PRESS) {
					engine.leftMouseHeldTick();
				}

				// Physics Update
				for (Entity entity : mainWorld.getEntities().values()) {
					if (entity.getClass() == EntityItem.class) {
						EntityItem item = (EntityItem) entity;
						item.previousPosition.set(item.position);
						item.velocity.y += -0.05f;
						if (isCubeColliding(mainWorld, item.position.x, item.position.y + item.velocity.y, item.position.z, 0.3f)) {
							item.velocity.y = 0;
							item.onGround = true;
						}

						item.position.y += item.velocity.y;

						float distance = (item.position.x - character.getPlayerCamera().cameraPos.x) * (item.position.x - character.getPlayerCamera().cameraPos.x)
											+ (item.position.y - character.getPlayerCamera().cameraPos.y + 0.9f) * (item.position.y - character.getPlayerCamera().cameraPos.y + 0.9f)
											+ (item.position.z - character.getPlayerCamera().cameraPos.z) * (item.position.z - character.getPlayerCamera().cameraPos.z);

						if (distance <= 2.3 && (mainWorld.getTick()-item.createdAtTick > (item.playerDropped ? 30 : 5))) {
							// Locate empty inventory slot
							byte slot = -1;
							for (byte i = 0; i < 36; i++) {
								byte inventoryType = character.readInventoryType(i);
								if (inventoryType == 0 && slot == -1) slot = i;
								if (inventoryType == item.item && character.readInventoryAmount(i) < 64) {
									slot = i;
									break;
								}
							}

							if (slot != -1) {
								character.setInventorySlot(slot, item.item, (byte)(character.readInventoryAmount(slot)+1));
								mainWorld.addEntityIdToDeleteList(item.entityId);
							} else System.out.println("Inventory full");
						}
					}
				}

				mainWorld.processEntityDeletions();
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

				int light = -1;
				byte block = -1;
				ChunkColumn c = mainWorld.getLoadedChunkAtPos(wx>>5, wz>>5);
				if (c != null) {
					ChunkSection sec = c.getSection(wy>>4);
					if (sec != null) light = c.getSection(wy>>4).getLightingData()[(wy&15)*32*32 + (wz&31)*32 + (wx&31)];
					if (sec != null) block = c.getBlockInChunk((wx&31), wy, (wz&31));
				}

				BiomeType surfaceBiome = TerrainTask.getBiomeType(surfaceHeight, temp, moist, TerrainTask.getContinental(wx, wz), erosion, TerrainTask.getWeirdness(wx, wz));
				//BiomeType biome = TerrainTask.getBiomeType(wy, temp, moist, TerrainTask.getContinental(wx, wz), erosion, TerrainTask.getWeirdness(wx, wz));
				System.out.println("____Log_________");
				System.out.println("CameraAt: ("+camera.cameraPos.x + ", " + camera.cameraPos.y + ", " + camera.cameraPos.z + ")");
				System.out.println(wx + ", " + wy + ", " + wz + " CC ("+(wx&31)+", " + (wy&15) + ", " + (wz&31) + ") | Surface Biome (@y-"+surfaceHeight+"): " + surfaceBiome + " [T "+Math.round(temp*100f)/100f+", M "+Math.round(moist*100f)/100f+", E "+Math.round(erosion*100f)/100f + "]");
				System.out.println("Light | Sky: " + ((light >> 4) & 0xF) + ", Block: " + (light&0xF) + " | BlockId@ = " + block);
			}

			window.swapBuffers();
			glfwPollEvents();
		}


		glfwTerminate();
	}
}
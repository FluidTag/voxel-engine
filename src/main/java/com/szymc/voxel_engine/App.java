package com.szymc.voxel_engine;
import org.joml.Vector3f;

import static org.lwjgl.glfw.GLFW.*;
public class App {
	public static void main(String[] args) {
		Camera camera = new Camera();
		Window window = new Window(1600, 900, "Voxel-Engine");

		World mainWorld = new World(window.getWindowId());
		TerrainTask.initNoise();

		window.attachCamera(camera);
		Engine engine = new Engine(mainWorld, camera);
		PlayerCharacter character = new PlayerCharacter(camera, mainWorld, window, engine);

		BiomeRegistry.init();
		Texture.readBlockJson("gameData/blocks.json");

		double lastFrameTime = 0.0;
		while (!window.shouldClose()) {
			double currentFrameTime = window.getFrameTime();
			float deltaTime = (float)(currentFrameTime - lastFrameTime);
			lastFrameTime = currentFrameTime;

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
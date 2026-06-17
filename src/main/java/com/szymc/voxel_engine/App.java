package com.szymc.voxel_engine;
import static org.lwjgl.glfw.GLFW.*;
public class App {
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
       		
       	mainWorld.pollGenerationThreads();
       	mainWorld.update(camera.cameraPos);
       	engine.render();
       	
       	float newCamSpeed = 100*deltaTime;
       	camera.pollCameraMovements(window.getWindowId(), newCamSpeed);
       	
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
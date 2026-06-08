package com.szymc.voxel_engine;


import org.lwjgl.opengl.GL;
import org.lwjgl.opengl.GL11;


import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.system.MemoryUtil.NULL;


import org.joml.Vector3f;


public class Window {
	private long windowId;
	private float lastX, lastY;
	private boolean firstMouse = true;
	private Camera cameraAttachment;
	private int width;
	private int height;
	
	public void attachCamera(Camera cam) {
		this.cameraAttachment = cam;
	}
	
	public Window(int width, int height, String title) {
		this.lastX = width/2;
		this.lastY = height/2;
		
		// 1. Initialize GLFW
        if (!glfwInit()) {
            throw new IllegalStateException("Unable to initialize GLFW");
        }
       
        // 2. Configure the window (Optional, but good practice)
        glfwWindowHint(GLFW_VISIBLE, GLFW_FALSE); // Stay hidden until centered
        glfwWindowHint(GLFW_RESIZABLE, GLFW_TRUE);
        glfwWindowHint(GLFW_CONTEXT_VERSION_MAJOR, 3);
        glfwWindowHint(GLFW_CONTEXT_VERSION_MINOR, 3);
        glfwWindowHint(GLFW_OPENGL_PROFILE, GLFW_OPENGL_CORE_PROFILE);
        
        glfwSwapInterval(1);
        
        // 3. Create the window
        long window = glfwCreateWindow(width, height, "Voxel Engine", NULL, NULL);
        if (window == NULL) {
            throw new RuntimeException("Failed to create the GLFW window");
        }
        
        glfwSetInputMode(window, GLFW_CURSOR, GLFW_CURSOR_DISABLED);
        
        // 4. Make the OpenGL context current
        glfwMakeContextCurrent(window);
        
        // This line is CRITICAL: it connects LWJGL to the GPU context
        GL.createCapabilities();
        glEnable(GL_DEPTH_TEST);
        glCullFace(GL_BACK); // Don't draw the backs of triangles
        glFrontFace(GL_CCW); // Triangles are "front" if points are Counter-Clockwise
        glEnable(GL_CULL_FACE);
        
        System.out.println(GL11.glGetString(GL11.GL_RENDERER));
    	System.out.println(GL11.glGetString(GL11.GL_VENDOR));
        
        // 5. Show the window
        glfwShowWindow(window);
        this.windowId = window;
        
        glfwSetCursorPosCallback(window, (windowHandle, xPos, yPos) -> {
    		if (firstMouse) {
    			lastX = (float) xPos;
    			lastY = (float) yPos;
    			firstMouse = false;
    		}
    		
    		float xOffset = (float) xPos - lastX;
    		float yOffset = lastY - (float) yPos;
    		lastX = (float) xPos;
    		lastY = (float) yPos;
    		
    		cameraAttachment.recieveMouseOffset(xOffset, yOffset);
    	});
	}
	
	public long getWindowId() {
		return this.windowId;
	}
	
	public void swapBuffers() {
		glfwSwapBuffers(this.windowId);
	}
	
	public boolean shouldClose() {
		return glfwWindowShouldClose(this.windowId);
	}
	
	public double getFrameTime() {
		return glfwGetTime();
	}
}






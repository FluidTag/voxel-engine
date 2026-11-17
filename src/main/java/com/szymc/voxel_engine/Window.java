package com.szymc.voxel_engine;


import org.lwjgl.openal.AL;
import org.lwjgl.openal.ALC;
import org.lwjgl.openal.ALCCapabilities;
import org.lwjgl.openal.ALCapabilities;
import org.lwjgl.opengl.GL;
import org.lwjgl.opengl.GL11;


import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.system.MemoryUtil.NULL;
import static org.lwjgl.opengl.GL13.*;
import static org.lwjgl.openal.ALC10.*;
import static org.lwjgl.openal.ALCCapabilities.*;

import org.joml.Vector3f;


public class Window {
	private long windowId;
    private long audioContext;
    private long audioDevice;

	public Window(int width, int height, String title) {
        if (!glfwInit()) {
            throw new IllegalStateException("Unable to initialize GLFW");
        }
       
        // Configure the window
        glfwWindowHint(GLFW_VISIBLE, GLFW_FALSE);
        glfwWindowHint(GLFW_RESIZABLE, GLFW_TRUE);
        glfwWindowHint(GLFW_CONTEXT_VERSION_MAJOR, 3);
        glfwWindowHint(GLFW_CONTEXT_VERSION_MINOR, 3);
        glfwWindowHint(GLFW_OPENGL_PROFILE, GLFW_OPENGL_CORE_PROFILE);
        glfwWindowHint(GLFW_SAMPLES, 8); // Request 4x MSAA
        
        // Create the window
        long window = glfwCreateWindow(width, height, "Voxel Engine", NULL, NULL);
        if (window == NULL) {
            throw new RuntimeException("Failed to create the GLFW window");
        }
        
        long monitor = glfwGetPrimaryMonitor();
        org.lwjgl.glfw.GLFWVidMode vidmode = glfwGetVideoMode(monitor);
        if (vidmode != null) {
        	int xPos = (vidmode.width() - width) / 2;
        	int yPos = (vidmode.height() - height) / 2;
        	
        	glfwSetWindowPos(window, xPos, yPos);
        }
        
        glfwSetInputMode(window, GLFW_CURSOR, GLFW_CURSOR_DISABLED);
        
        // Make the OpenGL context current
        glfwMakeContextCurrent(window);
        glfwShowWindow(window);

        // Initialize audio device
        String defaultDeviceName = alcGetString(0, ALC_DEFAULT_DEVICE_SPECIFIER);
        audioDevice = alcOpenDevice(defaultDeviceName);

        int[] attributes = {0};
        audioContext = alcCreateContext(audioDevice, attributes);
        alcMakeContextCurrent(audioContext);

        ALCCapabilities alcCapabilities = ALC.createCapabilities(audioDevice);
        ALCapabilities alCapabilities = AL.createCapabilities(alcCapabilities);

        if (!alCapabilities.OpenAL10) {
            throw new IllegalStateException("Audio library not supported");
        }

        // Connects LWJGL to the GPU context
        GL.createCapabilities();
        glEnable(GL_DEPTH_TEST);
        glCullFace(GL_BACK); // Don't draw the backs of triangles
        glFrontFace(GL_CCW); // Triangles are "front" if points are Counter-Clockwise
        glEnable(GL_CULL_FACE);
        glEnable(GL_MULTISAMPLE); // Tell OpenGL to use it
		glfwSwapInterval(1);
        
        System.out.println(GL11.glGetString(GL11.GL_RENDERER));
    	System.out.println(GL11.glGetString(GL11.GL_VENDOR));

        this.windowId = window;
	}

    public void destroyAudioContext() {
        alcDestroyContext(audioContext);
        alcCloseDevice(audioDevice);
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






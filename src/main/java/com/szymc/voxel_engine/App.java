package com.szymc.voxel_engine;


import org.lwjgl.BufferUtils;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.opengl.GL;
import org.lwjgl.opengl.GL11;


import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.system.MemoryUtil.NULL;
import org.lwjgl.system.MemoryStack;
import static org.lwjgl.system.MemoryStack.*;


import static org.lwjgl.opengl.GL11.*; // Basic functions (Clear, etc.)
import static org.lwjgl.opengl.GL15.*; // VBO functions (glGenBuffers)
import static org.lwjgl.opengl.GL20.*; // Shader/Attribute functions (glVertexAttribPointer)
import static org.lwjgl.opengl.GL30.*; // VAO functions (glGenVertexArrays)


import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.Arrays;
import java.util.stream.Collectors;


import org.joml.Matrix4f;
import org.joml.Vector3f;


public class App {
	private static String loadResource(String fileName) {
	    try (InputStream is = App.class.getResourceAsStream(fileName);
	         BufferedReader reader = new BufferedReader(new InputStreamReader(is))) {
	        return reader.lines().collect(Collectors.joining("\n"));
	    } catch (Exception e) {
	        throw new RuntimeException("Failed to load resource: " + fileName, e);
	    }
	}
	
	public static int compileShader(int type, String source) {
		int shader = glCreateShader(type);
		glShaderSource(shader, source);
		glCompileShader(shader);
		
		if (glGetShaderi(shader, GL_COMPILE_STATUS) == GL_FALSE) {
        	throw new RuntimeException("Fragment Shader Failed: " + glGetShaderInfoLog(shader));
        }
		
		return shader;
	}
	
	public static int createShaderProgram(String vertexShaderSource, String fragmentShaderSource) {
		int program = glCreateProgram();
		int vs = compileShader(GL_VERTEX_SHADER, vertexShaderSource);
		int fs = compileShader(GL_FRAGMENT_SHADER, fragmentShaderSource);
		
		glAttachShader(program, vs);
		glAttachShader(program, fs);
		glLinkProgram(program);
    		
    	glDeleteShader(vs);
    	glDeleteShader(fs);
    	
    	return program;
	}
	
	private static float lastX = 400, lastY = 300;
	private static float yaw = -90.0f;
	private static float pitch = 0.0f;
	private static boolean firstMouse = true;
	
	private static Vector3f cameraPos = new Vector3f(0.0f, 0.0f, 3.0f);
	private static Vector3f cameraFront = new Vector3f(0.0f, 0.0f, -1.0f);
	private static Vector3f cameraUp = new Vector3f(0.0f, 1.0f, 0.0f);
	
	private static void updateCameraVectors() {
		Vector3f direction = new Vector3f();
		direction.x = (float)(Math.cos(Math.toRadians(yaw)) * Math.cos(Math.toRadians(pitch)));
		direction.y = (float)(Math.sin(Math.toRadians(pitch)));
		direction.z = (float)(Math.sin(Math.toRadians(yaw)) * Math.cos(Math.toRadians(pitch)));
		
		cameraFront = direction.normalize();
	}
	
    public static void main(String[] args) {
    	String vertexShaderSource = loadResource("/shaders/scene.vert");
		String fragmentShaderSource = loadResource("/shaders/scene.frag");
    	
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
        glfwWindowHint(GLFW_OPENGL_PROFILE, GLFW_OPENGL_CORE_PROFILE);
        
        glfwSwapInterval(1);


        // 3. Create the window
        long window = glfwCreateWindow(800, 600, "Voxel Engine Test", NULL, NULL);
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
        
        int shaderProgram = createShaderProgram(vertexShaderSource, fragmentShaderSource);
        
        int modelLoc = glGetUniformLocation(shaderProgram, "model");
        int projLoc = glGetUniformLocation(shaderProgram, "projection");
        int viewLoc = glGetUniformLocation(shaderProgram, "view");
        int texLoc = glGetUniformLocation(shaderProgram, "textureArray");
        
        // 5. Show the window
        glfwShowWindow(window);
        
        Matrix4f projection = new Matrix4f().perspective((float)Math.toRadians(45.0f), 800.0f/600.0f, 0.1f, 500.0f);
    	Matrix4f model = new Matrix4f();
 
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
    		
    		float sensitivity = 0.1f;
    		xOffset *= sensitivity;
    		yOffset *= sensitivity;
    		
    		yaw += xOffset;
    		pitch += yOffset;
    		
    		if (pitch > 89.0f) pitch = 89.0f;
    		if (pitch < -89.0f) pitch = -89.0f;
    		
    		updateCameraVectors();
    	});
    	
    	System.out.println(GL11.glGetString(GL11.GL_RENDERER));
    	System.out.println(GL11.glGetString(GL11.GL_VENDOR));
    	World mainWorld = new World();
    	
    	Texture atlas = new Texture("textures");
    	//glPolygonMode(GL_FRONT_AND_BACK, GL_LINE);
    	
    	double lastFrameTime = 0.0;
        while (!glfwWindowShouldClose(window)) {
        	double currentFrameTime = glfwGetTime();
        	float deltaTime = (float)(currentFrameTime - lastFrameTime);
        	lastFrameTime = currentFrameTime;
        	
        	glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);


        	Matrix4f view = new Matrix4f().lookAt(
        			cameraPos,
        			new Vector3f(cameraPos).add(cameraFront),
        			cameraUp
        	);
        	
        	mainWorld.update(cameraPos);
        	
        	glUseProgram(shaderProgram);
        	glActiveTexture(GL_TEXTURE0);
        	atlas.bind();
        	
        	try (MemoryStack stack = stackPush()) {
        		FloatBuffer matrixBuffer = stack.mallocFloat(16); // Reuse buffer
        		
        		glUniformMatrix4fv(projLoc, false, projection.get(matrixBuffer));
        		glUniformMatrix4fv(viewLoc, false, view.get(matrixBuffer)); 
        		
        		for (ChunkColumn chunkMain : mainWorld.getLoaded().values()) {
        			if (chunkMain == null) continue;
        			
	        		for (int s = 0; s < 16; s++) {
	        			ChunkSection section = chunkMain.getSection(s);
	        			if (section == null) continue;
	        			
	        			float worldX = chunkMain.getWorldX() * 16;
	        			float worldY = s * 16;
	        			float worldZ = chunkMain.getWorldZ() * 16;
	        			
	        			model.translation(new Vector3f(worldX, worldY, worldZ));
	        			glUniformMatrix4fv(modelLoc, false, model.get(matrixBuffer));
	        			
	        			section.getMesh().render();
	        		}
        		}
        	}
        	
        	glUniform1i(texLoc, 0);
        	float newCamSpeed = 25*deltaTime;
        	if (glfwGetKey(window, GLFW_KEY_W) == GLFW_PRESS)
        		cameraPos.add(new Vector3f(cameraFront).mul(newCamSpeed));
        	if (glfwGetKey(window, GLFW_KEY_S) == GLFW_PRESS)
        		cameraPos.sub(new Vector3f(cameraFront).mul(newCamSpeed));
        	if (glfwGetKey(window, GLFW_KEY_A) == GLFW_PRESS)
        		cameraPos.sub(new Vector3f(cameraFront).cross(cameraUp).normalize().mul(newCamSpeed));
        	if (glfwGetKey(window, GLFW_KEY_D) == GLFW_PRESS)
        		cameraPos.add(new Vector3f(cameraFront).cross(cameraUp).normalize().mul(newCamSpeed));
        	
        	glfwSwapBuffers(window);
        	glfwPollEvents();
        }


        glfwTerminate();
    }
}




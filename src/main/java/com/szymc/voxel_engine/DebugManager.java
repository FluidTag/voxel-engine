package com.szymc.voxel_engine;


import static org.lwjgl.opengl.GL11.GL_FLOAT;
import static org.lwjgl.opengl.GL11.GL_LINES;
import static org.lwjgl.opengl.GL11.GL_UNSIGNED_INT;
import static org.lwjgl.opengl.GL11.glDrawElements;
import static org.lwjgl.opengl.GL11.glLineWidth;
import static org.lwjgl.opengl.GL15.GL_ARRAY_BUFFER;
import static org.lwjgl.opengl.GL15.GL_ELEMENT_ARRAY_BUFFER;
import static org.lwjgl.opengl.GL15.GL_STATIC_DRAW;
import static org.lwjgl.opengl.GL15.glBindBuffer;
import static org.lwjgl.opengl.GL15.glBufferData;
import static org.lwjgl.opengl.GL15.glGenBuffers;
import static org.lwjgl.opengl.GL20.glEnableVertexAttribArray;
import static org.lwjgl.opengl.GL20.glVertexAttribPointer;
import static org.lwjgl.opengl.GL30.glBindVertexArray;
import static org.lwjgl.opengl.GL30.glGenVertexArrays;


import java.nio.FloatBuffer;
import java.nio.IntBuffer;


import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.lwjgl.BufferUtils;


import com.szymc.localShaders.DebugShader;


public class DebugManager {
	// 8 vertices representing the corners of a 32x16x32 box
	private float[] debugVertices = {
	    0.0f,  0.0f,  0.0f,  // 0: Bottom-Left-Front
	   32.0f,  0.0f,  0.0f,  // 1: Bottom-Right-Front
	   32.0f,  0.0f, 32.0f,  // 2: Bottom-Right-Back
	    0.0f,  0.0f, 32.0f,  // 3: Bottom-Left-Back
	    0.0f, 16.0f,  0.0f,  // 4: Top-Left-Front
	   32.0f, 16.0f,  0.0f,  // 5: Top-Right-Front
	   32.0f, 16.0f, 32.0f,  // 6: Top-Right-Back
	    0.0f, 16.0f, 32.0f   // 7: Top-Left-Back
	};


	// Indices to draw the 12 edges of the cube using GL_LINES
	private int[] debugIndices = {
	    0, 1,  1, 2,  2, 3,  3, 0, // Bottom ring
	    4, 5,  5, 6,  6, 7,  7, 4, // Top ring
	    0, 4,  1, 5,  2, 6,  3, 7  // Vertical pillars
	};
	
	private int debugVao;
	private World worldReference;
	private Camera cameraReference;
	private DebugShader debugShader;
	
	public DebugManager(World worldReference, Camera camera) {
		this.cameraReference = camera;
		this.worldReference = worldReference;
		this.debugShader = new DebugShader();
		
		FloatBuffer vertexBuffer = BufferUtils.createFloatBuffer(debugVertices.length);
		vertexBuffer.put(debugVertices).flip();
		
		IntBuffer indexBuffer = BufferUtils.createIntBuffer(debugIndices.length);
		indexBuffer.put(debugIndices).flip();
		
		debugVao = glGenVertexArrays();
		glBindVertexArray(debugVao);
		
		int debugVbo = glGenBuffers();
		glBindBuffer(GL_ARRAY_BUFFER, debugVbo);
		glBufferData(GL_ARRAY_BUFFER, vertexBuffer, GL_STATIC_DRAW);
		
		glVertexAttribPointer(0, 3, GL_FLOAT, false, 0, 0);
		glEnableVertexAttribArray(0);
		
		int debugEbo = glGenBuffers();
		glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, debugEbo);
		glBufferData(GL_ELEMENT_ARRAY_BUFFER, indexBuffer, GL_STATIC_DRAW);
		
		glBindVertexArray(0);
		glBindBuffer(GL_ARRAY_BUFFER, 0);
	}
	
	private Vector3f tempModel = new Vector3f();
	private Matrix4f modelVec = new Matrix4f();
	public void renderDebug(FloatBuffer matrixBuffer) {
		glBindVertexArray(debugVao); 
		debugShader.start();
		debugShader.setCamera(cameraReference.getProjectionMatrix(), cameraReference.getViewMatrix(), matrixBuffer);
		debugShader.setColor(1.0f, 0.0f, 0.0f);
		
		for (ChunkColumn chunk : worldReference.getRendered().values()) {
			if (chunk == null) continue;
			
			for (int s = 0; s < 16; s++) {
				ChunkSection section = chunk.getSection(s);
				if (section == null) continue;
				if (section.getMesh() == null) continue;
				
				int minX = section.getWorldX();
    			int minY = section.getWorldY();
    			int minZ = section.getWorldZ();
    			int maxX = section.getWorldX() + 32;
    			int maxY = section.getWorldY() + 16;
    			int maxZ = section.getWorldZ() + 32;


    			if (!cameraReference.frustumInt.testAab(minX, minY, minZ, maxX, maxY, maxZ)) {
    				continue;
    			}
    			
    			float worldX = minX;
    			float worldY = minY;
    			float worldZ = minZ;
    			
    			tempModel.set(worldX, worldY, worldZ);
    			modelVec.translation(tempModel);
    			debugShader.setModel(modelVec, matrixBuffer);
    			
    			glLineWidth(2);
    			glDrawElements(GL_LINES, 24, GL_UNSIGNED_INT, 0);
			}
		}
		
		debugShader.stop();
	}
}
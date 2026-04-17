package com.szymc.voxel_engine;
import org.lwjgl.BufferUtils;
import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL15.*; // VBO functions (glGenBuffers)
import static org.lwjgl.opengl.GL20.*; // Shader/Attribute functions (glVertexAttribPointer)
import static org.lwjgl.opengl.GL30.*; // VAO functions (glGenVertexArrays)

import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.ArrayList;
import java.util.List;
import org.joml.Vector3f;

public class Mesh {
	private int vao, vbo, ebo, indexCount;
	
	public Mesh(float[] verticies, int[] indicies) {
		this.indexCount = indicies.length;
		
		vao = glGenVertexArrays();
		glBindVertexArray(vao);
		
		vbo = glGenBuffers();
		ebo = glGenBuffers();
		
    	FloatBuffer vertexBuffer = BufferUtils.createFloatBuffer(verticies.length);
    	vertexBuffer.put(verticies).flip();
    	
    	glBindBuffer(GL_ARRAY_BUFFER, vbo);
    	glBufferData(GL_ARRAY_BUFFER, vertexBuffer, GL_STATIC_DRAW);
    	
    	IntBuffer indexBuffer = BufferUtils.createIntBuffer(indicies.length);
    	indexBuffer.put(indicies).flip();
    	
    	glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, ebo);
    	glBufferData(GL_ELEMENT_ARRAY_BUFFER, indexBuffer, GL_STATIC_DRAW);
		
		// Position
		int stride = 6 * Float.BYTES; // 6 floats per vertex
		glVertexAttribPointer(0, 3, GL_FLOAT, false, stride, 0);
		glEnableVertexAttribArray(0);
		
		// Texture
		glVertexAttribPointer(1, 3, GL_FLOAT, false, stride, 3*Float.BYTES);
		glEnableVertexAttribArray(1);
		
		glBindVertexArray(0);
	}
	
	public void render() {
		glBindVertexArray(vao);
		glDrawElements(GL_TRIANGLES, indexCount, GL_UNSIGNED_INT, 0);
		glBindVertexArray(0);
	}
	
	public void cleanup() {
		glDeleteVertexArrays(vao);
		glDeleteBuffers(vbo);
		glDeleteBuffers(ebo);
	}
}




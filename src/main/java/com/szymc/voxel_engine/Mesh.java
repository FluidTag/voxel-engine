package com.szymc.voxel_engine;
import org.lwjgl.BufferUtils;
import org.lwjgl.system.MemoryUtil;


import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL15.*; // VBO functions (glGenBuffers)
import static org.lwjgl.opengl.GL20.*; // Shader/Attribute functions (glVertexAttribPointer)
import static org.lwjgl.opengl.GL30.*; // VAO functions (glGenVertexArrays)


import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.joml.Vector3f;


public class Mesh {
	private int vao, vbo, ebo, indexCount;

	// Only used for staging, uploads are only on the main thread, this is fine
	private static IntBuffer vertBuf = MemoryUtil.memAllocInt(1024*1024);
	private static IntBuffer intBuf = MemoryUtil.memAllocInt(1024*1024);

	private int numVerts;
	private int numIndices;

	private int selfAllocatedVertSize;
	private int selfAllocatedIndexSize;

	public Mesh(int[] verticies, int[] indicies, int numVerts, int numIndices) {
		this.indexCount = numIndices;
		this.numVerts = numVerts;
		this.numIndices = numIndices;

		this.selfAllocatedVertSize = numVerts;
		this.selfAllocatedIndexSize = numIndices;
		
		if (numVerts > vertBuf.capacity()) {
			MemoryUtil.memFree(vertBuf);
			vertBuf = MemoryUtil.memAllocInt(numVerts * 2);
		}
		
		if (numIndices > intBuf.capacity()) {
			MemoryUtil.memFree(intBuf);
			intBuf = MemoryUtil.memAllocInt(numIndices * 2);
		}
		
		vertBuf.clear();
		vertBuf.put(verticies, 0, numVerts);
		vertBuf.flip();
		
		intBuf.clear();
		intBuf.put(indicies, 0, numIndices);
		intBuf.flip();
		
		vao = glGenVertexArrays();
		glBindVertexArray(vao);
		
		vbo = glGenBuffers();
		glBindBuffer(GL_ARRAY_BUFFER, vbo);
		glBufferData(GL_ARRAY_BUFFER, vertBuf, GL_DYNAMIC_DRAW);
		
		ebo = glGenBuffers();
		glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, ebo);
		glBufferData(GL_ELEMENT_ARRAY_BUFFER, intBuf, GL_DYNAMIC_DRAW);
		
		// 18 for position
		// 6 bits posX
		// 6 bits posY
		// 6 bits posZ
		
		// int 2:
		// 8 bits for block type
		// 6 bits for uv width
		// 6 bits for uv height
		
		glVertexAttribIPointer(0, 2, GL_INT, 2*Integer.BYTES, 0);
		glEnableVertexAttribArray(0);
		
		glBindVertexArray(0);
	}

	public void updateMesh(int[] newVerts, int[] newInidices, int newVCount, int newICount) {
		// Setup staging/upload buffers
		// They wont grow more than 2x on an update
		if (newVCount > vertBuf.capacity()) {
			int expandedSize = vertBuf.capacity()*2;
			while (newVCount > expandedSize) expandedSize*=2;

			MemoryUtil.memFree(vertBuf);
			vertBuf = MemoryUtil.memAllocInt(expandedSize);
		}

		if (newICount > intBuf.capacity()) {
			int expandedSize = intBuf.capacity()*2;
			while (newICount > expandedSize) expandedSize*=2;

			MemoryUtil.memFree(intBuf);
			intBuf = MemoryUtil.memAllocInt(expandedSize);
		}

		glBindVertexArray(vao);

		glBindBuffer(GL_ARRAY_BUFFER, vbo);
		vertBuf.clear();
		vertBuf.put(newVerts, 0, newVCount);
		vertBuf.flip();
		if (newVCount > this.selfAllocatedVertSize) {
			while (newVCount > this.selfAllocatedVertSize) this.selfAllocatedVertSize *= 2;

			glBufferData(GL_ARRAY_BUFFER, this.selfAllocatedVertSize * 4L, GL_DYNAMIC_DRAW);
		}

		glBufferData(GL_ARRAY_BUFFER, (long) this.selfAllocatedVertSize * Integer.BYTES, GL_DYNAMIC_DRAW); // Orphan Buffer
		glBufferSubData(GL_ARRAY_BUFFER, 0, vertBuf);

		glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, ebo);
		intBuf.clear();
		intBuf.put(newInidices, 0, newICount);
		intBuf.flip();
		if (newICount > this.selfAllocatedIndexSize) {
			while (newICount > this.selfAllocatedIndexSize) this.selfAllocatedIndexSize *= 2;

			glBufferData(GL_ELEMENT_ARRAY_BUFFER, this.selfAllocatedIndexSize * 4L, GL_DYNAMIC_DRAW);
		}

		glBufferData(GL_ELEMENT_ARRAY_BUFFER, (long) this.selfAllocatedIndexSize * Integer.BYTES, GL_DYNAMIC_DRAW);
		glBufferSubData(GL_ELEMENT_ARRAY_BUFFER, 0, intBuf);

		this.indexCount = newICount;
		this.numVerts = newVCount;
		this.numIndices = newICount;
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
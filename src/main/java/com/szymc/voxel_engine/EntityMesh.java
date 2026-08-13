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
import java.util.List;
import org.joml.Vector3f;


public class EntityMesh {
    private int vao, vbo, ebo, indexCount;
    private static FloatBuffer vertBuf = MemoryUtil.memAllocFloat(1024*1024);
    private static IntBuffer intBuf = MemoryUtil.memAllocInt(1024*1024);

    public EntityMesh(float[] verticies, int[] indicies, int numVerts, int numIndicies) {
        this.indexCount = numIndicies;

        if (numVerts > vertBuf.capacity()) {
            MemoryUtil.memFree(vertBuf);
            vertBuf = MemoryUtil.memAllocFloat(numVerts * 2);
        }

        if (numIndicies > intBuf.capacity()) {
            MemoryUtil.memFree(intBuf);
            intBuf = MemoryUtil.memAllocInt(numIndicies * 2);
        }

        vertBuf.clear();
        vertBuf.put(verticies, 0, numVerts);
        vertBuf.flip();

        intBuf.clear();
        intBuf.put(indicies, 0, numIndicies);
        intBuf.flip();

        vao = glGenVertexArrays();
        glBindVertexArray(vao);

        vbo = glGenBuffers();
        glBindBuffer(GL_ARRAY_BUFFER, vbo);
        glBufferData(GL_ARRAY_BUFFER, vertBuf, GL_STATIC_DRAW);

        ebo = glGenBuffers();
        glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, ebo);
        glBufferData(GL_ELEMENT_ARRAY_BUFFER, intBuf, GL_STATIC_DRAW);

        // 18 for position
        // 32 bits posX
        // 32 bits posY
        // 32 bits posZ
        // 32 bits uvInfo

        int stride = (3*Float.BYTES) + (1*Integer.BYTES);
        glVertexAttribPointer(0, 3, GL_FLOAT, false, stride, 0L);
        glVertexAttribIPointer(1, 1, GL_INT, stride, 3*Float.BYTES);
        glEnableVertexAttribArray(0);
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
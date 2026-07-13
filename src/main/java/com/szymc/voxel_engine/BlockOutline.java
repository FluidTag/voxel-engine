package com.szymc.voxel_engine;

import static org.lwjgl.opengl.GL15.*;
import static org.lwjgl.opengl.GL20.*;
import static org.lwjgl.opengl.GL30.*;

public class BlockOutline {
    private int vao;

    private float[] vertices = {
            0.0f,  0.0f,  0.0f,  // 0
            1.0f,  0.0f,  0.0f,  // 1
            1.0f,  0.0f,  1.0f,  // 2
            0.0f,  0.0f,  1.0f,  // 3
            0.0f,  1.0f,  0.0f,  // 4
            1.0f,  1.0f,  0.0f,  // 5
            1.0f,  1.0f,  1.0f,  // 6
            0.0f,  1.0f,  1.0f   // 7
    };

    private int[] lineIndices = {
            0, 1,  1, 2,  2, 3,  3, 0, // Bottom ring
            4, 5,  5, 6,  6, 7,  7, 4, // Top ring
            0, 4,  1, 5,  2, 6,  3, 7  // Vertical pillars
    };

    public BlockOutline() {
        vao = glGenVertexArrays();
        glBindVertexArray(vao);

        int vbo = glGenBuffers();
        glBindBuffer(GL_ARRAY_BUFFER, vbo);
        glBufferData(GL_ARRAY_BUFFER, vertices, GL_STATIC_DRAW);
        glVertexAttribPointer(0, 3, GL_FLOAT, false, 0, 0);
        glEnableVertexAttribArray(0);

        int ebo = glGenBuffers();
        glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, ebo);
        glBufferData(GL_ELEMENT_ARRAY_BUFFER, lineIndices, GL_STATIC_DRAW);

        glBindVertexArray(0);
    }

    public int getVao() { return vao; }
}
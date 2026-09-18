package com.szymc.voxel_engine;

import static org.lwjgl.opengl.GL15.*;
import static org.lwjgl.opengl.GL20.*;
import static org.lwjgl.opengl.GL30.*;

public class BlockOutline {
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

    private float[] texturedBoxVertices = {
            // Position           // UV
            // Front Face
            0.0f, 0.0f, 1.0f,     0.0f, 0.0f, // 0: Bottom-Left
            1.0f, 0.0f, 1.0f,     1.0f, 0.0f, // 1: Bottom-Right
            1.0f, 1.0f, 1.0f,     1.0f, 1.0f, // 2: Top-Right
            0.0f, 1.0f, 1.0f,     0.0f, 1.0f, // 3: Top-Left

            // Back Face
            1.0f, 0.0f, 0.0f,     0.0f, 0.0f, // 4: Bottom-Left
            0.0f, 0.0f, 0.0f,     1.0f, 0.0f, // 5: Bottom-Right
            0.0f, 1.0f, 0.0f,     1.0f, 1.0f, // 6: Top-Right
            1.0f, 1.0f, 0.0f,     0.0f, 1.0f, // 7: Top-Left

            // Top Face
            0.0f, 1.0f, 1.0f,     0.0f, 0.0f, // 8: Bottom-Left
            1.0f, 1.0f, 1.0f,     1.0f, 0.0f, // 9: Bottom-Right
            1.0f, 1.0f, 0.0f,     1.0f, 1.0f, // 10: Top-Right
            0.0f, 1.0f, 0.0f,     0.0f, 1.0f, // 11: Top-Left

            // Bottom Face
            0.0f, 0.0f, 0.0f,     0.0f, 0.0f, // 12: Bottom-Left
            1.0f, 0.0f, 0.0f,     1.0f, 0.0f, // 13: Bottom-Right
            1.0f, 0.0f, 1.0f,     1.0f, 1.0f, // 14: Top-Right
            0.0f, 0.0f, 1.0f,     0.0f, 1.0f, // 15: Top-Left

            // Right Face
            1.0f, 0.0f, 1.0f,     0.0f, 0.0f, // 16: Bottom-Left
            1.0f, 0.0f, 0.0f,     1.0f, 0.0f, // 17: Bottom-Right
            1.0f, 1.0f, 0.0f,     1.0f, 1.0f, // 18: Top-Right
            1.0f, 1.0f, 1.0f,     0.0f, 1.0f, // 19: Top-Left

            // Left Face
            0.0f, 0.0f, 0.0f,     0.0f, 0.0f, // 20: Bottom-Left
            0.0f, 0.0f, 1.0f,     1.0f, 0.0f, // 21: Bottom-Right
            0.0f, 1.0f, 1.0f,     1.0f, 1.0f, // 22: Top-Right
            0.0f, 1.0f, 0.0f,     0.0f, 1.0f  // 23: Top-Left
    };

    private int[] triangleIndices = {
            // Front face
            0, 1, 2,   2, 3, 0,
            // Back face
            4, 5, 6,   6, 7, 4,
            // Top face
            8, 9, 10,  10, 11, 8,
            // Bottom face
            12, 13, 14, 14, 15, 12,
            // Right face
            16, 17, 18, 18, 19, 16,
            // Left face
            20, 21, 22, 22, 23, 20
    };

    private int lineVao;
    private int triangleVao;
    public BlockOutline() {
        lineVao = glGenVertexArrays();
        glBindVertexArray(lineVao);

        int vbo = glGenBuffers();
        glBindBuffer(GL_ARRAY_BUFFER, vbo);
        glBufferData(GL_ARRAY_BUFFER, vertices, GL_STATIC_DRAW);
        glVertexAttribPointer(0, 3, GL_FLOAT, false, 0, 0);
        glEnableVertexAttribArray(0);

        int ebo = glGenBuffers();
        glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, ebo);
        glBufferData(GL_ELEMENT_ARRAY_BUFFER, lineIndices, GL_STATIC_DRAW);

        triangleVao = glGenVertexArrays();
        glBindVertexArray(triangleVao);

        int tVbo = glGenBuffers();
        glBindBuffer(GL_ARRAY_BUFFER, tVbo);
        glBufferData(GL_ARRAY_BUFFER, texturedBoxVertices, GL_STATIC_DRAW);

        int tEbo = glGenBuffers();
        glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, tEbo);
        glBufferData(GL_ELEMENT_ARRAY_BUFFER, triangleIndices, GL_STATIC_DRAW);

        int stride = 5*Float.BYTES;
        glVertexAttribPointer(0, 3, GL_FLOAT, false, stride, 0L);
        glVertexAttribPointer(1, 2, GL_FLOAT, false, stride, (long)3*Float.BYTES);
        glEnableVertexAttribArray(0);
        glEnableVertexAttribArray(1);

        glBindVertexArray(0);
    }

    public int getLineVao() { return lineVao; }
    public int getTriangleVao() {return triangleVao;}
}
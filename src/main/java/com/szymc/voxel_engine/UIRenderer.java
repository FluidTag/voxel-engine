package com.szymc.voxel_engine;
import com.szymc.localShaders.UIShader;
import org.joml.Matrix4f;
import org.lwjgl.BufferUtils;
import java.nio.FloatBuffer;
import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL13.*;
import static org.lwjgl.opengl.GL15.*;
import static org.lwjgl.opengl.GL20.*;
import static org.lwjgl.opengl.GL30.*;

public class UIRenderer {
    private final int vao;
    private final int programId;
    private final int locProjection, locTransform, locColorTint, locUseTexture;
    private final FloatBuffer matrixBuffer = BufferUtils.createFloatBuffer(16);

    public UIRenderer() {
        this.programId = compileUIShaders();

        locProjection = glGetUniformLocation(programId, "projection");
        locTransform = glGetUniformLocation(programId, "transform");
        locColorTint = glGetUniformLocation(programId, "colorTint");
        locUseTexture = glGetUniformLocation(programId, "useTexture");

        float[] vertices = {
                0.0f, 0.0f, 0.0f, 0.0f,
                1.0f, 0.0f, 1.0f, 0.0f,
                1.0f, 1.0f, 1.0f, 1.0f,
                0.0f, 1.0f, 0.0f, 1.0f
        };
        int[] indices = {0, 1, 2, 2, 3, 0};

        vao = glGenVertexArrays();
        glBindVertexArray(vao);

        int vbo = glGenBuffers();
        glBindBuffer(GL_ARRAY_BUFFER, vbo);
        glBufferData(GL_ARRAY_BUFFER, vertices, GL_STATIC_DRAW);

        int ebo = glGenBuffers();
        glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, ebo);
        glBufferData(GL_ELEMENT_ARRAY_BUFFER, indices, GL_STATIC_DRAW);

        glEnableVertexAttribArray(0);
        glVertexAttribPointer(0, 2, GL_FLOAT, false, 4 * Float.BYTES, 0);
        glEnableVertexAttribArray(1);
        glVertexAttribPointer(1, 2, GL_FLOAT, false, 4 * Float.BYTES, 2 * Float.BYTES);

        glBindVertexArray(0);
        glBindBuffer(GL_ARRAY_BUFFER, 0);
        glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, 0);
    }

    public void begin(int windowWidth, int windowHeight) {
        glDisable(GL_DEPTH_TEST);
        glDisable(GL_CULL_FACE);
        glEnable(GL_BLEND);
        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);

        glUseProgram(programId);
        int locImage = glGetUniformLocation(programId, "uiTexture");
        if (locImage != -1) {
            glUniform1i(locImage, 2);
        }

        Matrix4f ortho = new Matrix4f().ortho2D(0, windowWidth, windowHeight, 0);
        matrixBuffer.clear();
        glUniformMatrix4fv(locProjection, false, ortho.get(matrixBuffer));

        glBindVertexArray(vao);
    }

    public void drawTexture(int textureId, float x, float y, float width, float height) {
        glUniform1i(locUseTexture, 1);
        glUniform4f(locTransform, x, y, width, height);
        glUniform4f(locColorTint, 1.0f, 1.0f, 1.0f, 1.0f);

        glActiveTexture(GL_TEXTURE2);
        glBindTexture(GL_TEXTURE_2D, textureId);

        glDrawElements(GL_TRIANGLES, 6, GL_UNSIGNED_INT, 0);
    }

    public void drawRect(float x, float y, float z, float height, float width, float r, float g, float b, float a) {
        glUniform1i(locUseTexture, 0);
        glUniform4f(locTransform, x, y, width, height);
        glUniform4f(locColorTint, r, g, b, a);

        glDrawElements(GL_TRIANGLES, 6, GL_UNSIGNED_INT, 0);
    }

    public void end() {
        glBindVertexArray(0);

        glActiveTexture(GL_TEXTURE2);
        glBindTexture(GL_TEXTURE_2D, 0);
        glActiveTexture(GL_TEXTURE0);

        glUseProgram(0);
        glEnable(GL_DEPTH_TEST);
        glEnable(GL_CULL_FACE);
    }

    public int compileUIShaders() {
        UIShader shader = new UIShader();
        return shader.getProgramID();
    }
}
package com.szymc.localShaders;

import java.nio.FloatBuffer;

import org.joml.Matrix4f;

import static org.lwjgl.opengl.GL20.*;

public class OutlineShader extends Shader {
    private int local_projection, local_view, local_model, local_color, local_renderMode, local_breakTexId;

    public OutlineShader() {
        super("/shaders/outline.vert", "/shaders/outline.frag");

        this.local_projection = glGetUniformLocation(this.programId, "projection");
        this.local_view = glGetUniformLocation(this.programId, "view");
        this.local_model = glGetUniformLocation(this.programId, "model");
        this.local_color = glGetUniformLocation(this.programId, "outlineColor");
        this.local_renderMode = glGetUniformLocation(this.programId, "drawFace");
        this.local_breakTexId = glGetUniformLocation(this.programId, "textureId");

        this.start();
        glUniform1i(glGetUniformLocation(this.programId, "textureArray"), 0);
        setLocal_breakTexId(9);
        this.stop();
    }

    public void setColor(float r, float g, float b) {
        glUniform3f(local_color, r, g, b);
    }

    public void setCamera(Matrix4f proj, Matrix4f view, FloatBuffer buffer) {
        this.setMatrix(local_projection, proj, buffer);
        this.setMatrix(local_view, view, buffer);
    }

    public void setModel(Matrix4f model, FloatBuffer buffer) {
        this.setMatrix(local_model, model, buffer);
    }

    public void setIsRenderingFace(boolean state) {
        glUniform1i(local_renderMode, state ? 1 : 0);
    }

    public void setLocal_breakTexId(int id) {
        glUniform1i(local_breakTexId, id);
    }
}
package com.szymc.localShaders;

import com.szymc.voxel_engine.Texture;
import org.joml.Matrix4f;

import java.nio.FloatBuffer;

import static org.lwjgl.opengl.GL13.GL_TEXTURE0;
import static org.lwjgl.opengl.GL13.glActiveTexture;
import static org.lwjgl.opengl.GL20.glGetUniformLocation;
import static org.lwjgl.opengl.GL20.glUniform1i;

public class EntityShader extends Shader {
    private int local_projection, local_view, local_model, local_textureArray;
    private Texture tex;
    public EntityShader(Texture tex) {
        super("/shaders/entityShader.vert", "/shaders/scene.frag");

        this.local_projection = glGetUniformLocation(this.programId, "projection");
        this.local_view = glGetUniformLocation(this.programId, "view");
        this.local_model = glGetUniformLocation(this.programId, "model");
        this.local_textureArray = glGetUniformLocation(this.programId, "textureArray");
        this.tex = tex;
    }

    @Override
    public void start() {
        super.start();
        if (tex != null) {
            glActiveTexture(GL_TEXTURE0);
            tex.bind(0);
        }
    }

    public Texture getTexture() {
        return tex;
    }

    public void setCamera(Matrix4f proj, Matrix4f view, FloatBuffer buffer) {
        this.setMatrix(local_projection, proj, buffer);
        this.setMatrix(local_view, view, buffer);
    }

    public void setModel(Matrix4f model, FloatBuffer buffer) {
        this.setMatrix(local_model, model, buffer);
    }
}

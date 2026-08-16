package com.szymc.localShaders;

import java.nio.FloatBuffer;

import com.szymc.voxel_engine.Texture;

// VBO functions (glGenBuffers)
import static org.lwjgl.opengl.GL20.*; // Shader/Attribute functions (glVertexAttribPointer)
// VAO functions (glGenVertexArrays)

import org.joml.Matrix4f;

public class WorldShader extends Shader {
	private int local_projection, local_view, local_model, local_textureArray;
	private Texture tex;
	public WorldShader() {
		super("/shaders/scene.vert", "/shaders/scene.frag");
		
		this.local_projection = glGetUniformLocation(this.programId, "projection");
		this.local_view = glGetUniformLocation(this.programId, "view");
		this.local_model = glGetUniformLocation(this.programId, "model");
		this.local_textureArray = glGetUniformLocation(this.programId, "textureArray");
	
		// Temporarily start to set texture uniform (constant)
		this.start();
		tex = new Texture("textures", 5);
		
		glUniform1i(local_textureArray, 0);
		tex.bind(0);
		this.stop();
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
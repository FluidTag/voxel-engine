package com.szymc.localShaders;

import static org.lwjgl.opengl.GL20.glGetUniformLocation;
import java.nio.FloatBuffer;

import org.joml.Matrix4f;
import static org.lwjgl.opengl.GL20.glUniform3f;

public class DebugShader extends Shader {
	private int local_projection, local_view, local_model, local_color;
	
	public DebugShader() {
		super("/shaders/debugColor.vert", "/shaders/debugColor.frag");
		
		this.local_projection = glGetUniformLocation(this.programId, "projection");
		this.local_view = glGetUniformLocation(this.programId, "view");
		this.local_model = glGetUniformLocation(this.programId, "model");
		this.local_color = glGetUniformLocation(this.programId, "debugColor");
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
}
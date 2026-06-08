package com.szymc.localShaders;

import java.nio.FloatBuffer;
import org.lwjgl.opengl.GL;
import org.lwjgl.opengl.GL11;

import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL13.GL_TEXTURE0;
import static org.lwjgl.opengl.GL13.glActiveTexture;
import static org.lwjgl.system.MemoryUtil.NULL;
import org.lwjgl.system.MemoryStack;

import com.szymc.voxel_engine.Texture;

import static org.lwjgl.system.MemoryStack.*;

import static org.lwjgl.opengl.GL15.*; // VBO functions (glGenBuffers)
import static org.lwjgl.opengl.GL20.*; // Shader/Attribute functions (glVertexAttribPointer)
import static org.lwjgl.opengl.GL30.*; // VAO functions (glGenVertexArrays)

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.FloatBuffer;
import java.util.stream.Collectors;

import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.joml.FrustumIntersection;
import org.joml.Matrix4f;

public class WorldShader extends Shader {
	private int local_projection, local_view, local_model, local_textureArray;
	
	public WorldShader() {
		super("/shaders/scene.vert", "/shaders/scene.frag");
		
		this.local_projection = glGetUniformLocation(this.programId, "projection");
		this.local_view = glGetUniformLocation(this.programId, "view");
		this.local_model = glGetUniformLocation(this.programId, "model");
		this.local_textureArray = glGetUniformLocation(this.programId, "textureArray");
	
		// Temporarily start to set texture uniform (constant)
		this.start();
		Texture tex = new Texture("textures");
		glUniform1i(local_textureArray, 0);
		tex.bind(0);
		this.stop();
	}
	
	public void setCamera(Matrix4f proj, Matrix4f view, FloatBuffer buffer) {
		this.setMatrix(local_projection, proj, buffer);
		this.setMatrix(local_view, view, buffer);
	}
	
	public void setModel(Matrix4f model, FloatBuffer buffer) {
		this.setMatrix(local_model, model, buffer);
	}
}
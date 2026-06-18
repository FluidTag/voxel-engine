package com.szymc.localShaders;

import org.lwjgl.opengl.GL;
import org.lwjgl.opengl.GL11;

import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL13.GL_TEXTURE0;
import static org.lwjgl.opengl.GL13.glActiveTexture;
import static org.lwjgl.system.MemoryUtil.NULL;
import org.lwjgl.system.MemoryStack;
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

import com.szymc.voxel_engine.App;

public abstract class Shader {
	protected int programId;
	
	private String loadResource(String fileName) {
	    try (InputStream is = App.class.getResourceAsStream(fileName);		
	    	BufferedReader reader = new BufferedReader(new InputStreamReader(is))) {
	        
	    	return reader.lines().collect(Collectors.joining("\n"));
	    } catch (Exception e) {
	        throw new RuntimeException("Failed to load resource: " + fileName, e);
	    }
	}
	
	private int compileShader(int type, String source) {
		int shader = glCreateShader(type);
		glShaderSource(shader, source);
		glCompileShader(shader);
		
		if (glGetShaderi(shader, GL_COMPILE_STATUS) == GL_FALSE) {
        	throw new RuntimeException("Fragment Shader Failed: " + glGetShaderInfoLog(shader));
        }
		
		return shader;
	}
	
	public Shader(String vertexShaderSource, String fragmentShaderSource) {
		String vertexShaderCode = loadResource(vertexShaderSource);
		String fragmentShaderCode = loadResource(fragmentShaderSource);
		
		int program = glCreateProgram();
		int vs = compileShader(GL_VERTEX_SHADER, vertexShaderCode);
		int fs = compileShader(GL_FRAGMENT_SHADER, fragmentShaderCode);
		
		glAttachShader(program, vs);
		glAttachShader(program, fs);
		glLinkProgram(program);
    		
    	glDeleteShader(vs);
    	glDeleteShader(fs);
    	
    	this.programId = program;
	}
	
	public int getProgramID() {
		return this.programId;
	}
	
	public void start() {
		glUseProgram(programId);
	}
	
	public void stop() {
		glUseProgram(0);
	}
	
	protected void setMatrix(int location, Matrix4f matrix, FloatBuffer buffer) {
		glUniformMatrix4fv(location, false, matrix.get(buffer));
	}
}
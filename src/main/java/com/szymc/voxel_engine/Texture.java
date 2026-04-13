package com.szymc.voxel_engine;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL;
import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.system.MemoryUtil.NULL;
import org.lwjgl.system.MemoryStack;
import static org.lwjgl.system.MemoryStack.*;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL30.glGenerateMipmap;
import static org.lwjgl.opengl.GL15.*; // VBO functions (glGenBuffers)
import static org.lwjgl.opengl.GL20.*; // Shader/Attribute functions (glVertexAttribPointer)
import static org.lwjgl.opengl.GL30.*; // VAO functions (glGenVertexArrays)

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.joml.Matrix4f;
import org.joml.Vector3f;

import static org.lwjgl.stb.STBImage.*;

public class Texture {
	private int id = 0;
	
	public static int[] getTextureCoords(byte blockType, String face) {
		switch (blockType) {
		case Blocks.GRASS:
			if (face.equals("TOP")) return new int[] {2,0};
			if (face.equals("SIDE")) return new int[] {1,0};
			return new int[] {0, 0};
		case Blocks.DIRT:
			return new int[] {0, 0};
		case Blocks.STONE:
			return new int[] {3, 0};
		default:
			return new int[] {0, 0};
		}
	}
	
	public Texture(String fileName) {
		int width, height;
		ByteBuffer data;
		
		try (MemoryStack stack = MemoryStack.stackPush()) {
			IntBuffer w = stack.mallocInt(1);
			IntBuffer h = stack.mallocInt(1);
			IntBuffer comp = stack.mallocInt(1);
			
			//stbi_set_flip_vertically_on_load(true);
			data = stbi_load(fileName, w, h, comp, 4);
			if (data == null) {
				throw new RuntimeException("Failed to load texture file: " + stbi_failure_reason());
			}
			
			width = w.get();
			height = h.get();
		}
		
		this.id = glGenTextures();
		glBindTexture(GL_TEXTURE_2D, id);
		
		glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_NEAREST);
		glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_NEAREST);
		glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_REPEAT);
		glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_REPEAT);
		
		glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA, width, height, 0, GL_RGBA, GL_UNSIGNED_BYTE, data);
		glGenerateMipmap(GL_TEXTURE_2D);
		
		stbi_image_free(data);
	}
	
	public void bind() {
		glBindTexture(GL_TEXTURE_2D, this.id);
	}
	
	public void cleanup() {
		glDeleteTextures(this.id);
	}
}

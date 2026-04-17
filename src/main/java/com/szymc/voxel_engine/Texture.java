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
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.joml.Matrix4f;
import org.joml.Vector3f;

import static org.lwjgl.stb.STBImage.*;

public class Texture {
	private int id = 0;
	
	public static int getTextureIndex(byte blockType, String face) {
		switch (blockType) {
		
		case Blocks.GRASS:
			if (face.equals("TOP")) return 2;
			if (face.equals("SIDE")) return 1;
			return 0;
		case Blocks.DIRT:
			return 0;
		case Blocks.STONE:
			return 5;
		case Blocks.WOOD:
			return 4;
		case Blocks.LEAVES:
			return 3;
		default:
			return 0;
		}
	}
	
	public Texture(String resourcePath) {
		List<String> textureFiles = new ArrayList<>();
		try {
			URL url = getClass().getClassLoader().getResource(resourcePath);
			if (url == null) throw new RuntimeException("Directory not found: " + resourcePath);
			
			Path path = Paths.get(url.toURI());
			try (Stream<Path> walk = Files.walk(path, 1)) {
				textureFiles = walk
						.filter(p -> p.toString().endsWith(".png"))
						.map(p -> p.getFileName().toString())
						.sorted()
						.collect(Collectors.toList());
			}
		} catch (Exception e) {
			throw new RuntimeException("Failed to load textures: ", e);
		}
		
		int tileSize = 16;
		int layerCount = textureFiles.size();
		
		this.id = glGenTextures();
		glBindTexture(GL_TEXTURE_2D_ARRAY, this.id);
		glTexImage3D(GL_TEXTURE_2D_ARRAY, 0, GL_RGBA, tileSize, tileSize, layerCount, 0, GL_RGBA, GL_UNSIGNED_BYTE, (ByteBuffer)null);
		
		for (int i = 0; i < layerCount; i++) {
			String fileName = resourcePath + "/" + textureFiles.get(i);
			uploadLayer(fileName, i, tileSize);
		}
		
		glTexParameteri(GL_TEXTURE_2D_ARRAY, GL_TEXTURE_MIN_FILTER, GL_NEAREST);
		glTexParameteri(GL_TEXTURE_2D_ARRAY, GL_TEXTURE_MAG_FILTER, GL_NEAREST);
		glTexParameteri(GL_TEXTURE_2D_ARRAY, GL_TEXTURE_WRAP_S, GL_REPEAT);
		glTexParameteri(GL_TEXTURE_2D_ARRAY, GL_TEXTURE_WRAP_T, GL_REPEAT);
	}
	
	private void uploadLayer(String path, int layer, int size) {
		try (MemoryStack stack = MemoryStack.stackPush()) {
			IntBuffer w = stack.mallocInt(1);
			IntBuffer h = stack.mallocInt(1);
			IntBuffer comp = stack.mallocInt(1);
			
			// Read bytes from classpath stream
			byte[] bytes = null;
			try (InputStream is = getClass().getResourceAsStream("/" + path)) {
				if (is == null) throw new RuntimeException("Could not find: " + path);
				bytes = is.readAllBytes();
			} catch (IOException e) {
				e.printStackTrace();
			}
			
			ByteBuffer buffer = stack.malloc(bytes.length);
			buffer.put(bytes);
			buffer.flip();
			
			ByteBuffer image = stbi_load_from_memory(buffer, w, h, comp, 4);
	        if (image == null) {
	            throw new RuntimeException("STB fail for " + path + ": " + stbi_failure_reason());
	        }


	        // Upload to the specific layer in your GL_TEXTURE_2D_ARRAY
	        glTexSubImage3D(GL_TEXTURE_2D_ARRAY, 0, 0, 0, layer, size, size, 1, GL_RGBA, GL_UNSIGNED_BYTE, image);
	        
	        stbi_image_free(image); // Important: STB memory must be freed
		}
	}
	
	public void bind() {
		glBindTexture(GL_TEXTURE_2D_ARRAY, this.id);
	}
	
	public void cleanup() {
		glDeleteTextures(this.id);
	}
}

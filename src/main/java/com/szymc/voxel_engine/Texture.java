package com.szymc.voxel_engine;
import static org.lwjgl.opengl.GL11.*;
import org.lwjgl.system.MemoryStack;
import static org.lwjgl.opengl.GL15.*;
import static org.lwjgl.opengl.GL30.*;
import static org.lwjgl.opengl.GL42.*; // Required for glTexStorage3D
import static org.lwjgl.opengl.EXTTextureFilterAnisotropic.*;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.lwjgl.stb.STBImage.*;


public class Texture {
	private int id = 0;
	
	public static int getTextureIndex(byte blockType, String face) {
		switch (blockType) {
		
		case Blocks.GRASS:
			if (face.equals("TOP")) return 11;
			if (face.equals("SIDE")) return 10;
			return 7;
		case Blocks.DIRT:
			return 7;
		case Blocks.STONE:
			return 26;
		case Blocks.OAK_WOOD:
			return 18;
		case Blocks.OAK_LEAVES:
			return 17;
		case Blocks.WATER:
			return 31;
		case Blocks.SAND:
			return 19;
		case Blocks.GRAVEL:
			return 12;
		case Blocks.BEDROCK:
			return 2;
		case Blocks.LAVA:	
			return 16;
		case Blocks.BIRCH_LEAVES:
			return 5;
		case Blocks.BIRCH_WOOD:
			return 6;
		case Blocks.SNOW:
			return 22;
		case Blocks.SAVANNA_GRASS:
			if (face.equals("TOP")) return 21;
			if (face.equals("SIDE")) return 20;
			
			return 7;
		case Blocks.JUNGLE_GRASS:
			if (face.equals("TOP")) return 15;
			if (face.equals("SIDE")) return 14;
			
			return 7;
		case Blocks.TAIGA_GRASS:
			if (face.equals("TOP")) return 28;
			if (face.equals("SIDE")) return 27;
			
			return 7;
		case Blocks.TUNDRA_GRASS:
			if (face.equals("TOP")) return 30;
			if (face.equals("SIDE")) return 29;
			
			return 7;
		case Blocks.BIRCH_GRASS:
			if (face.equals("TOP")) return 4;
			if (face.equals("SIDE")) return 3;
			
			return 7;
		case Blocks.FOREST_GRASS:
			if (face.equals("TOP")) return 9;
			if (face.equals("SIDE")) return 8;
			
			return 7;
		case Blocks.ICE:
			return 13;
		case Blocks.GRASS_DECORATION:
			return 32;
		case Blocks.SPRUCE_LEAVES:
			return 24;
		case Blocks.SPRUCE_LOG:
			return 25;
		case Blocks.ACACIA_LOG:
			return 1;
		case Blocks.ACACIA_LEAVES:
			return 0;
		case Blocks.SNOWY_SPRUCE_LEAVES:
			return 23;
		default:
			return 0;
		}
	}
	
	public Texture(String resourcePath, int mipLevels) {
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
		glTexStorage3D(GL_TEXTURE_2D_ARRAY, mipLevels, GL_RGBA8, tileSize, tileSize, layerCount);
		
		for (int i = 0; i < layerCount; i++) {
			String fileName = resourcePath + "/" + textureFiles.get(i);
			uploadLayer(fileName, i, tileSize);
		}
		
		glTexParameteri(GL_TEXTURE_2D_ARRAY, GL_TEXTURE_WRAP_S, GL_REPEAT);
		glTexParameteri(GL_TEXTURE_2D_ARRAY, GL_TEXTURE_WRAP_T, GL_REPEAT);
		
		// Keeps the pixels sharp within the level, but smoothly blends between mip levels
		glTexParameteri(GL_TEXTURE_2D_ARRAY, GL_TEXTURE_MIN_FILTER, GL_NEAREST_MIPMAP_LINEAR);
		glTexParameteri(GL_TEXTURE_2D_ARRAY, GL_TEXTURE_MAG_FILTER, GL_NEAREST);
		
		float maxAnisotropy =
			    glGetFloat(GL_MAX_TEXTURE_MAX_ANISOTROPY_EXT);

		glTexParameterf(
		    GL_TEXTURE_2D_ARRAY,
		    GL_TEXTURE_MAX_ANISOTROPY_EXT,
		    maxAnisotropy
		);
		
		glHint(GL_GENERATE_MIPMAP_HINT, GL_NICEST);

		//glTexParameterf(GL_TEXTURE_2D_ARRAY, GL_TEXTURE_LOD_BIAS, 1f);
		glGenerateMipmap(GL_TEXTURE_2D_ARRAY);
		
		glEnable(GL_BLEND);
		glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
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
	
	public void bind(int slot) {
		glActiveTexture(GL_TEXTURE0 + slot);
		glBindTexture(GL_TEXTURE_2D_ARRAY, this.id);
	}
	
	public void cleanup() {
		glDeleteTextures(this.id);
	}
}








package com.szymc.voxel_engine;
import static org.lwjgl.opengl.GL11.*;

import com.google.gson.Gson;
import org.lwjgl.BufferUtils;
import org.lwjgl.stb.STBImage;
import org.lwjgl.system.MemoryStack;
import static org.lwjgl.opengl.GL15.*;
import static org.lwjgl.opengl.GL30.*;
import static org.lwjgl.opengl.GL42.*; // Required for glTexStorage3D
import static org.lwjgl.opengl.EXTTextureFilterAnisotropic.*;

import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.lwjgl.stb.STBImage.*;

enum BLOCK_FACE {
	TOP,
	BOTTOM,
	WEST,
	EAST,
	NORTH,
	SOUTH,
}

public class Texture {
	private final int id;
	private static final HashMap<String, Integer> fileNameMap = new HashMap<>();
	private static final int[][] blockTextureArray = new int[256][BLOCK_FACE.values().length + 1];; // [blockId][faceOrdinal (first slot reserved for default)]
	public static void readBlockJson(String path) {
		Gson gson = new Gson();

		try (InputStream is = App.class.getClassLoader().getResourceAsStream(path)) {
			try (InputStreamReader reader = new InputStreamReader(is)) {
				Map<String, Map<String, ?>> data = gson.fromJson(reader, Map.class);

				data.forEach((key, subData) -> {
					Map<String, String> textures = (Map<String, String>) subData.get("textures");
					Arrays.fill(blockTextureArray[Integer.parseInt(key)], -1); // Indicates no texture unless specified (0 can be a texId)

					textures.forEach((faceName, texPath) -> {
						if (faceName.equals("DEFAULT")) {
							blockTextureArray[Integer.parseInt(key)][0] = fileNameMap.get(texPath);
							System.out.println("Default for " + subData.get("name") + ": " + texPath);
							return;
						}

						BLOCK_FACE face = BLOCK_FACE.valueOf(faceName);
						blockTextureArray[Integer.parseInt(key)][face.ordinal()+1] = fileNameMap.get(texPath);
						System.out.println(face + " for " + subData.get("name") + ": " + texPath);
					});
					System.out.println(subData.get("name") + ": " + Arrays.toString(blockTextureArray[Integer.parseInt(key)]));
				});
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public static int getTextureIndex(byte blockType, BLOCK_FACE face) {
		int val = blockTextureArray[blockType][face.ordinal()+1];
		System.out.println(face);
		return val != -1 ? val : blockTextureArray[blockType][0];
	}

	public static int loadTexturePath(String path) {
		IntBuffer width = BufferUtils.createIntBuffer(1);
		IntBuffer height = BufferUtils.createIntBuffer(1);
		IntBuffer channels = BufferUtils.createIntBuffer(1);

		STBImage.stbi_set_flip_vertically_on_load(true);
		ByteBuffer image = STBImage.stbi_load(path, width, height, channels, 4);
		STBImage.stbi_set_flip_vertically_on_load(false);

		if (image == null) throw new RuntimeException("Failed to load texture: " + STBImage.stbi_failure_reason());
		glActiveTexture(GL_TEXTURE3);
		int texture = glGenTextures();

		glBindTexture(GL_TEXTURE_2D, texture);

		glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA, width.get(0), height.get(0), 0, GL_RGBA, GL_UNSIGNED_BYTE, image);
		glGenerateMipmap(GL_TEXTURE_2D);

		glTexParameteri(GL_TEXTURE_2D,
				GL_TEXTURE_MIN_FILTER,
				GL_LINEAR_MIPMAP_LINEAR);

		glTexParameteri(GL_TEXTURE_2D,
				GL_TEXTURE_MAG_FILTER,
				GL_LINEAR);
		glBindTexture(GL_TEXTURE_2D, 0);
		glActiveTexture(GL_TEXTURE0);

		STBImage.stbi_image_free(image);

		return texture;
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
			fileNameMap.put(textureFiles.get(i), i);
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
	
	public int getId() {
		return this.id;
	}
	
	public void bind(int slot) {
		glActiveTexture(GL_TEXTURE0 + slot);
		glBindTexture(GL_TEXTURE_2D_ARRAY, this.id);
	}
	
	public void cleanup() {
		glDeleteTextures(this.id);
	}
}








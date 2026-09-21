package com.szymc.voxel_engine;
import static org.lwjgl.opengl.GL11.*;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import it.unimi.dsi.fastutil.objects.Object2ByteOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2ShortOpenHashMap;
import org.lwjgl.BufferUtils;
import org.lwjgl.stb.STBImage;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;

import static org.lwjgl.opengl.GL15.*;
import static org.lwjgl.opengl.GL30.*;
import static org.lwjgl.opengl.GL42.*; // Required for glTexStorage3D
import static org.lwjgl.opengl.EXTTextureFilterAnisotropic.*;

import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Type;
import java.net.URL;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import static org.lwjgl.opengl.GL45.*;

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
	// Used extremely hotly in meshing, rather use slightly more memory than have to unbitpack and read, instead simple array lookup
	public static final boolean[] isXShapedBlock = new boolean[256];
	public static final boolean[] isLeafBlock = new boolean[256];
	public static final int[] itemTexturePaths = new int[256];
	public static final byte[] lightLevels = new byte[256];

	public static final int[] breakStages = new int[8];
	public static final float[] hardnessLevels = new float[256];
	public static final boolean[] isItemOnly = new boolean[256];

	public static final byte[] idealToolType = new byte[256];
	public static final byte[] toolType = new byte[256];
	public static final float[] toolTargetedDamage = new float[256];

	public static final Object2ShortOpenHashMap<String> craftingRecipes = new Object2ShortOpenHashMap<>();

	public static void readInCraftingJson(String path) {
		Gson gson = new Gson();

		try (InputStream is = App.class.getClassLoader().getResourceAsStream(path)) {
			try (InputStreamReader reader = new InputStreamReader(is)) {
				Type mapType = new TypeToken<Map<String, Map<String, ?>>>() {}.getType();
				Map<String, Map<String, ?>> data = gson.fromJson(reader, mapType);
				StringBuilder resultBuilder = new StringBuilder();

				data.forEach((key, subData) -> {
					ArrayList<Object> listRecipe = (ArrayList<Object>)subData.get("recipe");
					int[] recipe = new int[9];
					for (int i = 0; i < listRecipe.size(); i++) {
						recipe[i] = ((Number) listRecipe.get(i)).intValue();
					}

					int amount = ((Number) subData.get("amount")).intValue();

					int blockKey = Integer.parseInt(key);

					int minX = 999; int maxX = -999;
					int minY = 999; int maxY = -999;
					boolean isEmpty = true;

					for (int y = 2; y >= 0; y--) {
						for (int x = 0; x < 3; x++) {
							int dat = recipe[y*3+x];
							if (dat != 0) {
								isEmpty = false;
								minX = Math.min(x, minX);
								maxX = Math.max(x, maxX);
								minY = Math.min(y, minY);
								maxY = Math.max(y, maxY);
							}
						}
					}

					if (isEmpty) {
						System.err.println("Error, crafting recipe for blockId=" + blockKey + " is empty.");
						return;
					}

					resultBuilder.setLength(0);
					for (int y = maxY; y >= minY; y--) {
						for (int x = minX; x < maxX+1; x++) {
							int dat = recipe[y*3+x];
							resultBuilder.append(dat);
							resultBuilder.append('.');
						}

						resultBuilder.append('/');
					}

					System.out.println(resultBuilder + " recipe read in");
					craftingRecipes.put(resultBuilder.toString().trim(), (short) ((blockKey & 0xFF) | (amount & 0xFF) << 8));
				});
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public static void readBlockJson(String path) {
		Gson gson = new Gson();
		Arrays.fill(itemTexturePaths, -1);

		for (int i = 1; i <= 8; i++) {
			int loc = fileNameMap.get("breakStage"+i+".png");
			breakStages[i-1] = loc;
		}

		try (InputStream is = App.class.getClassLoader().getResourceAsStream(path)) {
			try (InputStreamReader reader = new InputStreamReader(is)) {
				Map<String, Map<String, ?>> data = gson.fromJson(reader, Map.class);

				data.forEach((key, subData) -> {
					Map<String, String> textures = (Map<String, String>) subData.get("textures");
					int blockKey = Integer.parseInt(key);

					Arrays.fill(blockTextureArray[blockKey], -1); // Indicates no texture unless specified (0 can be a texId)

					if (subData.containsKey("xMesh")) isXShapedBlock[blockKey] = true;
					if (subData.containsKey("isLeaves")) isLeafBlock[blockKey] = true;
					if (subData.containsKey("icon")) itemTexturePaths[blockKey] = fileNameMap.get((String)subData.get("icon"));
					if (subData.containsKey("light-level")) lightLevels[blockKey] = (byte) ((double)subData.get("light-level"));
					if (subData.containsKey("hardness")) hardnessLevels[blockKey] = (float)((double)subData.get("hardness"));
					if (subData.containsKey("isItemOnly")) isItemOnly[blockKey] = (boolean)subData.get("isItemOnly");
					if (subData.containsKey("toolType")) toolType[blockKey] = (byte)(double) subData.get("toolType");
					if (subData.containsKey("targetedStrength")) toolTargetedDamage[blockKey] = (float)((double)subData.get("targetedStrength"));
					if (subData.containsKey("idealTool")) idealToolType[blockKey] = (byte)((double)subData.get("idealTool"));

					textures.forEach((faceName, texPath) -> {
						if (faceName.equals("DEFAULT")) {
							if (!fileNameMap.containsKey(texPath)) try {
                                throw new NoSuchFileException("The texture file " + texPath + " is not in the directory.");
                            } catch (NoSuchFileException e) {
                                throw new RuntimeException(e);
                            }

                            blockTextureArray[blockKey][0] = fileNameMap.get(texPath);
							return;
						}

						BLOCK_FACE face = BLOCK_FACE.valueOf(faceName);
						blockTextureArray[blockKey][face.ordinal()+1] = fileNameMap.get(texPath);
					});
				});
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public static int getTextureIndex(byte blockType, BLOCK_FACE face) {
		int val = blockTextureArray[blockType][face.ordinal()+1];

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

		//glTexParameterf(GL_TEXTURE_2D_ARRAY, GL_TEXTURE_LOD_BIAS, 1f);
		glGenerateMipmap(GL_TEXTURE_2D_ARRAY);
		
		glEnable(GL_BLEND);
		glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
	}

	public ByteBuffer getLayer(int layer) {
		ByteBuffer image = MemoryUtil.memAlloc(16 * 16 * 4);
		glGetTextureSubImage(this.id, 0, 0, 0, layer, 16, 16, 1, GL_RGBA, GL_UNSIGNED_BYTE, image);
		return image;
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
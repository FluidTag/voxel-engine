package com.szymc.voxel_engine;

import java.util.ArrayList;
import java.util.List;

public class ChunkSection {
	private byte[][][] chunk = new byte[16][16][16];
	private final int worldX, worldY, worldZ;
	private World worldReference;
	private Mesh mesh = null;
	
	// Each vertex is now: x, y, z, u, v (5 floats)
	private static final float[] TOP_VERTICES = {
	    -0.5f,  0.5f, -0.5f,  0.0f,    0.0f,
	    -0.5f,  0.5f,  0.5f,  0.0f,    0.0625f,
	     0.5f,  0.5f,  0.5f,  0.0625f, 0.0625f,
	     0.5f,  0.5f, -0.5f,  0.0625f, 0.0f
	};

	private static final float[] BOTTOM_VERTICES = {
	    -0.5f, -0.5f, -0.5f,  0.0f,    0.0f,
	     0.5f, -0.5f, -0.5f,  0.0625f, 0.0f,
	     0.5f, -0.5f,  0.5f,  0.0625f, 0.0625f,
	    -0.5f, -0.5f,  0.5f,  0.0f,    0.0625f
	};

	private static final float[] FRONT_VERTICES = {
	    -0.5f,  0.5f,  0.5f,  0.0f,    0.0f,
	    -0.5f, -0.5f,  0.5f,  0.0f,    0.0625f,
	     0.5f, -0.5f,  0.5f,  0.0625f, 0.0625f,
	     0.5f,  0.5f,  0.5f,  0.0625f, 0.0f
	};

	private static final float[] BACK_VERTICES = {
	     0.5f,  0.5f, -0.5f,  0.0f,    0.0f,
	     0.5f, -0.5f, -0.5f,  0.0f,    0.0625f,
	    -0.5f, -0.5f, -0.5f,  0.0625f, 0.0625f,
	    -0.5f,  0.5f, -0.5f,  0.0625f, 0.0f
	};

	private static final float[] LEFT_VERTICES = {
	    -0.5f,  0.5f, -0.5f,  0.0f,    0.0f,
	    -0.5f, -0.5f, -0.5f,  0.0f,    0.0625f,
	    -0.5f, -0.5f,  0.5f,  0.0625f, 0.0625f,
	    -0.5f,  0.5f,  0.5f,  0.0625f, 0.0f
	};

	private static final float[] RIGHT_VERTICES = {
	     0.5f,  0.5f,  0.5f,  0.0f,    0.0f,
	     0.5f, -0.5f,  0.5f,  0.0f,    0.0625f,
	     0.5f, -0.5f, -0.5f,  0.0625f, 0.0625f,
	     0.5f,  0.5f, -0.5f,  0.0625f, 0.0f
	};
	
	private static final int[] CUBE_INDEXES = {
	    // Face
		0, 1, 2, 
	    2, 3, 0
	};
		
	private static void addQuad(
			List<Float> finalVertices,
			List<Integer> finalIndexes,
			float[] faceVertexArray,
			int xMin, int xMax, int y, int zMin, int zMax, int tx, int ty) {
		int addedVerts = finalVertices.size()/5;
		
		float[][] corners = {
				{xMin, y, zMin, tx, ty},
				{xMin, y, zMax, tx, ty},
				{xMax, y, zMax, tx, ty},
				{xMax, y, zMin, tx, ty}
		};
		
		for (float[] arr : corners) {
			for (float v : arr) finalVertices.add(v);
		}
		
		for (int index : CUBE_INDEXES) {
			finalIndexes.add(index + addedVerts);
		}
	}
	
	private Mesh generateMeshChunk() {
		List<Float> finalVertices = new ArrayList<>();
		List<Integer> finalIndexes = new ArrayList<>();
		
		byte[][][] meshed = new byte[16][16][16];
		for (int y = 0; y < 16; y++) {
			for (int x = 0; x < 16; x++) {
				for (int z = 0; z < 16; z++) {
					byte currentBlock = chunk[x][y][z];
					if (currentBlock == Blocks.AIR) continue;
					if (meshed[x][y][z] != 0) continue;
					
					int absX = worldX + x;
					int absY = worldY + y;
					int absZ = worldZ + z;
					
					if (worldReference.getBlockAtWorldPos(absX, absY+1, absZ) != Blocks.AIR) {
						continue;
					}
					
					// Expand X first
					int xBoundMax = x;
					while (xBoundMax < 16) {
						if (chunk[xBoundMax][y][z] != currentBlock) break;
						if (worldReference.getBlockAtWorldPos(worldX + xBoundMax, absY+1, absZ) != Blocks.AIR) {
							break;
						}
						
						meshed[xBoundMax][y][z] = 1;
						xBoundMax++;
					}
					
					// Now expand z from the width
					int zBoundMax = z+1;
					while (zBoundMax < 16) {
						boolean rowPass = true;
						for (int xCheck = x; xCheck < xBoundMax; xCheck++) {
							if (meshed[xCheck][y][zBoundMax] != 0 || chunk[xCheck][y][zBoundMax] != currentBlock) {
								rowPass = false;
								break;
							}
							
							if (worldReference.getBlockAtWorldPos(worldX + xCheck, absY+1, worldZ + zBoundMax) != Blocks.AIR) {
								rowPass = false;
								break;
							}
						}
						
						if (rowPass) {
							for (int xCheck = x; xCheck < xBoundMax; xCheck++) meshed[xCheck][y][zBoundMax] = 1;
							zBoundMax++;
						} else break;
					}
					
					int[] tex = Texture.getTextureCoords(currentBlock, "TOP");
					addQuad(finalVertices, finalIndexes, TOP_VERTICES, x, xBoundMax, y, z, zBoundMax, tex[0], tex[1]);
					
					//System.out.println(x + " by " + z + " to " + xBoundMax + " by " + zBoundMax);
				}
			}
		}
		
		float[] primVerts = new float[finalVertices.size()];
		for (int v = 0; v < finalVertices.size(); v++) {
			primVerts[v] = finalVertices.get(v);
		}
		
		int[] primIndexes = new int[finalIndexes.size()];
		for (int i = 0; i < finalIndexes.size(); i++) {
			primIndexes[i] = finalIndexes.get(i);
		}
		
		return new Mesh(primVerts, primIndexes);
	}

	public ChunkSection(byte[][][] data, World worldReference, int wx, int wy, int wz) {
		this.chunk = data;
		this.worldReference = worldReference;
		
		this.worldX = wx;
		this.worldY = wy;
		this.worldZ = wz;
	}
	
	public byte getLocalBlock(int x, int y, int z) {
		if (x < 0 || x > 15) throw new IndexOutOfBoundsException();
		if (y < 0 || y > 15) throw new IndexOutOfBoundsException();
		if (z < 0 || z > 15) throw new IndexOutOfBoundsException();
		
		return chunk[x][y][z];
	}
	
	public void initializeMesh() {
		this.mesh = generateMeshChunk();
	}
	
	public Mesh getMesh() {
		return this.mesh;
	}
}
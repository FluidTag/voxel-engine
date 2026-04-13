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
		
	private static void addFace(List<Float> finalVertices, List<Integer> finalIndexes, float[] faceVertexArray, int x, int y, int z, int tx, int ty) {
		int addedVerts = finalVertices.size()/5;
		
		for (int i = 0; i < faceVertexArray.length; i+=5) {
			finalVertices.add(faceVertexArray[i] + x);
			finalVertices.add(faceVertexArray[i+1] + y);
			finalVertices.add(faceVertexArray[i+2] + z);
			finalVertices.add((float)(faceVertexArray[i+3] + (0.0625 * tx)));
			finalVertices.add((float)(faceVertexArray[i+4] + (0.0625 * ty)));
		}
		
		for (int index : CUBE_INDEXES) {
			finalIndexes.add(index + addedVerts);
		}
	}
	
	private Mesh generateMeshChunk() {
		List<Float> finalVertices = new ArrayList<>();
		List<Integer> finalIndexes = new ArrayList<>();
		
		for (int x = 0; x < 16; x++) {
			for (int y = 0; y < 16; y++) {
				for (int z = 0; z < 16; z++) {
					byte block = chunk[x][y][z];
					if (block == Blocks.AIR) continue;
					
					int[] topTex = Texture.getTextureCoords(block, "TOP");
					int[] sideTex = Texture.getTextureCoords(block, "SIDE");
					
					int absX = worldX + x;
					int absY = worldY + y;
					int absZ = worldZ + z;
					boolean onVerticalBorder = absY == 0 || absY == 255;
					
					if (worldReference.getBlockAtWorldPos(absX+1, absY, absZ) == 0)
						addFace(finalVertices, finalIndexes, RIGHT_VERTICES, x, y, z, sideTex[0], sideTex[1]);
					if (worldReference.getBlockAtWorldPos(absX-1, absY, absZ) == 0)
						addFace(finalVertices, finalIndexes, LEFT_VERTICES, x, y, z, sideTex[0], sideTex[1]);
					if (onVerticalBorder || worldReference.getBlockAtWorldPos(absX, absY+1, absZ) == 0)
						addFace(finalVertices, finalIndexes, TOP_VERTICES, x, y, z, topTex[0], topTex[1]);
					if (onVerticalBorder || worldReference.getBlockAtWorldPos(absX, absY-1, absZ) == 0)
						addFace(finalVertices, finalIndexes, BOTTOM_VERTICES, x, y, z, sideTex[0], sideTex[1]);
					if (worldReference.getBlockAtWorldPos(absX, absY, absZ+1) == 0)
						addFace(finalVertices, finalIndexes, FRONT_VERTICES, x, y, z, sideTex[0], sideTex[1]);
					if (worldReference.getBlockAtWorldPos(absX, absY, absZ-1) == 0)
						addFace(finalVertices, finalIndexes, BACK_VERTICES, x, y, z, sideTex[0], sideTex[1]);
					
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
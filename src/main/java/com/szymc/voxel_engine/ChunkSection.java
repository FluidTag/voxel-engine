package com.szymc.voxel_engine;


import java.util.ArrayList;
import java.util.List;


public class ChunkSection {
	private byte[][][] chunk = new byte[16][16][16];
	private final int worldX, worldY, worldZ;
	private World worldReference;
	private Mesh mesh = null;
	
	private static final int[] CUBE_INDEXES = {
	    // Face
		0, 1, 2, 
	    2, 1, 3
	};
		
	private static void addQuad(
			List<Float> finalVertices,
			List<Integer> finalIndexes,
			int x1, int y1, int z1,
			int x2, int y2, int z2,
			int x3, int y3, int z3,
			int x4, int y4, int z4,
			int width, int height, byte blockType, // U width, V height
			boolean backFace, int axis
			) {
		int addedVerts = finalVertices.size()/6;
		int texId = 0;
		if (axis == 1) texId = Texture.getTextureIndex(blockType, "TOP"); // Y
		if (axis == 2 || axis == 0) texId = Texture.getTextureIndex(blockType, "SIDE");
		
		float[][] corners;

		if (axis == 0) {
			corners = new float[][]{
		        {x1, y1, z1, height, width, texId},
		        {x2, y2, z2, 0, width, texId},
		        {x3, y3, z3, height, 0, texId},
		        {x4, y4, z4, 0, 0, texId}
		    };
		} else {
		    corners = new float[][]{
		        {x1, y1, z1, 0, height, texId},
		        {x2, y2, z2, 0, 0, texId},
		        {x3, y3, z3, width, height, texId},
		        {x4, y4, z4, width, 0, texId}
		    };
		}


		for (float[] arr : corners) {
			for (float vec : arr) finalVertices.add(vec);
		}
		
		if (backFace) {
	        // Reversed winding: 0-2-1 and 2-3-1
	        finalIndexes.add(addedVerts + 0);
	        finalIndexes.add(addedVerts + 2);
	        finalIndexes.add(addedVerts + 1);
	        finalIndexes.add(addedVerts + 2);
	        finalIndexes.add(addedVerts + 3);
	        finalIndexes.add(addedVerts + 1);
	    } else {
	        // Normal winding: 0-1-2 and 2-1-3
	        for (int index : CUBE_INDEXES) {
	            finalIndexes.add(index + addedVerts);
	        }
	    }
	}
	
	private Mesh generateMeshChunk() {
		List<Float> finalVertices = new ArrayList<>();
		List<Integer> finalIndexes = new ArrayList<>();
		
		// Greedy meshing / mesh generation
		byte[][][][] meshed = new byte[6][16][16][16];
		for (boolean backFace : new boolean[] {false, true}) {
			for (int axis = 0; axis < 3; axis++) {
				int u = (axis+1)%3;
				int v = (axis+2)%3;
				int[] point = new int[3];
				int direction = backFace ? -1 : 1;
				int axisMod = backFace ? 3 : 0;
				
				for (point[axis] = 0; point[axis] < 16; point[axis]++) { // Depth
					for (point[v] = 0; point[v] < 16; point[v]++) { // Vertical
						for (point[u] = 0; point[u] < 16; point[u]++) { // Horizontal
							byte currentBlock = chunk[point[0]][point[1]][point[2]];
							if (currentBlock == Blocks.AIR) continue;
							if (meshed[axis+axisMod][point[0]][point[1]][point[2]] != 0) continue;
							
							// Block to check if air or not
							int nx = point[0] + (axis == 0 ? direction : 0);
							int ny = point[1] + (axis == 1 ? direction : 0);
							int nz = point[2] + (axis == 2 ? direction : 0);
							
							int absNx = worldX + nx;
							int absNy = worldY + ny;
							int absNz = worldZ + nz;
							if (worldReference.getBlockAtWorldPos(absNx, absNy, absNz) != 0) continue;
							
							// Expand on u
							int uBound = point[u];
							int uStart = point[u];
							while (uBound < 16) {
								point[u] = uBound;
								
								int abSubX = worldX + point[0] + (axis == 0 ? direction : 0);
								int abSubY = worldY + point[1] + (axis == 1 ? direction : 0);
								int abSubZ = worldZ + point[2] + (axis == 2 ? direction : 0);
								if (meshed[axis+axisMod][point[0]][point[1]][point[2]] == 1) break;
								if (chunk[point[0]][point[1]][point[2]] != currentBlock) break;
								if (worldReference.getBlockAtWorldPos(abSubX, abSubY, abSubZ) != 0) {
									break;
								}
								
								uBound++;
							}
							
							point[u] = uStart;
							
							// Now expand along v upwards
							int vBound = point[v];
							int vStart = point[v];
							
							while (vBound < 16) {
								point[v] = vBound;
								boolean passed = true;
								
								for (int uCheck = uStart; uCheck < uBound; uCheck++) {
									point[u] = uCheck;
									
									int abSubX = worldX + point[0] + (axis == 0 ? direction : 0);
									int abSubY = worldY + point[1] + (axis == 1 ? direction : 0);
									int abSubZ = worldZ + point[2] + (axis == 2 ? direction : 0);
									if (meshed[axis + axisMod][point[0]][point[1]][point[2]] == 1) {
										passed = false;
										break;
									}
									if (chunk[point[0]][point[1]][point[2]] != currentBlock) {
										passed = false;
										break;
									}
									
									if (worldReference.getBlockAtWorldPos(abSubX, abSubY, abSubZ) != 0) {
										passed = false;
										break;
									}
								}
								
								if (passed) {
									vBound++;
								} else break;
							}
							
							point[u] = uStart;
							point[v] = vStart;
							
							// Mark area as meshed
							// Reset point to origin
							int depth = point[axis]+(backFace ? 0 : 1);
							
							int[] c1 = new int[3];
							c1[axis] = depth; c1[u] = uStart; c1[v] = vStart;
							
							int[] c2 = new int[3];
							c2[axis] = depth; c2[u] = uStart; c2[v] = vBound;
							
							int[] c3 = new int[3];
							c3[axis] = depth; c3[u] = uBound; c3[v] = vStart;
							
							int[] c4 = new int[3];
							c4[axis] = depth; c4[u] = uBound; c4[v] = vBound;
							
							int width = uBound - point[u];
							int height = vBound - point[v];
							
							addQuad(finalVertices, finalIndexes, 
						            c1[0], c1[1], c1[2],
						            c2[0], c2[1], c2[2],
						            c3[0], c3[1], c3[2],
						            c4[0], c4[1], c4[2],
						            width, height, currentBlock, !backFace, axis);
		
							for (int tv = vStart; tv < vBound; tv++) {
							    for (int tu = uStart; tu < uBound; tu++) {
							        int[] mP = new int[3];
							        mP[axis] = point[axis]; mP[u] = tu; mP[v] = tv;
							        meshed[axis+axisMod][mP[0]][mP[1]][mP[2]] = 1;
							    }
							}
						}
					}
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
	
	public byte[][][] getChunkData() {	
		return this.chunk;
	}
	
	public Mesh getMesh() {
		return this.mesh;
	}
}




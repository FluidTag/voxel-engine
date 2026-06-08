package com.szymc.voxel_engine;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;
import java.util.Arrays;
import java.util.BitSet;
import it.unimi.dsi.fastutil.floats.FloatArrayList;
import it.unimi.dsi.fastutil.ints.IntArrayList;


public class ChunkSection {
	private byte[] chunk = new byte[32*16*32]; // 16,384
	private final int worldX, worldY, worldZ;
	private World worldReference;
	private Mesh mesh = null;
	private Mesh waterMesh = null;
	private boolean dirtyMesh = false;
	
	// Chunk must be re-meshed if it is dirty, as either a neighbor chunk impacts
	// faces, or a player does an action
	public boolean isDirty() {
		return this.dirtyMesh;
	}
	
	public void setDirty(boolean status) {
		this.dirtyMesh = status;
	}
		
	private static void addQuad(
			IntArrayList vBuffer,
			IntArrayList iBuffer,
			int x1, int y1, int z1,
			int x2, int y2, int z2,
			int x3, int y3, int z3,
			int x4, int y4, int z4,
			int width, int height, byte blockType, // U width, V height
			boolean backFace, int axis
			) {	
		
		int addedVerts = vBuffer.size()/2;
		int texId = 0;
		if (axis == 1) texId = Texture.getTextureIndex(blockType, "TOP"); // Y
		if (axis == 2 || axis == 0) texId = Texture.getTextureIndex(blockType, "SIDE");


		if (axis == 0) {
			int vert1a = (x1 & 0x3F) | ((y1 & 0x3F) << 6) | ((z1 & 0x3F) << 12);
			int vert1b = (texId & 0xFF) | ((height & 0x3F) << 8) | ((width & 0x3F) << 14);
			
			int vert2a = (x2 & 0x3F) | ((y2 & 0x3F) << 6) | ((z2 & 0x3F) << 12); // Position
			int vert2b = (texId & 0xFF) | ((0 & 0x3F) << 8) | ((width & 0x3F) << 14);
			
			int vert3a = (x3 & 0x3F) | ((y3 & 0x3F) << 6) | ((z3 & 0x3F) << 12); // Position
			int vert3b = (texId & 0xFF) | ((height & 0x3F) << 8) | ((0 & 0x3F) << 14);
			
			int vert4a = (x4 & 0x3F) | ((y4 & 0x3F) << 6) | ((z4 & 0x3F) << 12); // Position
			int vert4b = (texId & 0xFF) | ((0 & 0x3F) << 8) | ((0 & 0x3F) << 14);
			
			vBuffer.add(vert1a);
			vBuffer.add(vert1b);
			vBuffer.add(vert2a);
			vBuffer.add(vert2b);
			vBuffer.add(vert3a);
			vBuffer.add(vert3b);
			vBuffer.add(vert4a);
			vBuffer.add(vert4b);
		} else {
			int vert1a = (x1 & 0x3F) | ((y1 & 0x3F) << 6) | ((z1 & 0x3F) << 12);
			int vert1b = (texId & 0xFF) | ((0 & 0x3F) << 8) | ((height & 0x3F) << 14);
			
			int vert2a = (x2 & 0x3F) | ((y2 & 0x3F) << 6) | ((z2 & 0x3F) << 12); // Position
			int vert2b = (texId & 0xFF) | ((0 & 0x3F) << 8) | ((0 & 0x3F) << 14);
			
			int vert3a = (x3 & 0x3F) | ((y3 & 0x3F) << 6) | ((z3 & 0x3F) << 12); // Position
			int vert3b = (texId & 0xFF) | ((width & 0x3F) << 8) | ((height & 0x3F) << 14);
			
			int vert4a = (x4 & 0x3F) | ((y4 & 0x3F) << 6) | ((z4 & 0x3F) << 12); // Position
			int vert4b = (texId & 0xFF) | ((width & 0x3F) << 8) | ((0 & 0x3F) << 14);
			
			vBuffer.add(vert1a);
			vBuffer.add(vert1b);
			vBuffer.add(vert2a);
			vBuffer.add(vert2b);
			vBuffer.add(vert3a);
			vBuffer.add(vert3b);
			vBuffer.add(vert4a);
			vBuffer.add(vert4b);
		}
		
		if (backFace) {
	        // Reversed winding: 0-2-1 and 2-3-1
	        iBuffer.add(addedVerts + 0);
	        iBuffer.add(addedVerts + 2);
	        iBuffer.add(addedVerts + 1);
	        iBuffer.add(addedVerts + 2);
	        iBuffer.add(addedVerts + 3);
	        iBuffer.add(addedVerts + 1);
	    } else {
	        // Normal winding: 0-1-2 and 2-1-3
	    	iBuffer.add(addedVerts + 0);
	    	iBuffer.add(addedVerts + 1);
	    	iBuffer.add(addedVerts + 2);
	    	iBuffer.add(addedVerts + 2);
	    	iBuffer.add(addedVerts + 1);
	    	iBuffer.add(addedVerts + 3);
	    }
	}
	
	// Cache nearby for meshing / face visibility
	// References passed in
	
	private final static ThreadLocal<IntArrayList> threadVertexBuffer = ThreadLocal.withInitial(() -> new IntArrayList(4096));
	private final static ThreadLocal<IntArrayList> threadIndexBuffer = ThreadLocal.withInitial(() -> new IntArrayList(4096));
	private final static ThreadLocal<IntArrayList> threadWaterVBuffer = ThreadLocal.withInitial(() -> new IntArrayList(4096));
	private final static ThreadLocal<IntArrayList> threadWaterIBuffer = ThreadLocal.withInitial(() -> new IntArrayList(4096));
	
	public void setMesh(Mesh mesh) {
		this.mesh = mesh;
	}
	
	public void setWaterMesh(Mesh mesh) {
		this.waterMesh = mesh;
	}
	
	public Mesh getWaterMesh() {
		return this.waterMesh;
	}
	
	public int[] vertices;
	public int[] indices;
	public int vCount;
	public int iCount;
	
	public int[] waterVertices;
	public int[] waterIndices;
	public int wvCount;
	public int wiCount;
	
	private byte[] fillPaddedArr(byte[] arr, ChunkSection xMajor, ChunkSection xMinor, ChunkSection yMajor, ChunkSection yMinor, ChunkSection zMajor, ChunkSection zMinor) {
		for (int x = 0; x < 32; x++) {
			for (int y = 0; y < 16; y++) {
				int srcPos = (x*(16*32) + y*32);
				int destPos = ((x+1) * (18*34)) + ((y+1)*34) + 1;
				System.arraycopy(chunk, srcPos, arr, destPos, 32);
			}
		}
		
		if (xMinor != null) {
			byte[] xMinDat = xMinor.getChunkData();
			for (int y = 0; y < 16; y++) {
				int srcPos = (31*(16*32)) + (y*32);
				int destPos = ((y+1)*34)+1;
				
				System.arraycopy(xMinDat, srcPos, arr, destPos, 32);
			}
		}
		
		if (xMajor != null) {
			byte[] xMaxDat = xMajor.getChunkData();
			for (int y = 0; y < 16; y++) {
				int srcPos = (0*(16*32)) + (y*32);
				int destPos = (33*(18*34)) + ((y+1)*34)+1;
				
				System.arraycopy(xMaxDat, srcPos, arr, destPos, 32);
			}
		}
		
		if (zMinor != null) {
			byte[] zMinDat = zMinor.getChunkData();
			for (int x = 0; x < 32; x++) {
				for (int y = 0; y < 16; y++) {
					int srcPos = (x*(16*32)) + (y*32) + 31;
					int destPos = ((x+1)*(18*34)) + ((y+1)*34) + 0;
					
					arr[destPos] = zMinDat[srcPos];
				}
			}
		}
		
		if (zMajor != null) {
			byte[] zMaxDat = zMajor.getChunkData();
			for (int x = 0; x < 32; x++) {
				for (int y = 0; y < 16; y++) {
					int srcPos = (x*(16*32)) + (y*32) + 0;
					int destPos = ((x+1)*(18*34)) + ((y+1)*34) + 33;
					
					arr[destPos] = zMaxDat[srcPos];
				}
			}
		}
		
		if (yMinor != null) {
			byte[] yMinDat = yMinor.getChunkData();
			for (int x = 0; x < 32; x++) {
				int srcPos = (x*(16*32)) + (15*32);
				int destPos = ((x+1)*(18*34)) + (0*34) + 1;
				
				System.arraycopy(yMinDat, srcPos, arr, destPos, 32);
			}
		}
		
		if (yMajor != null) {
			byte[] yMajDat = yMajor.getChunkData();
			for (int x = 0; x < 32; x++) {
				int srcPos = (x*(16*32)) + (0*32);
				int destPos = ((x+1)*(18*34)) + (17*34) + 1;
				
				System.arraycopy(yMajDat, srcPos, arr, destPos, 32);
			}
		}
		
		return arr;
	}
	
	private long[] negVisibleFaces = new long[32];
	private long[] posVisibleFaces = new long[32];
	private void meshAxis(long[] occupancyMask, long[] waterMask, long[] leavesMask, int methodAxis, 
			int axisLimit, int uLimit, int vLimit, int paddedULimit,
			IntArrayList vertexBuffer, IntArrayList indexBuffer, 
			IntArrayList waterVBuffer, IntArrayList waterIBuffer
			) {
		// visible faces in 0 to 15 regular chunk range
		// with information of -1 to 16 or 18 length padded arr
		
		for (int axis = 0; axis < axisLimit; axis++) {
			for (int u = 0; u < uLimit; u++) {
				int curIdx = (axis+1) * paddedULimit + (u+1);
				int negIdx = (axis) * paddedULimit + (u+1);
				int posIdx = (axis+2) * paddedULimit + (u+1);
				
				long currentSolid = occupancyMask[curIdx];
				long currentWater = waterMask[curIdx];
				long currentLeaves = leavesMask[curIdx];
				
				long negWater = waterMask[negIdx];
				long negLeaves = leavesMask[negIdx];
				long negSolid = occupancyMask[negIdx];
				
				long negFaces = ((currentSolid | currentLeaves) &~ negSolid) | (currentWater &~ negSolid & ~negWater);
				
				long posWater = waterMask[posIdx];
				long posLeaves = leavesMask[posIdx];
				long posSolid = occupancyMask[posIdx];
				
				long posFaces = ((currentSolid | currentLeaves) &~ posSolid) | (currentWater &~ posSolid & ~posWater);
				
		        // FIX: Discard neighbor block bits (bit 0 and bit 17) 
		        // and shift right by 1 so bit 1 becomes bit 0 (native 0-15 space)
		        long mask = (1L << vLimit) - 1L;
				negVisibleFaces[u] = (negFaces >> 1L) & mask;
		        posVisibleFaces[u] = (posFaces >> 1L) & mask;
			}
			
			for (int u = 0; u < uLimit; u++) {
				while (negVisibleFaces[u] != 0) {
					int vStart = Long.numberOfTrailingZeros(negVisibleFaces[u]);
					long widthMask = 0;
					int vEnd = vStart;
					byte startBlock = 0; // needs conversion
					if (methodAxis == 2) {
						startBlock = chunk[u*(16*32)+vStart*32+axis];
					} else if (methodAxis == 1) {
						startBlock = chunk[u*(16*32)+axis*32+vStart];
					} else if (methodAxis == 0) {
						startBlock = chunk[axis*(16*32)+u*32+vStart];
					}
					
					IntArrayList targetVBuffer = startBlock == Blocks.WATER ? waterVBuffer : vertexBuffer;
					IntArrayList targetIBuffer = startBlock == Blocks.WATER ? waterIBuffer : indexBuffer;
					
					while (vEnd < vLimit && (negVisibleFaces[u] & (1L << vEnd)) != 0) {
						if (methodAxis == 2) {
							if (chunk[u*(16*32)+vEnd*32+axis] != startBlock) break;
						} else if (methodAxis == 1) {
							if (chunk[u*(16*32)+axis*32+vEnd] != startBlock) break;
						} else if (methodAxis == 0) {
							if (chunk[axis*(16*32)+u*32+vEnd] != startBlock) break;
						}
						
						widthMask |= (1L << vEnd);
						vEnd++;
					}
					
					int quadWidth = vEnd-vStart;
					int uEnd = u;
					
					while (uEnd < uLimit && (negVisibleFaces[uEnd] & widthMask) == widthMask) {
						boolean cancelExpand = false;
						for (int vc = vStart; vc < vEnd; vc++) {
							if (methodAxis == 2) {
								if (chunk[uEnd*(16*32)+vc*32+axis] != startBlock) {cancelExpand = true; break;};
							} else if (methodAxis == 1) {
								if (chunk[uEnd*(16*32)+axis*32+vc] != startBlock) {cancelExpand = true; break;};
							} else if (methodAxis == 0) {
								if (chunk[axis*(16*32)+uEnd*32+vc] != startBlock) {cancelExpand = true; break;};
							}
						}
						
						if (cancelExpand) break;
						uEnd++;
					}
					
					int quadHeight = uEnd-u;
					for (int uH = u; uH < uEnd; uH++) {
						negVisibleFaces[uH] &= ~widthMask;
					}
					
					if (methodAxis == 2) {
					addQuad(targetVBuffer, targetIBuffer,
							uEnd, vStart, axis,
							u, vStart, axis,
							uEnd, vEnd, axis,
							u, vEnd, axis,
							
							quadWidth, quadHeight,
							startBlock, false, 0
							);
					} else if (methodAxis == 1) {
					addQuad(targetVBuffer, targetIBuffer,
							u, axis, vEnd,
							u, axis, vStart,
							uEnd, axis, vEnd,
							uEnd, axis, vStart,
							
							quadHeight, quadWidth, 
							startBlock, false, 1
							);
					} else if (methodAxis == 0) {
					addQuad(targetVBuffer, targetIBuffer,
							axis, u, vEnd,
							axis, u, vStart,
							axis, uEnd, vEnd,
							axis, uEnd, vStart,
							
							quadHeight, quadWidth, 
							startBlock, true, 0
							);	
					}
				}
			}
			
			for (int u = 0; u < uLimit; u++) {
				while (posVisibleFaces[u] != 0) {
					int vStart = Long.numberOfTrailingZeros(posVisibleFaces[u]);
					long widthMask = 0;
					int vEnd = vStart;
					byte startBlock = 0; // needs conversion
					if (methodAxis == 2) {
						startBlock = chunk[u*(16*32)+vStart*32+axis];
					} else if (methodAxis == 1) {
						startBlock = chunk[u*(16*32)+axis*32+vStart];
					} else if (methodAxis == 0) {
						startBlock = chunk[axis*(16*32)+u*32+vStart];
					}
					
					IntArrayList targetVBuffer = startBlock == Blocks.WATER ? waterVBuffer : vertexBuffer;
					IntArrayList targetIBuffer = startBlock == Blocks.WATER ? waterIBuffer : indexBuffer;
					
					while (vEnd < vLimit && (posVisibleFaces[u] & (1L << vEnd)) != 0) {
						if (methodAxis == 2) {
							if (chunk[u*(16*32)+vEnd*32+axis] != startBlock) break;
						} else if (methodAxis == 1) {
							if (chunk[u*(16*32)+axis*32+vEnd] != startBlock) break;
						} else if (methodAxis == 0) {
							if (chunk[axis*(16*32)+u*32+vEnd] != startBlock) break;
						}
						
						widthMask |= (1L << vEnd);
						vEnd++;
					}
					
					int quadWidth = vEnd-vStart;
					int uEnd = u;
					
					while (uEnd < uLimit && (posVisibleFaces[uEnd] & widthMask) == widthMask) {
						boolean cancelExpand = false;
						for (int vc = vStart; vc < vEnd; vc++) {
							if (methodAxis == 2) {
								if (chunk[uEnd*(16*32)+vc*32+axis] != startBlock) {cancelExpand = true; break;};
							} else if (methodAxis == 1) {
								if (chunk[uEnd*(16*32)+axis*32+vc] != startBlock) {cancelExpand = true; break;};
							} else if (methodAxis == 0) {
								if (chunk[axis*(16*32)+uEnd*32+vc] != startBlock) {cancelExpand = true; break;};
							}
						}
						
						if (cancelExpand) break;
						uEnd++;
					}
					
					int quadHeight = uEnd-u;
					for (int uH = u; uH < uEnd; uH++) {
						posVisibleFaces[uH] &= ~widthMask;
					}
					
					if (methodAxis == 2) {
					addQuad(targetVBuffer, targetIBuffer,
							uEnd, vStart, axis+1,
							u, vStart, axis+1,
							uEnd, vEnd, axis+1,
							u, vEnd, axis+1,
							
							quadWidth, quadHeight,
							startBlock, true, 0
							);
					} else if (methodAxis == 1) {
					addQuad(targetVBuffer, targetIBuffer,
							u, axis+1, vEnd,
							u, axis+1, vStart,
							uEnd, axis+1, vEnd,
							uEnd, axis+1, vStart,
							
							quadHeight, quadWidth, 
							startBlock, true, 1
							);
					} else if (methodAxis == 0) {
					addQuad(targetVBuffer, targetIBuffer,
							axis+1, u, vEnd,
							axis+1, u, vStart,
							axis+1, uEnd, vEnd,
							axis+1, uEnd, vStart,
							
							quadHeight, quadWidth, 
							startBlock, false, 0
							);	
					}
				}
			}
		}
	}
	
	private final static ThreadLocal<byte[]> threadPadded = ThreadLocal.withInitial(() -> new byte[34*18*34]);
	private final static ThreadLocal<long[]> tOccZ = ThreadLocal.withInitial(() -> new long[34*34]);
	private final static ThreadLocal<long[]> tWatZ = ThreadLocal.withInitial(() -> new long[34*34]);
	private final static ThreadLocal<long[]> tLeaZ = ThreadLocal.withInitial(() -> new long[34*34]);
	
	private final static ThreadLocal<long[]> tOccY = ThreadLocal.withInitial(() -> new long[18*34]);
	private final static ThreadLocal<long[]> tWatY = ThreadLocal.withInitial(() -> new long[18*34]);
	private final static ThreadLocal<long[]> tLeaY = ThreadLocal.withInitial(() -> new long[18*34]);
	
	private final static ThreadLocal<long[]> tOccX = ThreadLocal.withInitial(() -> new long[18*34]);
	private final static ThreadLocal<long[]> tWatX = ThreadLocal.withInitial(() -> new long[18*34]);
	private final static ThreadLocal<long[]> tLeaX = ThreadLocal.withInitial(() -> new long[18*34]);
	
	public void generateMeshData(ChunkSection xMajor, ChunkSection xMinor, ChunkSection yMajor, ChunkSection yMinor, ChunkSection zMajor, ChunkSection zMinor) {
		this.vertices = null;
		this.indices = null;
		this.waterVertices = null;
		this.waterIndices = null;
		this.vCount = 0;
		this.iCount = 0;
		this.wvCount = 0;
		this.wiCount = 0;
		
		final IntArrayList vertexBuffer = threadVertexBuffer.get();
		final IntArrayList indexBuffer = threadIndexBuffer.get();
		final IntArrayList waterVBuffer = threadWaterVBuffer.get();
		final IntArrayList waterIBuffer = threadWaterIBuffer.get();
		
		vertexBuffer.clear();
		indexBuffer.clear();
		waterVBuffer.clear();
		waterIBuffer.clear();
		
		byte[] padded = threadPadded.get();
		Arrays.fill(padded, (byte)0);
		fillPaddedArr(padded, xMajor, xMinor, yMajor, yMinor, zMajor, zMinor);
		
		// 34 x layers, 18 y layers, each mask is 34 z bits
		// 18 y layers, 34 x layers, each mask is 34 z bits
		// 34 z layers, 34 x layers, each mask is 18 y bits
		
		// Iterates x & y, holds z
		long[] occZ = tOccZ.get();
		Arrays.fill(occZ, 0L);
		long[] watZ = tWatZ.get();
		Arrays.fill(watZ, 0L);
		long[] leaZ = tLeaZ.get();
		Arrays.fill(leaZ, 0L);
		
		// Iterates x & z, holds y
		long[] occY = tOccY.get();
		Arrays.fill(occY, 0L);
		long[] watY = tWatY.get();
		Arrays.fill(watY, 0L);
		long[] leaY = tLeaY.get();
		Arrays.fill(leaY, 0L);
		// Iterates y & z, holds x
		
		long[] occX = tOccX.get();
		Arrays.fill(occX, 0L);
		long[] watX = tWatX.get();
		Arrays.fill(watX, 0L);
		long[] leaX = tLeaX.get();
		Arrays.fill(leaX, 0L);
		
		// Build the masks
		for (int x = 0; x < 34; x++) {
			for (int y = 0; y < 18; y++) {
				for (int z = 0; z < 34; z++) {
					byte block = padded[x*(18*34) + y*34 + z];
					if (block == Blocks.WATER) {
						watZ[z*34+x] |= (1L << y);
						watY[y*34+x] |= (1L << z);
						watX[x*18+y] |= (1L << z);
					} else if (block == Blocks.OAK_LEAVES) {
						leaZ[z*34+x] |= (1L << y);
						leaY[y*34+x] |= (1L << z);
						leaX[x*18+y] |= (1L << z);
					} else if (block != 0) {
						occZ[z*34+x] |= (1L << y);
						occY[y*34+x] |= (1L << z);
						occX[x*18+y] |= (1L << z);
					}
				}
			}
		}
		
		// z-axis
		meshAxis(occZ, watZ, leaZ, 2, 32, 32, 16, 34, vertexBuffer, indexBuffer, waterVBuffer, waterIBuffer);
		meshAxis(occY, watY, leaY, 1, 16, 32, 32, 34, vertexBuffer, indexBuffer, waterVBuffer, waterIBuffer);
		meshAxis(occX, watX, leaX, 0, 32, 16, 32, 18, vertexBuffer, indexBuffer, waterVBuffer, waterIBuffer);
		
		vertices = vertexBuffer.toIntArray();
		indices = indexBuffer.toIntArray();
		vCount = vertexBuffer.size();
		iCount = indexBuffer.size();
		
		if (!waterVBuffer.isEmpty()) {
			waterVertices = waterVBuffer.toIntArray();
			waterIndices = waterIBuffer.toIntArray();
			wvCount = waterVBuffer.size();
			wiCount = waterIBuffer.size();
		}
	}
	
	public ChunkSection(byte[] data, World worldReference, int wx, int wy, int wz) {
		this.chunk = data;
		this.worldReference = worldReference;
		
		this.worldX = wx;
		this.worldY = wy;
		this.worldZ = wz;
	}
	
	public int getWorldX() {
		return this.worldX;
	}
	
	public int getWorldY() {
		return this.worldY;
	}
	
	public int getWorldZ() {
		return this.worldZ;
	}
	
	public void setBlock(int x, int y, int z, byte block) {
		if (x < 0 || x > 31) throw new IndexOutOfBoundsException();
		if (y < 0 || y > 15) throw new IndexOutOfBoundsException();
		if (z < 0 || z > 31) throw new IndexOutOfBoundsException();
		
		int index = x*(16*32) + y*32 + z;
		chunk[index] = block;
	}
	
	public byte getLocalBlock(int x, int y, int z) {
		if (x < 0 || x > 31) throw new IndexOutOfBoundsException();
		if (y < 0 || y > 15) throw new IndexOutOfBoundsException();
		if (z < 0 || z > 31) throw new IndexOutOfBoundsException();
		
		int index = x*(16*32) + y*32 + z;
		return chunk[index];
	}
	
	public byte[] getChunkData() {	
		return this.chunk;
	}
	
	public Mesh getMesh() {
		return this.mesh;
	}
}
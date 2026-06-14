package com.szymc.voxel_engine;


import java.util.Random;
import java.util.SplittableRandom;


import it.unimi.dsi.fastutil.ints.IntArrayList;


public class DecorationTask {
	public ChunkColumn chunk;
	public int cx, cz;
	private int wx, wz;
	
	private int findSurface(int x, int z) {
		for (int y = 255; y >= 60; y--) {
			byte b = chunk.getBlockInChunk(x, y, z);
			if (b != Blocks.AIR) return y;
		}
		
		return -1;
	}
	
	private static int packLocal(int x, int y, int z, byte block) {
		return (x & 0x7F) | ((y & 0xFF) << 7) | ((z & 0x7F) << 15) | ((block & 0xFF) << 22);
	}
	
	private void addTrees(int cxOffset, int czOffset, IntArrayList edits) { 
		SplittableRandom rng = new SplittableRandom((long)(cx+cxOffset)*341873128712L ^ (long)(cz+czOffset) * 132897987541L);
		int cellSize = 10;
		
		for (int gx = 0; gx < 32; gx+=cellSize) {
			for (int gz = 0; gz < 32; gz+=cellSize) {
				if (rng.nextFloat() < 0.6f) continue;
				
				int x = gx + rng.nextInt(cellSize);
				int z = gz + rng.nextInt(cellSize);
				if (x < 0 || x > 31 || z < 0 || z > 31) continue;
				int relX = (cxOffset*32)+x;
				int relZ = (czOffset*32)+z;
				int surfaceHeight = -1;
				byte surfaceBlock = -1;
				
				if (cxOffset == 0 && czOffset == 0) {
					surfaceHeight = findSurface(x, z);
					if (surfaceHeight < 0) continue;
					surfaceBlock = chunk.getBlockInChunk(x, surfaceHeight, z);
				} else {
					surfaceHeight = TerrainTask.getNoiseHeight(wx+relX, wz+relZ);
					surfaceBlock = TerrainTask.noiseGetBlock(surfaceHeight, wx+relX, surfaceHeight, wx+relZ, TerrainTask.getBiomeBlock(wx+relX, wz+relZ), 0, 0); // Grass topblock/Default biome
				}
				
				//System.out.println(surfaceBlock);
				if (surfaceBlock != Blocks.GRASS && surfaceBlock != Blocks.JUNGLE_GRASS && surfaceBlock != Blocks.TAIGA_GRASS && surfaceBlock != Blocks.SAVANNA_GRASS && surfaceBlock != Blocks.TUNDRA_GRASS) continue;
				
				if (relX >= 0 && relX <= 31 && relZ >= 0 && relZ <= 31) {
					for (int j = 1; j<=5; j++) {
						edits.add(packLocal(relX, surfaceHeight+j, relZ, Blocks.OAK_WOOD));
					}
				}
				
				for (int jx = -2; jx <= 2; jx++) {
					for (int jy = surfaceHeight+5; jy<=surfaceHeight+8; jy++) {
						for (int jz = -2; jz <= 2; jz++) {
							int compX = relX+jx;
							int compZ = relZ+jz;
							
							if (compX >= 0 && compX <= 31 && compZ >= 0 && compZ <= 31) {
								edits.add(packLocal(compX, jy, compZ, Blocks.OAK_LEAVES));
							}
						}
					}
				}
			}
		}
	}
	
	public IntArrayList changeRequests;
	public IntArrayList decorate() {
		IntArrayList editRequests = new IntArrayList();


		addTrees(0, 0, editRequests);
		addTrees(1, 0, editRequests);
		addTrees(-1, 0, editRequests);
		addTrees(1, 1, editRequests);
		addTrees(1, -1, editRequests);
		addTrees(-1, -1, editRequests);
		addTrees(-1, 1, editRequests);
		addTrees(0, 1, editRequests);
		addTrees(0, -1, editRequests);
		
		return editRequests;
	}
	
	public DecorationTask(ChunkColumn chunk, int cx, int cz) {
		this.chunk = chunk;
		this.cx = cx;
		this.cz = cz;
		
		this.wx = chunk.getWorldX() * 32;
		this.wz = chunk.getWorldZ() * 32;
	}
}
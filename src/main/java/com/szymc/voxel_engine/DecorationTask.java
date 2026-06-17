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
				Biome currentBiome = BiomeRegistry.get(TerrainTask.getBiomeType(wx+relX, wz+relZ));
				
				if (cxOffset == 0 && czOffset == 0) {
					surfaceHeight = findSurface(x, z);
					if (surfaceHeight < 0) continue;
					surfaceBlock = chunk.getBlockInChunk(x, surfaceHeight, z);
				} else {
					surfaceHeight = TerrainTask.getNoiseHeight(wx+relX, wz+relZ);
					surfaceBlock = TerrainTask.noiseGetBlock(surfaceHeight, wx+relX, surfaceHeight, wx+relZ, currentBiome.topBlock, 0, 0); // Grass topblock/Default biome
				}
				
				//System.out.println(surfaceBlock);
				if (surfaceBlock != Blocks.GRASS && surfaceBlock != Blocks.BIRCH_GRASS && 
						surfaceBlock != Blocks.FOREST_GRASS && surfaceBlock != Blocks.JUNGLE_GRASS &&
						surfaceBlock != Blocks.TAIGA_GRASS 
						&& surfaceBlock != Blocks.SAVANNA_GRASS
						) continue;
				
				byte woodType = currentBiome.woodBlock;
				byte leaveType = currentBiome.leafBlock;
								
				if (relX >= 0 && relX <= 31 && relZ >= 0 && relZ <= 31) {
					for (int j = 1; j<=5; j++) {
						edits.add(packLocal(relX, surfaceHeight+j, relZ, woodType));
					}
				}
				
				for (int jx = -2; jx <= 2; jx++) {
					for (int jy = surfaceHeight+5; jy<=surfaceHeight+6; jy++) {
						for (int jz = -2; jz <= 2; jz++) {
							int compX = relX+jx;
							int compZ = relZ+jz;
							
							if (compX >= 0 && compX <= 31 && compZ >= 0 && compZ <= 31) {
								edits.add(packLocal(compX, jy, compZ, leaveType));
							}
						}
					}
				}
				
				for (int jx = -1; jx <= 1; jx++) {
					for (int jy = surfaceHeight+7; jy <= surfaceHeight+8; jy++) {
						for (int jz = -1; jz <= 1; jz++) {
							int compX = relX+jx;
							int compZ = relZ+jz;
							
							if (compX >= 0 && compX <= 31 && compZ >= 0 && compZ <= 31) {
								edits.add(packLocal(compX, jy, compZ, leaveType));
							}
						}
					}
				}
				
				if (cxOffset == 0 && czOffset == 0) {
					edits.add(packLocal(x, surfaceHeight+9, z, leaveType));
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
		
		SplittableRandom rng = new SplittableRandom((long)(cx)*341873128712L ^ (long)(cz) * 132897987541L);
		for (int x = 0; x < 32; x++) {
			for (int z = 0; z < 32; z++) {
				Biome currentBiome = BiomeRegistry.get(TerrainTask.getBiomeType(wx+x, wz+z));
				if (rng.nextFloat() > currentBiome.decorationChance) continue;
				
				int surfaceHeight = findSurface(x, z);
				if (surfaceHeight == -1) continue;
				byte topBlock = chunk.getBlockInChunk(x, surfaceHeight, z);
				
				if (topBlock != currentBiome.topBlock) continue;
				editRequests.add(packLocal(x, surfaceHeight+1, z, Blocks.GRASS_DECORATION));
			}
		}
		
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
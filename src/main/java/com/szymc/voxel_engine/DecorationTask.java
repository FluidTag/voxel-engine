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
	
	public IntArrayList changeRequests;
	public IntArrayList decorate() {
		IntArrayList editRequests = new IntArrayList();
		if (0 == 0) return editRequests;
		
		SplittableRandom rng = new SplittableRandom((long)cx * 341873128712L ^ (long)cz * 132897987541L);
		float noiseVal = TerrainTask.treeNoise.GetNoise(wx+16, wz+16);
		float chance = noiseVal > 0 ? 0.85f : 0.6f;
		
		int cellSize = noiseVal > 0 ? 13 : 8;
		for (int gx = 0; gx < 32; gx += cellSize) {
			for (int gz = 0; gz < 32; gz += cellSize) {
				int x = gx + rng.nextInt(cellSize);
				int z = gz + rng.nextInt(cellSize);
				
				if (rng.nextFloat() < chance) {
					continue;
				}
				
				if (x < 0 || x > 31 || z < 0 || z > 31) continue;
				int ySurface = findSurface(x, z);
				if (ySurface < 0) continue;
				
				byte surface = chunk.getBlockInChunk(x, ySurface, z);
				if (surface != Blocks.GRASS) continue;
				
				int trunkHeight = 5 + rng.nextInt(3);
				for (int i = 1; i <= trunkHeight; i++) {
					editRequests.add(packLocal(x, ySurface+i, z, Blocks.OAK_WOOD));
				}
				
				int leafBase = ySurface + trunkHeight;
				for (int dy = 0; dy <= 4; dy++) {
					int radius = 2 - (dy/2);
					for (int lx = -radius; lx <= radius; lx++) {
						for (int lz = -radius; lz <= radius; lz++) {
							if (Math.abs(lx) + Math.abs(lz) > radius + 1) continue;
							int localx = x+lx;
							int localz = z+lz;
							int localy = leafBase+dy;
							
							editRequests.add(packLocal(localx, localy, localz, Blocks.OAK_LEAVES));
						}
					}
				}
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






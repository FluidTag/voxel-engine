package com.szymc.voxel_engine;


import java.util.Arrays;
import java.util.SplittableRandom;


import it.unimi.dsi.fastutil.ints.IntArrayList;


public class DecorationTask {
	public ChunkColumn chunk;
	public int cx, cz;
	private int wx, wz;
	private static final ThreadLocal<boolean[]> TREE_OCCUPIED = ThreadLocal.withInitial(() -> new boolean[32*32]);
	private boolean[] treeOccupied = TREE_OCCUPIED.get();

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
	
	private void tryAddEdit(int blockWx, int blockWy, int blockWz, byte blockType, IntArrayList edits) {
		int lx = blockWx-wx;
		int lz = blockWz-wz;
		
		if (lx >= 0 && lx <= 31 && lz >= 0 && lz <= 31) {
			edits.add(packLocal(lx, blockWy, lz, blockType));
		}
	}

	private void acaciaTree(int trunkWx, int trunkWz, int surfaceHeight, byte woodType, byte leaveType, IntArrayList edits) {
		long treeSeed = ((long)trunkWx * 341873128712L) ^ ((long)trunkWz * 132897987541L);
		SplittableRandom treeRng = new SplittableRandom(treeSeed);

		int treeHeight = 5+treeRng.nextInt(2);
		for (int jy = 0; jy <= treeHeight; jy++) {
			tryAddEdit(trunkWx, surfaceHeight+jy, trunkWz, woodType, edits);
		}

		int branchXDir = treeRng.nextBoolean() ? 1 : -1;
		int branchZDir = treeRng.nextBoolean() ? 1 : -1;
		int branchLength = treeRng.nextInt(2, 5);
		int curBranchHeight = treeHeight;
		int x = 0; int y = 0; int z = 0;

		for (int i = 0; i < branchLength; i++) {
			x += branchXDir;
			z += branchZDir;
			y += treeRng.nextBoolean() ? 1 : 0;
			tryAddEdit(trunkWx+x, surfaceHeight+treeHeight+y, trunkWz+z, woodType, edits);
		};

		int radius = 14;
		for (int jy = 0; jy <= 3; jy++) {
			for (int jx = -3; jx <= 3; jx++) {
				for (int jz = -3; jz <= 3; jz++) {
					int dist = (jx * jx) + (jz * jz);
					if (dist <= radius)
						tryAddEdit(trunkWx + x + jx, surfaceHeight + treeHeight + y + jy + 1, trunkWz + z + jz, leaveType, edits);
				}
			}
			radius-=6;
		}

		if (treeRng.nextBoolean()) return;

		x = 0; y = 0; z = 0;
		branchXDir*=-1; branchZDir *=-1;
		treeHeight-=treeRng.nextInt(3);
		branchLength-=treeRng.nextInt(1, 4);
		radius = 10;
		if (branchLength <= 0) return;

		for (int i = 0; i < branchLength; i++) {
			x += branchXDir;
			z += branchZDir;
			y += treeRng.nextFloat() > 0.75 ? 1 : 0;
			tryAddEdit(trunkWx+x, surfaceHeight+treeHeight+y, trunkWz+z, woodType, edits);
		};

		for (int jy = 0; jy <= 3; jy++) {
			for (int jx = -3; jx <= 3; jx++) {
				for (int jz = -3; jz <= 3; jz++) {
					int dist = (jx * jx) + (jz * jz);
					if (dist <= radius)
						tryAddEdit(trunkWx + x + jx, surfaceHeight + treeHeight + y + jy + 1, trunkWz + z + jz, leaveType, edits);
				}
			}
			radius-=4;
		}
	}

	private void jungleTree(int trunkWx, int trunkWz, int surfaceHeight, byte woodType, byte leaveType, IntArrayList edits) {
		long treeSeed = ((long)trunkWx * 341873128712L) ^ ((long)trunkWz * 132897987541L);
		SplittableRandom treeRng = new SplittableRandom(treeSeed);
		int treeHeight = 20 + treeRng.nextInt(7);
		for (int jy = 0; jy <= treeHeight; jy++) {
			for (int jx = 0; jx <= 1; jx++) {
				for (int jz = 0; jz <= 1; jz++) {
					tryAddEdit(trunkWx + jx, surfaceHeight+jy, trunkWz+jz, woodType, edits);
				}
			}
		}

		double spawnChance = 0.9;
		for (int jy = 0; jy <= 5; jy++) {
			for (int jx = -3; jx <= 4; jx++) {
				for (int jz = -3; jz <= 4; jz++) {
					double distance = ((jx+0.5)*(jx+0.5))+((jz+0.5)*(jz+0.5));
					if (treeRng.nextFloat() < spawnChance - (distance*0.1)) tryAddEdit(trunkWx + jx, surfaceHeight+jy, trunkWz+jz, woodType, edits);
				}
			}
			spawnChance-=0.27;
		}

		int rad = 25;
		for (int jy = treeHeight; jy <= treeHeight+3; jy++) {
			for (int jx = -7; jx <= 6; jx++) {
				for (int jz = -7; jz <= 6; jz++) {
					double dist = ((jx + 0.5) * (jx + 0.5)) + ((jz + 0.5) * (jz + 0.5));
					if (dist <= rad) {
						tryAddEdit(trunkWx + jx + 1, surfaceHeight + jy, trunkWz + jz + 1, leaveType, edits);
					}
				}
			}
			rad-=4;
		}

		rad = 20;
		for (int jy = surfaceHeight+15; jy <= surfaceHeight + 17; jy++) {
			for (int jx = -6; jx <= 5; jx++) {
				for (int jz = -6; jz <= 5; jz++) {
					double dist = ((jx + 0.5) * (jx + 0.5)) + ((jz + 0.5) * (jz + 0.5));
					if (dist <= rad) {
						tryAddEdit(trunkWx + jx + 1, jy, trunkWz + jz + 1, leaveType, edits);
					}
				}
			}
			rad-=7;
		}
	}
	
	private void spruceTree(int trunkWx, int trunkWz, int surfaceHeight, byte woodType, byte leaveType, IntArrayList edits) {
//		long treeSeed = ((long)trunkWx * 341873128712L) ^ ((long)trunkWz * 132897987541L);
//      SplittableRandom treeRng = new SplittableRandom(treeSeed);
        
		for (int j = 1; j <= 12; j++) {
			tryAddEdit(trunkWx, surfaceHeight+j, trunkWz, woodType, edits);
		}
		
		// Ring 1
		int rad = 17;
		boolean shortRad = false;
		for (int ring = surfaceHeight+4; ring <= surfaceHeight+13; ring++) {
			int r = shortRad ? Math.max(rad-4, 3) : rad;
			for (int jx = -3; jx <= 3; jx++) {
				for (int jz = -3; jz <= 3; jz++) {
					int dist = (jx*jx) + (jz*jz);
					
					if (dist <= r && dist != 0) {
						tryAddEdit(trunkWx+jx, ring, trunkWz+jz, leaveType, edits);
					}
				}
			}
			
			if (shortRad) {
				shortRad = !shortRad;
			} else {
				shortRad = true;
				rad-=4;
			}
		}
		
		tryAddEdit(trunkWx, surfaceHeight+14, trunkWz, leaveType, edits);
	}
	
	
	private void regularTree(int trunkWx, int trunkWz, int surfaceHeight, byte woodType, byte leaveType, IntArrayList edits) {
		long treeSeed = ((long)trunkWx * 341873128712L) ^ ((long)trunkWz * 132897987541L);
        SplittableRandom treeRng = new SplittableRandom(treeSeed);
        
		for (int j = 1; j<=5+treeRng.nextInt(2); j++) {
			tryAddEdit(trunkWx, surfaceHeight+j, trunkWz, woodType, edits);
		}
		
		for (int jx = -2; jx <= 2; jx++) {
			for (int jy = surfaceHeight+5; jy<=surfaceHeight+6; jy++) {
				for (int jz = -2; jz <= 2; jz++) {
					tryAddEdit(trunkWx+jx, jy, trunkWz+jz, leaveType, edits);
				}
			}
		}
		
		for (int jx = -1; jx <= 1; jx++) {
			for (int jy = surfaceHeight+7; jy <= surfaceHeight+8; jy++) {
				for (int jz = -1; jz <= 1; jz++) {
					tryAddEdit(trunkWx+jx, jy, trunkWz+jz, leaveType, edits);
				}
			}
		}
		
		tryAddEdit(trunkWx, surfaceHeight+9, trunkWz, leaveType, edits);
	}
	
	private void simulateSourceChunkTrees(int sourceCx, int sourceCz, IntArrayList edits) { 
		SplittableRandom rng = new SplittableRandom((long)(sourceCx*341873128712L) ^ (long)(sourceCz * 132897987541L));
		int cellSize = 10;
		
		int sourceWx = sourceCx * 32;
        int sourceWz = sourceCz * 32;

		for (int gx = 0; gx < 32; gx+=cellSize) {
			for (int gz = 0; gz < 32; gz+=cellSize) {
				int x = gx + rng.nextInt(cellSize);
				int z = gz + rng.nextInt(cellSize);
				if (x < 0 || x > 31 || z < 0 || z > 31) continue;
				
				int trunkWx = sourceWx+x;
				int trunkWz = sourceWz+z;
				
				Biome currentBiome = BiomeRegistry.get(TerrainTask.getBiomeType(trunkWx, trunkWz, TerrainTask.getTemp(trunkWx, trunkWz), TerrainTask.getMoist(trunkWx, trunkWz)));
				
				if (rng.nextFloat() > currentBiome.treeDensity) continue;
				int surfaceHeight = TerrainTask.getNoiseHeight(trunkWx, trunkWz);
				if (surfaceHeight < 0) continue;
				
				byte surfaceBlock = TerrainTask.noiseGetBlock(surfaceHeight, trunkWx, surfaceHeight, trunkWz, currentBiome, 0, 0);
				
				//System.out.println(surfaceBlock);
				if (surfaceHeight <= 64 && surfaceBlock != Blocks.GRASS && surfaceBlock != Blocks.BIRCH_GRASS &&
						surfaceBlock != Blocks.FOREST_GRASS && surfaceBlock != Blocks.JUNGLE_GRASS &&
						surfaceBlock != Blocks.TAIGA_GRASS 
						&& surfaceBlock != Blocks.SAVANNA_GRASS
						) continue;
				
				if (sourceCx == this.cx && sourceCz == this.cz) {
					treeOccupied[x*32+z] = true;
				}
				
				byte woodType = currentBiome.woodBlock;
				byte leaveType = currentBiome.leafBlock;
				
				if (currentBiome.type == BiomeType.TAIGA) {
					spruceTree(trunkWx, trunkWz, surfaceHeight, woodType, leaveType, edits);
				} else if (currentBiome.type == BiomeType.JUNGLE) {
					if (rng.nextFloat() > 0.65) {jungleTree(trunkWx, trunkWz, surfaceHeight, woodType, leaveType, edits);} else regularTree(trunkWx, trunkWz, surfaceHeight, woodType, leaveType, edits);
				} else if (currentBiome.type == BiomeType.SAVANNA || currentBiome.type == BiomeType.DESERT) {
					acaciaTree(trunkWx, trunkWz, surfaceHeight, woodType, leaveType, edits);
				} else {
					regularTree(trunkWx, trunkWz, surfaceHeight, woodType, leaveType, edits);
				}
			}
		}
	}
	
	public IntArrayList changeRequests;
	public IntArrayList decorate() {
		Arrays.fill(treeOccupied, false);
		IntArrayList editRequests = new IntArrayList();

		for (int dx = -1; dx <= 1; dx++) {
			for (int dz = -1; dz <= 1; dz++) {
				simulateSourceChunkTrees(this.cx + dx, this.cz + dz, editRequests);
			}
		}
		
		SplittableRandom rng = new SplittableRandom((long)(cx)*341873128712L ^ (long)(cz) * 132897987541L);
		for (int x = 0; x < 32; x++) {
			for (int z = 0; z < 32; z++) {
				Biome currentBiome = BiomeRegistry.get(TerrainTask.getBiomeType(wx+x, wz+z, TerrainTask.getTemp(wx+x, wz+z), TerrainTask.getMoist(wx+x, wz+z)));

				if (currentBiome.type == BiomeType.DESERT) {
					if (rng.nextFloat() > 0.993f) {
						int surfaceHeight = findSurface(x, z);
						if (surfaceHeight == -1) continue;
						byte topBlock = chunk.getBlockInChunk(x, surfaceHeight, z);

						if (topBlock != currentBiome.topBlock) continue;
						if (chunk.getBlockInChunk(x, surfaceHeight+1, z) != Blocks.AIR) continue;

						for (int i = 1; i <= rng.nextInt(3); i++) {
							editRequests.add(packLocal(x, surfaceHeight + i, z, Blocks.CACTUS));
						}

						continue;
					}
				}

				if (rng.nextFloat() > currentBiome.decorationChance) continue;
				if (currentBiome.possibleDecorations == null) continue;
				
				int surfaceHeight = findSurface(x, z);
				if (surfaceHeight == -1) continue;
				byte topBlock = chunk.getBlockInChunk(x, surfaceHeight, z);
				
				if (topBlock != currentBiome.topBlock) continue;
				if (chunk.getBlockInChunk(x, surfaceHeight+1, z) != Blocks.AIR) continue;

				if (treeOccupied[x * 32 + z]) continue;
				
				byte decoration = currentBiome.possibleDecorations[rng.nextInt(currentBiome.possibleDecorations.length)];
				editRequests.add(packLocal(x, surfaceHeight+1, z, decoration));
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
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

	private void redwoodTreeAlt(int trunkWx, int trunkWz, int surfaceHeight, byte woodType, byte leaveType, IntArrayList edits) {
		long treeSeed = ((long)trunkWx * 341873128712L) ^ ((long)trunkWz * 132897987541L);
		SplittableRandom treeRng = new SplittableRandom(treeSeed);
		int treeHeight = 26 + treeRng.nextInt(11);

		for (int jy = 0; jy <= treeHeight+7; jy++) {
			for (int jx = 0; jx <= 1; jx++) {
				for (int jz = 0; jz <= 1; jz++) {
					tryAddEdit(trunkWx + jx, surfaceHeight+jy, trunkWz+jz, woodType, edits);
				}
			}
		}

		int trunkRad = 25;
		for (int jy = -4; jy <= 5; jy++) {
			for (int jx = -8; jx <= 7; jx++) {
				for (int jz = -8; jz <= 7; jz++) {
					double dist = ((jx + 0.5) * (jx + 0.5)) + ((jz + 0.5) * (jz + 0.5));
					if (dist <= trunkRad) {
						tryAddEdit(trunkWx + jx + 1, surfaceHeight + jy, trunkWz + jz + 1, woodType, edits);
					}
				}
			}
			trunkRad-=3;
		}

		int rad = 35;
		boolean smallRad = false;
		for (int jy = treeHeight-9; jy <= treeHeight+12; jy++) {
			for (int jx = -7; jx <= 6; jx++) {
				for (int jz = -7; jz <= 6; jz++) {
					double dist = ((jx + 0.5) * (jx + 0.5)) + ((jz + 0.5) * (jz + 0.5));
					int eRad = smallRad ? rad-6 : rad;
					if (jy == treeHeight-9) eRad = 10;
					if (dist <= eRad) {
						tryAddEdit(trunkWx + jx + 1, surfaceHeight + jy, trunkWz + jz + 1, leaveType, edits);
					}
				}
			}
			rad-=2;
			smallRad = !smallRad;
		}
	}

	private void redwoodTree(int trunkWx, int trunkWz, int surfaceHeight, byte woodType, byte leaveType, IntArrayList edits) {
		long treeSeed = ((long)trunkWx * 341873128712L) ^ ((long)trunkWz * 132897987541L);
		SplittableRandom treeRng = new SplittableRandom(treeSeed);
		int treeHeight = 26 + treeRng.nextInt(11);

		for (int jy = 0; jy <= treeHeight; jy++) {
			for (int jx = 0; jx <= 1; jx++) {
				for (int jz = 0; jz <= 1; jz++) {
					tryAddEdit(trunkWx + jx, surfaceHeight+jy, trunkWz+jz, woodType, edits);
				}
			}
		}

		int trunkRad = 25;
		for (int jy = -4; jy <= 5; jy++) {
			for (int jx = -8; jx <= 7; jx++) {
				for (int jz = -8; jz <= 7; jz++) {
					double dist = ((jx + 0.5) * (jx + 0.5)) + ((jz + 0.5) * (jz + 0.5));
					if (dist <= trunkRad) {
						tryAddEdit(trunkWx + jx + 1, surfaceHeight + jy, trunkWz + jz + 1, woodType, edits);
					}
				}
			}
			trunkRad-=3;
		}

		int rad = 29;
		boolean smallRad = false;
		for (int jy = treeHeight-7; jy <= treeHeight+9; jy++) {
			for (int jx = -7; jx <= 6; jx++) {
				for (int jz = -7; jz <= 6; jz++) {
					double dist = ((jx + 0.5) * (jx + 0.5)) + ((jz + 0.5) * (jz + 0.5));
					int eRad = smallRad ? rad-6 : rad;
					if (jy == treeHeight-7) eRad = 10;
					if (dist <= eRad) {
						tryAddEdit(trunkWx + jx + 1, surfaceHeight + jy, trunkWz + jz + 1, leaveType, edits);
					}
				}
			}
			if (jy <= treeHeight-4) rad+=3; else rad-= (jy >= treeHeight+5) ? 4 : 2;
			smallRad = !smallRad;
		}

		for (int jy = 5; jy < treeHeight-8; jy+=4) {
			if (treeRng.nextFloat() > 0.7f) {
				int subRad = 14;
				for (int ky = jy; ky < jy+treeRng.nextInt(2,5); ky++) {
					for (int jx = -7; jx <= 6; jx++) {
						for (int jz = -7; jz <= 6; jz++) {
							double dist = ((jx + 0.5) * (jx + 0.5)) + ((jz + 0.5) * (jz + 0.5));
							int eRad = smallRad ? subRad - 6 : subRad;
							if (dist <= eRad) {
								tryAddEdit(trunkWx + jx + 1, surfaceHeight + ky, trunkWz + jz + 1, leaveType, edits);
							}
						}
					}
					subRad-=4;
				}
			} else if (treeRng.nextBoolean()) {
				int branchXDir = treeRng.nextInt(-1, 2);
				int branchZDir = treeRng.nextInt(-1, 2);
				int jx = trunkWx + treeRng.nextInt(0, 2);
				int jz = trunkWz + treeRng.nextInt(0, 2);
				int length = treeRng.nextInt(1, 4);
				int i = 0;

				while (i < length) {
					tryAddEdit(jx, surfaceHeight+jy, jz, woodType, edits);
					jx += branchXDir;
					jz += branchZDir;
					i++;
				}

				int subRad = 9;
				for (int ky = jy+1; ky <= jy+2; ky++) {
					for (int kx = jx - 4; kx <= jx + 4; kx++) {
						for (int kz = jz - 4; kz <= jz + 4; kz++) {
							if ((kx == trunkWx || kx == trunkWx+1) && (kz == trunkWz || kz == trunkWz + 1)) continue;

							float dist = (kx-jx)*(kx-jx) + (kz-jz)*(kz-jz);
							if (dist <= subRad) tryAddEdit(kx, surfaceHeight+ky, kz, leaveType, edits);
						}
					}
					subRad-=3;
				}
			}
		}
	}

	private void darkOakTree(int trunkWx, int trunkWz, int surfaceHeight, byte woodType, byte leaveType, IntArrayList edits) {
		long treeSeed = ((long)trunkWx * 341873128712L) ^ ((long)trunkWz * 132897987541L);
		SplittableRandom treeRng = new SplittableRandom(treeSeed);
		int treeHeight = 6 + treeRng.nextInt(4);

		for (int jy = 0; jy <= treeHeight; jy++) {
			for (int jx = 0; jx <= 1; jx++) {
				for (int jz = 0; jz <= 1; jz++) {
					tryAddEdit(trunkWx + jx, surfaceHeight+jy, trunkWz+jz, woodType, edits);
				}
			}
		}

		double spawnChance = 0.9;
		for (int jy = 0; jy <= 3; jy++) {
			for (int jx = -3; jx <= 4; jx++) {
				for (int jz = -3; jz <= 4; jz++) {
					double distance = ((jx+0.5)*(jx+0.5))+((jz+0.5)*(jz+0.5));
					if (treeRng.nextFloat() < spawnChance - (distance*0.15)) tryAddEdit(trunkWx + jx, surfaceHeight+jy, trunkWz+jz, woodType, edits);
				}
			}
			spawnChance-=0.3;
		}

		int rad = 14;
		for (int jx = -7; jx <= 6; jx++) {
			for (int jz = -7; jz <= 6; jz++) {
				double dist = ((jx + 0.5) * (jx + 0.5)) + ((jz + 0.5) * (jz + 0.5));
				if (dist <= rad) {
					tryAddEdit(trunkWx + jx + 1, surfaceHeight + treeHeight-1, trunkWz + jz + 1, leaveType, edits);
				}
			}
		}

		int branchXDir = treeRng.nextBoolean() ? 1 : -1;
		int branchZDir = treeRng.nextBoolean() ? 1 : -1;
		int branchLength = treeRng.nextInt(2, 4);
		int x = 0; int z = 0;

		for (int i = 0; i < branchLength; i++) {
			x += branchXDir;
			z += branchZDir;
			tryAddEdit(trunkWx+x, surfaceHeight+treeHeight-1, trunkWz+z, woodType, edits);
		};
		branchXDir *= -1; branchZDir *= -1;
		branchLength = treeRng.nextInt(1, 4);
		x = 0; z = 0;
		for (int i = 0; i < branchLength; i++) {
			x += branchXDir;
			z += branchZDir;
			tryAddEdit(trunkWx+x, surfaceHeight+treeHeight-1, trunkWz+z, woodType, edits);
		};

		rad = 25;
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
		long treeSeed = ((long)trunkWx * 341873128712L) ^ ((long)trunkWz * 132897987541L);
      	SplittableRandom treeRng = new SplittableRandom(treeSeed);
        
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
						byte effectiveLeaf = leaveType;
						if (leaveType == Blocks.SNOWY_SPRUCE_LEAVES && treeRng.nextFloat() > 0.5f + (ring-surfaceHeight)*0.01f) effectiveLeaf = Blocks.SPRUCE_LEAVES;

						tryAddEdit(trunkWx+jx, ring, trunkWz+jz, effectiveLeaf, edits);
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

				int surfaceHeight = TerrainTask.getNoiseHeight(trunkWx, trunkWz);
				Biome currentBiome = BiomeRegistry.get(TerrainTask.getBiomeType(surfaceHeight, TerrainTask.getTemp(trunkWx, trunkWz), TerrainTask.getMoist(trunkWx, trunkWz),
						TerrainTask.getContinental(trunkWx, trunkWz), TerrainTask.getErosion(trunkWx, trunkWz),  TerrainTask.getWeirdness(trunkWx, trunkWz) ));
				
				if (rng.nextFloat() > currentBiome.treeDensity) continue;

				byte surfaceBlock = TerrainTask.getSurfaceBlock(trunkWx, surfaceHeight, trunkWz, currentBiome);

				if (surfaceHeight <= 64) continue;
				if (surfaceBlock != Blocks.GRASS && surfaceBlock != Blocks.BIRCH_GRASS &&
						surfaceBlock != Blocks.FOREST_GRASS && surfaceBlock != Blocks.JUNGLE_GRASS &&
						surfaceBlock != Blocks.TAIGA_GRASS && surfaceBlock != Blocks.TUNDRA_GRASS
						&& surfaceBlock != Blocks.SAVANNA_GRASS && surfaceBlock != Blocks.SAND && surfaceBlock != Blocks.SNOW && surfaceBlock != Blocks.SWAMP_GRASS && surfaceBlock != Blocks.RED_DIRT
						) continue;
				
				if (sourceCx == this.cx && sourceCz == this.cz) {
					treeOccupied[x*32+z] = true;
				}
				
				byte woodType = currentBiome.woodBlock;
				byte leaveType = currentBiome.leafBlock;
				
				if (currentBiome.type == BiomeType.TAIGA || currentBiome.type == BiomeType.SNOWY_TAIGA) {
					spruceTree(trunkWx, trunkWz, surfaceHeight, woodType, leaveType, edits);
				} else if (currentBiome.type == BiomeType.JUNGLE) {
					if (rng.nextFloat() > 0.65f) {jungleTree(trunkWx, trunkWz, surfaceHeight, woodType, leaveType, edits);} else regularTree(trunkWx, trunkWz, surfaceHeight, woodType, leaveType, edits);
				} else if (currentBiome.type == BiomeType.SAVANNA || currentBiome.type == BiomeType.DESERT) {
					acaciaTree(trunkWx, trunkWz, surfaceHeight, woodType, leaveType, edits);
				} else if (currentBiome.type == BiomeType.DARK_OAK_FOREST) {
					darkOakTree(trunkWx, trunkWz, surfaceHeight, woodType, leaveType, edits);
				} else if (currentBiome.type == BiomeType.REDWOOD_FOREST) {
					if (rng.nextFloat() < 0.6f) {redwoodTreeAlt(trunkWx, trunkWz, surfaceHeight, Blocks.RED_WOOD, Blocks.SPRUCE_LEAVES, edits);} else redwoodTree(trunkWx, trunkWz, surfaceHeight, Blocks.RED_WOOD, Blocks.SPRUCE_LEAVES, edits);
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
				int surfaceHeight = TerrainTask.getNoiseHeight(wx+x, wz+z);
				Biome currentBiome = BiomeRegistry.get(TerrainTask.getBiomeType(surfaceHeight, TerrainTask.getTemp(wx+x, wz+z),
						TerrainTask.getMoist(wx+x, wz+z), TerrainTask.getContinental(wx+x, wz+z), TerrainTask.getErosion(wx+x, wz+z), TerrainTask.getWeirdness(wx+x, wz+z)));
				if (currentBiome.possibleDecorations == null) continue;

				if (currentBiome.type == BiomeType.DESERT) {
					if (rng.nextFloat() > 0.993f) {
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
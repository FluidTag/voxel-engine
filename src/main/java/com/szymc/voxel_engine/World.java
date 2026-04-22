package com.szymc.voxel_engine;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import org.joml.Vector3f;

public class World {
	private final Long2ObjectMap<ChunkColumn> renderedColumns = new Long2ObjectOpenHashMap<>();
	private final Long2ObjectMap<ChunkColumn> loadedColumns = new Long2ObjectOpenHashMap<>();
	private final int renderDistance = 9;
	
	private long packKey(int cx, int cz) {
	    return ((long)cx & 0xFFFFFFFFL) | (((long)cz & 0xFFFFFFFFL) << 32);
	}

	private int getKeyX(long key) {
	    return (int) key;
	}

	private int getKeyZ(long key) {
	    return (int) (key >>> 32); // Use unsigned right shift
	}
	
	private final FastNoiseLite noise = new FastNoiseLite();
	public void initNoise() {
		noise.SetNoiseType(FastNoiseLite.NoiseType.OpenSimplex2);
	    noise.SetFrequency(0.012f);

	    noise.SetFractalType(FastNoiseLite.FractalType.FBm); 
	    noise.SetFractalOctaves(4);
	}
	
	private int getNoiseHeight(int wx, int wy, int wz) {
		float finalNoise = (noise.GetNoise(wx, wz) + 1.0f) / 2.0f;
		finalNoise = (float)(Math.pow(finalNoise, 2.2));
		int height = (int)(64 + (finalNoise * 40));
		
		return height;
	}
	
	private byte noiseGetBlock(int wx, int wy, int wz) {
		int height = getNoiseHeight(wx, wy, wz);
		
		if (wy < 69) return Blocks.WATER;
		
		if (wy == height) {
			return Blocks.GRASS;
		} else if (wy < height && wy >= height-3) {
			return Blocks.DIRT;
		} else if (wy < height-3) {
			return Blocks.STONE;
		}
		
		return Blocks.AIR;
	}
	
	public byte getBlockAtWorldPos(int wx, int wy, int wz) {
		int chunkX = wx >> 4;
		int chunkY = wy >> 4;
		int chunkZ = wz >> 4;
		
		if (wy < 0) return 1;
		long key = packKey(chunkX, chunkZ);
		
		if (!loadedColumns.containsKey(key)) {
			return noiseGetBlock(wx, wy, wz);
		} else {
			ChunkColumn chunk = loadedColumns.get(key);
			int localX = wx & 15;
			int localY = wy & 15;
			int localZ = wz & 15;
			
			ChunkSection section = chunk.getSection(chunkY);
			if (section == null) return Blocks.AIR;
			
			byte block = chunk.getSection(chunkY).getLocalBlock(localX, localY, localZ);
			if (block == Blocks.LEAVES) block = Blocks.AIR;
			
			return block;
		}
	}
	
	private ChunkColumn generateChunk(int cx, int cz) {
		ChunkSection[] sections = new ChunkSection[16];
		
		for (int sec = 0; sec < 16; sec++) {
			byte[][][] dat = null;
			
			for (int x = 0; x < 16; x++) {
				for (int z = 0; z < 16; z++) {
					int worldX = (cx*16)+x;
					int worldZ = (cz*16)+z;
					
					for (int y = 0; y < 16; y++) {
						int worldY = sec*16 + y;
						
						byte block = noiseGetBlock(worldX, worldY, worldZ);
						if (block != Blocks.AIR) {
							if (dat == null) dat = new byte[16][16][16];
							dat[x][y][z] = block;
						}
					}
				}
			}
			
			if (dat == null) continue;
			sections[sec] = new ChunkSection(dat, this, cx*16, sec*16, cz*16);
		}
		
		return new ChunkColumn(this, cx, cz, sections);
	}
	
	public Long2ObjectMap<ChunkColumn> getRendered() {
		return this.renderedColumns;
	}
	
	public ChunkColumn getLoadedChunkAtPos(int cx, int cz) {
		long index = packKey(cx, cz);
		
		return this.loadedColumns.get(index);
	}
	
	int lastChunkX = 0;
	int lastChunkZ = 0;
	public void update(Vector3f playerPosition) {
		int chunkX = (int)playerPosition.x >> 4;
		int chunkZ = (int)playerPosition.z >> 4;
		
		if (chunkX == lastChunkX && chunkZ == lastChunkZ) {
			return;
		}
		
		lastChunkX = chunkX;
		lastChunkZ = chunkZ;
		
		// Generate Terrain / Data
		for (int x = -renderDistance - 1; x < renderDistance + 1; x++) {
			for (int z = -renderDistance - 1; z < renderDistance + 1; z++) {
				int newX = chunkX + x;
				int newZ = chunkZ + z;
				
				long key = packKey(newX, newZ);
				if (!loadedColumns.containsKey(key)) {
					ChunkColumn chunk = loadedColumns.get(key);
					if (chunk == null) {
						chunk = generateChunk(newX, newZ);
					}
					
					loadedColumns.put(key, chunk);
				}
			}
		}
		
		// Decorations
		for (int x = -renderDistance; x < renderDistance; x++) {
			for (int z = -renderDistance; z < renderDistance; z++) {
				int newX = chunkX + x;
				int newZ = chunkZ + z;
				
				long key = packKey(newX, newZ);
				ChunkColumn chunk = loadedColumns.get(key);
				if (chunk.decorated == true) continue;
				chunk.decorated = true;
				
				if (x != -renderDistance-1 && x != renderDistance+1 && z != -renderDistance-1 && z != renderDistance+1) {
					for (int cx = 0; cx < 16; cx++) {
						for (int cz = 0; cz < 16; cz++) {
							int ySurface = -1;
							for (int yCheck = 255; yCheck > 64; yCheck--) {
								byte blockAt = chunk.getBlockInChunk(cx, yCheck, cz);
								if (blockAt == Blocks.GRASS) {
									ySurface = yCheck;
									break;
								}
							}
							
							if (ySurface != -1 && Math.random() > 0.999) {
								for (int j = 1; j <= 6; j++) {
									chunk.setBlockInChunk(cx, ySurface+j, cz, Blocks.WOOD);
								}
								
								for (int j = 7; j <= 12; j++) {
									int taper = j <= 9 ? 5/2 : 3/2;
									if (j == 12) taper = 0;
									for (int sx = cx - taper; sx <= cx+taper; sx++) {
										for (int sz = cz - taper; sz <= cz+taper; sz++) {
											int leafWorldX = newX * 16 + sx;
											int leafWorldZ = newZ * 16 + sz;
											long targetPos = packKey(leafWorldX >> 4, leafWorldZ >> 4);
											
											ChunkColumn target = loadedColumns.get(targetPos);
											if (target != chunk) {
												if (target.getSection((ySurface+j)>>4) == null) {
													target.initializeSection((ySurface+j)>>4);
												}
												
												target.getSection((ySurface+j)>>4).setDirty(true);
											}
											
											target.setBlockInChunk(sx&15, ySurface+j, sz&15, Blocks.LEAVES);
										}
									}
								}
							}
						}
					}
				}
			}
		}
		
		// Generate Meshes
		for (int x = -renderDistance; x < renderDistance; x++) {
			for (int z = -renderDistance; z < renderDistance; z++) {
				int newX = chunkX + x;
				int newZ = chunkZ + z;
				
				long key = packKey(newX, newZ);
				ChunkColumn chunk = loadedColumns.get(key);
				
				for (int sec = 0; sec < 16; sec++) {
					ChunkSection sectionObject = chunk.getSection(sec);
					if (sectionObject == null) continue;
					
					if (sectionObject.getMesh() == null || sectionObject.isDirty()) {
						sectionObject.initializeMesh();
						
						if (sectionObject.isDirty()) sectionObject.setDirty(false);
					}
					
					renderedColumns.put(key, chunk);
				}
			}
		}
		
		((Long2ObjectMap.FastEntrySet<ChunkColumn>) renderedColumns.long2ObjectEntrySet()).fastForEach(entry -> {
			long key = entry.getLongKey();
			int cx = getKeyX(key);
			int cz = getKeyZ(key);
			
			if (Math.abs(cx - chunkX) > renderDistance || Math.abs(cz - chunkZ) > renderDistance) {
				renderedColumns.remove(key);
			}
		});
	}
}
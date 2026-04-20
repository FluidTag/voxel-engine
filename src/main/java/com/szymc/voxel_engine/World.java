package com.szymc.voxel_engine;

import java.util.HashMap;
import java.util.Map;

import org.joml.Vector2i;
import org.joml.Vector3f;
import org.joml.SimplexNoise;

public class World {
	private final Map<Vector2i, ChunkColumn> renderedColumns = new HashMap<>();
	private final Map<Vector2i, ChunkColumn> loadedColumns = new HashMap<>();
	private final int renderDistance = 5;
	
	private byte noiseGetBlock(int wx, int wy, int wz) {
		float freq = 0.012f;
		
		float noise = org.joml.SimplexNoise.noise(wx*freq, wz*freq);
		int height = (int)(64 + (((noise+1.0f) / 2) * 15));
		
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
		
		Vector2i chunkIndex = new Vector2i(chunkX, chunkZ);
		if (!loadedColumns.containsKey(chunkIndex)) {
			return noiseGetBlock(wx, wy, wz);
		} else {
			ChunkColumn chunk = loadedColumns.get(chunkIndex);
			int localX = wx & 15;
			int localY = wy & 15;
			int localZ = wz & 15;
			
			ChunkSection section = chunk.getSection(chunkY);
			if (section == null) return Blocks.AIR;
			
			return chunk.getSection(chunkY).getLocalBlock(localX, localY, localZ);
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
	
	public Map<Vector2i, ChunkColumn> getLoaded() {
		return this.renderedColumns;
	}
	
	public void update(Vector3f playerPosition) {
		int chunkX = (int)playerPosition.x >> 4;
		int chunkZ = (int)playerPosition.z >> 4;
		
		// Generate Terrain / Data
		for (int x = -renderDistance; x < renderDistance; x++) {
			for (int z = -renderDistance; z < renderDistance; z++) {
				int newX = chunkX + x;
				int newZ = chunkZ + z;
				
				Vector2i pos = new Vector2i(newX, newZ);
				if (!renderedColumns.containsKey(pos)) {
					ChunkColumn chunk = loadedColumns.getOrDefault(pos, generateChunk(newX, newZ));
					loadedColumns.put(pos, chunk);
				}
			}
		}
		
		for (int x = -renderDistance; x < renderDistance; x++) {
			for (int z = -renderDistance; z < renderDistance; z++) { 
				int newX = chunkX + x;
				int newZ = chunkZ + z;
				
				Vector2i pos = new Vector2i(newX, newZ);
				ChunkColumn chunk = loadedColumns.get(pos);
				
				// Only generate trees past > 64 or cs (64/16) = 4 and above
				
				for (int cx = 0; cx < 16; cx++) {
					for (int cz = 0; cz < 16; cz++) {
						int ySurface = -1;
						for (int yCheck = 255; yCheck > 64; yCheck--) {
							if (chunk.getBlockInChunk(cx, yCheck, cz) == Blocks.GRASS) {
								ySurface = yCheck;
								break;
							}
						}
						
						if (ySurface != -1 && Math.random() > 0.99) {
							for (int h = 1; h <= 5; h++) {
								chunk.setBlockInChunk(cx, ySurface + h, cz, Blocks.WOOD);
							}
							
							for (int h = 6; h <= 8; h++) {
								for (int tx = cx-2; tx <= cx+2; tx++) {
									for (int tz = cz-2; tz <= cz+2; tz++) {
										if (tx < 0 || tx > 15 || tz < 0 || tz > 15) {
											Vector2i neighborPos = null;
											int neighborCX = newX + (tx >> 4);
		                                    int neighborCZ = newZ + (tz >> 4);
											neighborPos = new Vector2i(neighborCX, neighborCZ);
											if (!loadedColumns.containsKey(neighborPos)) {
												loadedColumns.put(neighborPos, generateChunk(neighborPos.x, neighborPos.y));
											}
											
											ChunkColumn nextNeighbor = loadedColumns.get(neighborPos);
											nextNeighbor.setBlockInChunk(tx&15, ySurface + h, tz&15, Blocks.LEAVES);
										} else {
											chunk.setBlockInChunk(tx, ySurface + h, tz, Blocks.LEAVES);
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
				
				Vector2i pos = new Vector2i(newX, newZ);
				ChunkColumn chunk = loadedColumns.get(pos);
				
				for (int sec = 0; sec < 16; sec++) {
					ChunkSection sectionObject = chunk.getSection(sec);
					if (sectionObject == null) continue;
					
					if (sectionObject.getMesh() == null) {
						sectionObject.initializeMesh();
					}
					
					renderedColumns.put(pos, chunk);
				}
			}
		}
		
		renderedColumns.entrySet().removeIf(entry -> {
			Vector2i p = entry.getKey();
			return (Math.abs(p.x - chunkX) > renderDistance + 2) || (Math.abs(p.y - chunkZ) > renderDistance + 2);
		});
	}
}
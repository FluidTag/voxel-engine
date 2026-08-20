package com.szymc.voxel_engine;


import java.util.concurrent.atomic.AtomicBoolean;


import it.unimi.dsi.fastutil.ints.IntArrayList;


public class ChunkColumn {
	private World worldReference;
	private ChunkSection[] sections = new ChunkSection[16];
	
	private int worldX = 0;
	private int worldZ = 0;
	public ChunkState state = ChunkState.EMPTY;
	public int dirtyBits = 0; // First 16 bits used to denote if a chunk section is dirty (Room to expand to 32 height later)
	
	public String toString() {
		return "Chunk (" + worldX + ", " + worldZ + ")\n" + state + "\n" +
					"Terrain Queued: " + terrainQueued.get() + "\n" +
					"Decoration Queued: " + decorationQueued.get() + "\n" +
					"Mesh Queued " + meshQueued.get() + "\n";
	}
	
	public static enum ChunkState {
		EMPTY,
		TERRAIN,
		DECORATED,
		MESHED;
		
		public ChunkState next() {
			ChunkState[] values = values();
			int nextOrdinal = this.ordinal()+1;
			if (nextOrdinal < values.length) {
				return values[nextOrdinal];
			}
			
			return this;
		}
		
		public boolean isAtleast(ChunkState other) {
			return this.ordinal() >= other.ordinal();
		}
	}
	
	AtomicBoolean terrainQueued = new AtomicBoolean();
	AtomicBoolean decorationQueued = new AtomicBoolean();
	AtomicBoolean meshQueued = new AtomicBoolean();
	
	public void applyTerrain(ChunkSection[] sections) {
		this.sections = sections;
	}
	
	// Returns 0 if null sector
	public byte getBlockInChunk(int cx, int cy, int cz) {
		int sectorI = cy >> 4;
		ChunkSection section = getSection(sectorI);
		if (section == null) return 0;
		
		return section.getLocalBlock(cx, cy & 15, cz);
	}

	public void setSectionDirty(int sectorI) {
		if (sections[sectorI] == null) return;

		dirtyBits |= (1 << sectorI);
	}

	public void updateSkyLighting(int yLevel) {
		for (int x = 0; x < 32; x++) {
			for (int z = 0; z < 32; z++) {
				int skyLight = 15;
				for (int y = yLevel; y >= 0; y--) {
					ChunkSection section = sections[y>>4];
					if (section == null) {
						y-=15; continue;
					};
					byte block = section.getLocalBlock(x, y&15, z);
					byte[] lightArr = section.getLightingData();

					if (block == Blocks.AIR || Texture.isLeafBlock[block] || Texture.isXShapedBlock[block]) {
						lightArr[(y&15)*32*32 + z*32 + x] &= (byte) ~(0xF << 4);
						lightArr[(y&15)*32*32 + z*32 + x] |= (byte) ((skyLight & 0xF) << 4 | (skyLight & 0xF));
					}

					if (block != Blocks.AIR && !Texture.isLeafBlock[block] && !Texture.isXShapedBlock[block]) {
						skyLight = 0;
					} else if (Texture.isLeafBlock[block] || Texture.isXShapedBlock[block]) {
						skyLight--;
					}
				}
			}
		}
	}

	// Also need to set neighboring chunk segments to dirty if its on a border
	public void setBlockInChunk(int cx, int cy, int cz, byte blockType) {
		int sectorI = cy >> 4;
		ChunkSection section = getSection(sectorI);
		if (section == null) section = initializeSection(sectorI);
		
		section.setBlock(cx, (cy & 15), cz, blockType);
		
		if (sectorI > 0 && (cy&15) == 0) {
			setSectionDirty(sectorI-1);
		}

		if (sectorI < 15 && (cy&15) == 15) {
			setSectionDirty(sectorI+1);
		}
		
		ChunkColumn xMinorChunk = worldReference.getLoadedChunkAtPos(worldX-1, worldZ);
		if (cx == 0 && xMinorChunk != null) {
			xMinorChunk.setSectionDirty(sectorI);
		}
		
		ChunkColumn xMajorChunk = worldReference.getLoadedChunkAtPos(worldX+1, worldZ);
		if (cx == 31 && xMajorChunk != null) {
			xMajorChunk.setSectionDirty(sectorI);
		}
		
		ChunkColumn zMinorChunk = worldReference.getLoadedChunkAtPos(worldX, worldZ-1);
		if (cz == 0 && zMinorChunk != null) {
			zMinorChunk.setSectionDirty(sectorI);
		}
		
		ChunkColumn zMajorChunk = worldReference.getLoadedChunkAtPos(worldX, worldZ+1);
		if (cz == 31 && zMajorChunk != null) {
			zMajorChunk.setSectionDirty(sectorI);
		}
	}
	
	public ChunkSection initializeSection(int yIndex) {
		if (yIndex > 15) throw new ArrayIndexOutOfBoundsException("World limit exceeded, attempting init of section index " + yIndex);
		sections[yIndex] = new ChunkSection(new byte[32*16*32], new byte[32*16*32], worldReference, worldX*32, yIndex*16, worldZ*32);
		updateSkyLighting(yIndex*16+3);
		return sections[yIndex];
	}
	
	public ChunkSection getSection(int yIndex) {
		if (yIndex > (sections.length-1)) return null;
		return sections[yIndex];
	}
	
	public void cleanupMeshes() {
		for (int i = 0; i < 16; i++) {
			ChunkSection sec = sections[i];
			if (sec == null) continue;
			
			Mesh mainMesh = sec.getMesh();
			Mesh waterMesh = sec.getWaterMesh();
			
			if (mainMesh != null) {
				mainMesh.cleanup();
				sec.setMesh(null);
			}
			
			if (waterMesh != null) {
				waterMesh.cleanup();
				sec.setWaterMesh(null);
			}
 		}
	}
	
	public ChunkColumn(World worldReference, int worldX, int worldZ) {
		this.worldReference = worldReference;
		this.worldX = worldX;
		this.worldZ = worldZ;
	}
	
	public ChunkColumn(World worldReference, int worldX, int worldZ, ChunkSection[] inSections) {
		this.worldReference = worldReference;
		this.sections = inSections;
		this.worldX = worldX;
		this.worldZ = worldZ;
	}
	
	public int getWorldX() {
		return this.worldX;
	}
	
	public int getWorldZ() {
		return this.worldZ;
	}
}
package com.szymc.voxel_engine;


import java.util.concurrent.atomic.AtomicBoolean;


import it.unimi.dsi.fastutil.ints.IntArrayList;


public class ChunkColumn {
	private World worldReference;
	private ChunkSection[] sections = new ChunkSection[16];
	
	private int worldX = 0;
	private int worldZ = 0;
	public ChunkState state = ChunkState.EMPTY;
	public int dirtyCount = 0;
	
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
	
	// Also need to set neighboring chunk segments to dirty if its on a border
	public void setBlockInChunk(int cx, int cy, int cz, byte blockType) {
		int sectorI = cy >> 4;
		ChunkSection section = getSection(sectorI);
		if (section == null) section = initializeSection(sectorI);
		
		section.setBlock(cx, (cy & 15), cz, blockType);
		
		if (cy == 15 && sectorI < 15) {
			ChunkSection target = getSection(sectorI+1);
			
			if (target != null) {
				if (!target.isDirty()) this.dirtyCount++;
				target.setDirty(true);
			}
		}
		
		if (cy == 0 && sectorI > 0) {
			ChunkSection target = getSection(sectorI-1);
			if (target != null) {
				if (!target.isDirty()) this.dirtyCount++;
				target.setDirty(true);
			}
		}
		
		ChunkColumn xMinorChunk = worldReference.getLoadedChunkAtPos(worldX-1, worldZ);
		if (cx == 0 && xMinorChunk != null) {
			ChunkSection target = xMinorChunk.getSection(sectorI);
			
			if (target != null) {
				if (!target.isDirty()) xMinorChunk.dirtyCount++;
				target.setDirty(true);
			}
		}
		
		ChunkColumn xMajorChunk = worldReference.getLoadedChunkAtPos(worldX+1, worldZ);
		if (cx == 31 && xMajorChunk != null) {
			ChunkSection target = xMajorChunk.getSection(sectorI);
			
			if (target != null) {
				if (!target.isDirty()) xMajorChunk.dirtyCount++;
				target.setDirty(true);	
			}
		}
		
		ChunkColumn zMinorChunk = worldReference.getLoadedChunkAtPos(worldX, worldZ-1);
		if (cz == 0 && zMinorChunk != null) {
			ChunkSection target = zMinorChunk.getSection(sectorI);
			
			if (target != null) {
				if (!target.isDirty()) zMinorChunk.dirtyCount++;
				target.setDirty(true);
			}
		}
		
		ChunkColumn zMajorChunk = worldReference.getLoadedChunkAtPos(worldX, worldZ+1);
		if (cz == 31 && zMajorChunk != null) {
			ChunkSection target = zMajorChunk.getSection(sectorI);


			if (target != null) {
				if (!target.isDirty()) zMajorChunk.dirtyCount++;
				target.setDirty(true);
			}
		}
	}
	
	public ChunkSection initializeSection(int yIndex) {
		sections[yIndex] = new ChunkSection(new byte[32*16*32], worldReference, worldX*32, yIndex*16, worldZ*32);
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
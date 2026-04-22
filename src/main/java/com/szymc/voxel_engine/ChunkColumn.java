package com.szymc.voxel_engine;
public class ChunkColumn {
	private World worldReference;
	private ChunkSection[] sections = new ChunkSection[16];
	private int worldX = 0;
	private int worldZ = 0;
	public boolean decorated = false;
	
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
		
		byte[][][] dat = section.getChunkData();
		dat[cx][cy & 15][cz] = blockType;
		
		if (cy == 15 && sectorI < 15) {
			getSection(sectorI + 1).setDirty(true);
		}
		
		if (cy == 0 && sectorI > 0) {
			getSection(sectorI + 1).setDirty(true);
		}
		
		ChunkColumn xMinorChunk = worldReference.getLoadedChunkAtPos(worldX-1, worldZ);
		if (cx == 0 && xMinorChunk != null) {
			if (xMinorChunk.getSection(sectorI) == null) {
				xMinorChunk.initializeSection(sectorI);
			}
			xMinorChunk.getSection(sectorI).setDirty(true);
		}
		
		ChunkColumn xMajorChunk = worldReference.getLoadedChunkAtPos(worldX+1, worldZ);
		if (cx == 15 && xMajorChunk != null) {
			if (xMajorChunk.getSection(sectorI) == null) {
				xMajorChunk.initializeSection(sectorI);
			}
			xMajorChunk.getSection(sectorI).setDirty(true);
		}
		
		ChunkColumn zMinorChunk = worldReference.getLoadedChunkAtPos(worldX, worldZ-1);
		if (cz == 0 && zMinorChunk != null) {
			if (zMinorChunk.getSection(sectorI) == null) {
				zMinorChunk.initializeSection(sectorI);
			}
			
			zMinorChunk.getSection(sectorI).setDirty(true);
		}
		
		ChunkColumn zMajorChunk = worldReference.getLoadedChunkAtPos(worldX, worldZ+1);
		if (cz == 15 && zMajorChunk != null) {
			if (zMajorChunk.getSection(sectorI) == null) {
				zMajorChunk.initializeSection(sectorI);
			}
			
			zMajorChunk.getSection(sectorI).setDirty(true);
		}
	}
	
	public ChunkSection initializeSection(int yIndex) {
		sections[yIndex] = new ChunkSection(new byte[16][16][16], worldReference, worldX*16, yIndex*16, worldZ*16);
		return sections[yIndex];
	}
	
	public ChunkSection getSection(int yIndex) {
		if (yIndex > (sections.length-1)) return null;
		return sections[yIndex];
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
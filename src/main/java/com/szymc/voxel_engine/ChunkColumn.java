package com.szymc.voxel_engine;

public class ChunkColumn {
	private World worldReference;
	private ChunkSection[] sections = new ChunkSection[16];
	private int worldX = 0;
	private int worldZ = 0;
	private boolean isDirty = false;
	
	public boolean isDirty() {
		return this.isDirty;
	}
	
	public void setDirty(boolean dirty) {
		this.isDirty = dirty;
	}
	
	// Returns 0 if null sector
	public byte getBlockInChunk(int cx, int cy, int cz) {
		int sectorI = cy >> 4;
		ChunkSection section = getSection(sectorI);
		if (section == null) return 0;
		
		return section.getLocalBlock(cx, cy & 15, cz);
	}
	
	public void setBlockInChunk(int cx, int cy, int cz, byte blockType) {
		int sectorI = cy >> 4;
		ChunkSection section = getSection(sectorI);
		if (section == null) section = initializeSection(sectorI);
		
		byte[][][] dat = section.getChunkData();
		dat[cx][cy & 15][cz] = blockType;
	}
	
	private ChunkSection initializeSection(int yIndex) {
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

package com.szymc.voxel_engine;

public class ChunkColumn {
	private ChunkSection[] sections = new ChunkSection[16];
	private int worldX = 0;
	private int worldZ = 0;
	
	public ChunkSection getSection(int yIndex) {
		if (yIndex > (sections.length-1)) return null;
		return sections[yIndex];
	}
	
	public ChunkColumn(int worldX, int worldZ, ChunkSection[] inSections) {
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

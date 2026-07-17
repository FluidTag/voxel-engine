package com.szymc.voxel_engine;
public class MeshTask {
	public int cx, cz;
	public ChunkColumn chunk;
	private ChunkColumn xMajor, xMinor, zMajor, zMinor;
	public int targetedCSection = -1;
	
	public MeshTask(int cx, int cs, int cz,
			ChunkColumn chunk, ChunkColumn xMajor, ChunkColumn xMinor, ChunkColumn zMajor, ChunkColumn zMinor) {
		this.chunk = chunk;
		this.xMajor = xMajor;
		this.xMinor = xMinor;
		this.zMajor = zMajor;
		this.zMinor = zMinor;
		
		this.cx = cx;
		this.targetedCSection = cs;
		this.cz = cz;
	}
	
	public void runTargeted() {
		ChunkSection section = chunk.getSection(targetedCSection);
		if (section == null) return;
		
		ChunkSection xMajSec = xMajor.getSection(targetedCSection);
		ChunkSection xMinSec = xMinor.getSection(targetedCSection);
		ChunkSection yMajSec = targetedCSection < 15 ? chunk.getSection(targetedCSection+1) : null;
		ChunkSection yMinSec = targetedCSection > 0 ? chunk.getSection(targetedCSection-1) : null;
		ChunkSection zMajSec = zMajor.getSection(targetedCSection);
		ChunkSection zMinSec = zMinor.getSection(targetedCSection);
		
		section.meshSection(xMajSec, xMinSec, yMajSec, yMinSec, zMajSec, zMinSec);
	}
	
	public MeshTask(int cx, int cz, ChunkColumn chunk, ChunkColumn xMajor, ChunkColumn xMinor, ChunkColumn zMajor, ChunkColumn zMinor) {
		this.chunk = chunk;
		this.xMajor = xMajor;
		this.xMinor = xMinor;
		this.zMajor = zMajor;
		this.zMinor = zMinor;
		
		this.cx = cx;
		this.cz = cz;
	}
	
	public void runTask() {
		for (int i = 0; i < 16; i++) {
			ChunkSection section = chunk.getSection(i);
			if (section == null) continue;
			
			ChunkSection xMajSec = xMajor.getSection(i);
			ChunkSection xMinSec = xMinor.getSection(i);
			ChunkSection yMajSec = i < 15 ? chunk.getSection(i+1) : null;
			ChunkSection yMinSec = i > 0 ? chunk.getSection(i-1) : null;
			ChunkSection zMajSec = zMajor.getSection(i);
			ChunkSection zMinSec = zMinor.getSection(i);
			
			section.meshSection(xMajSec, xMinSec, yMajSec, yMinSec, zMajSec, zMinSec);
		}
	}
}
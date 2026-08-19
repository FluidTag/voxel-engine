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
	
	public void runFullMeshTask() {
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

	public void fastTargetDirty(int dirtyBits) {
		int n = dirtyBits;
		while (n != 0) {
			int index = Integer.numberOfTrailingZeros(n);
			ChunkSection xMajSec = xMajor.getSection(index);
			ChunkSection xMinSec = xMinor.getSection(index);
			ChunkSection yMajSec = index < 15 ? chunk.getSection(index+1) : null;
			ChunkSection yMinSec = index > 0 ? chunk.getSection(index-1) : null;
			ChunkSection zMajSec = zMajor.getSection(index);
			ChunkSection zMinSec = zMinor.getSection(index);

			ChunkSection section = chunk.getSection(index);
			section.meshSection(xMajSec, xMinSec, yMajSec, yMinSec, zMajSec, zMinSec);

			n &= (n - 1);
		}
	}
}
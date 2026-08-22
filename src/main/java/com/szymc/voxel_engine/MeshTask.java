package com.szymc.voxel_engine;
public class MeshTask {
	public int cx, cz;
	public ChunkColumn chunk;
	private ChunkColumn xMajor, xMinor, zMajor, zMinor, xMajorZmajor, xMajorZminor, xMinorZmajor, xMinorZminor;
	public int targetedCSection = -1;
	
	public MeshTask(int cx, int cs, int cz,
			ChunkColumn chunk, ChunkColumn xMajor, ChunkColumn xMinor, ChunkColumn zMajor, ChunkColumn zMinor, ChunkColumn xMajorZmajor, ChunkColumn xMajorZminor, ChunkColumn xMinorZmajor, ChunkColumn xMinorZminor) {
		this.chunk = chunk;
		this.xMajor = xMajor;
		this.xMinor = xMinor;
		this.zMajor = zMajor;
		this.zMinor = zMinor;
		this.xMajorZmajor = xMajorZmajor;
		this.xMajorZminor = xMajorZminor;
		this.xMinorZmajor = xMinorZmajor;
		this.xMinorZminor = xMinorZminor;
		
		this.cx = cx;
		this.targetedCSection = cs;
		this.cz = cz;
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
			ChunkSection xMajTop = i < 15 ? xMajor.getSection(i+1) : null;
			ChunkSection xMajBottom = i > 0 ? xMajor.getSection(i-1) : null;

			ChunkSection xMinSec = xMinor.getSection(i);
			ChunkSection xMinTop = i < 15 ? xMinor.getSection(i+1) : null;
			ChunkSection xMinBottom = i > 0 ? xMinor.getSection(i-1) : null;

			ChunkSection yMajSec = i < 15 ? chunk.getSection(i+1) : null;
			ChunkSection yMinSec = i > 0 ? chunk.getSection(i-1) : null;

			ChunkSection zMajSec = zMajor.getSection(i);
			ChunkSection zMajTop = i < 15 ? zMajor.getSection(i+1) : null;
			ChunkSection zMajBottom = i > 0 ? zMajor.getSection(i-1) : null;

			ChunkSection zMinSec = zMinor.getSection(i);
			ChunkSection zMinTop = i < 15 ? zMinor.getSection(i+1) : null;
			ChunkSection zMinBottom = i > 0 ? zMinor.getSection(i-1) : null;
			
			section.meshSection(xMajSec, xMinSec, yMajSec, yMinSec, zMajSec, zMinSec, xMinTop, xMinBottom, xMajTop, xMajBottom, zMinTop, zMinBottom, zMajTop, zMajBottom);
		}
	}

	public void fastTargetDirty(int dirtyBits) {
		int n = dirtyBits;
		while (n != 0) {
			int i = Integer.numberOfTrailingZeros(n);
			ChunkSection section = chunk.getSection(i);
			if (section == null) {
				n &= (n - 1);
				continue;
			};

			ChunkSection xMajSec = xMajor.getSection(i);
			ChunkSection xMajTop = i < 15 ? xMajor.getSection(i+1) : null;
			ChunkSection xMajBottom = i > 0 ? xMajor.getSection(i-1) : null;

			ChunkSection xMinSec = xMinor.getSection(i);
			ChunkSection xMinTop = i < 15 ? xMinor.getSection(i+1) : null;
			ChunkSection xMinBottom = i > 0 ? xMinor.getSection(i-1) : null;

			ChunkSection yMajSec = i < 15 ? chunk.getSection(i+1) : null;
			ChunkSection yMinSec = i > 0 ? chunk.getSection(i-1) : null;

			ChunkSection zMajSec = zMajor.getSection(i);
			ChunkSection zMajTop = i < 15 ? zMajor.getSection(i+1) : null;
			ChunkSection zMajBottom = i > 0 ? zMajor.getSection(i-1) : null;

			ChunkSection zMinSec = zMinor.getSection(i);
			ChunkSection zMinTop = i < 15 ? zMinor.getSection(i+1) : null;
			ChunkSection zMinBottom = i > 0 ? zMinor.getSection(i-1) : null;

			section.meshSection(xMajSec, xMinSec, yMajSec, yMinSec, zMajSec, zMinSec, xMinTop, xMinBottom, xMajTop, xMajBottom, zMinTop, zMinBottom, zMajTop, zMajBottom);

			n &= (n - 1);
		}
	}
}
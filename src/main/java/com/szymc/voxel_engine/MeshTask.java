package com.szymc.voxel_engine;
public class MeshTask {
	public int cx, cz;
	public ChunkColumn chunk;
	private ChunkColumn xMajor, xMinor, zMajor, zMinor, xMajorZmajor, xMajorZminor, xMinorZmajor, xMinorZminor;
	private static final ThreadLocal<GreedyMesher.SectionContext> localCtx = ThreadLocal.withInitial(GreedyMesher.SectionContext::new);
	
	public MeshTask(int cx, int cz,
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
		this.cz = cz;
	}
	
	public void runFullMeshTask() {
		GreedyMesher.SectionContext ctx = localCtx.get();

		for (int i = 0; i < 16; i++) {
			ChunkSection section = chunk.getSection(i);
			if (section == null) continue;

			ctx.xMajor = xMajor.getSection(i);
			ctx.xMajorTop = i < 15 ? xMajor.getSection(i+1) : null;
			ctx.xMajorBottom = i > 0 ? xMajor.getSection(i-1) : null;

			ctx.xMajorZmajor = xMajorZmajor.getSection(i);
			ctx.xMajorZmajorTop = i < 15 ? xMajorZmajor.getSection(i+1) : null;
			ctx.xMajorZmajorBottom = i > 0 ? xMajorZmajor.getSection(i-1) : null;

			ctx.xMajorZminor = xMajorZminor.getSection(i);
			ctx.xMajorZminorTop = i < 15 ? xMajorZminor.getSection(i+1) : null;
			ctx.xMajorZminorBottom = i > 0 ? xMajorZminor.getSection(i-1) : null;

			ctx.xMinor = xMinor.getSection(i);
			ctx.xMinorTop = i < 15 ? xMinor.getSection(i+1) : null;
			ctx.xMinorBottom = i > 0 ? xMinor.getSection(i-1) : null;

			ctx.xMinorZmajor = xMinorZmajor.getSection(i);
			ctx.xMinorZmajorTop = i < 15 ? xMinorZmajor.getSection(i+1) : null;
			ctx.xMinorZmajorBottom = i > 0 ? xMinorZmajor.getSection(i-1) : null;

			ctx.xMinorZminor = xMinorZminor.getSection(i);
			ctx.xMinorZminorTop = i < 15 ? xMinorZminor.getSection(i+1) : null;
			ctx.xMinorZminorBottom = i > 0 ? xMinorZminor.getSection(i-1) : null;

			ctx.yMajor = i < 15 ? chunk.getSection(i+1) : null;
			ctx.yMinor = i > 0 ? chunk.getSection(i-1) : null;

			ctx.zMajor = zMajor.getSection(i);
			ctx.zMajorTop = i < 15 ? zMajor.getSection(i+1) : null;
			ctx.zMajorBottom = i > 0 ? zMajor.getSection(i-1) : null;

			ctx.zMinor = zMinor.getSection(i);
			ctx.zMinorTop = i < 15 ? zMinor.getSection(i+1) : null;
			ctx.zMinorBottom = i > 0 ? zMinor.getSection(i-1) : null;
			
			section.meshSection(ctx);
		}
	}

	public void fastTargetDirty(int dirtyBits) {
		int n = dirtyBits;
		GreedyMesher.SectionContext ctx = localCtx.get();

		while (n != 0) {
			int i = Integer.numberOfTrailingZeros(n);
			ChunkSection section = chunk.getSection(i);
			if (section == null) {
				n &= (n - 1);
				continue;
			};

			ctx.xMajor = xMajor.getSection(i);
			ctx.xMajorTop = i < 15 ? xMajor.getSection(i+1) : null;
			ctx.xMajorBottom = i > 0 ? xMajor.getSection(i-1) : null;

			ctx.xMajorZmajor = xMajorZmajor.getSection(i);
			ctx.xMajorZmajorTop = i < 15 ? xMajorZmajor.getSection(i+1) : null;
			ctx.xMajorZmajorBottom = i > 0 ? xMajorZmajor.getSection(i-1) : null;

			ctx.xMajorZminor = xMajorZminor.getSection(i);
			ctx.xMajorZminorTop = i < 15 ? xMajorZminor.getSection(i+1) : null;
			ctx.xMajorZminorBottom = i > 0 ? xMajorZminor.getSection(i-1) : null;

			ctx.xMinor = xMinor.getSection(i);
			ctx.xMinorTop = i < 15 ? xMinor.getSection(i+1) : null;
			ctx.xMinorBottom = i > 0 ? xMinor.getSection(i-1) : null;

			ctx.xMinorZmajor = xMinorZmajor.getSection(i);
			ctx.xMinorZmajorTop = i < 15 ? xMinorZmajor.getSection(i+1) : null;
			ctx.xMinorZmajorBottom = i > 0 ? xMinorZmajor.getSection(i-1) : null;

			ctx.xMinorZminor = xMinorZminor.getSection(i);
			ctx.xMinorZminorTop = i < 15 ? xMinorZminor.getSection(i+1) : null;
			ctx.xMinorZminorBottom = i > 0 ? xMinorZminor.getSection(i-1) : null;

			ctx.yMajor = i < 15 ? chunk.getSection(i+1) : null;
			ctx.yMinor = i > 0 ? chunk.getSection(i-1) : null;

			ctx.zMajor = zMajor.getSection(i);
			ctx.zMajorTop = i < 15 ? zMajor.getSection(i+1) : null;
			ctx.zMajorBottom = i > 0 ? zMajor.getSection(i-1) : null;

			ctx.zMinor = zMinor.getSection(i);
			ctx.zMinorTop = i < 15 ? zMinor.getSection(i+1) : null;
			ctx.zMinorBottom = i > 0 ? zMinor.getSection(i-1) : null;

			section.meshSection(ctx);

			n &= (n - 1);
		}
	}
}
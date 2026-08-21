package com.szymc.voxel_engine;


import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.Queue;
import java.util.concurrent.atomic.AtomicBoolean;


import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntArrayFIFOQueue;


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

	public void setSkylight(int amount, int cx, int y, int cz) {
		ChunkSection sec = sections[y>>4];
		if (sec == null) return;

		byte[] dat = sec.getLightingData();
		dat[(y&15)*32*32 + cz*32 + cx] &= (byte) ~(0xF << 4);
		dat[(y&15)*32*32 + cz*32 + cx] |= (byte) ((amount & 0xF) << 4);
	}

	public int getSkylight(int cx, int y, int cz) {
		ChunkSection sec = sections[y>>4];
		if (sec == null) return 15;
		byte[] dat = sec.getLightingData();

		return (dat[(y&15)*32*32 + cz*32 + cx] >>> 4) & 0xF;
	}

	private static int packLightsource(int x, int y, int z) {
		return ((x | (x >>> 22)) & 0x3FF) | (((y | (y >>> 22)) & 0x3FF) << 10) | (((z | (z >>> 22)) & 0x3FF) << 20);
	}

	private final static int[][] directions = {
			{1, 0, 0}, {-1, 0, 0},
			{0, 1, 0}, {0, -1, 0},
			{0, 0, 1}, {0, 0, -1}
	};

	// NOTE: All 8 surronding chunks (including corners) must be checked for loaded prior to running
	private final static IntArrayFIFOQueue pendingSkyPropQueue = new IntArrayFIFOQueue(4089);
	private final static ChunkColumn[] tempChunkMap = new ChunkColumn[9];
	public void updateSkyLighting() {
		for (int x = 0; x < 32; x++) {
			for (int z = 0; z < 32; z++) {
				int skyLight = 15;
				for (int y = 255; y >= 0; y--) {
					ChunkSection sec = sections[y>>4];
					if (sec == null) continue;

					byte block = sec.getLocalBlock(x, y&15, z);
					if (block == 0 && !Texture.isXShapedBlock[block] && !Texture.isLeafBlock[block]) {
						setSkylight(skyLight, x, y, z);
						pendingSkyPropQueue.enqueue(packLightsource(x, y, z));
					} else if (block != 0 && (Texture.isXShapedBlock[block] || Texture.isLeafBlock[block])) {
						skyLight = Math.max(0, skyLight-1);
						setSkylight(skyLight, x, y, z);

						if (skyLight > 0) pendingSkyPropQueue.enqueue(packLightsource(x, y, z));
					} else {
						break;
					}
				}
			}
		}

		ChunkColumn xMajZmin = worldReference.getLoadedChunkAtPos(worldX+1, worldZ-1);
		ChunkColumn xMajZmax = worldReference.getLoadedChunkAtPos(worldX+1, worldZ+1);
		ChunkColumn xMinZmin = worldReference.getLoadedChunkAtPos(worldX-1, worldZ-1);
		ChunkColumn xMinZmax = worldReference.getLoadedChunkAtPos(worldX-1, worldZ+1);

		ChunkColumn xLeft = worldReference.getLoadedChunkAtPos(worldX-1, worldZ);
		ChunkColumn xRight = worldReference.getLoadedChunkAtPos(worldX+1, worldZ);
		ChunkColumn zTop = worldReference.getLoadedChunkAtPos(worldX, worldZ+1);
		ChunkColumn zBottom = worldReference.getLoadedChunkAtPos(worldX, worldZ-1);

		tempChunkMap[0] = xMinZmax; tempChunkMap[1] = zTop; tempChunkMap[2] = xMajZmax;
		tempChunkMap[3] = xLeft; tempChunkMap[4] = this; tempChunkMap[5] = xRight;
		tempChunkMap[6] = xMinZmin; tempChunkMap[7] = zBottom; tempChunkMap[8] = xMajZmin;

		while (!pendingSkyPropQueue.isEmpty()) {
			int source = pendingSkyPropQueue.dequeueInt();
			int x = ((source & 0x200) << 22) | (source & 0x1FF);
			int y = (((source >>> 10) & 0x200) << 22) | ((source >>> 10) & 0x1FF);
			int z = (((source >>> 20) & 0x200) << 22) | ((source >>> 20) & 0x1FF);
			int xInd, zInd;
			if (x >= 0 && x <= 31) xInd = 1; else if (x < 0) xInd = 0; else xInd = 2;
			if (z >= 0 && z <= 31) zInd = 1; else if (z < 0) zInd = 0; else zInd = 2;

			ChunkColumn primaryChunk = tempChunkMap[zInd*3+xInd];
			if (primaryChunk == null) continue;

			int currentLight = primaryChunk.getSkylight(x&31, y, z&31);
			if (currentLight <= 1) continue;

			for (int[] dir : directions) {
				int nx = (x + dir[0]);
				int ny = y + dir[1];
				int nz = (z + dir[2]);
				int nxInd, nzInd;
				if (nx >= 0 && nx <= 31) nxInd = 1; else if (nx < 0) nxInd = 0; else nxInd = 2;
				if (nz >= 0 && nz <= 31) nzInd = 1; else if (nz < 0) nzInd = 0; else nzInd = 2;

				ChunkColumn chunk = tempChunkMap[nzInd*3+nxInd];
				if (chunk == null) continue;
				ChunkSection sec = chunk.getSection(ny >> 4);
				if (sec == null) continue;
				byte block = sec.getLocalBlock(nx&31, ny&15, nz&31);

				int nextLight = chunk.getSkylight(nx&31, ny, nz&31);
				int targetLight = currentLight-1;

				if ((targetLight > nextLight) && (block == Blocks.AIR || Texture.isXShapedBlock[block] || Texture.isLeafBlock[block])) {
					chunk.setSkylight(targetLight, nx&31, ny, nz&31);
					pendingSkyPropQueue.enqueue(packLightsource(nx, ny, nz));
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
		updateSkyLighting();
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
package com.szymc.voxel_engine;


import it.unimi.dsi.fastutil.ints.IntArrayList;

import java.util.Arrays;
import java.util.concurrent.atomic.AtomicBoolean;


public class ChunkColumn {
	private World worldReference;
	private ChunkSection[] sections = new ChunkSection[16];
	private int[] heightMap;

	private int worldX = 0;
	private int worldZ = 0;
	public ChunkState state = ChunkState.EMPTY;
	public int dirtyBits = 0; // First 16 bits used to denote if a chunk section is dirty (Room to expand to 32 height later)
	public boolean processLightDirty = false;
	public boolean hasBeenPlayerModified = false;


	public String toString() {
		return "Chunk (" + worldX + ", " + worldZ + ")\n" + state + "\n" +
					"Terrain Queued: " + terrainQueued.get() + "\n" +
					"Decoration Queued: " + decorationQueued.get() + "\n" +
					"Light Queued: " + lightQueued.get() + "\n" +
					"Mesh Queued " + meshQueued.get() + "\n";
	}
	
	public static enum ChunkState {
		EMPTY,
		TERRAIN,
		DECORATED,
		LIGHT,
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
	AtomicBoolean lightQueued = new AtomicBoolean();
	AtomicBoolean meshQueued = new AtomicBoolean();
	
	public void applyTerrain(ChunkSection[] sections) {
		this.sections = sections;
	}
	public void applyHeightmap(int[] heightMap) {this.heightMap = heightMap;}
	public int readTerrainHeight(int x, int z) {
		return heightMap[z * 32 + x];
	}

	public void updateTerrainHeight(int x, int z, int newValue) {
		heightMap[z * 32 + x] = newValue;
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

	public void setSkylight(int cx, int y, int cz, int amount) {
		ChunkSection sec = sections[y>>4];
		if (sec == null) return;

		byte[] dat = sec.getLightingData();
		dat[(y&15)*32*32 + cz*32 + cx] &= (byte) ~(0xF << 4);
		dat[(y&15)*32*32 + cz*32 + cx] |= (byte) ((amount & 0xF) << 4);
	}

	public byte getSkylight(int cx, int y, int cz) {
		ChunkSection sec = sections[y>>4];
		if (sec == null) return 15;
		byte[] dat = sec.getLightingData();

		return (byte) ((dat[(y&15)*32*32 + cz*32 + cx] >>> 4) & 0xF);
	}

	public void setBlockLight(int cx, int y, int cz, int amount) {
		ChunkSection sec = sections[y>>4];
		if (sec == null) return;

		byte[] dat = sec.getLightingData();
		dat[(y&15)*32*32 + cz*32 + cx] &= (byte) ~(0xF);
		dat[(y&15)*32*32 + cz*32 + cx] |= (byte) ((amount & 0xF));
	}

	public byte getBlockLight(int cx, int y, int cz) {
		ChunkSection sec = sections[y>>4];
		if (sec == null) return 15;
		byte[] dat = sec.getLightingData();

		return (byte) (dat[(y&15)*32*32 + cz*32 + cx] & 0xF);
	}

	public void clearChunkLighting() {
		for (int i = 0; i < 16; i++) {
			ChunkSection sec = getSection(i);
			if (sec == null) continue;

			byte[] lightDat = sec.getLightingData();
			Arrays.fill(lightDat, (byte)0);
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

		byte[] lightingData = sections[yIndex].getLightingData();
		Arrays.fill(lightingData, (byte)(0xF << 4));

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

	public byte[] serialize() {
		int targetedSections = 0;
		int size = 0;
		for (int i = 0; i < 16; i++) {
			if (sections[i] != null) size++;
		}

		byte[] data = new byte[2 + (72 + 32*16*32 + 32*16*32 + 1 + 4*64)*size];
		int baseSize = 72 + 32*16*32 + 32*16*32 + 1 + 4*64;
		int realIndex = 0;

		for (int i = 0; i < 16; i++) {
			ChunkSection sec = sections[i];
			if (sec == null) continue;
			targetedSections |= (1 << i);

			PaletteContainer palette = sec.getRawPaletteContainer();
			byte[] rawPaletteBytes = palette.serialize();
			int offset = 2 + baseSize*realIndex;

			System.arraycopy(rawPaletteBytes, 0, data, offset, rawPaletteBytes.length);
			byte[] light = sec.getLightingData();

			System.arraycopy(light, 0, data, offset + 72 + 32*16*32, light.length);

			IntArrayList sourceCache = sec.getLightBlocks();
			data[offset + 72 + 32*16*32 + light.length] = (byte) sourceCache.size();
			byte[] createdByteSourceArr = new byte[sourceCache.size()*4];

			for (int j = 0; j < sourceCache.size(); j++) {
				int val = sourceCache.getInt(j);
				int baseInd = j*4;

				createdByteSourceArr[baseInd] = (byte) ((val >>> 24)&0xFF);
				createdByteSourceArr[baseInd+1] = (byte) ((val >>> 16)&0xFF);
				createdByteSourceArr[baseInd+2] = (byte) ((val >>> 8)&0xFF);
				createdByteSourceArr[baseInd+3] = (byte) (val&0xFF);
			}

			System.arraycopy(createdByteSourceArr, 0, data, offset + 72 + 32*16*32 + light.length + 1, createdByteSourceArr.length);
			realIndex++;
		}

		data[0] = (byte) (targetedSections & 0xFF);
		data[1] = (byte) ((targetedSections >>> 8) & 0xFF);

		return data;
	}

	public static ChunkColumn deserialize(World worldReference, int wx, int wz, byte[] encodedData) {
		ChunkSection[] createdSections = new ChunkSection[16];
		int targetedSections = (encodedData[0] & 0xFF) | ((encodedData[1] & 0xFF) << 8);
        int baseSize = 72 + 32*16*32 + 32*16*32 + 1 + 4*64;
		int realIndex = 0;
		//System.out.println(Integer.toBinaryString(targetedSections));
		while (targetedSections != 0) {
			int i = Integer.numberOfTrailingZeros(targetedSections);
			int offset = 2 + baseSize*realIndex;
			byte palSize = (byte) (encodedData[offset] & 0xFF);

			byte[] palette = Arrays.copyOfRange(encodedData, offset+1, offset+1+palSize);
			byte[] data = Arrays.copyOfRange(encodedData, offset+72,  offset+72 + 32*16*32);

			byte[] lighting = Arrays.copyOfRange(encodedData, offset+72 + 32*16*32, offset+72 + 32*16*32 + 32*16*32);
			ChunkSection section = new ChunkSection(PaletteContainer.deserialize(palette, data), lighting, worldReference, wx*32, 16*i, wz*32);
			createdSections[i] = section;

			targetedSections &= targetedSections -1;
			realIndex++;
		}

		return new ChunkColumn(worldReference, wx, wz, createdSections);
	}
}
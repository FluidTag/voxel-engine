package com.szymc.voxel_engine;
import java.util.Arrays;

import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.shorts.ShortArrayList;


public class ChunkSection {
	private PaletteContainer blockData = new PaletteContainer();
	private byte[] lightLevels;
	private final int worldX, worldY, worldZ;
	private World worldReference;
	private Mesh mesh = null;
	private Mesh waterMesh = null;
	private final IntArrayList lightBlocks = new IntArrayList();

	public ChunkSection(byte[] data, byte[] skylightData, World worldReference, int wx, int wy, int wz) {
		lightLevels = skylightData;
		for (int y = 0; y < 16; y++) {
			for (int z = 0; z < 32; z++) {
				for (int x = 0; x < 32; x++) {
					setBlock(x, y, z, data[y*32*32 + z*32 + x]);
				}
			}
		}

		this.worldReference = worldReference;

		this.worldX = wx;
		this.worldY = wy;
		this.worldZ = wz;
	}

	public void setBlock(int x, int y, int z, byte block) {
		if (x < 0 || x > 31) throw new IndexOutOfBoundsException();
		if (y < 0 || y > 15) throw new IndexOutOfBoundsException();
		if (z < 0 || z > 31) throw new IndexOutOfBoundsException();

		byte oldBlock = getLocalBlock(x, y, z);
		if (Texture.lightLevels[block] > 0) {
			int data = (x & 0x1F) | ((y & 0xFF) << 5) | ((z & 0x1F) << 13) | ((Texture.lightLevels[block] & 0xFF) << 18);
			lightBlocks.add(data);
		} else if (block == Blocks.AIR && Texture.lightLevels[oldBlock] > 0) {
			int data = (x & 0x1F) | ((y & 0xFF) << 5) | ((z & 0x1F) << 13) | ((Texture.lightLevels[oldBlock] & 0xFF) << 18);

			lightBlocks.removeIf(item -> item == data);
		}

		blockData.writeBlock(x, y, z, block);
	}

	public byte getLocalBlock(int x, int y, int z) {
		if (x < 0 || x > 31) throw new IndexOutOfBoundsException();
		if (y < 0 || y > 15) throw new IndexOutOfBoundsException();
		if (z < 0 || z > 31) throw new IndexOutOfBoundsException();

		return blockData.readBlock(x, y, z);
	}

	public byte[] getChunkData() {
		return blockData.toByteArray();
	}
	public byte[] getLightingData() {return lightLevels;}

	public int getWorldX() {
		return this.worldX;
	}

	public int getWorldY() {
		return this.worldY;
	}

	public int getWorldZ() {
		return this.worldZ;
	}

	public IntArrayList getLightBlocks() {
		return this.lightBlocks;
	}

	public SectionMeshResult meshResult;
	public void meshSection(GreedyMesher.SectionContext ctx) {
		GreedyMesher mesher = new GreedyMesher(this);
		meshResult = mesher.generateMeshData(ctx);
	}

	public Mesh getMesh() {
		return this.mesh;
	}

	// Chunk must be re-meshed if it is dirty, as either a neighbor chunk impacts
	// faces, or a player does an action

	// Cache nearby for meshing / face visibility
	// References passed in

	public void setMesh(Mesh mesh) {
		this.mesh = mesh;
	}

	public void setWaterMesh(Mesh mesh) {
		this.waterMesh = mesh;
	}

	public Mesh getWaterMesh() {
		return this.waterMesh;
	}
}
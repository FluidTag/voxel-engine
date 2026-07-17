package com.szymc.voxel_engine;
import java.util.Arrays;

import it.unimi.dsi.fastutil.ints.IntArrayList;


public class ChunkSection {
	PaletteContainer blockData = new PaletteContainer();
	private final int worldX, worldY, worldZ;
	private World worldReference;
	private Mesh mesh = null;
	private Mesh waterMesh = null;
	private boolean dirtyMesh = false;

	public ChunkSection(byte[] data, World worldReference, int wx, int wy, int wz) {
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

	public int getWorldX() {
		return this.worldX;
	}

	public int getWorldY() {
		return this.worldY;
	}

	public int getWorldZ() {
		return this.worldZ;
	}

	public SectionMeshResult meshResult;
	public void meshSection(ChunkSection xMajor, ChunkSection xMinor, ChunkSection yMajor, ChunkSection yMinor, ChunkSection zMajor, ChunkSection zMinor) {
		GreedyMesher mesher = new GreedyMesher(this);
		meshResult = mesher.generateSectionMesh(xMajor, xMinor, yMajor, yMinor, zMajor, zMinor);
	}

	public Mesh getMesh() {
		return this.mesh;
	}

	// Chunk must be re-meshed if it is dirty, as either a neighbor chunk impacts
	// faces, or a player does an action
	public boolean isDirty() {
		return this.dirtyMesh;
	}

	public void setDirty(boolean status) {
		this.dirtyMesh = status;
	}

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
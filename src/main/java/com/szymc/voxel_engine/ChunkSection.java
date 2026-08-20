package com.szymc.voxel_engine;
import java.util.Arrays;

import it.unimi.dsi.fastutil.ints.IntArrayList;


public class ChunkSection {
	private PaletteContainer blockData = new PaletteContainer();
	private byte[] lightLevels;
	private final int worldX, worldY, worldZ;
	private World worldReference;
	private Mesh mesh = null;
	private Mesh waterMesh = null;
	private final static boolean[] visitedLight = new boolean[32*16*32];

	private void introduceLightSource(int lightAmount, int cx, int cy, int cz) {
		if (lightAmount <= 0) return;

		if (cx < 0 || cx > 31 || cy < 0 || cy > 15 || cz < 0 || cz > 31) return;
		int index = cy * 32 * 32 + cz * 32 + cx;

		byte block = getLocalBlock(cx, cy, cz);
		byte currentLight = lightLevels[index];
		if (block != 0 && !Texture.isXShapedBlock[block] && !Texture.isLeafBlock[block]) return;

		if (visitedLight[index] && lightAmount <= currentLight) return;
		visitedLight[index] = true;

		lightLevels[index] &= ~(0xF);
		lightLevels[index] |= (byte) (lightAmount & 0xF);

		System.out.println(lightAmount + " at " + cx + ", " + cy + ", " + cz);

		// 6 Cardinal Orthogonal Directions (Up, Down, North, South, East, West)
		int[][] directions = {
				{-1,  0,  0}, { 1,  0,  0},
				{ 0, -1,  0}, { 0,  1,  0},
				{ 0,  0, -1}, { 0,  0,  1}
		};

		for (int[] d : directions) {
			introduceLightSource(lightAmount - 2, cx + d[0], cy + d[1], cz + d[2]);
		}
	}

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

		blockData.writeBlock(x, y, z, block);
		if (Texture.lightLevels[block] > 0) {
			Arrays.fill(visitedLight, false);
			introduceLightSource(Texture.lightLevels[block], x, y, z);
		}
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
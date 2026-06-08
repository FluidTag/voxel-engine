package com.szymc.voxel_engine;
public enum Biome {
	OCEAN(Blocks.SAND, Blocks.SAND, Blocks.GRAVEL, 45, 4),
	PLAINS(Blocks.GRASS, Blocks.DIRT, Blocks.STONE, 70, 5),
	DESERT(Blocks.SAND, Blocks.SAND, Blocks.SAND, 71, 2),
	MOUNTAINS(Blocks.STONE, Blocks.STONE, Blocks.STONE, 64, 60);
	
	public final byte topBlock;
	public final byte fillerBlock;
	public final byte deepBlock;
	public final int baseHeight;
	public final int heightVarience;
	
	Biome(byte top, byte filler, byte deep, int base, int var) {
		this.topBlock = top;
		this.fillerBlock = filler;
		this.deepBlock = deep;
		this.baseHeight = base;
		this.heightVarience = var;
	}
}
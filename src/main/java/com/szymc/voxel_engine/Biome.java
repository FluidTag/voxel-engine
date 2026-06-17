package com.szymc.voxel_engine;

public class Biome {
	public final BiomeType type;
    public final String name;
    
    public final byte topBlock;
    public final byte fillerBlock;
    
    public final byte woodBlock;
    public final byte leafBlock;
    public final float treeDensity;
    public final float decorationChance;
    public final byte[] possibleDecorations;
    
    public Biome(BiomeType type, String name, byte topBlock, byte fillerBlock,
    		byte woodBlock, byte leafBlock, float treeDensity, float decorationChance, byte[] possibleDecorations) {
    	this.type = type;
    	this.name = name;
    	this.topBlock = topBlock;
    	this.fillerBlock = fillerBlock;
    	this.woodBlock = woodBlock;
    	this.leafBlock = leafBlock;
    	this.treeDensity = treeDensity;
    	this.decorationChance = decorationChance;
    	this.possibleDecorations = possibleDecorations;
    }
}

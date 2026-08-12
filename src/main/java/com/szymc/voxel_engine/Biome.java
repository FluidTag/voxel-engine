package com.szymc.voxel_engine;

import org.lwjgl.system.linux.XGenericEvent;

class GroundLayer {
	public byte block;
	public FastNoiseLite generator;
	public float threshold;
	private static int createdLayers = 0;

	public GroundLayer(byte block, float frequency, float threshold) {
		this.block = block;
		this.generator = new FastNoiseLite();
		this.threshold = threshold;
		generator.SetNoiseType(FastNoiseLite.NoiseType.OpenSimplex2);
		generator.SetSeed(createdLayers++);
		generator.SetFrequency(frequency);
		generator.SetFractalType(FastNoiseLite.FractalType.FBm);
		generator.SetFractalOctaves(4);      // Adds finer detail layers (3-5 is ideal)
		generator.SetFractalLacunarity(2.0f); // Scale of frequency per octave
		generator.SetFractalGain(0.5f);
	}
}

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
	public final GroundLayer[] topLayers;

    public Biome(BiomeType type, String name, byte topBlock, byte fillerBlock,
    		byte woodBlock, byte leafBlock, float treeDensity, float decorationChance, byte[] possibleDecorations, GroundLayer[] topLayers) {
    	this.type = type;
    	this.name = name;
    	this.topBlock = topBlock;
    	this.fillerBlock = fillerBlock;
    	this.woodBlock = woodBlock;
    	this.leafBlock = leafBlock;
    	this.treeDensity = treeDensity;
    	this.decorationChance = decorationChance;
    	this.possibleDecorations = possibleDecorations;
		this.topLayers = topLayers;
    }

	// Biome Ground Utilities

	public byte getTopBlock(int wx, int wz) {
		if (topLayers == null || topLayers.length == 0) return this.topBlock;
		float bestResult = -999;
		byte target = this.topBlock;

		for (GroundLayer layer : topLayers) {
			float v = layer.generator.GetNoise(wx, wz);
			if (v > bestResult && v >= layer.threshold) {
				bestResult = v;
				target = layer.block;
			}
		}

		if (bestResult == -999) return this.topBlock; else return target;
	}
}

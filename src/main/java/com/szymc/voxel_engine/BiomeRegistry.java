package com.szymc.voxel_engine;

import java.util.EnumMap;

public class BiomeRegistry {
	private static final EnumMap<BiomeType, Biome> REGISTRY = new EnumMap<>(BiomeType.class);
	
	public static void init() {
		register(new Biome(BiomeType.ARTIC, "Artic", Blocks.SNOW, Blocks.DIRT,
				Blocks.OAK_WOOD, Blocks.OAK_LEAVES, 0f, 0.04f));
		register(new Biome(BiomeType.TUNDRA, "Tundra", Blocks.TUNDRA_GRASS, Blocks.DIRT,
				Blocks.OAK_WOOD, Blocks.OAK_LEAVES, 0f, 0.08f));
		register(new Biome(BiomeType.TAIGA, "Taiga", Blocks.TAIGA_GRASS, Blocks.DIRT,
				Blocks.SPRUCE_LOG, Blocks.SPRUCE_LEAVES, 0.55f, 0.12f));
		register(new Biome(BiomeType.BIRCH_FOREST, "Birch Forest", Blocks.BIRCH_GRASS, Blocks.DIRT,
				Blocks.BIRCH_WOOD, Blocks.BIRCH_LEAVES, 0.78f, 0.14f));
		register(new Biome(BiomeType.PLAINS, "Plains", Blocks.GRASS, Blocks.DIRT,
				Blocks.OAK_WOOD, Blocks.OAK_LEAVES, 0.1f, 0.14f));
		register(new Biome(BiomeType.FOREST, "Forest", Blocks.FOREST_GRASS, Blocks.DIRT,
				Blocks.OAK_WOOD, Blocks.OAK_LEAVES, 0.78f, 0.14f));
		register(new Biome(BiomeType.SAVANNA, "Savanna", Blocks.SAVANNA_GRASS, Blocks.DIRT,
				Blocks.ACACIA_LOG, Blocks.ACACIA_LEAVES, 0.45f, 0.2f));
		register(new Biome(BiomeType.JUNGLE, "Jungle", Blocks.JUNGLE_GRASS, Blocks.DIRT,
				Blocks.OAK_WOOD, Blocks.OAK_LEAVES, 0.65f, 0.15f));
		register(new Biome(BiomeType.DESERT, "Desert", Blocks.SAND, Blocks.DIRT,
				Blocks.OAK_WOOD, Blocks.OAK_LEAVES, 0, 0.06f));
		
	}
	
	private static void register(Biome biome) {
		REGISTRY.put(biome.type, biome);
	}
	
	public static Biome get(BiomeType type) {
		return REGISTRY.get(type);
	}
}
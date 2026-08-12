package com.szymc.voxel_engine;

import java.util.EnumMap;

public class BiomeRegistry {
	private static final EnumMap<BiomeType, Biome> REGISTRY = new EnumMap<>(BiomeType.class);
	
	public static void init() {
		register(new Biome(BiomeType.OCEAN, "Ocean", Blocks.GRAVEL, Blocks.DIRT, Blocks.OAK_WOOD, Blocks.OAK_LEAVES, 0f, 0f, null, null));

		register(new Biome(BiomeType.BEACH, "Beach", Blocks.SAND, Blocks.SAND, Blocks.OAK_WOOD, Blocks.OAK_LEAVES, 0f, 0f, null, null));

		register(new Biome(BiomeType.SWAMP, "Swamp", Blocks.SWAMP_GRASS, Blocks.DIRT,
				Blocks.OAK_WOOD, Blocks.OAK_LEAVES, 0.3f, 0.1f, new byte[] {Blocks.GRASS_DECORATION}, null));

		register(new Biome(BiomeType.MEADOW, "Meadow", Blocks.GRASS, Blocks.DIRT,
				Blocks.OAK_WOOD, Blocks.OAK_LEAVES, 0.3f, 0.06f, new byte[] {Blocks.GRASS_DECORATION, Blocks.RED_FLOWER, Blocks.YELLOW_FLOWER}, null));

		register(new Biome(BiomeType.STEPPE, "Steppe", Blocks.GRASS, Blocks.DIRT,
				Blocks.OAK_WOOD, Blocks.OAK_LEAVES, 0f, 0.03f, new byte[] {Blocks.GRASS_DECORATION}, null));

		register(new Biome(BiomeType.REDWOOD_FOREST, "Redwood Forest", Blocks.RED_DIRT, Blocks.DIRT,
				Blocks.RED_WOOD, Blocks.SPRUCE_LEAVES, 0.65f, 0.07f, new byte[] {Blocks.GRASS_DECORATION, Blocks.RED_MUSHROOM_SMALL, Blocks.BROWN_MUSHROOM_SMALL}, new GroundLayer[]{new GroundLayer(Blocks.COARSE_DIRT, 0.02f, 0.5f), new GroundLayer(Blocks.TAIGA_GRASS, 0.01f, 0.3f), new GroundLayer(Blocks.GRAVEL, 0.03f, 0.6f)}));

		register(new Biome(BiomeType.RED_DESERT, "Red Desert", Blocks.RED_SAND, Blocks.SANDSTONE,
				Blocks.OAK_WOOD, Blocks.OAK_LEAVES, 0.1f, 0.12f, new byte[] {Blocks.DEAD_SHRUB, Blocks.GRASS_DECORATION}, null));

		register(new Biome(BiomeType.WINDSWEPT_HILLS, "Windswept Hills", Blocks.TAIGA_GRASS, Blocks.DIRT,
				Blocks.SPRUCE_LOG, Blocks.SPRUCE_LEAVES, 0f, 0.08f, new byte[] {Blocks.GRASS_DECORATION}, new GroundLayer[]{new GroundLayer(Blocks.GRAVEL, 0.04f, 0.4f), new GroundLayer(Blocks.STONE, 0.04f, 0.3f)}));

		register(new Biome(BiomeType.FROZEN_PEAKS, "Frozen Peaks", Blocks.SNOW, Blocks.STONE,
				Blocks.OAK_WOOD, Blocks.OAK_LEAVES, 0f, 0f, null, new GroundLayer[]{new GroundLayer(Blocks.ICE, 0.07f, 0.5f)}));

		register(new Biome(BiomeType.ARCTIC, "Arctic", Blocks.SNOW, Blocks.DIRT,
				Blocks.OAK_WOOD, Blocks.OAK_LEAVES, 0f, 0.04f, null, null));

		register(new Biome(BiomeType.TUNDRA, "Tundra", Blocks.TUNDRA_GRASS, Blocks.DIRT,
				Blocks.OAK_WOOD, Blocks.OAK_LEAVES, 0f, 0.08f, new byte[] {Blocks.GRASS_DECORATION}, null));

		register(new Biome(BiomeType.SNOWY_TAIGA, "Snowy Taiga", Blocks.SNOW, Blocks.DIRT,
				Blocks.SPRUCE_LOG, Blocks.SNOWY_SPRUCE_LEAVES, 0.45f, 0.08f, new byte[] {Blocks.FERN, Blocks.SNOWY_FERN}, new GroundLayer[]{new GroundLayer(Blocks.GRAVEL, 0.04f, 0.78f), new GroundLayer(Blocks.TAIGA_GRASS, 0.05f, 0.83f)}));

		register(new Biome(BiomeType.SNOWY_MOUNTAIN, "Snowy Mountain", Blocks.SNOW, Blocks.STONE,
				Blocks.OAK_WOOD, Blocks.OAK_LEAVES, 0f, 0.06f, null, null));

		register(new Biome(BiomeType.TAIGA, "Taiga", Blocks.TAIGA_GRASS, Blocks.DIRT,
				Blocks.SPRUCE_LOG, Blocks.SPRUCE_LEAVES, 0.55f, 0.06f, new byte[] {Blocks.GRASS_DECORATION, Blocks.RED_MUSHROOM_SMALL, Blocks.BROWN_MUSHROOM_SMALL, Blocks.RED_FLOWER}, new GroundLayer[]{new GroundLayer(Blocks.COARSE_DIRT, 0.01f, 0.6f)}));

		register(new Biome(BiomeType.ROCKY_MOUNTAIN, "Rocky Mountain", Blocks.STONE, Blocks.STONE,
				Blocks.OAK_WOOD, Blocks.OAK_LEAVES, 0f, 0.06f, null, null));

		register(new Biome(BiomeType.BIRCH_FOREST, "Birch Forest", Blocks.BIRCH_GRASS, Blocks.DIRT,
				Blocks.BIRCH_WOOD, Blocks.BIRCH_LEAVES, 0.78f, 0.07f, new byte[] {Blocks.GRASS_DECORATION, Blocks.RED_FLOWER, Blocks.YELLOW_FLOWER, Blocks.RED_MUSHROOM_SMALL, Blocks.BROWN_MUSHROOM_SMALL}, null));

		register(new Biome(BiomeType.PLAINS, "Plains", Blocks.GRASS, Blocks.DIRT,
				Blocks.OAK_WOOD, Blocks.OAK_LEAVES, 0.1f, 0.07f, new byte[] {Blocks.GRASS_DECORATION, Blocks.RED_FLOWER, Blocks.YELLOW_FLOWER}, null));

		register(new Biome(BiomeType.FOREST, "Forest", Blocks.FOREST_GRASS, Blocks.DIRT,
				Blocks.OAK_WOOD, Blocks.OAK_LEAVES, 0.84f, 0.07f, new byte[] {Blocks.GRASS_DECORATION, Blocks.RED_FLOWER, Blocks.YELLOW_FLOWER, Blocks.RED_MUSHROOM_SMALL, Blocks.BROWN_MUSHROOM_SMALL}, null));

		register(new Biome(BiomeType.DARK_OAK_FOREST, "Dark Oak Forest", Blocks.FOREST_GRASS, Blocks.DIRT,
				Blocks.DARK_OAK_LOG, Blocks.DARK_OAK_LEAVES, 0.9f, 0.12f, new byte[] {Blocks.GRASS_DECORATION, Blocks.RED_MUSHROOM_SMALL, Blocks.BROWN_MUSHROOM_SMALL}, null));

		register(new Biome(BiomeType.SAVANNA, "Savanna", Blocks.SAVANNA_GRASS, Blocks.DIRT,
				Blocks.ACACIA_LOG, Blocks.ACACIA_LEAVES, 0.45f, 0.13f, new byte[] {Blocks.GRASS_DECORATION, Blocks.DEAD_SHRUB, Blocks.YELLOW_FLOWER}, null));

		register(new Biome(BiomeType.JUNGLE, "Jungle", Blocks.JUNGLE_GRASS, Blocks.DIRT,
				Blocks.JUNGLE_LOG, Blocks.OAK_LEAVES, 0.45f, 0.12f, new byte[] {Blocks.GRASS_DECORATION}, null));

		register(new Biome(BiomeType.DESERT, "Desert", Blocks.SAND, Blocks.SANDSTONE,
				Blocks.ACACIA_LOG, Blocks.ACACIA_LEAVES, 0.03f, 0.05f, new byte[] {Blocks.GRASS_DECORATION, Blocks.DEAD_SHRUB}, null));

		register(new Biome(BiomeType.MESA, "MESA", Blocks.RED_SAND, Blocks.SANDSTONE,
				Blocks.ACACIA_LOG, Blocks.ACACIA_LEAVES, 0.024f, 0.06f, new byte[] {Blocks.GRASS_DECORATION, Blocks.DEAD_SHRUB}, null));
		
	}
	
	private static void register(Biome biome) {
		REGISTRY.put(biome.type, biome);
	}
	
	public static Biome get(BiomeType type) {
		if (!REGISTRY.containsKey(type)) System.err.println("Error: No biome exists in registry with type: " + type);
		return REGISTRY.get(type);
	}
}
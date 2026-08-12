package com.szymc.voxel_engine;

import java.util.ArrayList;

public class TerrainTask {
	private final World worldReference;
	private static final FastNoiseLite noise = new FastNoiseLite();
	public static final FastNoiseLite treeNoise = new FastNoiseLite(); // Tree density
	private static final FastNoiseLite caveNoise = new FastNoiseLite();
	public static final FastNoiseLite continentalNoise = new FastNoiseLite();
	public static final FastNoiseLite regionalNoise = new FastNoiseLite();
	public static final FastNoiseLite erosionNoise = new FastNoiseLite();
	private static final FastNoiseLite mountainNoise = new FastNoiseLite();
	private static final FastNoiseLite iceOceanNoise = new FastNoiseLite();
	private static final FastNoiseLite subIceOceanNoise = new FastNoiseLite();

	private static final FastNoiseLite temperatureNoise = new FastNoiseLite();
	private static final FastNoiseLite moistureNoise = new FastNoiseLite();
	private static final FastNoiseLite weirdnessNoise = new FastNoiseLite();

	public static void initNoise() {
		continentalNoise.SetNoiseType(FastNoiseLite.NoiseType.OpenSimplex2);
		continentalNoise.SetFrequency(0.0005f);
		continentalNoise.SetFractalType(FastNoiseLite.FractalType.FBm);
		continentalNoise.SetFractalOctaves(3);

		temperatureNoise.SetNoiseType(FastNoiseLite.NoiseType.OpenSimplex2);
		temperatureNoise.SetFrequency(0.0003f);
		temperatureNoise.SetFractalType(FastNoiseLite.FractalType.FBm);
		temperatureNoise.SetFractalOctaves(5);
		temperatureNoise.SetSeed(33);

		moistureNoise.SetNoiseType(FastNoiseLite.NoiseType.OpenSimplex2);
		moistureNoise.SetFrequency(0.0004f);
		moistureNoise.SetSeed(49);
		moistureNoise.SetFractalType(FastNoiseLite.FractalType.FBm);
		moistureNoise.SetFractalOctaves(4);

		weirdnessNoise.SetNoiseType(FastNoiseLite.NoiseType.OpenSimplex2);
		weirdnessNoise.SetFrequency(0.003f);
		weirdnessNoise.SetSeed(44);
		weirdnessNoise.SetFractalType(FastNoiseLite.FractalType.FBm);
		weirdnessNoise.SetFractalOctaves(4);

		regionalNoise.SetNoiseType(FastNoiseLite.NoiseType.OpenSimplex2);
		regionalNoise.SetFrequency(0.002f);
		regionalNoise.SetFractalType(FastNoiseLite.FractalType.FBm);
		regionalNoise.SetFractalOctaves(5);
		regionalNoise.SetSeed(2);

		iceOceanNoise.SetNoiseType(FastNoiseLite.NoiseType.OpenSimplex2);
		iceOceanNoise.SetFrequency(0.01f);
		iceOceanNoise.SetFractalType(FastNoiseLite.FractalType.FBm);
		iceOceanNoise.SetFractalOctaves(4);
		iceOceanNoise.SetSeed(67);

		subIceOceanNoise.SetNoiseType(FastNoiseLite.NoiseType.OpenSimplex2);
		subIceOceanNoise.SetFrequency(0.06f);
		subIceOceanNoise.SetFractalType(FastNoiseLite.FractalType.FBm);
		subIceOceanNoise.SetFractalOctaves(4);
		subIceOceanNoise.SetSeed(67);

		erosionNoise.SetNoiseType(FastNoiseLite.NoiseType.OpenSimplex2);
		erosionNoise.SetFrequency(0.0014f);
		erosionNoise.SetFractalType(FastNoiseLite.FractalType.FBm);
		erosionNoise.SetFractalOctaves(2);

		treeNoise.SetFrequency(0.005f);
		treeNoise.SetNoiseType(FastNoiseLite.NoiseType.OpenSimplex2);
		treeNoise.SetFractalType(FastNoiseLite.FractalType.FBm);
		treeNoise.SetFractalOctaves(4);

		caveNoise.SetNoiseType(FastNoiseLite.NoiseType.OpenSimplex2);
		caveNoise.SetFrequency(0.004f);
		caveNoise.SetFractalType(FastNoiseLite.FractalType.FBm);
		caveNoise.SetFractalOctaves(5);

		mountainNoise.SetNoiseType(FastNoiseLite.NoiseType.OpenSimplex2);
		mountainNoise.SetFrequency(0.0004f);
		mountainNoise.SetFractalType(FastNoiseLite.FractalType.Ridged);
		mountainNoise.SetFractalOctaves(4);

		noise.SetNoiseType(FastNoiseLite.NoiseType.OpenSimplex2);
	    noise.SetFrequency(0.012f);
	    noise.SetFractalType(FastNoiseLite.FractalType.FBm);
	    noise.SetFractalOctaves(3);
	}

	private static float calculateSpline(float x, float x1, float y1, float d1, float x2, float y2, float d2) {
		float t = (x - x1) /(x2-x1);

        return (2*t*t*t - 3*t*t + 1)*y1 + (-2*t*t*t + 3*t*t)*y2 + (t*t*t -2*t*t + t)*(x2-x1)*d1 + (t*t*t - t*t)*(x2-x1)*d2;
	}

	private static float[][] splinePoints = {
			{0, 150, 0, 0.2f, 50, 0},
			{0.2f, 50, 0, 0.5f, 30, -10},
			{0.5f, 30, -10, 0.6f, 20, 0},
			{0.6f, 20, 0, 0.65f, 60, 0},
			{0.65f, 60, 0, 0.7f, 60, 0},
			{0.7f, 60, 0, 0.75f, 20, 0},
			{0.75f, 20, 0, 1, 0, 0}
	};

	public static int getNoiseHeight(int wx, int wz) {
		float contVal = (continentalNoise.GetNoise(wx, wz) + 1.0f) / 2.0f;
		float noiseVal = (noise.GetNoise(wx, wz)+1.0f)/2.0f;
		float erosionVal = (erosionNoise.GetNoise(wx, wz)+1.0f)/2.0f;
		float regionalVal = (regionalNoise.GetNoise(wx, wz)+1.0f)/2.0f;

		float coastalOcean = (56+noiseVal*7);
		float shallowOcean = (35+noiseVal*5);
		float deepOcean = (2+noiseVal*12);

		if (contVal < 0.08f) {
			return (int)deepOcean;
		} else if (contVal < 0.1f) {
			float t = (contVal-0.08f)/(0.1f-0.08f);
			return (int)(deepOcean + t*t*(3-2*t)*(shallowOcean-deepOcean));
		} else if (contVal < 0.15f) {
			return (int)shallowOcean;
		} else if (contVal < 0.2f) {
			float t = (contVal-0.15f)/(0.2f-0.15f);
			return (int)(shallowOcean + t*t*(3-2*t)*(coastalOcean-shallowOcean));
		} else if (contVal < 0.25f) {
			return (int)coastalOcean;
		}

		float baseHeight = 70 + noiseVal*15;
		float n = mountainNoise.GetNoise(wx+regionalVal*100f, wz-regionalVal*100f);
		n = 1-Math.abs(n);
		n = n*n*2f;

		float steepness = 1f-erosionVal;
		float mountainHeight = (n-0.5f)*steepness*(steepness+0.2f)*(contVal-0.1f)*180;
		float height = baseHeight + (regionalVal-0.7f)*55 + mountainHeight;

		if (contVal < 0.32f) {
			float t = (contVal-0.25f)/(0.32f-0.25f);
			return (int)(coastalOcean + t*t*(3-2*t)*(height-coastalOcean));
		} else {
			return (int)height;
		}
	}

	private static final Object[][] biomePoints = new Object[][]{
			// Format: { Temp, Temp-Min, Temp-Max, Moist, Moist-Min, Moist-Max, Cont, Eros, Weird, Height, Height-Min, Height-Max, Biome }

			// --- AQUATIC & COASTAL TRANSITIONS ---
			{ 0.50f,  0.0f, 1.0f, 0.50f, 0.0f, 1.0f,  0.05f,  0.50f,  0.50f,  0.08f, 0f, 0.25f,  BiomeType.OCEAN },
			{ 0.55f,  0.0f, 1.0f, 0.40f, 0.0f, 1.0f,  0.22f,  0.70f,  0.50f,  0.26f, 0.25f, 0.263f,   BiomeType.BEACH },

			// --- COLD / POLAR BIOMES ---
			{ 0.05f,  0.0f, 0.3f, 0.20f, 0.0f, 0.36f,  0.50f,  0.80f,  0.50f,  0.30f, 0f, 1f,  BiomeType.ARCTIC },
			{ 0.18f,  0.0f, 0.35f, 0.30f, 0.0f, 1f,  0.50f,  0.65f,  0.50f,  0.32f, 0f, 1f,  BiomeType.TUNDRA },
			{ 0.20f,  0.0f, 0.37f, 0.60f, 0.2f, 1f,  0.55f,  0.50f,  0.40f,  0.45f, 0f, 1f,  BiomeType.SNOWY_TAIGA },
			{ 0.10f,  0.0f, 0.31f, 0.40f, 0.2f, 0.65f,  0.60f,  0.20f,  0.50f,  0.75f, 0.5f, 1f, BiomeType.SNOWY_MOUNTAIN },
			{ 0.02f,  0.0f, 0.08f, 0.2f, 0.0f, 0.34f,  0.70f,  0.05f,  0.50f,  0.92f, 0.71f, 1f,  BiomeType.FROZEN_PEAKS }, // NEW: Extreme altitude

			// --- TEMPERATE & COOL BIOMES ---
			{ 0.32f,  0.1f, 0.5f, 0.50f, 0.15f, 0.7f,  0.50f,  0.55f,  0.50f,  0.38f, 0f, 1f,  BiomeType.TAIGA },
			{ 0.4f, 0.3f, 0.65f, 0.90f, 0.60f, 1.0f,  0.55f,  0.40f,  0.50f,  0.36f, 0f, 1f,  BiomeType.REDWOOD_FOREST}, // NEW: High-moisture temperate
			{ 0.45f,  0.37f, 0.57f, 0.65f, 0.53f, 0.74f,  0.50f,  0.45f,  0.30f,  0.36f, 0f, 1f,  BiomeType.BIRCH_FOREST },
			{ 0.50f,  0.35f, 0.65f,  0.10f,  0.00f, 0.34f,  0.50f,  0.70f,  0.50f,  0.32f,  0.00f, 1f,  BiomeType.STEPPE },
			{ 0.50f,  0.38f, 0.65f, 0.4f, 0f, 1f,  0.50f,  0.85f,  0.50f,  0.32f, 0f, 1f,  BiomeType.PLAINS },
			{ 0.52f,  0.38f, 0.65f, 0.55f, 0.3f, 0.64f,  0.50f,  0.50f,  0.40f,  0.36f, 0f, 1f,  BiomeType.FOREST },
			{ 0.55f,  0.42f, 0.67f, 0.60f, 0.45f, 0.7f,  0.55f,  0.40f,  0.80f,  0.38f, 0f, 1f,  BiomeType.DARK_OAK_FOREST },
			{ 0.45f,  0.3f, 0.76f, 0.50f, 0.4f, 0.6f,  0.55f,  0.30f,  0.50f,  0.58f, 0f, 1f,  BiomeType.MEADOW }, // NEW: Elevated mountain valley
			{ 0.50f,  0.33f, 0.76f, 0.40f, 0.34f, 0.54f,  0.60f,  0.15f,  0.85f,  0.52f, 0f, 1f,  BiomeType.WINDSWEPT_HILLS }, // NEW: Jagged low-elevation hills
			{ 0.45f,  0.3f, 0.52f, 0.85f, 0.65f, 0.9f,  0.40f,  0.90f,  0.50f,  0.27f, 0f, 0.28f,  BiomeType.SWAMP }, // NEW: Temperate wetland
			{ 0.40f,  0.3f, 1f, 0.40f, 0f, 1f,  0.65f,  0.15f,  0.50f,  0.78f, 0.49f, 1f, BiomeType.ROCKY_MOUNTAIN },

			// --- WARM / HOT / ARID BIOMES ---
			{ 0.70f, 0.6f, 0.8f,  0.35f, 0.1f, 0.52f,  0.55f,  0.60f,  0.40f,  0.35f, 0f, 1f,  BiomeType.SAVANNA },
			{ 0.9f, 0.85f, 1.0f, 0.08f, 0f, 0.35f, 0.67f, 0.76f, 0f, 0.32f, 0f, 1f,  BiomeType.RED_DESERT}, // NEW: Semi-arid shrubland
			{ 0.88f,  0.6f, 1.0f, 0.10f, 0f, 0.5f,  0.65f,  0.80f,  0.30f,  0.30f, 0f, 1f,  BiomeType.DESERT },
			{ 0.85f,  0.8f, 1.0f, 0.15f, 0.02f, 0.34f,  0.70f,  0.30f,  1f,  0.48f, 0.35f, 1f,  BiomeType.MESA },
			{ 0.80f,  0.6f, 0.95f, 0.80f, 0.5f, 1.0f,  0.55f,  0.40f,  0.40f,  0.38f, 0f, 1f,  BiomeType.JUNGLE },
	};

	public static float getTemp(int wx, int wz) {
		return (temperatureNoise.GetNoise(wx, wz)+1.0f)/2.0f;
	}

	public static float getMoist(int wx, int wz) {
		return (moistureNoise.GetNoise(wx, wz)+1.0f)/2.0f;
	}

	public static float getContinental(int wx, int wz) {
		return (continentalNoise.GetNoise(wx, wz) + 1.0f) / 2.0f;
	}

	public static float getErosion(int wx, int wz) {
		return (erosionNoise.GetNoise(wx, wz)+1.0f)/2.0f;
	}

	public static float getWeirdness(int wx, int wz) {
		return (weirdnessNoise.GetNoise(wx, wz)+1.0f)/2.0f;
	}

	private final static ThreadLocal<ArrayList<String>> biomeDebug = ThreadLocal.withInitial(ArrayList::new);
	public static BiomeType getBiomeType(int height, float temp, float moist, float cont, float erosion, float weirdness) {
		float normHeight = Math.max(0f, Math.min(0.99f, (float) height / 256.0f)); // Scale height into 0.0 - 1.0 range
		//ArrayList<String> debug = biomeDebug.get();
		//debug.clear();

		float lowestDist = Float.MAX_VALUE;
		BiomeType resultType = null;

		for (Object[] point : biomePoints) {
			BiomeType pointType = (BiomeType) point[12];

			float dT = temp - (float)point[0];
			float tMin = (float) point[1];
			float tMax = (float) point[2];
			float dM = moist - (float)point[3];
			float mMin = (float) point[4];
			float mMax = (float) point[5];

			float dC = cont - (float)point[6];
			float dE = erosion - (float)point[7];
			float dW = weirdness - (float)point[8];
			float dH = normHeight - (float)point[9];
			float hMin = (float) point[10];
			float hMax = (float) point[11];

			if (temp < tMin || temp > tMax || moist < mMin || moist > mMax || normHeight < hMin || normHeight > hMax) {
				//debug.add(pointType + " was denied: [T: " + temp + ", Min: " + tMin + ", Max: " + tMax + ", Passed: " + (temp >= tMin && temp <= tMax) + "|| [M: " + moist + ", Min: " + mMin + ", Max: " + mMax + ", Passed: " + (moist >= mMin && moist <= mMax) + " || " + "[H: " + normHeight + ", Min: " + hMin + ", Max: " + hMax + ", Passed " + (normHeight >= hMin && normHeight <= hMax) + "]");
				continue;
			};

			// Weighted Squared Euclidean Distance
			float dist = (dT*dT*3) + (dM*dM*3) + dC*dC + dE*dE + (dW*dW*2) + dH*dH;

			if (dist < lowestDist) {
				lowestDist = dist;
				resultType = pointType;
			}
		}

//		if (resultType == null) {
//			for (String log : debug) System.out.println(log);
//			System.out.println("_________________________");
//		}
		return resultType;
	}

	public static byte noiseGetBlock(int height, int wx, int wy, int wz, Biome biome, float temp, float moist) {
		//if (wy < 69) return Blocks.WATER;
		if (wy == 0) return Blocks.BEDROCK;
		if (wy > height && wy == 64 && temp < 0.2f && ((iceOceanNoise.GetNoise(wx, wz) > 0.3f) || subIceOceanNoise.GetNoise(wx, wz) > 0.3f)) return Blocks.ICE;
		if (wy > height && wy <= 64) return Blocks.WATER;

		// Caves
		if (wy <= height) {
			double threshold = -0.4 - (wy*0.002);

			if (noise.GetNoise(wx, wy, wz) < threshold) return wy < 5 ? Blocks.LAVA : Blocks.AIR;
		}

		if (wy == height) {
			if (wy < 59) return Blocks.GRAVEL;
			if (wy < 66) return Blocks.SAND;
			if (biome.type == BiomeType.MESA) {
				if (wy <= 100) return biome.getTopBlock(wx, wz);
				if (wy % 2 == 0) return Blocks.RED_TERRACOTTA; else if (wy % 3 == 0) return Blocks.ORANGE_TERRACOTTA; else if (wy % 5 == 0) return Blocks.YELLOW_TERRACOTTA;
			}

			return biome.getTopBlock(wx, wz);
		} else if (wy < height && wy >= height-3) {
			return biome.fillerBlock;
		} else if (wy < height-3) {
			return Blocks.STONE;
		}

		return Blocks.AIR;
	}

	private static final ThreadLocal<int[]> tNoiseMap = ThreadLocal.withInitial(() -> new int[32*32]);
	private static final ThreadLocal<float[]> tTempMap = ThreadLocal.withInitial(() -> new float[32*32]);
	private static final ThreadLocal<float[]> tMoistMap = ThreadLocal.withInitial(() -> new float[32*32]);
	private static final ThreadLocal<Biome[]> tBiomeMap  = ThreadLocal.withInitial(() -> new Biome[32*32]);
	private static final ThreadLocal<float[]> tSlopeMap = ThreadLocal.withInitial(() -> new float[32*32]);
	public static byte getSurfaceBlock(int wx, int noiseHeight, int wz, Biome biome) { // Simulates generate chunk for one tile
		float temp = getTemp(wx, wz); float moist = getMoist(wx, wz); float cont = getContinental(wx, wz); float erosion = getErosion(wx, wz); float weird = getWeirdness(wx, wz);
		if (biome == null) biome = BiomeRegistry.get(getBiomeType(noiseHeight, temp, moist, cont, erosion, weird));

		int hxl = getNoiseHeight(wx-1, wz);
		int hxr = getNoiseHeight(wx+1, wz);
		int hzl = getNoiseHeight(wx, wz-1);
		int hzr = getNoiseHeight(wx, wz+1);

		float steepX = (float)Math.abs(hxr - hxl) / 2.0f;
		float steepZ = (float)Math.abs(hzr - hzl) / 2.0f;

		float steepness = (float)Math.sqrt(steepX * steepX + steepZ * steepZ);
		boolean isCliff = steepness >= 1.5f;

		byte block = noiseGetBlock(noiseHeight, wx, noiseHeight, wz, biome, temp, moist);
		if (isCliff && block != Blocks.AIR && block != Blocks.WATER) {
			block = (steepness < 2.2f) ? Blocks.STONE : Blocks.HARDENED_STONE;
		}

		return block;
	}

	private ChunkSection[] generateChunk() {
		ChunkSection[] sections = new ChunkSection[16];
		int[] chunkNoise = tNoiseMap.get();
		float[] chunkTemp = tTempMap.get();
		float[] chunkMoist = tMoistMap.get();
		Biome[] chunkBiome = tBiomeMap.get();
		float[] chunkSteep = tSlopeMap.get();

		for (int z = 0; z < 32; z++) {
			for (int x = 0; x < 32; x++) {
				int worldX = (cx*32)+x;
				int worldZ = (cz*32)+z;
				int height = getNoiseHeight(worldX, worldZ);
				float temp = getTemp(worldX, worldZ); float moist = getMoist(worldX, worldZ); float cont = getContinental(worldX, worldZ); float erosion = getErosion(worldX, worldZ);
				float weirdness = getWeirdness(worldX, worldZ);

				chunkNoise[z*32+x] = height;
				chunkTemp[z*32+x] = temp;
				chunkMoist[z*32+x] = moist;
				chunkBiome[z*32+x] = BiomeRegistry.get(getBiomeType(height, temp, moist, cont, erosion, weirdness));
			}
		}

		for (int z = 0; z < 32; z++) {
			for (int x = 0; x < 32; x++) {
				int worldX = (cx*32)+x;
				int worldZ = (cz*32)+z;
				int hxl = (x > 0) ? chunkNoise[z*32+(x-1)] : getNoiseHeight(worldX-1, worldZ);
				int hxr = (x < 31) ? chunkNoise[z*32+(x+1)] : getNoiseHeight(worldX+1, worldZ);
				int hzl = (z > 0) ? chunkNoise[(z-1)*32+x] : getNoiseHeight(worldX, worldZ-1);
				int hzr = (z < 31) ? chunkNoise[(z+1)*32+x] : getNoiseHeight(worldX, worldZ+1);

				float steepX = (float)Math.abs(hxr - hxl) / 2.0f;
				float steepZ = (float)Math.abs(hzr - hzl) / 2.0f;

				float steepness = (float)Math.sqrt(steepX * steepX + steepZ * steepZ);
				chunkSteep[z*32+x] = steepness;
			}
		}

		for (int sec = 0; sec < 16; sec++) {
			byte[] chunkData = null;
			boolean changed = false;
			for (int z = 0; z < 32; z++) {
				for (int x = 0; x < 32; x++) {
					int worldX = (cx*32)+x;
					int worldZ = (cz*32)+z;

					int noiseHeight = chunkNoise[z*32+x];
					float temp = chunkTemp[z*32+x];
					float moist = chunkMoist[z*32+x];
					Biome biome = chunkBiome[z*32+x];
					float steepness = chunkSteep[z*32+x];

					boolean isCliff = (steepness >= 1.5f);

					for (int y = 0; y < 16; y++) {
						int worldY = sec*16+y;
						int depth = noiseHeight-worldY;

						byte block = noiseGetBlock(noiseHeight, worldX, worldY, worldZ, biome, temp, moist);
						if (isCliff && depth >= 0 && depth <= (1 + (int)(steepness)) && block != Blocks.AIR && block != Blocks.WATER) {
							block = (steepness < 2.2f) ? Blocks.STONE : Blocks.HARDENED_STONE;
						}

						if (block != Blocks.AIR) {
							if (chunkData == null) chunkData = new byte[32*16*32];
							chunkData[y*(32*32) + z*32 + x] = block;
							changed = true;
						}
					}
				}
			}

			if (!changed) continue;
			sections[sec] = new ChunkSection(chunkData, worldReference, cx*32, sec*16, cz*32);
		}

		return sections;
	}

	public ChunkSection[] terrainGenerated;
	public final int cx, cz;

	public void runTask() {
		this.terrainGenerated = generateChunk();
	}

	public ChunkColumn chunkReference;
	public TerrainTask(ChunkColumn chunkReference, int cx, int cz, World worldReference) {
		this.cx = cx;
		this.cz = cz;
		this.worldReference = worldReference;
		this.chunkReference = chunkReference;
	}
}
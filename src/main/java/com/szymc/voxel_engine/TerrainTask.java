package com.szymc.voxel_engine;
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
	public static void initNoise() {
		continentalNoise.SetNoiseType(FastNoiseLite.NoiseType.OpenSimplex2);
		continentalNoise.SetFrequency(0.0005f);
		continentalNoise.SetFractalType(FastNoiseLite.FractalType.FBm);
		continentalNoise.SetFractalOctaves(3);
		
		temperatureNoise.SetNoiseType(FastNoiseLite.NoiseType.OpenSimplex2);
		temperatureNoise.SetFrequency(0.0001f);
		temperatureNoise.SetFractalType(FastNoiseLite.FractalType.FBm);
		temperatureNoise.SetFractalOctaves(5);
		temperatureNoise.SetSeed(33);
		
		moistureNoise.SetNoiseType(FastNoiseLite.NoiseType.OpenSimplex2);
		moistureNoise.SetFrequency(0.0001f);
		moistureNoise.SetSeed(49);
		moistureNoise.SetFractalType(FastNoiseLite.FractalType.FBm);
		moistureNoise.SetFractalOctaves(4);
		
		regionalNoise.SetNoiseType(FastNoiseLite.NoiseType.OpenSimplex2);
		regionalNoise.SetFrequency(0.002f);
		regionalNoise.SetFractalType(FastNoiseLite.FractalType.FBm);
		regionalNoise.SetFractalOctaves(4);
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
		erosionNoise.SetFrequency(0.0015f);
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
	    noise.SetFrequency(0.013f);
	    noise.SetFractalType(FastNoiseLite.FractalType.FBm);
	    noise.SetFractalOctaves(4);
	}
	
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
	
	private static final float[][] dataPoints = {
		    // Temp, Moist, Block
		    {0.1f,  0.2f,  Blocks.SNOW},          // Cold & Dry (Arctic)
		    {0.25f, 0.35f, Blocks.TAIGA_GRASS},   // Cold & Semi-Dry
		    {0.45f, 0.2f,  Blocks.TUNDRA_GRASS},  // Intermediate Cool/Dry Buffer
		    {0.5f,  0.5f,  Blocks.GRASS},         // Temperate & Medium Moist
		    {0.7f,  0.2f,  Blocks.SAND},          // Hot & Arid (Desert)
		    {0.75f, 0.4f,  Blocks.SAVANNA_GRASS}, // Hot & Semi-Arid
		    {0.85f, 0.8f,  Blocks.JUNGLE_GRASS}   // Hot & Extremely Wet
		};
	
	public static byte getBiomeBlock(int wx, int wz) {
		float temp = (temperatureNoise.GetNoise(wx+noise.GetNoise(wx, wz)*100, wz+noise.GetNoise(wx, wz)*100) + 1.0f) / 2.0f;
		float moist = (moistureNoise.GetNoise(wx+noise.GetNoise(wx, wz)*100, wz+noise.GetNoise(wx, wz)*100) + 1.0f) / 2.0f;
		
		float lowestDist = 999;
		byte resultBlock = Blocks.GRASS;
		for (float[] point : dataPoints) {
			float dist = (point[0]-temp)*(point[0]-temp) + (point[1]-moist)*(point[1]-moist);
			if (dist < lowestDist) {
				lowestDist = dist;
				resultBlock = (byte)point[2];
			}
		}
		
		return resultBlock;
	}
	
	public static byte noiseGetBlock(int height, int wx, int wy, int wz, byte topBlock, float temp, float moist) {
		//if (wy < 69) return Blocks.WATER;
		if (wy == 0) return Blocks.BEDROCK;
		if (wy > height && wy == 64 && temp < 0.2f && ((iceOceanNoise.GetNoise(wx, wz) > 0.3f) || subIceOceanNoise.GetNoise(wx, wz) > 0.3f)) return Blocks.ICE;
		if (wy > height && wy <= 64) return Blocks.WATER;
		
		if (wy == height) {
			//if (wy > 90) return Blocks.STONE;
			if (wy < 59) return Blocks.GRAVEL;
			if (wy < 66) return Blocks.SAND;
			return topBlock;		
		} else if (wy < height && wy >= height-3) {
			//if (wy > 90) return Blocks.STONE;
			return Blocks.DIRT;
		} else if (wy < height-3) {
//			float caveResult = caveNoise.GetNoise(wx, wy, wz);
//			if (caveResult > 0.76) {
//				if (wy < 2) return Blocks.LAVA;
//				return Blocks.AIR;
//			}
			
			return Blocks.STONE;
		}
		
		return Blocks.AIR;
	}
	
	private ChunkSection[] generateChunk() {
		ChunkSection[] sections = new ChunkSection[16];
		for (int sec = 0; sec < 16; sec++) {
			byte[] chunkData = null;
			boolean changed = false;
			for (int x = 0; x < 32; x++) {
				for (int z = 0; z < 32; z++) {
					int worldX = (cx*32)+x;
					int worldZ = (cz*32)+z;
					
					int noiseHeight = getNoiseHeight(worldX, worldZ);
					byte topBlock = getBiomeBlock(worldX, worldZ);
					float temp = (temperatureNoise.GetNoise(worldX+noise.GetNoise(worldX, worldZ)*100, worldZ+noise.GetNoise(worldX, worldZ)*100) + 1.0f) / 2.0f;
					float moist = (moistureNoise.GetNoise(worldX+noise.GetNoise(worldX, worldZ)*100, worldZ+noise.GetNoise(worldX, worldZ)*100) + 1.0f) / 2.0f;
					
					for (int y = 0; y < 16; y++) {
						int worldY = sec*16 +y;
						
						byte block = noiseGetBlock(noiseHeight, worldX, worldY, worldZ, topBlock, temp, moist);
						if (block != Blocks.AIR) {
							if (chunkData == null) chunkData = new byte[32*16*32];
							chunkData[x*(16*32) + y*32 + z] = block;
							changed = true;
						}
					}
				}
			}
			
			if (changed == false) continue;
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
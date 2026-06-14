package com.szymc.voxel_engine;
import it.unimi.dsi.fastutil.ints.IntArrayList;


import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap.Entry;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectIterator;

import java.util.concurrent.ExecutorService;


import static org.lwjgl.glfw.GLFW.GLFW_KEY_W;
import static org.lwjgl.glfw.GLFW.GLFW_KEY_P;
import static org.lwjgl.glfw.GLFW.GLFW_PRESS;
import static org.lwjgl.glfw.GLFW.glfwGetKey;


import java.util.concurrent.ConcurrentLinkedQueue;


import org.joml.Vector3f;


import com.szymc.voxel_engine.ChunkColumn.ChunkState;


import java.util.concurrent.PriorityBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.ConcurrentHashMap;


public class World {
	private final Long2ObjectMap<ChunkColumn> renderedColumns = new Long2ObjectOpenHashMap<>();
	private final Long2ObjectMap<ChunkColumn> loadedColumns = new Long2ObjectOpenHashMap<>();

	private final int renderDistance = 14;
	private final long winId;
	
	public World(long winId) {
		this.winId = winId;
	}
	
	private final int threads = Runtime.getRuntime().availableProcessors()-1;
	private final int terrainThreadCount = 2;
	private final int meshThreadCount = Math.max(1, threads-3);
	private final ExecutorService terrainPool = new ThreadPoolExecutor(
			terrainThreadCount,
			terrainThreadCount,
			0L, TimeUnit.MILLISECONDS,
			new PriorityBlockingQueue<>(100, (r1, r2) ->
				Integer.compare(((Prioritizable)r1).getPriorityValue(), ((Prioritizable)r2).getPriorityValue())
			)
	);
	
	private final ExecutorService meshPool = new ThreadPoolExecutor(
			meshThreadCount,
			meshThreadCount,
			0L, TimeUnit.MILLISECONDS,
			new PriorityBlockingQueue<>(100, (r1, r2) -> 
				Integer.compare(((Prioritizable)r1).getPriorityValue(), ((Prioritizable)r2).getPriorityValue())
			)
	);
			
	public long packKey(int cx, int cz) {
	    return ((long)cx & 0xFFFFFFFFL) | (((long)cz & 0xFFFFFFFFL) << 32);
	}


	private int getKeyX(long key) {
	    return (int) (key & 0xFFFFFFFFL);
	}


	private int getKeyZ(long key) {
	    return (int) (key >> 32); // Use unsigned right shift
	}
	
	public Long2ObjectMap<ChunkColumn> getRendered() {
		return this.renderedColumns;
	}


	ConcurrentLinkedQueue<TerrainTask> completedTerrain = new ConcurrentLinkedQueue<>();
	ConcurrentLinkedQueue<DecorationTask> completedDecorations = new ConcurrentLinkedQueue<>();
	ConcurrentLinkedQueue<MeshTask> completedMeshes = new ConcurrentLinkedQueue<>();
	
	public ChunkColumn getLoadedChunkAtPos(int cx, int cz) {
		return loadedColumns.get(packKey(cx, cz));
	}
	
	private boolean neighborsQualify(ChunkState state, ChunkColumn xMaj, ChunkColumn xMin, ChunkColumn zMaj, ChunkColumn zMin) {
		return (
			xMaj.state.isAtleast(state) &&
			xMin.state.isAtleast(state) &&
			zMaj.state.isAtleast(state) &&
			zMin.state.isAtleast(state)
		);		
	}
	
	private void checkStateAdvances(int cx, int cz) {
		for (int x = cx-1; x <= cx+1; x++) {
			for (int z = cz-1; z <= cz+1; z++) {
				ChunkColumn chunk = loadedColumns.get(packKey(x, z));
				if (chunk == null) continue;
				
				ChunkColumn xMaj = loadedColumns.get(packKey(x+1, z));		
				ChunkColumn xMin = loadedColumns.get(packKey(x-1, z));


				ChunkColumn zMaj = loadedColumns.get(packKey(x, z+1));
				ChunkColumn zMin = loadedColumns.get(packKey(x, z-1));
				
				final int fx = x;
				final int fz = z;


				if (chunk.state == ChunkState.TERRAIN) {
					if (chunk.decorationQueued.compareAndSet(false, true)) {
						terrainPool.execute(new PriorityGenTask(0, () -> {
							DecorationTask task = new DecorationTask(chunk, fx, fz);
							
							task.changeRequests = task.decorate();
							completedDecorations.add(task);
						}));
					}
				}
				
				if (xMaj == null || xMin == null || zMaj == null || zMin == null) continue; 
				if ((chunk.state == ChunkState.DECORATED || (chunk.state == ChunkState.MESHED && chunk.dirtyCount > 0)) &&
						neighborsQualify(ChunkState.DECORATED, xMaj, xMin, zMaj, zMin)) {
					if (chunk.meshQueued.compareAndSet(false, true)) {
						meshPool.execute(new PriorityGenTask(0, () -> {
							MeshTask task = new MeshTask(fx, fz, chunk, xMaj, xMin, zMaj, zMin);
							task.runTask();
							
							completedMeshes.add(task);
						}));
					}
				}
			}
		}
	}
	
	// Main thread
	public void pollGenerationThreads() {
		if (glfwGetKey(winId, GLFW_KEY_P) == GLFW_PRESS) {
			//ChunkColumn cur = loadedColumns.get(packKey(lastX, lastZ));
			//System.out.println(cur);
			//System.out.println("In rendered columns?: " + renderedColumns.containsKey(packKey(lastX, lastZ)));
			
//			Runtime rt = Runtime.getRuntime();
//			long usedMB = (rt.totalMemory() - rt.freeMemory()) / 1024 / 1024;
//			System.out.println("Used Memory: " + usedMB + " MB");
			
			
		}
		
		while (!completedTerrain.isEmpty()) {
			TerrainTask task = completedTerrain.poll();
			long key = packKey(task.cx, task.cz);
			ChunkColumn chunk = loadedColumns.get(key);
			
			if (chunk == null) continue; // Deloaded
			if (task.chunkReference != chunk) continue; // Reloaded and restarted
			
			chunk.applyTerrain(task.terrainGenerated);
			chunk.state = chunk.state.next();
			
			checkStateAdvances(task.cx, task.cz);
		}
		
		while (!completedDecorations.isEmpty()) {
			DecorationTask task = completedDecorations.poll();
			long key = packKey(task.cx, task.cz);
			ChunkColumn chunk = loadedColumns.get(key);
			
			if (chunk == null) continue; // Deloaded
			if (task.chunk != chunk) continue;
			
			for (int change : task.changeRequests) {
				int x = (change << 25) >> 25;
				int y = (change >>> 7) & 0xFF;
				int z = (change << 10) >> 25;
				byte block = (byte)((change >>> 22) & 0xFF);
				
				if (x >= 0 && x < 32 && z >= 0 && z < 32) {
					task.chunk.setBlockInChunk(x, y, z, block);
					continue;
				}
			}
			
			chunk.state = chunk.state.next();
			checkStateAdvances(task.cx, task.cz);
		}
		
		while (!completedMeshes.isEmpty()) {
			MeshTask task = completedMeshes.poll();
			long key = packKey(task.cx, task.cz);
			ChunkColumn chunk = loadedColumns.get(key);
			
			if (chunk == null) continue; // Deloaded
			if (task.chunk != chunk) continue;
			
			// Directly modifies chunk data, this is safe, 
			// no other will read or write or access till finalized
			
			for (int i = 0; i < 16; i++) {
				ChunkSection sec = chunk.getSection(i);
				if (sec == null) continue;
				
				if ((sec.getMesh() == null || sec.isDirty()) && sec.vertices != null) {
					sec.setMesh(new Mesh(sec.vertices, sec.indices, sec.vCount, sec.iCount));
				}
				
				if ((sec.getWaterMesh() == null || sec.isDirty()) && sec.waterVertices != null) {
					sec.setWaterMesh(new Mesh(sec.waterVertices, sec.waterIndices, sec.wvCount, sec.wiCount));
				}
				
				if (sec.isDirty()) {
					sec.setDirty(false);
					chunk.dirtyCount--;
				}
			}
			
			chunk.state = chunk.state.next();
			chunk.meshQueued.set(false);


		    renderedColumns.put(key, chunk);
			checkStateAdvances(task.cx, task.cz);
		}
	}
	
	int lastX = 0;
	int lastZ = 0;
	
	public void update(Vector3f playerPosition) {
		int chunkX = (int)playerPosition.x >> 5;
		int chunkZ = (int)playerPosition.z >> 5;
		
		if (chunkX == lastX && chunkZ == lastZ) {
			return;
		}
		
		lastX = chunkX;
		lastZ = chunkZ;
		
		for (int x = chunkX-renderDistance; x <= chunkX+renderDistance; x++) {
			for (int z = chunkZ-renderDistance; z <= chunkZ+renderDistance; z++) {
				long key = packKey(x, z);
				ChunkColumn chunk = loadedColumns.get(key);
				
				if (chunk == null) {
					chunk = new ChunkColumn(this, x, z);
					chunk.state = ChunkState.EMPTY;
					loadedColumns.put(key, chunk);
				}
				
				final ChunkColumn fChunk = chunk;
				
				final int fx = x;
				final int fz = z;
				
				if (chunk.state == ChunkState.EMPTY && chunk.terrainQueued.compareAndSet(false, true)) {
					terrainPool.execute(new PriorityGenTask(0, () -> {
						TerrainTask task = new TerrainTask(fChunk, fx, fz, this);
						task.runTask(); // Populates data into task
						
						completedTerrain.add(task);
					}));
				}
				
				if (chunk.state == ChunkState.MESHED && !renderedColumns.containsKey(key)) {
					renderedColumns.put(key, chunk);
				}
				
				if (x == chunkX-renderDistance || x == chunkX+renderDistance) {
					checkStateAdvances(x, z);
				}
				
				if (z == chunkZ-renderDistance || z == chunkZ+renderDistance) {
					checkStateAdvances(x, z);
				}
			}
		}
		
		// Deload chunks
		ObjectIterator<Entry<ChunkColumn>> iter = loadedColumns.long2ObjectEntrySet().iterator();
		while (iter.hasNext()) {
			Entry<ChunkColumn> subEntry = iter.next();
			long key = subEntry.getLongKey();
			ChunkColumn val = subEntry.getValue();
			
			int cx = getKeyX(key);
			int cz = getKeyZ(key);
			
			int xDist = Math.abs(cx - chunkX);
			int zDist = Math.abs(cz - chunkZ);
			
			if (xDist > renderDistance || zDist > renderDistance) {
				renderedColumns.remove(key);
				
				if (xDist > renderDistance+2 || zDist > renderDistance+2) {
					val.cleanupMeshes();
					iter.remove();
				}
			}
		}
	}
}






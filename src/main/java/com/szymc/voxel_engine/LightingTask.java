package com.szymc.voxel_engine;

import it.unimi.dsi.fastutil.bytes.ByteArrayList;
import it.unimi.dsi.fastutil.bytes.ByteOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongArrayFIFOQueue;
import it.unimi.dsi.fastutil.ints.*;
import it.unimi.dsi.fastutil.longs.LongArrayList;

import java.util.Arrays;

public class LightingTask {
    public int cx, cz;
    public ChunkColumn chunk;
    public ByteOpenHashSet neighborsToRemesh;
    
    private ChunkColumn xMajor, xMinor, zMajor, zMinor,   xMajorZMajor, xMajorZMinor, xMinorZMajor, xMinorZMinor;
    private byte[] scratchPad;
    public LightingTask(int cx, int cz,
                        ChunkColumn chunk, ChunkColumn xMajor, ChunkColumn xMinor, ChunkColumn zMajor, ChunkColumn zMinor,
                        ChunkColumn xMajorZMajor, ChunkColumn xMajorZMinor, ChunkColumn xMinorZMajor, ChunkColumn xMinorZMinor) {
        this.cx = cx; this.cz = cz;
        this.chunk = chunk;
        this.xMajor = xMajor;
        this.xMinor = xMinor;
        this.zMajor = zMajor;
        this.zMinor = zMinor;
        this.xMajorZMajor = xMajorZMajor;
        this.xMajorZMinor = xMajorZMinor;
        this.xMinorZMajor = xMinorZMajor;
        this.xMinorZMinor = xMinorZMinor;

        this.scratchPad = tChunkLightPad.get();
        Arrays.fill(scratchPad, (byte)0);
        chunk.clearChunkLighting();
    }

    private void setLocalSkyLevel(int ax, int y, int az, byte light) {
        if (ax >= 16 && ax <= 47 && az >= 16 && az <= 47) {
            chunk.setSkylight(ax-16, y, az-16, light);
            return;
        }

        scratchPad[(y<<12) | (az << 6) | ax] &= (byte) ~(0xF << 4);
        scratchPad[(y<<12) | (az << 6) | ax] |= (byte) (light << 4);
    }

    private byte readLocalSkyLevel(int ax, int y, int az) {
        if (ax >= 16 && ax <= 47 && az >= 16 && az <= 47) {
            return (byte) chunk.getSkylight(ax-16, y, az-16);
        }

        return (byte) ((scratchPad[(y<<12) | (az << 6) | ax] >>> 4) & 0xF);
    }

    private void setLocalBlockLevel(int ax, int y, int az, byte light) {
        if (ax >= 16 && ax <= 47 && az >= 16 && az <= 47) {
            chunk.setBlockLight(ax-16, y, az-16, light);
            return;
        }

        scratchPad[(y<<12) | (az << 6) | ax] &= (byte) ~(0xF);
        scratchPad[(y<<12) | (az << 6) | ax] |= (byte) (light);
    }

    private byte readLocalBlockLevel(int ax, int y, int az) {
        if (ax >= 16 && ax <= 47 && az >= 16 && az <= 47) {
            return (byte) chunk.getBlockLight(ax-16, y, az-16);
        }

        return (byte) (scratchPad[(y<<12) | (az << 6) | ax] & 0xF);
    }

    private static long packLightsource(int x, int y, int z, int light, boolean fromSource, int sx, int sy, int sz) {
        return (long)(((x + 32) & 0x7FL) | (((y & 0xFFL) << 7L)) | (((z + 32) & 0x7FL) << 15L) | ((light & 0xFL) << 22L) | ((fromSource ? 1 : 0) << 26L) | ((sx & 0x1FL) << 27L) | ((sy & 0xFFL) << 32L) | ((sz & 0x1FL) << 40L));
    }

    private final static int[][] directions = {
            {1, 0, 0}, {-1, 0, 0},
            {0, 1, 0}, {0, -1, 0},
            {0, 0, 1}, {0, 0, -1}
    };

    // NOTE: All 8 surronding chunks (including corners) must be checked for loaded prior to running
    private final static ThreadLocal<LongArrayFIFOQueue> tPendingLightPropQueue = ThreadLocal.withInitial(() -> new LongArrayFIFOQueue(4089));
    private final static ThreadLocal<ChunkColumn[]> tTempChunkMap = ThreadLocal.withInitial(() -> new ChunkColumn[9]);
    private final static ThreadLocal<byte[]> tChunkLightPad = ThreadLocal.withInitial(() -> new byte[256*64*64]);

    private static long addDirtyToDataLong(long original, int xInd, int section, int zInd) {
        long presence = (original & 0x7L);
        long payload = ((original >>> 3) & 0xFFFL);
        long targetKey = (xInd & 0x3) | ((zInd & 0x3) << 2);
        long repTarget = targetKey * 0x111L;
        long diff = payload ^ repTarget;
        long zeroMatches = (~diff & (diff - 0x111L)) & 0x888L;
        long presenceMask = ((presence & 1L) << 3) | ((presence & 2L) << 6) | ((presence & 4L) << 9);
        long validMatches = zeroMatches & presenceMask;

        int targetIndex = validMatches == 0 ? -1 : (Long.numberOfTrailingZeros(validMatches) - 3) >> 2;

        long data = original;
        if (targetIndex == -1) {
            targetIndex = Long.numberOfTrailingZeros(~(original & 0x7));
            if (targetIndex >= 3) {
                return original;
            }

            data = original | (1L << targetIndex);
            data |= (long) (xInd & 0x3) << (3L + 4L*targetIndex);
            data |= (long) (zInd & 0x3) << (3L + 4L*targetIndex + 2L);
        }

        data |= (1L << (long)(3*1 + 3*4 + 16*targetIndex + section));

        return data;
    }

    private static byte generateDirtyKey(int xInd, int sec, int zInd) {
        return (byte) ((xInd & 0x3) | ((sec & 0xF) << 2) | ((zInd & 0x3) << 6));
    }

    public void updateBlockLighting() {
        LongArrayFIFOQueue pendingBlockPropQueue = tPendingLightPropQueue.get();
        ChunkColumn[] tempChunkMap = tTempChunkMap.get();
        pendingBlockPropQueue.clear();

        tempChunkMap[0] = xMinorZMinor; tempChunkMap[1] = zMinor; tempChunkMap[2] = xMajorZMinor;
        tempChunkMap[3] = xMinor; tempChunkMap[4] = chunk; tempChunkMap[5] = xMajor;
        tempChunkMap[6] = xMinorZMajor; tempChunkMap[7] = zMajor; tempChunkMap[8] = xMajorZMajor;

        for (int z = 0; z < 3; z++) {
            for (int x = 0; x < 3; x++) {
                ChunkColumn targetChunk = tempChunkMap[z*3+x];
                if (targetChunk == null) continue;

                for (int sectorI = 0; sectorI < 16; sectorI++) {
                    ChunkSection section = targetChunk.getSection(sectorI);
                    if (section == null) continue;

                    IntArrayList sources = section.getLightBlocks();
                    for (int j = 0; j < sources.size(); j++) {
                        int dat = sources.getInt(j);
                        int sx = dat & 0x1F;
                        int sy = ((dat >>> 5) & 0xFF) + (16*sectorI);
                        int sz = (dat >>> 13) & 0x1F;
                        int light = (dat >>> 18) & 0xFF;

                        int ax = sx + (x*32) - 16; int az = sz + (z*32) - 16;
                        if (ax < 0 || ax >= 64 || az < 0 || az >= 64) continue;

                        boolean isEnclosed = true;
                        for (int[] dir : directions) {
                            int nx = sx+dir[0]; int ny = sy+dir[1]; int nz = sz+dir[2];
                            int gridX = x + (nx < 0 ? -1 : (nx >= 32 ? 1 : 0));
                            int gridZ = z + (nz < 0 ? -1 : (nz >= 32 ? 1 : 0));

                            ChunkColumn subChunk = tempChunkMap[gridZ*3+gridX];
                            byte block = subChunk.getBlockInChunk(nx&31, ny, nz&31);

                            if (block == Blocks.AIR || Texture.isXShapedBlock[block] || Texture.isLeafBlock[block]) {isEnclosed = false; break;}
                        }

                        if (isEnclosed) {
                            IntArrayList removals = section.getLremovals();
                            removals.add(dat); // Act like removal, doesn't actually remove, but updates necessary chunks to no light
                        }

                        setLocalBlockLevel(ax, sy, az, (byte) light);
                        pendingBlockPropQueue.enqueue(packLightsource(ax-16, sy, az-16, light, (x == 1 && z == 1), sx, sy, sz));
                    }

                    IntArrayList removals = section.getLremovals();
                    IntIterator it = removals.listIterator();
                    while (it.hasNext()) {
                        int dat = it.nextInt();

                        Int2LongOpenHashMap mapChunksEffected = section.getlBlockExtChunksEffected();
                        long extChunksEffected = mapChunksEffected.remove(dat & 0x3FFFF);

                        for (int i = 0; i < 3; i++) {
                            if ((extChunksEffected >>> i & 1) == 0) continue;
                            int xInd = Math.toIntExact(((extChunksEffected >>> (long) (3 + 4 * i)) & 0x3L));
                            int zInd = Math.toIntExact(((extChunksEffected >>> (long) (3 + 4 * i + 2) & 0x3L)));
                            int data = Math.toIntExact(((extChunksEffected >>> (long) (3 + 4 * 3 + 16 * i)) & 0xFFFFL));

                            while (data != 0) {
                                int sec = Integer.numberOfTrailingZeros(data);
                                if (neighborsToRemesh == null) neighborsToRemesh = new ByteOpenHashSet(8);
                                neighborsToRemesh.add(generateDirtyKey(xInd, sec, zInd));
                                data &= (data-1);
                            }
                        }

                        it.remove();
                    }
                }
            }
        }

        while (!pendingBlockPropQueue.isEmpty()) {
            long node = pendingBlockPropQueue.dequeueLong();
            int x = (int) ((node & 0x7FL) - 32);
            int y = (int) ((node >>> 7L) & 0xFFL);
            int z = (int) (((node >>> 15L) & 0x7FL) - 32);
            int currentLight = (int) ((node >>> 22L) & 0xF);
            boolean isSource = ((node >>> 26L) & 1L) == 1;
            int sourceX = (int) ((node >>> 27L) & 0x1FL);
            int sourceY = (int) ((node >>> 32L) & 0xFFL);
            int sourceZ = (int) ((node >>> 40L) & 0x1FL);

            int ax = x + 16, az = z + 16;
            if (ax < 0 || ax >= 64 || az < 0 || az >= 64) continue;

            if (currentLight <= 1) continue;
            for (int[] dir : directions) {
                int nx = x + dir[0];
                int ny = y + dir[1];
                int nz = z + dir[2];
                int anx = nx + 16, anz = nz + 16;

                if (anx < 0 || anx >= 64 || anz < 0 || anz >= 64 || ny < 0 || ny > 255) continue;

                int xInd = (nx < 0) ? 0 : (nx < 32 ? 1 : 2);
                int zInd = (nz < 0) ? 0 : (nz < 32 ? 1 : 2);
                ChunkColumn targetChunk = tempChunkMap[zInd*3 + xInd];
                if (targetChunk == null) continue;

                byte atLight = readLocalBlockLevel(anx, ny, anz);
                byte requestedLight = (byte) (currentLight-1);
                byte block = targetChunk.getBlockInChunk(nx&31, ny, nz&31);
                boolean isTransparent = (block == Blocks.AIR || Texture.isXShapedBlock[block] || Texture.isLeafBlock[block]);

                if (isTransparent && requestedLight > atLight && (xInd != 1 || zInd != 1) && isSource) {
                    int newSection = ny >> 4;
                    if (neighborsToRemesh == null) neighborsToRemesh = new ByteOpenHashSet(8);
                    neighborsToRemesh.add(generateDirtyKey(xInd, newSection, zInd));

                    Int2LongOpenHashMap lightToEffected = chunk.getSection(sourceY>>4).getlBlockExtChunksEffected(); // Source chunk

                    int key = ((sourceX & 31) & 0x1F) | (((sourceY&15) & 0xFF) << 5) | (((sourceZ & 31) & 0x1F) << 13);
                    long prev = lightToEffected.get(key);

                    lightToEffected.put(key, addDirtyToDataLong(prev, xInd, newSection, zInd));
                }

                if (requestedLight > atLight && isTransparent) {
                    setLocalBlockLevel(anx, ny, anz, requestedLight);
                    pendingBlockPropQueue.enqueue(packLightsource(nx, ny, nz, requestedLight, isSource, sourceX, sourceY, sourceZ));
                }
            }
        }
    }

    // Of block place obstructing or covering skylight directly or indirectly node
    public void addSkylightChunksToRemesh(int chunkX, int chunkY, int chunkZ) {
        if (neighborsToRemesh == null) neighborsToRemesh = new ByteOpenHashSet(8);
        // Section 15 is normal now, signal for all. Temporary this is CORRECT having 15 remesh all to test update simply
        int sectorI = chunkY >> 4;

        Int2LongOpenHashMap mapChunksEffected = chunk.getSection(sectorI).getlSkyExtChunksEffected();
        int key = ((chunkX & 31) & 0x1F) | (((chunkY&15) & 0xFF) << 5) | (((chunkZ & 31) & 0x1F) << 13);
        long extChunksEffected = mapChunksEffected.remove(key);

        for (int i = 0; i < 3; i++) {
            if ((extChunksEffected >>> i & 1) == 0) continue;
            int xInd = Math.toIntExact(((extChunksEffected >>> (long) (3 + 4 * i)) & 0x3L));
            int zInd = Math.toIntExact(((extChunksEffected >>> (long) (3 + 4 * i + 2) & 0x3L)));
            int data = Math.toIntExact(((extChunksEffected >>> (long) (3 + 4 * 3 + 16 * i)) & 0xFFFFL));

            while (data != 0) {
                int sec = Integer.numberOfTrailingZeros(data);
                if (neighborsToRemesh == null) neighborsToRemesh = new ByteOpenHashSet(8);
                neighborsToRemesh.add(generateDirtyKey(xInd, sec, zInd));
                data &= (data-1);
            }
        }
    }

    public void updateSkyLighting(boolean playerCaused) {
        LongArrayFIFOQueue pendingSkyPropQueue = tPendingLightPropQueue.get();
        ChunkColumn[] tempChunkMap = tTempChunkMap.get();
        pendingSkyPropQueue.clear();

        tempChunkMap[0] = xMinorZMinor; tempChunkMap[1] = zMinor; tempChunkMap[2] = xMajorZMinor;
        tempChunkMap[3] = xMinor; tempChunkMap[4] = chunk; tempChunkMap[5] = xMajor;
        tempChunkMap[6] = xMinorZMajor; tempChunkMap[7] = zMajor; tempChunkMap[8] = xMajorZMajor;

        for (int ax = 0; ax < 64; ax++) {
            for (int az = 0; az < 64; az++) {
                int xInd = (ax < 16) ? 0 : (ax <= 47 ? 1 : 2);
                int zInd = (az < 16) ? 0 : (az <= 47 ? 1 : 2);
                ChunkColumn targetChunk = tempChunkMap[zInd*3+xInd];
                if (targetChunk == null) continue;
                int lx = ax-16;
                int lz = az-16;
                int skyLight = 15;

                for (int y = 255; y >= 0; y--) {
                    byte block = targetChunk.getBlockInChunk(lx&31, y, lz&31);
                    if (block == Blocks.AIR) {
                        setLocalSkyLevel(ax, y, az, (byte) skyLight);
                        if (skyLight > 0) pendingSkyPropQueue.enqueue(packLightsource(lx, y, lz, skyLight, (xInd == 1 && zInd == 1), lx&31, y, lz&31));
                    } else if (Texture.isXShapedBlock[block] || Texture.isLeafBlock[block]) {
                        skyLight = Math.max(0, skyLight-1);
                        setLocalSkyLevel(ax, y, az, (byte) skyLight);
                        if (skyLight > 0) pendingSkyPropQueue.enqueue(packLightsource(lx, y, lz, skyLight, (xInd == 1 && zInd == 1), lx&31, y, lz&31));
                    } else {
                        break;
                    }
                }
            }
        }

        while (!pendingSkyPropQueue.isEmpty()) {
            long node = pendingSkyPropQueue.dequeueLong();
            int x = (int) ((node & 0x7FL) - 32);
            int y = (int) ((node >>> 7L) & 0xFFL);
            int z = (int) (((node >>> 15L) & 0x7FL) - 32L);
            int currentLight = (int) ((node >>> 22L) & 0xFL);
            boolean isSource = ((node >>> 26L) & 1L) == 1;
            int sourceX = (int) ((node >>> 27L) & 0x1FL);
            int sourceY = (int) ((node >>> 32L) & 0xFFL);
            int sourceZ = (int) ((node >>> 40L) & 0x1FL);

            int ax = x + 16, az = z + 16;
            if (ax < 0 || ax >= 64 || az < 0 || az >= 64) continue;
            int nodeXInd = (x < 0) ? 0 : (x < 32 ? 1 : 2);

            int nodeZInd = (z < 0) ? 0 : (z < 32 ? 1 : 2);

            if (currentLight <= 1) continue;
            for (int[] dir : directions) {
                int nx = x + dir[0];
                int ny = y + dir[1];
                int nz = z + dir[2];
                int anx = nx + 16, anz = nz + 16;

                if (anx < 0 || anx >= 64 || anz < 0 || anz >= 64 || ny < 0 || ny > 255) continue;

                int xInd = (nx < 0) ? 0 : (nx < 32 ? 1 : 2);
                int zInd = (nz < 0) ? 0 : (nz < 32 ? 1 : 2);
                ChunkColumn targetChunk = tempChunkMap[zInd*3 + xInd];
                if (targetChunk == null) continue;

                byte atLight = readLocalSkyLevel(anx, ny, anz);
                byte requestedLight = (byte) (currentLight-1);
                byte block = targetChunk.getBlockInChunk(nx&31, ny, nz&31);
                boolean isTransparent = (block == Blocks.AIR || Texture.isXShapedBlock[block] || Texture.isLeafBlock[block]);

                if (playerCaused && isTransparent && requestedLight > atLight && (xInd != 1 || zInd != 1) && isSource) {
                    if (neighborsToRemesh == null) neighborsToRemesh = new ByteOpenHashSet(8);
                    neighborsToRemesh.add(generateDirtyKey(xInd, ny >> 4, zInd));
                }

                if ((nodeXInd == 1 && nodeZInd == 1) && (xInd != 1 || zInd != 1) && isSource && requestedLight > atLight && isTransparent && playerCaused) {
                    // 1. Get the section using sourceY instead of current node Y
                    ChunkSection sourceSection = chunk.getSection(sourceY >> 4);
                    if (sourceSection != null) {
                        Int2LongOpenHashMap lightToEffected = sourceSection.getlSkyExtChunksEffected();

                        // 2. Build the key using source coordinates (sourceX, sourceY, sourceZ)
                        int key = (sourceX & 0x1F) | ((sourceY & 0x0F) << 5) | ((sourceZ & 0x1F) << 13);
                        long prev = lightToEffected.get(key);

                        lightToEffected.put(key, addDirtyToDataLong(prev, xInd, ny >> 4, zInd));
                    }
                }

                if (requestedLight > atLight) {
                    if ((block == Blocks.AIR || Texture.isXShapedBlock[block] || Texture.isLeafBlock[block])) {
                        setLocalSkyLevel(anx, ny, anz, requestedLight);
                        pendingSkyPropQueue.enqueue(packLightsource(nx, ny, nz, requestedLight, isSource, sourceX, sourceY, sourceZ));
                    }
                }
            }
        }
    }
}
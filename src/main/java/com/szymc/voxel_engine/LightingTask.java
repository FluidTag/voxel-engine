package com.szymc.voxel_engine;

import it.unimi.dsi.fastutil.bytes.ByteOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongArrayFIFOQueue;
import it.unimi.dsi.fastutil.ints.*;

import java.util.Arrays;

public class LightingTask {
    public int cx, cz;
    public ChunkColumn chunk;
    public ByteOpenHashSet neighborsToRemesh;

    private final ChunkColumn xMajor, xMinor, zMajor, zMinor,   xMajorZMajor, xMajorZMinor, xMinorZMajor, xMinorZMinor;
    private byte[] scratchPad;
    private ChunkColumn[] tempChunkMap;
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

        tempChunkMap = tTempChunkMap.get();
        tempChunkMap[0] = xMinorZMinor; tempChunkMap[1] = zMinor; tempChunkMap[2] = xMajorZMinor;
        tempChunkMap[3] = xMinor; tempChunkMap[4] = chunk; tempChunkMap[5] = xMajor;
        tempChunkMap[6] = xMinorZMajor; tempChunkMap[7] = zMajor; tempChunkMap[8] = xMajorZMajor;
    }

    public void clearChunkLighting() {
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
    private final static ThreadLocal<LongArrayFIFOQueue> tPendingSecondPropQueue = ThreadLocal.withInitial(() -> new LongArrayFIFOQueue(4089));
    private final static ThreadLocal<ChunkColumn[]> tTempChunkMap = ThreadLocal.withInitial(() -> new ChunkColumn[9]);
    private final static ThreadLocal<byte[]> tChunkLightPad = ThreadLocal.withInitial(() -> new byte[256*64*64]);

    private static byte generateDirtyKey(int xInd, int sec, int zInd) {
        return (byte) ((xInd & 0x3) | ((sec & 0xF) << 2) | ((zInd & 0x3) << 6));
    }

    private void processSkylightProp(LongArrayFIFOQueue pendingSkylightPropQueue, ChunkColumn[] tempChunkMap, boolean remeshAndDirect) {
        while (!pendingSkylightPropQueue.isEmpty()) {
            long node = pendingSkylightPropQueue.dequeueLong();
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

                byte atLight = remeshAndDirect ? targetChunk.getSkylight(nx&31, ny, nz&31) : readLocalSkyLevel(anx, ny, anz);
                byte requestedLight = (byte) (currentLight-1);
                byte block = targetChunk.getBlockInChunk(nx&31, ny, nz&31);
                boolean isTransparent = (block == Blocks.AIR || Texture.isXShapedBlock[block] || Texture.isLeafBlock[block]);

                if (remeshAndDirect && isTransparent && requestedLight > atLight && (xInd != 1 || zInd != 1) && isSource) {
                    int newSection = ny >> 4;
                    if (neighborsToRemesh == null) neighborsToRemesh = new ByteOpenHashSet(8);
                    neighborsToRemesh.add(generateDirtyKey(xInd, newSection, zInd));
                }

                if (requestedLight > atLight && isTransparent) {
                    if (remeshAndDirect) targetChunk.setSkylight(nx&31, ny, nz&31, requestedLight); else setLocalSkyLevel(anx, ny, anz, requestedLight);
                    pendingSkylightPropQueue.enqueue(packLightsource(nx, ny, nz, requestedLight, isSource, sourceX, sourceY, sourceZ));
                }
            }
        }
    }

    private void processBlockProp(LongArrayFIFOQueue pendingBlockPropQueue, ChunkColumn[] tempChunkMap, boolean remeshAndDirect) {
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

                byte atLight = remeshAndDirect ? targetChunk.getBlockLight(nx&31, ny, nz&31) : readLocalBlockLevel(anx, ny, anz);
                byte requestedLight = (byte) (currentLight-1);
                byte block = targetChunk.getBlockInChunk(nx&31, ny, nz&31);
                boolean isTransparent = (block == Blocks.AIR || Texture.isXShapedBlock[block] || Texture.isLeafBlock[block]);

                if (remeshAndDirect && isTransparent && requestedLight > atLight && (xInd != 1 || zInd != 1) && isSource) {
                    int newSection = ny >> 4;
                    if (neighborsToRemesh == null) neighborsToRemesh = new ByteOpenHashSet(8);
                    neighborsToRemesh.add(generateDirtyKey(xInd, newSection, zInd));
                }

                if (requestedLight > atLight && isTransparent) {
                    if (remeshAndDirect) targetChunk.setBlockLight(nx&31, ny, nz&31, requestedLight); else setLocalBlockLevel(anx, ny, anz, requestedLight);
                    pendingBlockPropQueue.enqueue(packLightsource(nx, ny, nz, requestedLight, isSource, sourceX, sourceY, sourceZ));
                }
            }
        }
    }

    public void updateBlockLighting() {
        LongArrayFIFOQueue pendingBlockPropQueue = tPendingLightPropQueue.get();
        pendingBlockPropQueue.clear();

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
                }
            }
        }

        processBlockProp(pendingBlockPropQueue, tempChunkMap, false);
    }

    // Removing blocks
    public void updateChunkLightingRemoval(int chunkX, int chunkY, int chunkZ) {
        LongArrayFIFOQueue pendingSkyAdditionQueue = tPendingLightPropQueue.get();
        LongArrayFIFOQueue pendingLightRepropQueue = tPendingSecondPropQueue.get();

        pendingSkyAdditionQueue.clear();
        pendingLightRepropQueue.clear();

        if (readLocalSkyLevel(chunkX+16, chunkY+1, chunkZ+16) == 15) {
            for (int y = chunkY; y >= 0; y--) {
                byte block = chunk.getBlockInChunk(chunkX, y, chunkZ);
                boolean isTransparent = (block == Blocks.AIR || Texture.isXShapedBlock[block] || Texture.isLeafBlock[block]);
                byte skyLevel = readLocalSkyLevel(chunkX + 16, y, chunkZ + 16);

                if (isTransparent && skyLevel != 15) {
                    setLocalSkyLevel(chunkX + 16, y, chunkZ + 16, (byte) 15);
                    pendingSkyAdditionQueue.enqueue(packLightsource(chunkX, y, chunkZ, 15, true, chunkX, y, chunkZ));
                }

                if (block != Blocks.AIR && y != chunkY) break;
            }
        } else {
            for (int[] dir : directions) {
                int nx = chunkX+dir[0]; int ny = chunkY+dir[1]; int nz = chunkZ+dir[2];
                int xInd = (nx < 0 ? 0 : (nx < 32 ? 1 : 2)); int zInd = (nz < 0 ? 0 : (nz < 32 ? 1 : 2));
                ChunkColumn target = tempChunkMap[zInd*3+xInd];
                byte skylight = target.getSkylight(nx&31, ny, nz&31);
                if (skylight > 1) pendingSkyAdditionQueue.enqueue(packLightsource(nx, ny, nz, skylight, true, nx, ny, nz));
            }
        }

        // Skylight
        processSkylightProp(pendingSkyAdditionQueue, tempChunkMap, true);
        pendingSkyAdditionQueue.clear();

        // Standard Blocklight (Needs better remesh support or other section border lighting fixes) (likely meshing issue)
        for (int[] dir : directions) {
            int nx = chunkX+dir[0]; int ny = chunkY+dir[1]; int nz = chunkZ+dir[2];
            int xInd = (nx < 0 ? 0 : (nx < 32 ? 1 : 2)); int zInd = (nz < 0 ? 0 : (nz < 32 ? 1 : 2));
            ChunkColumn target = tempChunkMap[zInd*3+xInd];
            byte blockLight = target.getBlockLight(nx&31, ny, nz&31);
            if (blockLight > 1) pendingSkyAdditionQueue.enqueue(packLightsource(nx, ny, nz, blockLight, true, nx, ny, nz));
        }
        processBlockProp(pendingSkyAdditionQueue, tempChunkMap, true);
        pendingSkyAdditionQueue.clear();

        // Block Lighting (Add Negative)
        for (int i = 0; i < 16; i++) {
            ChunkSection section = chunk.getSection(i);
            if (section == null) continue;

            IntArrayList removals = section.getLremovals();
            IntIterator it = removals.intIterator();
            while (it.hasNext()) {
                int dat = it.nextInt();
                int lx = (dat & 0x1F); int ly = (dat >>> 5) & 0xFF; int lz = (dat >>> 13) & 0x1F; int light = (dat >>> 18) & 0xFF;
                setLocalBlockLevel(lx+16, ly+16*i, lz+16, (byte)0);
                pendingSkyAdditionQueue.enqueue(packLightsource(lx, ly + 16*i, lz, light, true, lx, ly+16*i, lz));

                it.remove();
            }
        }

        // Block Lighting (Depropogate)
        while (!pendingSkyAdditionQueue.isEmpty()) {
            long node = pendingSkyAdditionQueue.dequeueLong();
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

                byte atLight = targetChunk.getBlockLight(nx&31, ny, nz&31);
                byte requestedLight = (byte) (currentLight-1);
                byte block = targetChunk.getBlockInChunk(nx&31, ny, nz&31);
                boolean isTransparent = (block == Blocks.AIR || Texture.isXShapedBlock[block] || Texture.isLeafBlock[block]);

                if (isTransparent && requestedLight == atLight && (xInd != 1 || zInd != 1) && isSource) {
                    int newSection = ny >> 4;
                    if (neighborsToRemesh == null) neighborsToRemesh = new ByteOpenHashSet(8);
                    neighborsToRemesh.add(generateDirtyKey(xInd, newSection, zInd));
                }
                //System.out.println(requestedLight + " vs " + atLight + " @ " + nx + ", " + ny + ", " + nz);
                if (requestedLight == atLight && isTransparent) {
                    targetChunk.setBlockLight(nx&31, ny, nz&31, (byte)0);
                    pendingSkyAdditionQueue.enqueue(packLightsource(nx, ny, nz, requestedLight, isSource, sourceX, sourceY, sourceZ));
                } else if (atLight > requestedLight && isTransparent) {
                    pendingLightRepropQueue.enqueue(packLightsource(nx, ny, nz, atLight, isSource, sourceX, sourceY, sourceZ));
                }
            }
        }

        // Reback block lighting
        processBlockProp(pendingLightRepropQueue, tempChunkMap, true);
    }

    // Placing blocks
    public void updateChunkLightingPlacement(int chunkX, int chunkY, int chunkZ, byte placedBlock) {
        LongArrayFIFOQueue pendingSkyRemovalQueue = tPendingLightPropQueue.get();
        LongArrayFIFOQueue pendingSkyRebackQueue = tPendingSecondPropQueue.get();

        pendingSkyRemovalQueue.clear();
        pendingSkyRebackQueue.clear();

        byte currentSkylevel = readLocalSkyLevel(chunkX+16, chunkY, chunkZ+16);
        if (currentSkylevel > 0) {
            setLocalSkyLevel(chunkX + 16, chunkY, chunkZ+16, (byte) 0);
            pendingSkyRemovalQueue.enqueue(packLightsource(chunkX, chunkY, chunkZ, currentSkylevel, true, chunkX, chunkY, chunkZ));
        }

        if (currentSkylevel == 15) {
            System.out.println("Invalidating down sky column at " + chunkX + ", " + chunkY + ", " + chunkZ);
            for (int y = chunkY-1; y >= 0; y--) {
                byte block = chunk.getBlockInChunk(chunkX, y, chunkZ);
                boolean isTransparent = (block == Blocks.AIR || Texture.isXShapedBlock[block] || Texture.isLeafBlock[block]);
                byte skyLevel = readLocalSkyLevel(chunkX + 16, y, chunkZ + 16);

                if (isTransparent) {
                    setLocalSkyLevel(chunkX + 16, y, chunkZ + 16, (byte) 0);
                    pendingSkyRemovalQueue.enqueue(packLightsource(chunkX, y, chunkZ, skyLevel, true, chunkX, y, chunkZ));
                }

                if (block != Blocks.AIR && y != chunkY) break;
            }
        }

        while (!pendingSkyRemovalQueue.isEmpty()) {
            long node = pendingSkyRemovalQueue.dequeueLong();
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

                byte atLight = targetChunk.getSkylight(nx&31, ny, nz&31);
                byte requestedLight = (byte) (currentLight-1);
                byte block = targetChunk.getBlockInChunk(nx&31, ny, nz&31);
                boolean isTransparent = (block == Blocks.AIR || Texture.isXShapedBlock[block] || Texture.isLeafBlock[block]);

                if (isTransparent && requestedLight == atLight && (xInd != 1 || zInd != 1) && isSource) {
                    int newSection = ny >> 4;
                    if (neighborsToRemesh == null) neighborsToRemesh = new ByteOpenHashSet(8);
                    neighborsToRemesh.add(generateDirtyKey(xInd, newSection, zInd));
                }

                if (requestedLight == atLight && isTransparent) {
                    targetChunk.setSkylight(nx&31, ny, nz&31, 0);
                    pendingSkyRemovalQueue.enqueue(packLightsource(nx, ny, nz, requestedLight, isSource, sourceX, sourceY, sourceZ));
                } else if (atLight > requestedLight && isTransparent) {
                    pendingSkyRebackQueue.enqueue(packLightsource(nx, ny, nz, atLight, (xInd == 1 && zInd == 1), sourceX, sourceY, sourceZ));
                }
            }
        }

        processSkylightProp(pendingSkyRebackQueue, tempChunkMap, true);

        if (Texture.lightLevels[placedBlock] == 0) return;
        setLocalBlockLevel(chunkX+16, chunkY, chunkZ+16, Texture.lightLevels[placedBlock]);
        pendingSkyRebackQueue.enqueue(packLightsource(chunkX, chunkY, chunkZ, Texture.lightLevels[placedBlock], true, chunkX, chunkY, chunkZ));

        // Blocklight Propogation
        processBlockProp(pendingSkyRebackQueue, tempChunkMap, true);
    }

    public void updateSkyLighting() {
        LongArrayFIFOQueue pendingSkyPropQueue = tPendingLightPropQueue.get();
        pendingSkyPropQueue.clear();

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

        processSkylightProp(pendingSkyPropQueue, tempChunkMap, false);
    }
}
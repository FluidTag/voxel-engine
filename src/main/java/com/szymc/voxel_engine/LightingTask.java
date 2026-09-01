package com.szymc.voxel_engine;

import it.unimi.dsi.fastutil.bytes.ByteArrayList;
import it.unimi.dsi.fastutil.bytes.ByteOpenHashSet;
import it.unimi.dsi.fastutil.ints.Int2BooleanOpenHashMap;
import it.unimi.dsi.fastutil.ints.IntArrayFIFOQueue;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntIterator;

import java.util.Arrays;

public class LightingTask {
    public int cx, cz;
    public ChunkColumn chunk;
    public ByteOpenHashSet neighborsToRemesh = null;

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

    // Pack the light level (0-15) into bits 22-25
    private static int packLightsource(int x, int y, int z, int light, boolean negativeLight, boolean fromSource) {
        return ((x + 32) & 0x7F) | (((y & 0xFF) << 7)) | (((z + 32) & 0x7F) << 15) | ((light & 0xF) << 22) | ((negativeLight ? 1 : 0) << 26) | ((fromSource ? 1 : 0) << 27);
    }

    private final static int[][] directions = {
            {1, 0, 0}, {-1, 0, 0},
            {0, 1, 0}, {0, -1, 0},
            {0, 0, 1}, {0, 0, -1}
    };

    // NOTE: All 8 surronding chunks (including corners) must be checked for loaded prior to running
    private final static ThreadLocal<IntArrayFIFOQueue> tPendingLightPropQueue = ThreadLocal.withInitial(() -> new IntArrayFIFOQueue(4089));
    private final static ThreadLocal<ChunkColumn[]> tTempChunkMap = ThreadLocal.withInitial(() -> new ChunkColumn[9]);
    private final static ThreadLocal<byte[]> tChunkLightPad = ThreadLocal.withInitial(() -> new byte[256*64*64]);
    private final static ThreadLocal<Int2BooleanOpenHashMap> tNegativeVisited = ThreadLocal.withInitial(Int2BooleanOpenHashMap::new);

    public void updateBlockLighting() {
        IntArrayFIFOQueue pendingBlockPropQueue = tPendingLightPropQueue.get();
        ChunkColumn[] tempChunkMap = tTempChunkMap.get();
        pendingBlockPropQueue.clear();

        tempChunkMap[0] = xMinorZMinor; tempChunkMap[1] = zMinor; tempChunkMap[2] = xMajorZMinor;
        tempChunkMap[3] = xMinor; tempChunkMap[4] = chunk; tempChunkMap[5] = xMajor;
        tempChunkMap[6] = xMinorZMajor; tempChunkMap[7] = zMajor; tempChunkMap[8] = xMajorZMajor;

        Int2BooleanOpenHashMap negativeVisited = tNegativeVisited.get();
        negativeVisited.clear();

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

                        setLocalBlockLevel(ax, sy, az, (byte) light);
                        pendingBlockPropQueue.enqueue(packLightsource(ax-16, sy, az-16, light, false, (x == 1 && z == 1)));
                    }

                    IntArrayList removals = section.getLremovals();
                    IntIterator it = removals.listIterator();
                    while (it.hasNext()) {
                        int dat = it.nextInt();
                        int sx = dat & 0x1F;
                        int sy = ((dat >>> 5) & 0xFF) + (16*sectorI);
                        int sz = (dat >>> 13) & 0x1F;
                        int light = (dat >>> 18) & 0xFF;

                        int ax = sx + (x*32) - 16; int az = sz + (z*32) - 16;
                        if (ax < 0 || ax >= 64 || az < 0 || az >= 64) continue;

                        pendingBlockPropQueue.enqueue(packLightsource(ax-16, sy, az-16, light, true, (x == 1 && z == 1)));
                        it.remove();
                    }
                }
            }
        }

        while (!pendingBlockPropQueue.isEmpty()) {
            int node = pendingBlockPropQueue.dequeueInt();
            int x = (node & 0x7F) - 32;
            int y = (node >>> 7) & 0xFF;
            int z = ((node >>> 15) & 0x7F) - 32;
            int currentLight = (node >>> 22) & 0xF;
            boolean negativeLight = ((node >>> 26) & 1) == 1;
            boolean isSource = ((node >>> 27) & 1) == 1;

            if (negativeLight && negativeVisited.containsKey(node)) {
                continue;
            } else if (negativeLight) negativeVisited.put(node, true);

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

                if (((atLight > requestedLight) || negativeLight) && (xInd != 1 || zInd != 1) && ((!negativeLight && isSource) || (negativeLight && !isSource))) {
                    if (neighborsToRemesh == null) neighborsToRemesh = new ByteOpenHashSet(8);

                    int section = ny >> 4;
                    byte dirtyDat = (byte) ((xInd & 0x3) | ((section & 0xF) << 2) | ((zInd & 0x3) << 6));
                    neighborsToRemesh.add(dirtyDat);
                }

                if (negativeLight || requestedLight > atLight) {
                    byte block = targetChunk.getBlockInChunk(nx&31, ny, nz&31);

                    if ((block == Blocks.AIR || Texture.isXShapedBlock[block] || Texture.isLeafBlock[block])) {
                        if (!negativeLight) setLocalBlockLevel(anx, ny, anz, requestedLight);
                        pendingBlockPropQueue.enqueue(packLightsource(nx, ny, nz, requestedLight, negativeLight, isSource));
                    }
                }
            }
        }
    }

    public void updateSkyLighting() {
        IntArrayFIFOQueue pendingSkyPropQueue = tPendingLightPropQueue.get();
        ChunkColumn[] tempChunkMap = tTempChunkMap.get();
        pendingSkyPropQueue.clear();

        tempChunkMap[0] = xMinorZMinor; tempChunkMap[1] = zMinor; tempChunkMap[2] = xMajorZMinor;
        tempChunkMap[3] = xMinor; tempChunkMap[4] = chunk; tempChunkMap[5] = xMajor;
        tempChunkMap[6] = xMinorZMajor; tempChunkMap[7] = zMajor; tempChunkMap[8] = xMajorZMajor;

        for (int ax = 0+15; ax < 64-15; ax++) {
            for (int az = 0+15; az < 64-15; az++) {
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
                        if (skyLight > 0) pendingSkyPropQueue.enqueue(packLightsource(lx, y, lz, skyLight, false, (xInd == 1 && zInd == 1)));
                    } else if (Texture.isXShapedBlock[block] || Texture.isLeafBlock[block]) {
                        skyLight = Math.max(0, skyLight-1);
                        setLocalSkyLevel(ax, y, az, (byte) skyLight);
                        if (skyLight > 0) pendingSkyPropQueue.enqueue(packLightsource(lx, y, lz, skyLight, false, (xInd == 1 && zInd == 1)));
                    } else {
                        break;
                    }
                }
            }
        }

        while (!pendingSkyPropQueue.isEmpty()) {
            int node = pendingSkyPropQueue.dequeueInt();
            int x = (node & 0x7F) - 32;
            int y = (node >>> 7) & 0xFF;
            int z = ((node >>> 15) & 0x7F) - 32;
            int currentLight = (node >>> 22) & 0xF;

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

                byte atLight = readLocalSkyLevel(anx, ny, anz);
                byte requestedLight = (byte) (currentLight-1);

                if (requestedLight > atLight) {
                    byte block = targetChunk.getBlockInChunk(nx&31, ny, nz&31);

                    if ((block == Blocks.AIR || Texture.isXShapedBlock[block] || Texture.isLeafBlock[block])) {
                        setLocalSkyLevel(anx, ny, anz, requestedLight);
                        pendingSkyPropQueue.enqueue(packLightsource(nx, ny, nz, requestedLight, false, (xInd == 1 && zInd == 1)));
                    }
                }
            }
        }
    }
}
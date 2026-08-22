package com.szymc.voxel_engine;

import it.unimi.dsi.fastutil.ints.IntArrayFIFOQueue;

import java.util.Arrays;

public class LightingTask {
    public int cx, cz;
    public ChunkColumn chunk;

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
    }

    private void setLocalLightLevel(byte[] arr, int ax, int y, int az, byte light) {
        if (ax >= 16 && ax <= 47 && az >= 16 && az <= 47) {
            chunk.setSkylight(ax-16, y, az-16, light);
            return;
        }

        scratchPad[(y<<12) | (az << 6) | ax] = light;
    }

    private byte readLocalLightLevel(byte[] arr, int ax, int y, int az) {
        if (ax >= 16 && ax <= 47 && az >= 16 && az <= 47) {
            return (byte) chunk.getSkylight(ax-16, y, az-16);
        }

        return scratchPad[(y<<12) | (az << 6) | ax];
    }

    // Pack the light level (0-15) into bits 22-25
    private static int packLightsource(int x, int y, int z, int light) {
        return ((x + 32) & 0x7F) | (((y & 0xFF) << 7)) | (((z + 32) & 0x7F) << 15) | ((light & 0xF) << 22);
    }

    private final static int[][] directions = {
            {1, 0, 0}, {-1, 0, 0},
            {0, 1, 0}, {0, -1, 0},
            {0, 0, 1}, {0, 0, -1}
    };

    // NOTE: All 8 surronding chunks (including corners) must be checked for loaded prior to running
    private final static ThreadLocal<IntArrayFIFOQueue> tPendingSkyPropQueue = ThreadLocal.withInitial(() -> new IntArrayFIFOQueue(4089));
    private final static ThreadLocal<ChunkColumn[]> tTempChunkMap = ThreadLocal.withInitial(() -> new ChunkColumn[9]);
    private final static ThreadLocal<byte[]> tChunkLightPad = ThreadLocal.withInitial(() -> new byte[256*64*64]);

    public void updateSkyLighting() {
        IntArrayFIFOQueue pendingSkyPropQueue = tPendingSkyPropQueue.get();
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
                        setLocalLightLevel(scratchPad, ax, y, az, (byte) skyLight);
                        if (skyLight > 0) pendingSkyPropQueue.enqueue(packLightsource(lx, y, lz, skyLight));
                    } else if (Texture.isXShapedBlock[block] || Texture.isLeafBlock[block]) {
                        skyLight = Math.max(0, skyLight-1);
                        setLocalLightLevel(scratchPad, ax, y, az, (byte) skyLight);
                        if (skyLight > 0) pendingSkyPropQueue.enqueue(packLightsource(lx, y, lz, skyLight));
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

                byte atLight = readLocalLightLevel(scratchPad, anx, ny, anz);
                byte requestedLight = (byte) (currentLight-1);

                if (requestedLight > atLight) {
                    byte block = targetChunk.getBlockInChunk(nx&31, ny, nz&31);

                    if ((block == Blocks.AIR || Texture.isXShapedBlock[block] || Texture.isLeafBlock[block])) {
                        setLocalLightLevel(scratchPad, anx, ny, anz, requestedLight);
                        pendingSkyPropQueue.enqueue(packLightsource(nx, ny, nz, requestedLight));
                    }
                }
            }
        }
    }
}

package com.szymc.voxel_engine;

import it.unimi.dsi.fastutil.ints.IntArrayList;

import java.util.Arrays;
import java.util.Objects;

public class GreedyMesher {
    private final static ThreadLocal<IntArrayList> threadVertexBuffer = ThreadLocal.withInitial(() -> new IntArrayList(8192));
    private final static ThreadLocal<IntArrayList> threadIndexBuffer = ThreadLocal.withInitial(() -> new IntArrayList(12288));
    private final static ThreadLocal<IntArrayList> threadWaterVBuffer = ThreadLocal.withInitial(() -> new IntArrayList(8192));
    private final static ThreadLocal<IntArrayList> threadWaterIBuffer = ThreadLocal.withInitial(() -> new IntArrayList(12288));

    private final ChunkSection chunkData;
    public GreedyMesher(ChunkSection section) {
        this.chunkData = Objects.requireNonNull(section, "Meshing cannot be started without a section to mesh.");
    }

    public SectionMeshResult generateSectionMesh(ChunkSection xMajor, ChunkSection xMinor, ChunkSection yMajor, ChunkSection yMinor, ChunkSection zMajor, ChunkSection zMinor) {
        return generateMeshData(xMajor, xMinor, yMajor, yMinor, zMajor, zMinor);
    }

    public static void addGrassShrub(IntArrayList vBuffer, IntArrayList iBuffer, int x, int y, int z, byte blockType) {
        int bx = x * 10;
        int by = y * 10;
        int bz = z * 10;

        int uWidth = 1;
        int vHeight = 1;
        byte noAO = (byte) (3 | (3 << 2) | (3 << 4) | (3 << 6));
        boolean flipQuad = false;
        int topY = by + 10;

        // --- DIAGONAL 1 ---
        // Front Face (axis set to 3)
        addQuad(vBuffer, iBuffer,
                bx + 1, by, bz + 1,
                bx + 1, topY, bz + 1,
                bx + 9, by, bz + 9,
                bx + 9, topY, bz + 9,
                uWidth, vHeight, blockType, false, 1, noAO, flipQuad, true);

        // Back Face (axis set to 3)
        addQuad(vBuffer, iBuffer,
                bx + 1, by, bz + 1,
                bx + 1, topY, bz + 1,
                bx + 9, by, bz + 9,
                bx + 9, topY, bz + 9,
                uWidth, vHeight, blockType, true, 1, noAO, flipQuad, true);


        // --- DIAGONAL 2 ---
        // Front Face (axis set to 3)
        addQuad(vBuffer, iBuffer,
                bx + 9, by, bz + 1,
                bx + 9, topY, bz + 1,
                bx + 1, by, bz + 9,
                bx + 1, topY, bz + 9,
                uWidth, vHeight, blockType, false, 1, noAO, flipQuad, true);

        // Back Face (axis set to 3)
        addQuad(vBuffer, iBuffer,
                bx + 9, by, bz + 1,
                bx + 9, topY, bz + 1,
                bx + 1, by, bz + 9,
                bx + 1, topY, bz + 9,
                uWidth, vHeight, blockType, true, 1, noAO, flipQuad, true);
    }

    private static void addQuad(
            IntArrayList vBuffer,
            IntArrayList iBuffer,
            int x1, int y1, int z1,
            int x2, int y2, int z2,
            int x3, int y3, int z3,
            int x4, int y4, int z4,
            int width, int height, byte blockType, // U width, V height
            boolean backFace, int axis, byte packedAO, boolean flipQuad, boolean downscale
    ) {

        int addedVerts = vBuffer.size()/2;
        int texId = 0;
        if (axis == 1) texId = Texture.getTextureIndex(blockType, "TOP"); // Y
        if (axis == 2 || axis == 0) texId = Texture.getTextureIndex(blockType, "SIDE");

        int ao1 = packedAO & 0x3;
        int ao2 = (packedAO >> 2) & 0x3;
        int ao3 = (packedAO >> 4) & 0x3;
        int ao4 = (packedAO >> 6) & 0x3;
        byte scaleFlag = (byte) (downscale ? 1 : 0);

        if (axis == 0) {
            int vert1a = (x1 & 0x1FF) | ((y1 & 0x1FF) << 9) | ((z1 & 0x1FF) << 18);
            int vert1b = (texId & 0xFF) | ((height & 0x3F) << 8) | ((width & 0x3F) << 14) | ((ao1 & 0x3)) << 20 | (scaleFlag << 22);

            int vert2a = (x2 & 0x1FF) | ((y2 & 0x1FF) << 9) | ((z2 & 0x1FF) << 18); // Position
            int vert2b = (texId & 0xFF) | ((0 & 0x3F) << 8) | ((width & 0x3F) << 14) | ((ao2 & 0x3) << 20) | (scaleFlag << 22);

            int vert3a = (x3 & 0x1FF) | ((y3 & 0x1FF) << 9) | ((z3 & 0x1FF) << 18); // Position
            int vert3b = (texId & 0xFF) | ((height & 0x3F) << 8) | ((0 & 0x3F) << 14) | ((ao3 & 0x3) << 20) | (scaleFlag << 22);

            int vert4a = (x4 & 0x1FF) | ((y4 & 0x1FF) << 9) | ((z4 & 0x1FF) << 18); // Position
            int vert4b = (texId & 0xFF) | ((0 & 0x3F) << 8) | ((0 & 0x3F) << 14) | ((ao4 & 0x3) << 20) | (scaleFlag << 22);

            vBuffer.add(vert1a);
            vBuffer.add(vert1b);
            vBuffer.add(vert2a);
            vBuffer.add(vert2b);
            vBuffer.add(vert3a);
            vBuffer.add(vert3b);
            vBuffer.add(vert4a);
            vBuffer.add(vert4b);
        } else {
            int vert1a = (x1 & 0x1FF) | ((y1 & 0x1FF) << 9) | ((z1 & 0x1FF) << 18);
            int vert1b = (texId & 0xFF) | ((0 & 0x3F) << 8) | ((height & 0x3F) << 14) | ((ao1 & 0x3) << 20) | (scaleFlag << 22);

            int vert2a = (x2 & 0x1FF) | ((y2 & 0x1FF) << 9) | ((z2 & 0x1FF) << 18); // Position
            int vert2b = (texId & 0xFF) | ((0 & 0x3F) << 8) | ((0 & 0x3F) << 14) | ((ao2 & 0x3) << 20) | (scaleFlag << 22);

            int vert3a = (x3 & 0x1FF) | ((y3 & 0x1FF) << 9) | ((z3 & 0x1FF) << 18); // Position
            int vert3b = (texId & 0xFF) | ((width & 0x3F) << 8) | ((height & 0x3F) << 14) | ((ao3 & 0x3) << 20) | (scaleFlag << 22);

            int vert4a = (x4 & 0x1FF) | ((y4 & 0x1FF) << 9) | ((z4 & 0x1FF) << 18); // Position
            int vert4b = (texId & 0xFF) | ((width & 0x3F) << 8) | ((0 & 0x3F) << 14) | ((ao4 & 0x3) << 20) | (scaleFlag << 22);

            vBuffer.add(vert1a);
            vBuffer.add(vert1b);
            vBuffer.add(vert2a);
            vBuffer.add(vert2b);
            vBuffer.add(vert3a);
            vBuffer.add(vert3b);
            vBuffer.add(vert4a);
            vBuffer.add(vert4b);
        }

        if (backFace) {
            if (flipQuad) {
                // Slanted the other way for backface: 0-3-1 and 0-2-3
                iBuffer.add(addedVerts + 0);
                iBuffer.add(addedVerts + 3);
                iBuffer.add(addedVerts + 1);

                iBuffer.add(addedVerts + 0);
                iBuffer.add(addedVerts + 2);
                iBuffer.add(addedVerts + 3);
            } else {
                // Standard back face winding: 0-2-1 and 2-3-1
                iBuffer.add(addedVerts + 0);
                iBuffer.add(addedVerts + 2);
                iBuffer.add(addedVerts + 1);

                iBuffer.add(addedVerts + 2);
                iBuffer.add(addedVerts + 3);
                iBuffer.add(addedVerts + 1);
            }
        } else {
            if (flipQuad) {
                // Slanted the other way for frontface: 0-1-3 and 0-3-2
                iBuffer.add(addedVerts + 0);
                iBuffer.add(addedVerts + 1);
                iBuffer.add(addedVerts + 3);

                iBuffer.add(addedVerts + 0);
                iBuffer.add(addedVerts + 3);
                iBuffer.add(addedVerts + 2);
            } else {
                // Standard front face winding: 0-1-2 and 2-1-3
                iBuffer.add(addedVerts + 0);
                iBuffer.add(addedVerts + 1);
                iBuffer.add(addedVerts + 2);

                iBuffer.add(addedVerts + 2);
                iBuffer.add(addedVerts + 1);
                iBuffer.add(addedVerts + 3);
            }
        }
    }

    private void fillPaddedArr(byte[] arr, ChunkSection xMajor, ChunkSection xMinor, ChunkSection yMajor, ChunkSection yMinor, ChunkSection zMajor, ChunkSection zMinor) {
        byte[] chunk = chunkData.getChunkData(); // Using getChunkData() for the main chunk

        // --- MAIN CHUNK FILL ---
        // We can copy along the X axis (size 32) in one go because X is the fastest-moving index
        for (int y = 0; y < 16; y++) {
            for (int z = 0; z < 32; z++) {
                // Source format: x + (z*32) + (y*32*32). We start at x = 0
                int srcPos = (0) + (z * 32) + (y * 32 * 32);

                // Padded Destination format: (x+1) + ((z+1)*34) + ((y+1)*34*34)
                int destPos = (0 + 1) + ((z + 1) * 34) + ((y + 1) * 34 * 34);

                System.arraycopy(chunk, srcPos, arr, destPos, 32);
            }
        }

        // --- X NEIGHBORS (Padded along the X-edges) ---
        // Since X is contiguous, we cannot copy a whole row for X-boundaries.
        // We must copy individual bytes.
        if (xMinor != null) {
            byte[] xMinDat = xMinor.getChunkData();
            for (int y = 0; y < 16; y++) {
                for (int z = 0; z < 32; z++) {
                    // Grab the maximum X coordinate (31) of the minor neighbor
                    int srcPos = (31) + (z * 32) + (y * 32 * 32);
                    // Place at X = 0 in the padded array
                    int destPos = (0) + ((z + 1) * 34) + ((y + 1) * 34 * 34);

                    arr[destPos] = xMinDat[srcPos];
                }
            }
        }

        if (xMajor != null) {
            byte[] xMaxDat = xMajor.getChunkData();
            for (int y = 0; y < 16; y++) {
                for (int z = 0; z < 32; z++) {
                    // Grab the minimum X coordinate (0) of the major neighbor
                    int srcPos = (0) + (z * 32) + (y * 32 * 32);
                    // Place at X = 33 in the padded array
                    int destPos = (33) + ((z + 1) * 34) + ((y + 1) * 34 * 34);

                    arr[destPos] = xMaxDat[srcPos];
                }
            }
        }

        // --- Z NEIGHBORS ---
        if (zMinor != null) {
            byte[] zMinDat = zMinor.getChunkData();
            for (int y = 0; y < 16; y++) {
                // Grab the maximum Z row (31) of the minor neighbor
                int srcPos = (0) + (31 * 32) + (y * 32 * 32);
                // Place at Z = 0 in the padded array
                int destPos = (0 + 1) + (0 * 34) + ((y + 1) * 34 * 34);

                System.arraycopy(zMinDat, srcPos, arr, destPos, 32);
            }
        }

        if (zMajor != null) {
            byte[] zMaxDat = zMajor.getChunkData();
            for (int y = 0; y < 16; y++) {
                // Grab the minimum Z row (0) of the major neighbor
                int srcPos = (0) + (0 * 32) + (y * 32 * 32);
                // Place at Z = 33 in the padded array
                int destPos = (0 + 1) + (33 * 34) + ((y + 1) * 34 * 34);

                System.arraycopy(zMaxDat, srcPos, arr, destPos, 32);
            }
        }

        // --- Y NEIGHBORS ---
        if (yMinor != null) {
            byte[] yMinDat = yMinor.getChunkData();
            for (int z = 0; z < 32; z++) {
                // Grab the maximum Y slice (15) of the minor neighbor
                int srcPos = (0) + (z * 32) + (15 * 32 * 32);
                // Place at Y = 0 in the padded array
                int destPos = (0 + 1) + ((z + 1) * 34) + (0 * 34 * 34);

                System.arraycopy(yMinDat, srcPos, arr, destPos, 32);
            }
        }

        if (yMajor != null) {
            byte[] yMajDat = yMajor.getChunkData();
            for (int z = 0; z < 32; z++) {
                // Grab the minimum Y slice (0) of the major neighbor
                int srcPos = (0) + (z * 32) + (0 * 32 * 32);
                // Place at Y = 17 in the padded array
                int destPos = (0 + 1) + ((z + 1) * 34) + (17 * 34 * 34);

                System.arraycopy(yMajDat, srcPos, arr, destPos, 32);
            }
        }
    }

    // Used for AO calculations
    private static boolean isOpqaue(byte block) {
        return block != 0 && block != Blocks.WATER && block != Blocks.GRASS_DECORATION
                && block != Blocks.RED_MUSHROOM_SMALL && block != Blocks.BROWN_MUSHROOM_SMALL && block != Blocks.RED_FLOWER;
    }

    private static byte calculateCornerAO(byte side1, byte side2, byte corner) {
        byte AO = 3;
        boolean side1Opaque = isOpqaue(side1);
        boolean side2Opqaue = isOpqaue(side2);

        if (side1Opaque && side2Opqaue) {
            AO = 0;
        } else {
            if (side1Opaque) AO--;
            if (side2Opqaue) AO--;
            if (isOpqaue(corner)) AO--;
        }

        return AO;
    }

    private static byte calculateBlockAO(byte[] padded, int u, int axis, int v, int methodAxis, boolean backFace, int uStride, int vStride, int nStride) {
        byte corner1AO = 3, corner2AO = 3, corner3AO = 3, corner4AO = 3;

        // Calculate the face's normal coordinate layer in padded space
        int normalCoord = (axis + 1) + (backFace ? -1 : 1);
        int normalIdx = normalCoord * nStride;

        // Corner 1: du = -1, dv = +1
        byte c1s1 = padded[(u+1-1)*uStride + (v+1)*vStride + normalIdx];
        byte c1s2 = padded[(u+1)*uStride + (v+1+1)*vStride + normalIdx];
        byte c1c  = padded[(u+1-1)*uStride + (v+1+1)*vStride + normalIdx];
        corner1AO = calculateCornerAO(c1s1, c1s2, c1c);

        // Corner 2: du = +1, dv = +1
        byte c2s1 = padded[(u+1+1)*uStride + (v+1)*vStride + normalIdx];
        byte c2s2 = padded[(u+1)*uStride + (v+1+1)*vStride + normalIdx];
        byte c2c  = padded[(u+1+1)*uStride + (v+1+1)*vStride + normalIdx];
        corner2AO = calculateCornerAO(c2s1, c2s2, c2c);

        // Corner 3: du = -1, dv = -1
        byte c3s1 = padded[(u+1-1)*uStride + (v+1)*vStride + normalIdx];
        byte c3s2 = padded[(u+1)*uStride + (v+1-1)*vStride + normalIdx];
        byte c3c  = padded[(u+1-1)*uStride + (v+1-1)*vStride + normalIdx];
        corner3AO = calculateCornerAO(c3s1, c3s2, c3c);

        // Corner 4: du = +1, dv = -1
        byte c4s1 = padded[(u+1+1)*uStride + (v+1)*vStride + normalIdx];
        byte c4s2 = padded[(u+1)*uStride + (v+1-1)*vStride + normalIdx];
        byte c4c  = padded[(u+1+1)*uStride + (v+1-1)*vStride + normalIdx];
        corner4AO = calculateCornerAO(c4s1, c4s2, c4c);

        return (byte) ((corner1AO & 0x3) | ((corner2AO & 0x3) << 2) | ((corner3AO & 0x3) << 4) | ((corner4AO & 0x3) << 6));
    }

    private static final ThreadLocal<long[]> TnegVisibleFaces = ThreadLocal.withInitial(() -> new long[32]);
    private static final ThreadLocal<long[]> TposVisibleFaces = ThreadLocal.withInitial(() -> new long[32]);
    private static final ThreadLocal<byte[]> TlocalAOFront = ThreadLocal.withInitial(() -> new byte[32 * 32]);
    private static final ThreadLocal<byte[]> TlocalAOBack = ThreadLocal.withInitial(() -> new byte[32 * 32]);

    private void meshAxis(byte[] chunk, long[] occupancyMask, long[] waterMask, long[] leavesMask, int methodAxis,
                          int axisLimit, int uLimit, int vLimit, int paddedULimit, byte[] padded,
                          IntArrayList vertexBuffer, IntArrayList indexBuffer,
                          IntArrayList waterVBuffer, IntArrayList waterIBuffer
    ) {
        // visible faces in 0 to 15 regular chunk range
        // with information of -1 to 16 or 18 length padded arr

        long[] negVisibleFaces = TnegVisibleFaces.get();
        long[] posVisibleFaces = TposVisibleFaces.get();
        byte[] localAOFront = TlocalAOFront.get();
        byte[] localAOBack = TlocalAOBack.get();

        int uStride = 0, vStride = 0, nStride = 0;
        if (methodAxis == 2) { // Z-normal (u=X, v=Y, n=Z)
            uStride = 1;       // X-stride
            vStride = 34 * 34; // Y-stride
            nStride = 34;      // Z-stride
        } else if (methodAxis == 1) { // Y-normal (u=X, v=Z, n=Y)
            uStride = 1;       // X-stride
            vStride = 34;      // Z-stride
            nStride = 34 * 34; // Y-stride
        } else if (methodAxis == 0) { // X-normal (u=Y, v=Z, n=X)
            uStride = 34 * 34; // Y-stride
            vStride = 34;      // Z-stride
            nStride = 1;       // X-stride
        }

        for (int axis = 0; axis < axisLimit; axis++) {
            // Visibility building for axis
            for (int u = 0; u < uLimit; u++) {
                int curIdx = (axis+1) * paddedULimit + (u+1);
                int negIdx = (axis) * paddedULimit + (u+1);
                int posIdx = (axis+2) * paddedULimit + (u+1);

                long currentSolid = occupancyMask[curIdx];
                long currentWater = waterMask[curIdx];
                long currentLeaves = leavesMask[curIdx];

                long negWater = waterMask[negIdx];
                long negLeaves = leavesMask[negIdx];
                long negSolid = occupancyMask[negIdx];

                long negFaces = ((currentSolid | currentLeaves) &~ negSolid) | (currentWater &~ negSolid & ~negWater);

                long posWater = waterMask[posIdx];
                long posLeaves = leavesMask[posIdx];
                long posSolid = occupancyMask[posIdx];

                long posFaces = ((currentSolid | currentLeaves) &~ posSolid) | (currentWater &~ posSolid & ~posWater);

                // FIX: Discard neighbor block bits (bit 0 and bit 17)
                // and shift right by 1 so bit 1 becomes bit 0 (native 0-15 space)
                long mask = (1L << vLimit) - 1L;
                negVisibleFaces[u] = (negFaces >> 1L) & mask;
                posVisibleFaces[u] = (posFaces >> 1L) & mask;
            }

            for (int u = 0; u < uLimit; u++) {
                for (int v = 0; v < vLimit; v++) {
                    localAOFront[u*vLimit + v] = calculateBlockAO(padded, u, axis, v, methodAxis, false, uStride, vStride, nStride);
                    localAOBack[u*vLimit + v] = calculateBlockAO(padded, u, axis, v, methodAxis, true, uStride, vStride, nStride);
                }
            }

            for (int u = 0; u < uLimit; u++) {
                while (negVisibleFaces[u] != 0) {
                    int vStart = Long.numberOfTrailingZeros(negVisibleFaces[u]);
                    long widthMask = 0;
                    int vEnd = vStart;
                    byte startBlock = 0; // needs conversion
                    byte startAO = localAOBack[u*vLimit + vStart];
                    //y, z, x
                    if (methodAxis == 2) {
                        startBlock = chunk[vStart*(32*32)+axis*32+u];
                    } else if (methodAxis == 1) {
                        startBlock = chunk[axis*(32*32)+vStart*32+u];
                    } else if (methodAxis == 0) {
                        startBlock = chunk[u*(32*32)+vStart*32+axis];
                    }

                    IntArrayList targetVBuffer = startBlock == Blocks.WATER ? waterVBuffer : vertexBuffer;
                    IntArrayList targetIBuffer = startBlock == Blocks.WATER ? waterIBuffer : indexBuffer;

                    while (vEnd < vLimit && (negVisibleFaces[u] & (1L << vEnd)) != 0) {
                        if (localAOBack[u*vLimit + vEnd] != startAO) break;

                        if (methodAxis == 2) {
                            if (chunk[vEnd*(32*32)+axis*32+u] != startBlock) break;
                        } else if (methodAxis == 1) {
                            if (chunk[axis*(32*32)+vEnd*32+u] != startBlock) break;
                        } else if (methodAxis == 0) {
                            if (chunk[u*(32*32)+vEnd*32+axis] != startBlock) break;
                        }

                        widthMask |= (1L << vEnd);
                        vEnd++;
                    }

                    int quadWidth = vEnd-vStart;
                    int uEnd = u;

                    while (uEnd < uLimit && (negVisibleFaces[uEnd] & widthMask) == widthMask) {
                        boolean cancelExpand = false;
                        for (int vc = vStart; vc < vEnd; vc++) {
                            if (localAOBack[uEnd*vLimit + vc] != startAO) {cancelExpand = true; break;}

                            if (methodAxis == 2) {
                                if (chunk[vc*(32*32)+axis*32+uEnd] != startBlock) {cancelExpand = true; break;};
                            } else if (methodAxis == 1) {
                                if (chunk[axis*(32*32)+vc*32+uEnd] != startBlock) {cancelExpand = true; break;};
                            } else if (methodAxis == 0) {
                                if (chunk[uEnd*(32*32)+vc*32+axis] != startBlock) {cancelExpand = true; break;};
                            }
                        }

                        if (cancelExpand) break;
                        uEnd++;
                    }

                    int quadHeight = uEnd-u;
                    for (int uH = u; uH < uEnd; uH++) {
                        negVisibleFaces[uH] &= ~widthMask;
                    }

                    int ao_TL = (startAO >> 0) & 0x3;
                    int ao_TR = (startAO >> 2) & 0x3;
                    int ao_BL = (startAO >> 4) & 0x3;
                    int ao_BR = (startAO >> 6) & 0x3;

                    boolean flipQuad = (ao_BL + ao_TR) < (ao_BR + ao_TL);

                    byte quadAo;
                    if (methodAxis == 2) {
                        quadAo = (byte) ((ao_BR & 0x3) | ((ao_BL & 0x3) << 2) | ((ao_TR & 0x3) << 4) | ((ao_TL & 0x3) << 6));
                    } else {
                        quadAo = (byte) ((ao_TL & 0x3) | ((ao_BL & 0x3) << 2) | ((ao_TR & 0x3) << 4) | ((ao_BR & 0x3) << 6));
                    }

                    if (methodAxis == 2) {
                        addQuad(targetVBuffer, targetIBuffer,
                                uEnd, vStart, axis,
                                u, vStart, axis,
                                uEnd, vEnd, axis,
                                u, vEnd, axis,

                                quadWidth, quadHeight,
                                startBlock, false, 0, quadAo, flipQuad, false
                        );
                    } else if (methodAxis == 1) {
                        addQuad(targetVBuffer, targetIBuffer,
                                u, axis, vEnd,
                                u, axis, vStart,
                                uEnd, axis, vEnd,
                                uEnd, axis, vStart,

                                quadHeight, quadWidth,
                                startBlock, false, 1, quadAo, flipQuad, false
                        );
                    } else if (methodAxis == 0) {
                        addQuad(targetVBuffer, targetIBuffer,
                                axis, u, vEnd,
                                axis, u, vStart,
                                axis, uEnd, vEnd,
                                axis, uEnd, vStart,

                                quadHeight, quadWidth,
                                startBlock, true, 0, quadAo, flipQuad, false
                        );
                    }
                }
            }

            for (int u = 0; u < uLimit; u++) {
                while (posVisibleFaces[u] != 0) {
                    int vStart = Long.numberOfTrailingZeros(posVisibleFaces[u]);
                    long widthMask = 0;
                    int vEnd = vStart;
                    byte startBlock = 0; // needs conversion
                    byte startAO = localAOFront[u*vLimit + vStart];

                    if (methodAxis == 2) {
                        startBlock = chunk[vStart*(32*32)+axis*32+u];
                    } else if (methodAxis == 1) {
                        startBlock = chunk[axis*(32*32)+vStart*32+u];
                    } else if (methodAxis == 0) {
                        startBlock = chunk[u*(32*32)+vStart*32+axis];
                    }

                    IntArrayList targetVBuffer = startBlock == Blocks.WATER ? waterVBuffer : vertexBuffer;
                    IntArrayList targetIBuffer = startBlock == Blocks.WATER ? waterIBuffer : indexBuffer;

                    while (vEnd < vLimit && (posVisibleFaces[u] & (1L << vEnd)) != 0) {
                        if (localAOFront[u*vLimit + vEnd] != startAO) break;

                        if (methodAxis == 2) {
                            if (chunk[vEnd*(32*32)+axis*32+u] != startBlock) break;
                        } else if (methodAxis == 1) {
                            if (chunk[axis*(32*32)+vEnd*32+u] != startBlock) break;
                        } else if (methodAxis == 0) {
                            if (chunk[u*(32*32)+vEnd*32+axis] != startBlock) break;
                        }

                        widthMask |= (1L << vEnd);
                        vEnd++;
                    }

                    int quadWidth = vEnd-vStart;
                    int uEnd = u;

                    while (uEnd < uLimit && (posVisibleFaces[uEnd] & widthMask) == widthMask) {
                        boolean cancelExpand = false;
                        for (int vc = vStart; vc < vEnd; vc++) {
                            if (localAOFront[uEnd*vLimit + vc] != startAO) {cancelExpand = true; break;}

                            if (methodAxis == 2) {
                                if (chunk[vc*(32*32)+axis*32+uEnd] != startBlock) {cancelExpand = true; break;};
                            } else if (methodAxis == 1) {
                                if (chunk[axis*(32*32)+vc*32+uEnd] != startBlock) {cancelExpand = true; break;};
                            } else if (methodAxis == 0) {
                                if (chunk[uEnd*(32*32)+vc*32+axis] != startBlock) {cancelExpand = true; break;};
                            }
                        }

                        if (cancelExpand) break;
                        uEnd++;
                    }

                    int quadHeight = uEnd-u;
                    for (int uH = u; uH < uEnd; uH++) {
                        posVisibleFaces[uH] &= ~widthMask;
                    }

                    int ao_TL = (startAO >> 0) & 0x3;
                    int ao_TR = (startAO >> 2) & 0x3;
                    int ao_BL = (startAO >> 4) & 0x3;
                    int ao_BR = (startAO >> 6) & 0x3;

                    boolean flipQuad = (ao_BL + ao_TR) < (ao_BR + ao_TL);

                    byte quadAo;
                    if (methodAxis == 2) {
                        quadAo = (byte) ((ao_BR & 0x3) | ((ao_BL & 0x3) << 2) | ((ao_TR & 0x3) << 4) | ((ao_TL & 0x3) << 6));
                    } else {
                        quadAo = (byte) ((ao_TL & 0x3) | ((ao_BL & 0x3) << 2) | ((ao_TR & 0x3) << 4) | ((ao_BR & 0x3) << 6));
                    }

                    if (methodAxis == 2) {
                        addQuad(targetVBuffer, targetIBuffer,
                                uEnd, vStart, axis+1,
                                u, vStart, axis+1,
                                uEnd, vEnd, axis+1,
                                u, vEnd, axis+1,

                                quadWidth, quadHeight,
                                startBlock, true, 0, quadAo, flipQuad, false
                        );
                    } else if (methodAxis == 1) {
                        addQuad(targetVBuffer, targetIBuffer,
                                u, axis+1, vEnd,
                                u, axis+1, vStart,
                                uEnd, axis+1, vEnd,
                                uEnd, axis+1, vStart,

                                quadHeight, quadWidth,
                                startBlock, true, 1, quadAo, flipQuad, false
                        );
                    } else if (methodAxis == 0) {
                        addQuad(targetVBuffer, targetIBuffer,
                                axis+1, u, vEnd,
                                axis+1, u, vStart,
                                axis+1, uEnd, vEnd,
                                axis+1, uEnd, vStart,

                                quadHeight, quadWidth,
                                startBlock, false, 0, quadAo, flipQuad, false
                        );
                    }
                }
            }
        }
    }

    private final static ThreadLocal<byte[]> threadPadded = ThreadLocal.withInitial(() -> new byte[34*18*34]);
    private final static ThreadLocal<long[]> tOccZ = ThreadLocal.withInitial(() -> new long[34*34]);
    private final static ThreadLocal<long[]> tWatZ = ThreadLocal.withInitial(() -> new long[34*34]);
    private final static ThreadLocal<long[]> tLeaZ = ThreadLocal.withInitial(() -> new long[34*34]);

    private final static ThreadLocal<long[]> tOccY = ThreadLocal.withInitial(() -> new long[18*34]);
    private final static ThreadLocal<long[]> tWatY = ThreadLocal.withInitial(() -> new long[18*34]);
    private final static ThreadLocal<long[]> tLeaY = ThreadLocal.withInitial(() -> new long[18*34]);

    private final static ThreadLocal<long[]> tOccX = ThreadLocal.withInitial(() -> new long[18*34]);
    private final static ThreadLocal<long[]> tWatX = ThreadLocal.withInitial(() -> new long[18*34]);
    private final static ThreadLocal<long[]> tLeaX = ThreadLocal.withInitial(() -> new long[18*34]);

    private SectionMeshResult generateMeshData(ChunkSection xMajor, ChunkSection xMinor, ChunkSection yMajor, ChunkSection yMinor, ChunkSection zMajor, ChunkSection zMinor) {
        SectionMeshResult result = new SectionMeshResult();
        result.vertices = null;
        result.indices = null;
        result.waterVertices = null;
        result.waterIndices = null;
        result.vCount = 0;
        result.iCount = 0;
        result.wvCount = 0;
        result.wiCount = 0;

        final IntArrayList vertexBuffer = threadVertexBuffer.get();
        final IntArrayList indexBuffer = threadIndexBuffer.get();
        final IntArrayList waterVBuffer = threadWaterVBuffer.get();
        final IntArrayList waterIBuffer = threadWaterIBuffer.get();

        vertexBuffer.clear();
        indexBuffer.clear();
        waterVBuffer.clear();
        waterIBuffer.clear();

        byte[] padded = threadPadded.get();
        Arrays.fill(padded, (byte)0);
        fillPaddedArr(padded, xMajor, xMinor, yMajor, yMinor, zMajor, zMinor);

        // 34 x layers, 18 y layers, each mask is 34 z bits
        // 18 y layers, 34 x layers, each mask is 34 z bits
        // 34 z layers, 34 x layers, each mask is 18 y bits

        // Iterates x & y, holds z
        long[] occZ = tOccZ.get();
        Arrays.fill(occZ, 0L);
        long[] watZ = tWatZ.get();
        Arrays.fill(watZ, 0L);
        long[] leaZ = tLeaZ.get();
        Arrays.fill(leaZ, 0L);

        // Iterates x & z, holds y
        long[] occY = tOccY.get();
        Arrays.fill(occY, 0L);
        long[] watY = tWatY.get();
        Arrays.fill(watY, 0L);
        long[] leaY = tLeaY.get();
        Arrays.fill(leaY, 0L);
        // Iterates y & z, holds x

        long[] occX = tOccX.get();
        Arrays.fill(occX, 0L);
        long[] watX = tWatX.get();
        Arrays.fill(watX, 0L);
        long[] leaX = tLeaX.get();
        Arrays.fill(leaX, 0L);

        byte[] chunk = chunkData.getChunkData();

        for (int y = 0; y < 16; y++) {
            for (int z = 0; z < 32; z++) {
                for (int x = 0; x < 32; x++) {
                    byte block = chunk[y*32*32 + z*32 + x];
                    if (block == Blocks.GRASS_DECORATION || block == Blocks.RED_MUSHROOM_SMALL
                            || block == Blocks.BROWN_MUSHROOM_SMALL || block == Blocks.RED_FLOWER) {
                        padded[(y+1)*34*34 + (z+1)*34 + (x+1)] = Blocks.AIR; // = 0
                        addGrassShrub(vertexBuffer, indexBuffer, x,y,z, block);
                    }
                }
            }
        }

        // Build the masks
        for (int y = 0; y < 18; y++) {
            for (int z = 0; z < 34; z++) {
                for (int x = 0; x < 34; x++) {
                    byte block = padded[y*(34*34) + z*34 + x];

                    if (block == Blocks.WATER) {
                        watZ[z*34+x] |= (1L << y);
                        watY[y*34+x] |= (1L << z);
                        watX[x*18+y] |= (1L << z);
                    } else if (block == Blocks.OAK_LEAVES
                            || block == Blocks.BIRCH_LEAVES
                            || block == Blocks.SPRUCE_LEAVES
                            || block == Blocks.ACACIA_LEAVES) {
                        leaZ[z*34+x] |= (1L << y);
                        leaY[y*34+x] |= (1L << z);
                        leaX[x*18+y] |= (1L << z);
                    } else if (block != 0 && block != Blocks.GRASS_DECORATION
                            && block != Blocks.RED_MUSHROOM_SMALL &&
                            block != Blocks.BROWN_MUSHROOM_SMALL && block != Blocks.RED_FLOWER) {
                        occZ[z*34+x] |= (1L << y);
                        occY[y*34+x] |= (1L << z);
                        occX[x*18+y] |= (1L << z);
                    }
                }
            }
        }

        meshAxis(chunk, occZ, watZ, leaZ, 2, 32, 32, 16, 34, padded, vertexBuffer, indexBuffer, waterVBuffer, waterIBuffer);
        meshAxis(chunk, occY, watY, leaY, 1, 16, 32, 32, 34, padded, vertexBuffer, indexBuffer, waterVBuffer, waterIBuffer);
        meshAxis(chunk, occX, watX, leaX, 0, 32, 16, 32, 18, padded, vertexBuffer, indexBuffer, waterVBuffer, waterIBuffer);

        result.vertices = vertexBuffer.toIntArray();
        result.indices = indexBuffer.toIntArray();
        result.vCount = vertexBuffer.size();
        result.iCount = indexBuffer.size();

        if (!waterVBuffer.isEmpty()) {
            result.waterVertices = waterVBuffer.toIntArray();
            result.waterIndices = waterIBuffer.toIntArray();
            result.wvCount = waterVBuffer.size();
            result.wiCount = waterIBuffer.size();
        }

        return result;
    }
}

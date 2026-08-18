package com.szymc.voxel_engine;

import it.unimi.dsi.fastutil.floats.FloatArrayList;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import org.lwjgl.stb.STBImage;
import org.lwjgl.system.MemoryUtil;

import java.nio.ByteBuffer;
import java.util.Arrays;

public class EntityItem extends Entity {
    byte item;
    boolean isBlock;

    private static void addQuad(
            FloatArrayList vBuffer,
            IntArrayList iBuffer,
            float x1, float y1, float z1,
            float x2, float y2, float z2,
            float x3, float y3, float z3,
            float x4, float y4, float z4,
            int width, int height, byte blockType, // U width, V height
            boolean backFace, int axis, byte packedAO
    ) {

        int addedVerts = vBuffer.size()/4;
        int texId = 0;

        if (axis == 1) texId = Texture.getTextureIndex(blockType, backFace ? BLOCK_FACE.TOP : BLOCK_FACE.BOTTOM); // Y
        if (axis == 0) texId = Texture.getTextureIndex(blockType, backFace ? BLOCK_FACE.WEST : BLOCK_FACE.EAST); // X
        if (axis == 2) texId = Texture.getTextureIndex(blockType, backFace ? BLOCK_FACE.SOUTH : BLOCK_FACE.NORTH); // Z
        //System.out.println(blockType + ": " + texId + ": On Axis " + axis + ", Backface? " + backFace);
        int ao1 = packedAO & 0x3;
        int ao2 = (packedAO >> 2) & 0x3;
        int ao3 = (packedAO >> 4) & 0x3;
        int ao4 = (packedAO >> 6) & 0x3;
        byte scaleFlag = 0;

        if (axis == 2) {
            int vert1b = (texId & 0xFF) | ((height & 0x3F) << 8) | ((width & 0x3F) << 14) | ((ao1 & 0x3)) << 20;
            int vert2b = (texId & 0xFF) | ((0 & 0x3F) << 8) | ((width & 0x3F) << 14) | ((ao2 & 0x3) << 20);
            int vert3b = (texId & 0xFF) | ((height & 0x3F) << 8) | ((0 & 0x3F) << 14) | ((ao3 & 0x3) << 20);
            int vert4b = (texId & 0xFF) | ((0 & 0x3F) << 8) | ((0 & 0x3F) << 14) | ((ao4 & 0x3) << 20);

            vBuffer.add(x1);
            vBuffer.add(y1);
            vBuffer.add(z1);
            vBuffer.add(Float.intBitsToFloat(vert1b));

            vBuffer.add(x2);
            vBuffer.add(y2);
            vBuffer.add(z2);
            vBuffer.add(Float.intBitsToFloat(vert2b));

            vBuffer.add(x3);
            vBuffer.add(y3);
            vBuffer.add(z3);
            vBuffer.add(Float.intBitsToFloat(vert3b));

            vBuffer.add(x4);
            vBuffer.add(y4);
            vBuffer.add(z4);
            vBuffer.add(Float.intBitsToFloat(vert4b));
        } else {
            int vert1b = (texId & 0xFF) | ((0 & 0x3F) << 8) | ((height & 0x3F) << 14) | ((ao1 & 0x3) << 20);
            int vert2b = (texId & 0xFF) | ((0 & 0x3F) << 8) | ((0 & 0x3F) << 14) | ((ao2 & 0x3) << 20);
            int vert3b = (texId & 0xFF) | ((width & 0x3F) << 8) | ((height & 0x3F) << 14) | ((ao3 & 0x3) << 20);
            int vert4b = (texId & 0xFF) | ((width & 0x3F) << 8) | ((0 & 0x3F) << 14) | ((ao4 & 0x3) << 20);

            vBuffer.add(x1);
            vBuffer.add(y1);
            vBuffer.add(z1);
            vBuffer.add(Float.intBitsToFloat(vert1b));

            vBuffer.add(x2);
            vBuffer.add(y2);
            vBuffer.add(z2);
            vBuffer.add(Float.intBitsToFloat(vert2b));

            vBuffer.add(x3);
            vBuffer.add(y3);
            vBuffer.add(z3);
            vBuffer.add(Float.intBitsToFloat(vert3b));

            vBuffer.add(x4);
            vBuffer.add(y4);
            vBuffer.add(z4);
            vBuffer.add(Float.intBitsToFloat(vert4b));
        }

        if (backFace) {
            iBuffer.add(addedVerts + 0);
            iBuffer.add(addedVerts + 2);
            iBuffer.add(addedVerts + 1);

            iBuffer.add(addedVerts + 2);
            iBuffer.add(addedVerts + 3);
            iBuffer.add(addedVerts + 1);
        } else {
            iBuffer.add(addedVerts + 0);
            iBuffer.add(addedVerts + 1);
            iBuffer.add(addedVerts + 2);

            iBuffer.add(addedVerts + 2);
            iBuffer.add(addedVerts + 1);
            iBuffer.add(addedVerts + 3);
        }
    }

    private static EntityMesh buildBlockMesh(byte blockType) {
        FloatArrayList vBuffer = new FloatArrayList(4*4);
        IntArrayList iBuffer = new IntArrayList(4*4*3);

        // 0xFF sets AO level to 3 on all 4 corners (vAoFactor = 1.0)
        byte fullAO = (byte) 0xFF;

        // 1. TOP (Y = 1, Axis 1, backFace = true)
        addQuad(vBuffer, iBuffer, 0,1,0, 1,1,0, 0,1,1, 1,1,1, 1, 1, blockType, true, 1, fullAO);

        // 2. BOTTOM (Y = 0, Axis 1, backFace = false)
        addQuad(vBuffer, iBuffer, 0,0,0, 1,0,0, 0,0,1, 1,0,1, 1, 1, blockType, false, 1, fullAO);

        // 3. WEST (X = 0, Axis 0, backFace = true)
        addQuad(vBuffer, iBuffer, 0,0,0, 0,1,0, 0,0,1, 0,1,1, 1, 1, blockType, true, 0, fullAO);

        // 4. EAST (X = 1, Axis 0, backFace = false)
        addQuad(vBuffer, iBuffer, 1,0,0, 1,1,0, 1,0,1, 1,1,1, 1, 1, blockType, false, 0, fullAO);

        // 5. NORTH (Z = 0, Axis 2, backFace = true)
        addQuad(vBuffer, iBuffer, 0,0,0, 1,0,0, 0,1,0, 1,1,0, 1, 1, blockType, true, 2, fullAO);

        // 6. SOUTH (Z = 1, Axis 2, backFace = false)
        addQuad(vBuffer, iBuffer, 0,0,1, 1,0,1, 0,1,1, 1,1,1, 1, 1, blockType, false, 2, fullAO);

        return new EntityMesh(vBuffer.toFloatArray(), iBuffer.toIntArray(), vBuffer.size(), iBuffer.size());
    }

    private static int packUV(int texId, int x, int y) {
        return (texId & 0xFF) | ((x & 0x3F) << 8) | ((y & 0x3F) << 14);
    }

    private static void addZWall(byte block, int y0, int y1, int x, int x1, ByteBuffer pixels16, FloatArrayList verts, IntArrayList indices, boolean backFace) {
        int startY = y0;
        int wallHeight = 0;
        int offsetX = backFace ? 0 : 1;
        int texId = Texture.getTextureIndex(block, BLOCK_FACE.NORTH);

        for (int jy = y0; jy < y1; jy++) {
            boolean sideTransparent = ((backFace && x == 0) || (!backFace && x == 15)) || (pixels16.get((jy*16+(x + (backFace ? -1 : 1)))*4 + 3) & 0xFF) < 20;
            //System.out.println("At x = " + (x + (backFace ? -1 : 1)) + " y = " + jy + ": " + (pixels16.get((jy*16+(x + (backFace ? -1 : 1)))*4 + 3) & 0xFF));
            if (sideTransparent) {
                if (wallHeight == 0) startY = jy;
                wallHeight += 1;
            } else {
                if (wallHeight == 0) continue;
                int vsAdded = verts.size()/4;

                verts.add(x+offsetX); verts.add(startY); verts.add(-1); verts.add(Float.intBitsToFloat(packUV(texId, x, y0)));
                verts.add(x+offsetX); verts.add(startY+wallHeight); verts.add(-1); verts.add(Float.intBitsToFloat(packUV(texId, x1, y0)));
                verts.add(x+offsetX); verts.add(startY); verts.add(0); verts.add(Float.intBitsToFloat(packUV(texId, x, y1)));
                verts.add(x+offsetX); verts.add(startY+wallHeight); verts.add(0); verts.add(Float.intBitsToFloat(packUV(texId, x1, y1)));

                if (backFace) {
                    indices.add(vsAdded); indices.add(vsAdded + 2); indices.add(vsAdded + 1);
                    indices.add(vsAdded + 2); indices.add(vsAdded + 3); indices.add(vsAdded + 1);
                } else {
                    indices.add(vsAdded); indices.add(vsAdded + 1); indices.add(vsAdded + 2);
                    indices.add(vsAdded + 2); indices.add(vsAdded + 1); indices.add(vsAdded + 3);
                }

                wallHeight = 0;
            }
        }

        if (wallHeight > 0) {
            int vsAdded = verts.size()/4;

            verts.add(x+offsetX); verts.add(startY); verts.add(-1); verts.add(Float.intBitsToFloat(packUV(texId, x, y0)));
            verts.add(x+offsetX); verts.add(startY+wallHeight); verts.add(-1); verts.add(Float.intBitsToFloat(packUV(texId, x1, y0)));
            verts.add(x+offsetX); verts.add(startY); verts.add(0); verts.add(Float.intBitsToFloat(packUV(texId, x, y1)));
            verts.add(x+offsetX); verts.add(startY+wallHeight); verts.add(0); verts.add(Float.intBitsToFloat(packUV(texId, x1, y1)));

            if (backFace) {
                indices.add(vsAdded); indices.add(vsAdded + 2); indices.add(vsAdded + 1);
                indices.add(vsAdded + 2); indices.add(vsAdded + 3); indices.add(vsAdded + 1);
            } else {
                indices.add(vsAdded); indices.add(vsAdded + 1); indices.add(vsAdded + 2);
                indices.add(vsAdded + 2); indices.add(vsAdded + 1); indices.add(vsAdded + 3);
            }
        }
    }

    private static void addYWall(byte block, int x0, int x1, int y, int y1, ByteBuffer pixels16, FloatArrayList verts, IntArrayList indices, boolean backFace) {
        int startX = x0;
        int wallHeight = 0;
        int offsetY = backFace ? 0 : 1;
        int texId = Texture.getTextureIndex(block, BLOCK_FACE.NORTH);

        for (int jx = x0; jx < x1; jx++) {
            int byteIndex = ((y + (backFace ? -1 : 1)) * 16 + jx) * 4;
            boolean sideTransparent = ((backFace && y == 0) || (!backFace && y == 15)) || ((pixels16.get(byteIndex+3)) & 0xFF) < 20;
            //System.out.println("At x = " + (x + (backFace ? -1 : 1)) + " y = " + jy + ": " + (pixels16.get((jy*16+(x + (backFace ? -1 : 1)))*4 + 3) & 0xFF));
            if (sideTransparent) {
                if (wallHeight == 0) startX = jx;
                wallHeight += 1;
            } else {
                if (wallHeight == 0) continue;
                int vsAdded = verts.size()/4;

                verts.add(startX); verts.add(y+offsetY); verts.add(-1);                 verts.add(Float.intBitsToFloat(packUV(texId, x0, y)));
                verts.add(startX+wallHeight); verts.add(y+offsetY); verts.add(-1);      verts.add(Float.intBitsToFloat(packUV(texId, x1, y)));
                verts.add(startX); verts.add(y+offsetY); verts.add(0);                 verts.add(Float.intBitsToFloat(packUV(texId, x0, y1)));
                verts.add(startX+wallHeight); verts.add(y+offsetY); verts.add(0);      verts.add(Float.intBitsToFloat(packUV(texId, x1, y1)));

                if (!backFace) {
                    indices.add(vsAdded); indices.add(vsAdded + 2); indices.add(vsAdded + 1);
                    indices.add(vsAdded + 2); indices.add(vsAdded + 3); indices.add(vsAdded + 1);
                } else {
                    indices.add(vsAdded); indices.add(vsAdded + 1); indices.add(vsAdded + 2);
                    indices.add(vsAdded + 2); indices.add(vsAdded + 1); indices.add(vsAdded + 3);
                }

                wallHeight = 0;
            }
        }

        if (wallHeight > 0) {
            int vsAdded = verts.size()/4;

            verts.add(startX); verts.add(y+offsetY); verts.add(-1);                 verts.add(Float.intBitsToFloat(packUV(texId, x0, y)));
            verts.add(startX+wallHeight); verts.add(y+offsetY); verts.add(-1);      verts.add(Float.intBitsToFloat(packUV(texId, x1, y)));
            verts.add(startX); verts.add(y+offsetY); verts.add(0);                 verts.add(Float.intBitsToFloat(packUV(texId, x0, y1)));
            verts.add(startX+wallHeight); verts.add(y+offsetY); verts.add(0);      verts.add(Float.intBitsToFloat(packUV(texId, x1, y1)));

            if (!backFace) {
                indices.add(vsAdded); indices.add(vsAdded + 2); indices.add(vsAdded + 1);
                indices.add(vsAdded + 2); indices.add(vsAdded + 3); indices.add(vsAdded + 1);
            } else {
                indices.add(vsAdded); indices.add(vsAdded + 1); indices.add(vsAdded + 2);
                indices.add(vsAdded + 2); indices.add(vsAdded + 1); indices.add(vsAdded + 3);
            }
        }
    }

    private static void addPrimaryFace(byte block, FloatArrayList verts, IntArrayList indices, int x0, int y0, int x1, int y1, int zOffset, boolean backFace) {
        int texId = Texture.getTextureIndex(block, BLOCK_FACE.NORTH);
        int vertsAdded = verts.size()/4;

        verts.add(x0); verts.add(y0); verts.add(zOffset);
        verts.add(Float.intBitsToFloat(packUV(texId, x0, y0)));

        verts.add(x1); verts.add(y0); verts.add(zOffset);
        verts.add(Float.intBitsToFloat(packUV(texId, x1, y0)));

        verts.add(x0); verts.add(y1); verts.add(zOffset);
        verts.add(Float.intBitsToFloat(packUV(texId, x0, y1)));

        verts.add(x1); verts.add(y1); verts.add(zOffset);
        verts.add(Float.intBitsToFloat(packUV(texId, x1, y1)));

        if (!backFace) {
            indices.add(vertsAdded + 0); indices.add(vertsAdded + 1); indices.add(vertsAdded + 2);
            indices.add(vertsAdded + 2); indices.add(vertsAdded + 1); indices.add(vertsAdded + 3);
        } else {
            indices.add(vertsAdded + 0); indices.add(vertsAdded + 2); indices.add(vertsAdded + 1);
            indices.add(vertsAdded + 2); indices.add(vertsAdded + 3); indices.add(vertsAdded + 1);
        }
    }

    private static boolean[] meshed = new boolean[16*16]; // Use red channel as key
    private static EntityMesh threeDimensionalFaceMesh(ByteBuffer pixels16, byte block) {
        Arrays.fill(meshed, false);
        FloatArrayList verts = new FloatArrayList(128);
        IntArrayList indices = new IntArrayList(128);

        // 4 bytes per pixel, rgba

        for (int y = 0; y < 16; y++) {
            for (int x = 0; x < 16; x++) {
                int pixelIndex = y*16+x;
                //System.out.println((pixels16.get((y*16+x)*4 + 3) & 0xFF));
                if (meshed[pixelIndex]) continue;

                int byteIndex = (pixelIndex*4);

                int r = ((pixels16.get(byteIndex)) & 0xFF);
                int g = ((pixels16.get(byteIndex+1)) & 0xFF);
                int b = ((pixels16.get(byteIndex+2)) & 0xFF);
                int a = ((pixels16.get(byteIndex+3)) & 0xFF);

                if (a < 20) continue;

                int width = 1;
                while (x + width < 16) {
                    int curByteIndex = (y*16+(x+width))*4;
                    if (meshed[y*16+(x+width)] ||
                            (pixels16.get(curByteIndex) & 0xFF) != r
                            || (pixels16.get(curByteIndex+1) & 0xFF) != g
                            || (pixels16.get(curByteIndex+2) & 0xFF) != b
                            || (pixels16.get(curByteIndex+3) & 0xFF) != a) break;
                    width++;
                }

                int height = 1;
                while (y + height < 16) {
                    boolean passed = true;
                    for (int jx = x; jx < x+width; jx++) {
                        int curByteIndex = ((y+height)*16 + jx) * 4;
                        if (meshed[(y+height)*16+jx] ||
                                (pixels16.get(curByteIndex) & 0xFF) != r
                                || (pixels16.get(curByteIndex+1) & 0xFF) != g
                                || (pixels16.get(curByteIndex+2) & 0xFF) != b
                                || (pixels16.get(curByteIndex+3) & 0xFF) != a) {
                            passed = false;
                            break;
                        }
                    }

                    if (!passed) break;
                    height++;
                }

                //System.out.println(x + ", " + y + ": " + width + " x " + height);

                int x0 = x;
                int y0 = y;
                int x1 = x+width;
                int y1 = y+height;

                addZWall(block, y0, y1, x, x1, pixels16, verts, indices, true);
                addZWall(block, y0, y1, x1-1, x1, pixels16, verts, indices, false);

                addYWall(block, x0, x1, y, y1, pixels16, verts, indices, true);
                addYWall(block, x0, x1, y1-1, y1, pixels16, verts, indices, false);

                for (int jy = y0; jy < y1; jy++) {
                    for (int jx = x0; jx < x1; jx++) {
                        meshed[jy*16+jx] = true;
                    }
                }

                addPrimaryFace(block, verts, indices, x0, y0, x1, y1, 0, false);
                addPrimaryFace(block, verts, indices, x0, y0, x1, y1, -1, true);
            }
        }

        return new EntityMesh(verts.toFloatArray(), indices.toIntArray(), verts.size(), indices.size());
    }

    public static Texture blockTextures;
    public EntityMesh itemMesh;
    public EntityItem(byte item) {
        this.item = item;
        this.entityId = Entity.entitiesCreated++;

        //itemMesh = buildBlockMesh(item);
        if (Texture.itemTexturePaths[item] == null && !Texture.isXShapedBlock[item]) {
            // 3d mesh - priority 1
            itemMesh = buildBlockMesh(item);
        } else if (Texture.isXShapedBlock[item]) {
            ByteBuffer pixels = blockTextures.getLayer(Texture.getTextureIndex(item, BLOCK_FACE.NORTH));

            itemMesh = threeDimensionalFaceMesh(pixels, item);
            MemoryUtil.memFree(pixels);
        }
    }

    public static void setBlockTextures(Texture texAttachmentRef) {
        blockTextures = texAttachmentRef;
    }
}

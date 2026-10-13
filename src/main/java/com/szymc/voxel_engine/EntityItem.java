package com.szymc.voxel_engine;

import it.unimi.dsi.fastutil.floats.FloatArrayList;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import org.lwjgl.system.MemoryUtil;

import java.nio.ByteBuffer;
import java.util.Arrays;

// VBO functions (glGenBuffers)
// Shader/Attribute functions (glVertexAttribPointer)
// VAO functions (glGenVertexArrays)

public class EntityItem extends Entity {
    short item;
    boolean isBlock;

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
                createZquad(verts, indices, x, y0, x+offsetX, x1, startY+wallHeight, backFace, texId);

                wallHeight = 0;
            }
        }

        if (wallHeight > 0) {
            createZquad(verts, indices, x, y0, x+offsetX, x1, y1, backFace, texId);
        }
    }

    private static void createZquad(FloatArrayList verts, IntArrayList indices, int x0, int y0, int x, int x1, int y1, boolean backFace, int texId) {
        int vsAdded = verts.size()/4;
        float sy0 = 16.0f-y0;
        float sy1 = 16.0f-y1;

        verts.add(x/16.0f); verts.add(sy0/16.0f); verts.add(-1/16.0f); verts.add(Float.intBitsToFloat(packUV(texId, x0, y0)));
        verts.add(x/16.0f); verts.add(sy1/16.0f); verts.add(-1/16.0f); verts.add(Float.intBitsToFloat(packUV(texId, x1, y0)));
        verts.add(x/16.0f); verts.add(sy0/16.0f); verts.add(0/16.0f); verts.add(Float.intBitsToFloat(packUV(texId, x0, y1)));
        verts.add(x/16.0f); verts.add(sy1/16.0f); verts.add(0/16.0f); verts.add(Float.intBitsToFloat(packUV(texId, x1, y1)));

        if (!backFace) {
            indices.add(vsAdded); indices.add(vsAdded + 2); indices.add(vsAdded + 1);
            indices.add(vsAdded + 2); indices.add(vsAdded + 3); indices.add(vsAdded + 1);
        } else {
            indices.add(vsAdded); indices.add(vsAdded + 1); indices.add(vsAdded + 2);
            indices.add(vsAdded + 2); indices.add(vsAdded + 1); indices.add(vsAdded + 3);
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
                createYquad(verts, indices, startX, y, y+offsetY, startX+wallHeight, y1, backFace, texId);

                wallHeight = 0;
            }
        }

        if (wallHeight > 0) {
            createYquad(verts, indices, startX, y, y+offsetY, x1, y1, backFace, texId);
        }
    }

    // x/z quad
    private static void createYquad(FloatArrayList verts, IntArrayList indices, int x0, int y0, int y, int x1, int y1, boolean backFace, int texId) {
        int vsAdded = verts.size()/4;
        float fixedY = 16.0f-y;

        verts.add(x0/16.0f); verts.add(fixedY/16.0f); verts.add(-1/16.0f);     verts.add(Float.intBitsToFloat(packUV(texId, x0, y0)));
        verts.add(x1/16.0f); verts.add(fixedY/16.0f); verts.add(-1/16.0f);     verts.add(Float.intBitsToFloat(packUV(texId, x1, y0)));
        verts.add(x0/16.0f); verts.add(fixedY/16.0f); verts.add(0/16.0f);      verts.add(Float.intBitsToFloat(packUV(texId, x0, y1)));
        verts.add(x1/16.0f); verts.add(fixedY/16.0f); verts.add(0/16.0f);      verts.add(Float.intBitsToFloat(packUV(texId, x1, y1)));

        if (backFace) {
            indices.add(vsAdded); indices.add(vsAdded + 2); indices.add(vsAdded + 1);
            indices.add(vsAdded + 2); indices.add(vsAdded + 3); indices.add(vsAdded + 1);
        } else {
            indices.add(vsAdded); indices.add(vsAdded + 1); indices.add(vsAdded + 2);
            indices.add(vsAdded + 2); indices.add(vsAdded + 1); indices.add(vsAdded + 3);
        }
    }

    private static void addPrimaryFace(byte block, FloatArrayList verts, IntArrayList indices, int x0, int y0, int x1, int y1, int zOffset, boolean backFace) {
        int texId = Texture.getTextureIndex(block, BLOCK_FACE.NORTH);
        int vertsAdded = verts.size()/4;
        float sy0 = 16.0f-y0;
        float sy1 = 16.0f-y1;

        verts.add(x0/16.0f); verts.add(sy0/16.0f); verts.add(zOffset/16.0f);
        verts.add(Float.intBitsToFloat(packUV(texId, x0, y0)));

        verts.add(x1/16.0f); verts.add(sy0/16.0f); verts.add(zOffset/16.0f);
        verts.add(Float.intBitsToFloat(packUV(texId, x1, y0)));

        verts.add(x0/16.0f); verts.add(sy1/16.0f); verts.add(zOffset/16.0f);
        verts.add(Float.intBitsToFloat(packUV(texId, x0, y1)));

        verts.add(x1/16.0f); verts.add(sy1/16.0f); verts.add(zOffset/16.0f);
        verts.add(Float.intBitsToFloat(packUV(texId, x1, y1)));

        if (backFace) {
            indices.add(vertsAdded + 0); indices.add(vertsAdded + 1); indices.add(vertsAdded + 2);
            indices.add(vertsAdded + 2); indices.add(vertsAdded + 1); indices.add(vertsAdded + 3);
        } else {
            indices.add(vertsAdded + 0); indices.add(vertsAdded + 2); indices.add(vertsAdded + 1);
            indices.add(vertsAdded + 2); indices.add(vertsAdded + 3); indices.add(vertsAdded + 1);
        }
    }

    private static boolean[] meshed = new boolean[16*16]; // Use red channel as key
    private static MeshDrawCommand threeDimensionalFaceMesh(ByteBuffer pixels16, byte block) {
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

        return Entity.generateEAODrawCommand(verts.toFloatArray(), indices.toIntArray(), verts.size()/4, indices.size());
    }

    public static Texture blockTextures;
    public float xWidth, zWidth;

    public static void generateStaticItemMeshes() {
        for (byte item = 1; item <= 71; item++) {
            if (Texture.itemTexturePaths[item] != -1) {
                ByteBuffer pixels = blockTextures.getLayer(Texture.itemTexturePaths[item]);

                Entity.cacheDrawCommand(item, threeDimensionalFaceMesh(pixels, item));
                MemoryUtil.memFree(pixels);
            } else if (Texture.itemTexturePaths[item] == -1 && !Texture.isXShapedBlock[item]) {
                Entity.cacheDrawCommand(item, buildBlockMesh(item, 0.4f));
            } else if (Texture.isXShapedBlock[item]) {
                ByteBuffer pixels = blockTextures.getLayer(Texture.getTextureIndex(item, BLOCK_FACE.NORTH));

                Entity.cacheDrawCommand(item, threeDimensionalFaceMesh(pixels, item));
                MemoryUtil.memFree(pixels);
            }
        }
    }

    public boolean playerDropped = false;
    public EntityItem(short item, int currentTick, boolean playerDropped) {
        super(currentTick);
        this.item = item;
        this.itemMesh = meshCache[item];
        this.playerDropped = playerDropped;

        if (Texture.itemTexturePaths[item] == -1 && !Texture.isXShapedBlock[item]) {
            isBlock = true;
            xWidth = 0.4f;
            zWidth = 0.4f;
        } else if (Texture.isXShapedBlock[item] || Texture.itemTexturePaths[item] != -1) {
            xWidth = 1;
            zWidth = 1/16f;
        }
    }

    public static void setBlockTextures(Texture texAttachmentRef) {
        blockTextures = texAttachmentRef;
    }
}

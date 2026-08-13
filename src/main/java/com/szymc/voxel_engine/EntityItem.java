package com.szymc.voxel_engine;

import it.unimi.dsi.fastutil.floats.FloatArrayList;
import it.unimi.dsi.fastutil.ints.IntArrayList;

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
            boolean backFace, int axis, byte packedAO, boolean flipQuad, boolean downscale
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
            int vert1b = (texId & 0xFF) | ((height & 0x3F) << 8) | ((width & 0x3F) << 14) | ((ao1 & 0x3)) << 20 | (scaleFlag << 22);
            int vert2b = (texId & 0xFF) | ((0 & 0x3F) << 8) | ((width & 0x3F) << 14) | ((ao2 & 0x3) << 20) | (scaleFlag << 22);
            int vert3b = (texId & 0xFF) | ((height & 0x3F) << 8) | ((0 & 0x3F) << 14) | ((ao3 & 0x3) << 20) | (scaleFlag << 22);
            int vert4b = (texId & 0xFF) | ((0 & 0x3F) << 8) | ((0 & 0x3F) << 14) | ((ao4 & 0x3) << 20) | (scaleFlag << 22);

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
            int vert1b = (texId & 0xFF) | ((0 & 0x3F) << 8) | ((height & 0x3F) << 14) | ((ao1 & 0x3) << 20) | (scaleFlag << 22);
            int vert2b = (texId & 0xFF) | ((0 & 0x3F) << 8) | ((0 & 0x3F) << 14) | ((ao2 & 0x3) << 20) | (scaleFlag << 22);
            int vert3b = (texId & 0xFF) | ((width & 0x3F) << 8) | ((height & 0x3F) << 14) | ((ao3 & 0x3) << 20) | (scaleFlag << 22);
            int vert4b = (texId & 0xFF) | ((width & 0x3F) << 8) | ((0 & 0x3F) << 14) | ((ao4 & 0x3) << 20) | (scaleFlag << 22);

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

    private static EntityMesh buildBlockMesh(byte blockType) {
        FloatArrayList vBuffer = new FloatArrayList(48); // 24 vertices * 2 ints
        IntArrayList iBuffer = new IntArrayList(36); // 6 faces * 6 indices

        // 0xFF sets AO level to 3 on all 4 corners (vAoFactor = 1.0)
        byte fullAO = (byte) 0xFF;

        // 1. TOP (Y = 1, Axis 1, backFace = true)
        addQuad(vBuffer, iBuffer, 0,1,0, 1,1,0, 0,1,1, 1,1,1, 1, 1, blockType, true, 1, fullAO, false, false);

        // 2. BOTTOM (Y = 0, Axis 1, backFace = false)
        addQuad(vBuffer, iBuffer, 0,0,0, 1,0,0, 0,0,1, 1,0,1, 1, 1, blockType, false, 1, fullAO, false, false);

        // 3. WEST (X = 0, Axis 0, backFace = true)
        addQuad(vBuffer, iBuffer, 0,0,0, 0,1,0, 0,0,1, 0,1,1, 1, 1, blockType, true, 0, fullAO, false, false);

        // 4. EAST (X = 1, Axis 0, backFace = false)
        addQuad(vBuffer, iBuffer, 1,0,0, 1,1,0, 1,0,1, 1,1,1, 1, 1, blockType, false, 0, fullAO, false, false);

        // 5. NORTH (Z = 0, Axis 2, backFace = true)
        addQuad(vBuffer, iBuffer, 0,0,0, 1,0,0, 0,1,0, 1,1,0, 1, 1, blockType, true, 2, fullAO, false, false);

        // 6. SOUTH (Z = 1, Axis 2, backFace = false)
        addQuad(vBuffer, iBuffer, 0,0,1, 1,0,1, 0,1,1, 1,1,1, 1, 1, blockType, false, 2, fullAO, false, false);

        return new EntityMesh(vBuffer.toFloatArray(), iBuffer.toIntArray(), vBuffer.size(), iBuffer.size());
    }

    public EntityMesh itemMesh;
    public EntityItem(byte item) {
        this.item = item;
        this.entityId = Entity.entitiesCreated++;

        if (Texture.itemTexturePaths[item] == null || Texture.isXShapedBlock[item]) {
            // 3d mesh - priority 1
            itemMesh = buildBlockMesh(item);
        } else {
            // icon
        }
    }
}

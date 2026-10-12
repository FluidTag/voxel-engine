package com.szymc.voxel_engine;

public class BlockEntity extends Entity {
    private int sizeOfInv = 36;
    private byte[] inventory;
    private byte[] inventoryAmounts;

    public static void generateBlockEntityMeshes() {
        for (byte i = 0; i <= 71; i++) {
            if (Texture.isBlockEntity[i]) {
                Entity.cacheDrawCommand(256 + i, buildBlockMesh(i, 1f));
            }
        }
    }

    public BlockEntity(byte item, int currentTick) {
        super(currentTick);
        System.out.println("Block entity of " + item + ": " + meshCache[item]);
        this.itemMesh = meshCache[256 + item];
    }
}

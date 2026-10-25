package com.szymc.voxel_engine;

public class BlockEntity extends Entity {
    private int sizeOfInv = 36;
    private byte[] inventory;
    private byte[] inventoryAmounts;

    // BlockEntity index format
    // 256 + (itemId * 16) for alloted space per block entity in the cache
    // [Default Command, 6x rotation types, 3 axis, 2 flip options each]
    public static void generateBlockEntityMeshes() {
        for (byte i = 0; i <= 71; i++) {
            if (Texture.isBlockEntity[i]) {
                Entity.cacheDrawCommand(256 + i*16, buildBlockMesh(i, 1f)); // No rotation, default state

                for (int axisRot = 0; axisRot < 3; axisRot++) {
                    for (int isBackFace = 0; isBackFace <= 1; isBackFace++) {
                        short modifiers = (short) (0b01 | ((axisRot & 0x3) << 2) | (isBackFace << 4));
                        int blockAndState = (i & 0xFFFF) | ((modifiers & 0xFFFF) << 16);

                        Entity.cacheDrawCommand(256 + i*16 + 1 + axisRot*2 + isBackFace, buildBlockMesh(blockAndState, 1f));
                    }
                }

                for (int rotationRotateAmount = 0; rotationRotateAmount < 4; rotationRotateAmount++) {
                    short modifiers = (short) (0b10 | ((rotationRotateAmount & 0x3) << 2));
                    int blockAndState = (i & 0xFFFF) | ((modifiers & 0xFFFF) << 16);

                    Entity.cacheDrawCommand(256 + i*16 + 1 + 6 + rotationRotateAmount, buildBlockMesh(blockAndState, 1f));
                }
            }
        }
    }

    public short blockType;
    public short blockModifiers;
    public BlockEntity(short item, short modifiers, int currentTick) {
        super(currentTick);
        int rotationType = (modifiers & 0x3);
        int coreRotationParameters = (modifiers >>> 2) & 0x3;
        int axisTypeBackfaceData = (modifiers >>> 4) & 0x1; // axis backface extra data

        System.out.printf("Block entity being made, rotation?: %d, params: %d%n", rotationType, coreRotationParameters);

        this.itemMesh = switch (rotationType) {
            case 0 -> meshCache[256 + item*16];
            case 1 -> meshCache[256 + item*16 + 1 + coreRotationParameters*2 + axisTypeBackfaceData];
            case 2 -> meshCache[256 + item*16 + 1 + 6 + coreRotationParameters];
            default -> throw new IllegalArgumentException("Rotation type " + rotationType + " is not currently supported");
        };

        this.blockType = item;
        this.blockModifiers = modifiers;
    }
}

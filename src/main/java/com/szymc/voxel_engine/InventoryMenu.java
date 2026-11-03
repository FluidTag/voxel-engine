package com.szymc.voxel_engine;

public class InventoryMenu extends AbstractContainerMenu {
    private ContainerData playerInv;
    private static final int hotbarGap = 15;

    public InventoryMenu(ContainerData playerInv) {
        this.playerInv = playerInv;

        for (int y = 0; y < 4; y++) {
            for (int x = 0; x < 9; x++) {
                int index = (3-y)*9 + x;
                this.addSlot(new Slot(playerInv, index, 0, 64*4, 64*x, 64*y - (y < 3 ? hotbarGap : 0), 64));
            }
        }
    }
}

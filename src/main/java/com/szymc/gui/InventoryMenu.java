package com.szymc.gui;

import com.szymc.voxel_engine.ContainerData;

public class InventoryMenu extends AbstractContainerMenu {
    private ContainerData playerInv;
    private static final int hotbarGap = 15;

    public InventoryMenu(ContainerData playerInv) {
        this.playerInv = playerInv;

        for (int y = 0; y < 2; y++) {
            for (int x = 0; x < 2; x++) {
                int index = 36 + (1-y)*2 + x;
                this.addSlot(new Slot(playerInv, index, 6*64, 64-hotbarGap, 64*x, 64*y, 64));
            }
        }

        for (int y = 0; y < 4; y++) {
            for (int x = 0; x < 9; x++) {
                int index = y != 3 ? (y+1)*9 + x : x;
                this.addSlot(new Slot(playerInv, index, 0, 64*4, 64*x, 64*y - (y < 3 ? hotbarGap : 0), 64));
            }
        }
    }
}

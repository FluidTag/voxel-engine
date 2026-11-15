package com.szymc.gui;

import com.szymc.voxel_engine.ContainerData;

public class ChestMenu extends AbstractContainerMenu {
    private ContainerData playerInv;
    private ContainerData chestInv;
    private static final int hotbarGap = 15;

    public ChestMenu(ContainerData playerInv, ContainerData chestInv) {
        this.playerInv = playerInv;
        this.chestInv = chestInv;

        for (int y = 0; y < 4; y++) {
            for (int x = 0; x < 9; x++) {
                int index = y*9 + x;
                this.addSlot(new Slot(chestInv, index, 0, 0, 64*x, 64*y + hotbarGap/2, 64));
            }
        }

        for (int y = 0; y < 4; y++) {
            for (int x = 0; x < 9; x++) {
                int index = y != 3 ? (y+1)*9 + x : x;
                this.addSlot(new Slot(playerInv, index, 0, 64*5, 64*x, 64*y - (y < 3 ? hotbarGap : 0), 64));
            }
        }
    }
}

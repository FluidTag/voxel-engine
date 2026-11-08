package com.szymc.gui;

import com.szymc.voxel_engine.ContainerData;

import java.awt.event.ItemEvent;

public class InventoryMenu extends AbstractContainerMenu {
    private ContainerData playerInv;
    private CraftingContext craftingContext;
    private static final int hotbarGap = 15;

    public InventoryMenu(ContainerData playerInv) {
        this.playerInv = playerInv;
        this.craftingContext = new CraftingContext(2, 2);

        for (int y = 0; y < 2; y++) {
            for (int x = 0; x < 2; x++) {
                int index = 1 + y*2 + x;
                this.addSlot(new Slot(craftingContext.getCraftingInventory(), index, 4*64, 64-hotbarGap, 64*x, 64*y, 64));
            }
        }

        this.addSlot(new ResultSlot(craftingContext.getCraftingInventory(), 0, 7*64, 64-hotbarGap + 32, 0, 0, 64, craftingContext));

        for (int y = 0; y < 4; y++) {
            for (int x = 0; x < 9; x++) {
                int index = y != 3 ? (y+1)*9 + x : x;
                this.addSlot(new Slot(playerInv, index, 0, 64*4, 64*x, 64*y - (y < 3 ? hotbarGap : 0), 64));
            }
        }
    }

    private final static int[][] passes = {
            {9, 39, 0},
            {0, 8,  0},
            {9, 39, 1},
            {0, 8,  1}
    };

    @Override
    public void shiftClickTransfer(Slot slotData) {
        int startSearch = slotData.index < 9 ? 9 : 0;
        int endSearch = slotData.index < 9 ? 36 : 8;
        System.out.println(slotData);
        int amountRemaining = slotData.readAmount();
        for (int i = startSearch; i <= endSearch; i++) {
            if (playerInv.readType(i) == slotData.readType()) {
                int maxCanBeApplied = 64-playerInv.readAmount(i);
                int beingApplied = Math.min(amountRemaining, maxCanBeApplied);

                playerInv.set(i, slotData.readType(), playerInv.readAmount(i)+beingApplied);
                amountRemaining-=beingApplied;
            }
        }

        if (amountRemaining > 0) {
            for (int i = startSearch; i <= endSearch; i++) {
                if (playerInv.isEmptyAt(i)) {
                    playerInv.set(i, slotData.readType(), amountRemaining);
                    amountRemaining = 0;
                    break;
                }
            }
        }

        slotData.set(slotData.readType(), (byte) amountRemaining);
    }
}

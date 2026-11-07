package com.szymc.gui;

import com.szymc.voxel_engine.ContainerData;

import java.awt.event.ItemEvent;

public class InventoryMenu extends AbstractContainerMenu {
    private ContainerData playerInv;
    private static final int hotbarGap = 15;

    public InventoryMenu(ContainerData playerInv) {
        this.playerInv = playerInv;

        for (int y = 0; y < 2; y++) {
            for (int x = 0; x < 2; x++) {
                int index = 36 + y*2 + x;
                this.addSlot(new Slot(playerInv, index, 4*64, 64-hotbarGap, 64*x, 64*y, 64));
            }
        }

        this.addSlot(new Slot(playerInv, 40, 7*64, 64-hotbarGap + 32, 0, 0, 64).setOutputOnly());

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

        playerInv.set(slotData.index, slotData.readType(), amountRemaining);
    }

    @Override
    public ItemStack doubleClickTransfer(Slot slotData, ItemStack currentActiveItem, ClickContext context) {
        int amountTaken = currentActiveItem == null ? slotData.readAmount() : currentActiveItem.amount();
        short gatherType = currentActiveItem == null ? slotData.readType() : currentActiveItem.type();
        if (gatherType == 0) return null;

        if (currentActiveItem == null) {
            playerInv.set(slotData.index, 0, 0);
        }

        for (int p = 0; p < passes.length; p++) {
            int[] dat = passes[p];
            int startInd = dat[0];
            int endInd = dat[1];
            boolean fullStacksOnly = dat[2] == 1;
            boolean endLoop = false;

            for (int i = startInd; i <= endInd; i++) {
                if (playerInv.readType(i) == gatherType) {
                    if (!fullStacksOnly && playerInv.readAmount(i) == 64) continue;

                    int maxCanTake = 64-amountTaken;
                    int beingTaken = Math.min(playerInv.readAmount(i), maxCanTake);
                    playerInv.set(i, playerInv.readType(i), playerInv.readAmount(i)-beingTaken);

                    amountTaken += beingTaken;
                    if (amountTaken == 64) {endLoop = true; break;}
                }
            }

            if (endLoop) break;
        }

        currentActiveItem = new ItemStack(gatherType, (byte) amountTaken);
        return currentActiveItem;
    }
}

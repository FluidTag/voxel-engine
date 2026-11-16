package com.szymc.gui;

import com.szymc.voxel_engine.ContainerData;

public class TableCraftMenu extends AbstractContainerMenu {
    private final ContainerData playerInv;
    private final CraftingContext craftingContext;
    private static final int hotbarGap = 15;

    public TableCraftMenu(ContainerData playerInv) {
        this.playerInv = playerInv;
        this.craftingContext = new CraftingContext(3, 3);

        for (int y = 0; y < 3; y++) {
            for (int x = 0; x < 3; x++) {
                int index = 1 + y*3 + x;
                this.addSlot(new Slot(craftingContext.getCraftingInventory(), index, 3*64, 64-hotbarGap-32, 64*x, 64*y, 64));
            }
        }

        this.addSlot(new ResultSlot(craftingContext.getCraftingInventory(), 0, 7*64, 64-hotbarGap+32, 0, 0, 64, craftingContext));

        for (int y = 0; y < 4; y++) {
            for (int x = 0; x < 9; x++) {
                int index = y != 3 ? (y+1)*9 + x : x;
                this.addSlot(new Slot(playerInv, index, 0, 64*4, 64*x, 64*y - (y < 3 ? hotbarGap : 0), 64));
            }
        }
    }

    @Override
    public void shiftClickTransfer(Slot slotData) {
        ContainerData contTarget = slotData.container == playerInv ? craftingContext.getCraftingInventory() : playerInv;
        int startSearch;
        int endSearch;
        if (slotData.container == playerInv) {
            startSearch = 1;
            endSearch = 9;
        } else {
            startSearch = 9;
            endSearch = 36;
        }

        System.out.println(slotData);
        int amountRemaining = slotData.readAmount();
        for (int i = startSearch; i <= endSearch; i++) {
            if (contTarget.readType(i) == slotData.readType()) {
                int maxCanBeApplied = 64-contTarget.readAmount(i);
                int beingApplied = Math.min(amountRemaining, maxCanBeApplied);

                contTarget.set(i, slotData.readType(), contTarget.readAmount(i)+beingApplied);
                amountRemaining -= beingApplied;

                if (amountRemaining == 0) break;
            }
        }

        if (amountRemaining > 0) {
            for (int i = startSearch; i <= endSearch; i++) {
                if (contTarget.isEmptyAt(i)) {
                    contTarget.set(i, slotData.readType(), amountRemaining);
                    amountRemaining = 0;
                    break;
                }
            }
        }

        slotData.set(slotData.readType(), (byte) amountRemaining);
    }

    @Override
    public void onCloseScreenHandle() {
        for (int i = 0; i < 9; i++) {
            this.flushSlotFromCraftIntoInventory(getSlots().get(i));
        }
    }
}

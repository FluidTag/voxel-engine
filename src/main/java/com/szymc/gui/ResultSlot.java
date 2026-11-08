package com.szymc.gui;

import com.szymc.voxel_engine.ContainerData;

public class ResultSlot extends Slot {
    private CraftingContext craftingContext;
    public ResultSlot(ContainerData container, int index, int anchorX, int anchorY, int x, int y, int size, CraftingContext context) {
        super(container, index, anchorX, anchorY, x, y, size);
        this.craftingContext = context;
    }

    @Override
    public boolean mayPlace(short itemType) {
        return false;
    }

    @Override
    public boolean mayPickup() {
        return true;
    }

    @Override
    public boolean canQuickCollect() {
        return false;
    }

    @Override
    public boolean maySplitAndPickup() {return false;}

    @Override
    public boolean isSourceSlot() {return true;}

    // Intercepts pickup before interacted with
    @Override
    public void onPickup(ClickContext context) {
//        if (!craftingContext.canCraft()) {
//            this.set((short) 0, (byte) 0);
//            return;
//        };

        short oldType = this.readType();
        byte amountMade = craftingContext.craft(context.isShiftClick());

        System.out.println("Type: " + oldType + " amount: " + amountMade);
        this.container.silentSet(this.index, oldType, amountMade);
    }

    @Override
    public void updateSlotContexts() {
        System.out.println(craftingContext.canCraft());
    }
}

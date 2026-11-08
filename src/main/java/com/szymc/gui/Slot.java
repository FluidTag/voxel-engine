package com.szymc.gui;

import com.szymc.voxel_engine.ContainerData;

public class Slot {
    public ContainerData container;
    public int index;

    // Top left corner of overall slot grid
    public int anchorX;
    public int anchorY;

    public int x;
    public int y;
    public int size;

    public Slot(ContainerData container, int index, int anchorX, int anchorY, int x, int y, int size) {
        this.container = container;
        this.index = index;

        this.anchorX = anchorX;
        this.anchorY = anchorY;

        this.x = x;
        this.y = y;
        this.size = size;
    }

    public boolean isEmpty() {
        return readType() == 0;
    }

    public void set(short type, byte amount) {
        this.container.set(index, type, amount);
    }

    public short readType() {
        return this.container.readType(index);
    }

    public byte readAmount() {
        return this.container.readAmount(index);
    }

    // Whether you can place the cursor item into a slot
    public boolean mayPlace(short itemType) {
        return true;
    }

    // Determines whether you can pick it up into your cursor
    public boolean mayPickup() {
        return true;
    }

    // Whether you can pick up half into your cursor from right click
    public boolean maySplitAndPickup() {return true;}

    // Not decremented when picked up, used for crafting primarily
    public boolean isSourceSlot() {return false;}

    // Can be considered in double click gather all
    public boolean canQuickCollect() {
        return true;
    }

    // Whether you can use shift click to transfer
    public boolean canShiftTransfer() {return true; }

    // Additional
    public void onPickup(ClickContext context) {}

    public void updateSlotContexts() {}

    @Override
    public String toString() {
        return String.format(
                "Slot[idx=%d, pos=(%d,%d), anchor=(%d,%d), size=%d, item=%dx%d, c=%s, flags=%s%s%s%s%s]",
                index, x, y, anchorX, anchorY, size, readType(), readAmount(),
                container != null ? container.getClass().getSimpleName() : "null",
                isSourceSlot() ? "S" : "-",
                canQuickCollect() ? "Q" : "-",
                canShiftTransfer() ? "T" : "-",
                mayPickup() ? "P" : "-",
                mayPlace((short) 0) ? "L" : "-"
        );
    }

}

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
    public boolean isOutputOnly = false;

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

    public Slot setOutputOnly() {
        Slot newSlot = new Slot(this.container, this.index, this.anchorX, this.anchorY, this.x, this.y, this.size);
        newSlot.isOutputOnly = true;

        return newSlot;
    }

    // Whether you can place the cursor item into a slot
    public boolean mayPlace(short itemType) {
        return !isOutputOnly;
    }

    // Determines whether you can pick it up into your cursor
    public boolean mayPickup() {
        return true;
    }

    // Can be considered in double click gather all
    public boolean canQuickCollect() {
        return true;
    }
}

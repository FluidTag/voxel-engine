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

    public Slot setOutputOnly() {
        Slot newSlot = new Slot(this.container, this.index, this.anchorX, this.anchorY, this.x, this.y, this.size);
        newSlot.isOutputOnly = true;

        return newSlot;
    }
}

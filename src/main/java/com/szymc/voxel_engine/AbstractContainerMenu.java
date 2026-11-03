package com.szymc.voxel_engine;

import java.util.ArrayList;
import java.util.List;

public class AbstractContainerMenu {
    protected final List<Slot> slots = new ArrayList<>();

    public void addSlot(Slot slot) {
        this.slots.add(slot);
    }

    public List<Slot> getSlots() {
        return this.slots;
    }

    public void handleSlotClick(int slotId, int clickType) {

    }
}

package com.szymc.gui;

import com.szymc.voxel_engine.ContainerData;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class AbstractContainerMenu {
    private final List<Slot> slots = new ArrayList<>();
    private final Set<ContainerData> containersIncluded = new HashSet<>(); // For quick access

    public void addSlot(Slot slot) {
        this.slots.add(slot);
        this.containersIncluded.add(slot.container);
    }

    public List<Slot> getSlots() {
        return this.slots;
    }
    public Set<ContainerData> getContainersIncluded() {return this.containersIncluded;}

    public void handleSlotClick(int slotId, int clickType) {

    }
}

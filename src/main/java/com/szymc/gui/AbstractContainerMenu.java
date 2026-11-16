package com.szymc.gui;

import com.szymc.voxel_engine.ContainerData;
import com.szymc.voxel_engine.PlayerCharacter;
import org.lwjgl.system.macosx.MacOSXLibraryDL;

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

    public void transferResultSlotIntoInventory(Slot slotData) {
        ContainerData playerInv = null;
        for (ContainerData cont : containersIncluded) {
            if (cont != slotData.container) {
                playerInv = cont;
                break;
            }
        }

        if (playerInv == null) return;

        int amountRemaining = slotData.readAmount();
        short type = slotData.readType();

        for (int y = 3; y >= 0; y--) {
            for (int x = 8; x >= 0; x--) {
                int index = (y == 3 ? 0 : y+1)*9 + x;
                if (playerInv.readType(index) == type) {
                    int beingApplied = Math.min(amountRemaining, 64-playerInv.readAmount(index));
                    playerInv.set(index, type, playerInv.readAmount(index)+beingApplied);
                    amountRemaining-=beingApplied;
                    if (amountRemaining == 0) break;
                }
            }

            if (amountRemaining == 0) break;
        }

        if (amountRemaining > 0) {
            for (int y = 3; y >= 0; y--) {
                for (int x = 8; x >= 0; x--) {
                    int index = (y == 3 ? 0 : y + 1)*9 + x;
                    if (playerInv.isEmptyAt(index)) {
                        playerInv.set(index, type, amountRemaining);
                        amountRemaining = 0;
                        break;
                    }
                }

                if (amountRemaining == 0) break;
            }
        }

        slotData.set(slotData.readType(), (byte) amountRemaining);
    };

    public void flushSlotFromCraftIntoInventory(Slot slotData) {
        ContainerData playerInv = null;
        for (ContainerData cont : containersIncluded) {
            if (cont != slotData.container) {
                playerInv = cont;
                break;
            }
        }

        if (playerInv == null) return;

        int amountRemaining = slotData.readAmount();
        short type = slotData.readType();

        for (int y = 3; y >= 0; y--) {
            for (int x = 0; x < 9; x++) {
                int index = (y == 3 ? 0 : y+1)*9 + x;
                if (playerInv.readType(index) == type) {
                    int beingApplied = Math.min(amountRemaining, 64-playerInv.readAmount(index));
                    playerInv.set(index, type, playerInv.readAmount(index)+beingApplied);
                    amountRemaining-=beingApplied;
                    if (amountRemaining == 0) break;
                }
            }

            if (amountRemaining == 0) break;
        }

        if (amountRemaining > 0) {
            for (int y = 3; y >= 0; y--) {
                for (int x = 0; x < 9; x++) {
                    int index = (y == 3 ? 0 : y + 1)*9 + x;
                    if (playerInv.isEmptyAt(index)) {
                        playerInv.set(index, type, amountRemaining);
                        amountRemaining = 0;
                        break;
                    }
                }

                if (amountRemaining == 0) break;
            }
        }

        slotData.set(slotData.readType(), (byte) amountRemaining);
    }

    public void onCloseScreenHandle() {};

    public void shiftClickTransfer(Slot slot) {
        // Generic transfer implementation

        ContainerData otherContainer = null;
        for (ContainerData cont : containersIncluded) {
            if (cont != slot.container) {
                otherContainer = cont;
                break;
            }
        }

        if (otherContainer == null) return;
        int amountRemaining = slot.readAmount();

        for (int i = 0; i < otherContainer.getSize(); i++) {
            if (otherContainer.readType(i) == slot.readType()) {
                int maxCanBeApplied = 64-otherContainer.readAmount(i);
                int beingApplied = Math.min(amountRemaining, maxCanBeApplied);

                otherContainer.set(i, slot.readType(), otherContainer.readAmount(i)+beingApplied);
                amountRemaining-=beingApplied;
            }
        }

        if (amountRemaining > 0) {
            for (int i = 0; i < otherContainer.getSize(); i++) {
                if (otherContainer.isEmptyAt(i)) {
                    otherContainer.set(i, slot.readType(), amountRemaining);
                    amountRemaining-=amountRemaining;
                    break;
                }
            }
        }

        slot.set(slot.readType(), (byte) amountRemaining);
    }

    private int collectFromContainers(int currentAmountTaken, short gatherType, boolean allowFullStacks) {
        for (Slot slot : slots) {
            if (slot.readType() == gatherType && (slot.readAmount() != 64 || allowFullStacks)) {
                if (!slot.canQuickCollect()) continue;

                int maxCanTake = 64-currentAmountTaken;
                int beingTaken = Math.min(slot.readAmount(), maxCanTake);
                slot.set(slot.readType(), (byte) (slot.readAmount()-beingTaken));

                currentAmountTaken += beingTaken;
                if (currentAmountTaken == 64) break;
            }
        }

        return currentAmountTaken;
    }

    public ItemStack doubleClickTransfer(Slot slotData, ItemStack currentActiveItem, ClickContext context) {
        // Generic collection implementation

        int amountTaken = currentActiveItem == null ? slotData.readAmount() : currentActiveItem.amount();
        short gatherType = currentActiveItem == null ? slotData.readType() : currentActiveItem.type();
        if (gatherType == 0) return null;

        if (currentActiveItem == null) {
            slotData.set((short) 0, (byte) 0);
        }

        amountTaken = collectFromContainers(amountTaken, gatherType, false);
        if (amountTaken < 64) amountTaken = collectFromContainers(amountTaken, gatherType, true);

        currentActiveItem = new ItemStack(gatherType, (byte) amountTaken);
        return currentActiveItem;
    }

    // On Click
    public ItemStack getNewActiveItemStack(Slot slotData, ItemStack activeItemStack, ClickContext clickContext) {
        if (slotData == null) return activeItemStack;
//        System.out.println(slotData);
//        System.out.println(activeItemStack);
//        System.out.println(clickContext);
        // Container sets automatically handle <= 0 amounts

        if (!slotData.isEmpty() && activeItemStack != null && slotData.mayPickup() && slotData.isSourceSlot()) {
            if (activeItemStack.amount() + slotData.readAmount() > 64) return activeItemStack;
            if (activeItemStack.type() != slotData.readType()) return activeItemStack;

            slotData.onPickup(clickContext);
            activeItemStack = new ItemStack(slotData.readType(), (byte) (activeItemStack.amount() + slotData.readAmount()));
            if (slotData.isSourceSlot()) slotData.updateSlotContexts();

            return activeItemStack;
        }

        if (!slotData.isEmpty() && activeItemStack == null && slotData.mayPickup()) {
            if (!clickContext.isLeftClick()) { // Right click
                if (!slotData.maySplitAndPickup()) return activeItemStack;
                if (slotData.isSourceSlot()) return activeItemStack;
                if (slotData.readAmount() <= 1) return activeItemStack;

                int half = slotData.readAmount() / 2;
                slotData.set(slotData.readType(), (byte) (slotData.readAmount()-half));

                return new ItemStack(slotData.readType(), (byte) half);
            }

            slotData.onPickup(clickContext);
            activeItemStack = new ItemStack(slotData.readType(), slotData.readAmount());

            if (!slotData.isSourceSlot()) {
                slotData.set((short) 0, (byte) 0);
            } else {
                slotData.updateSlotContexts();
            }

            return activeItemStack;
        }

        if (activeItemStack != null && slotData.mayPlace(activeItemStack.type())) {
            if (!clickContext.isLeftClick()) {
                if (activeItemStack.amount() <= 0 || slotData.readAmount() == 64) return activeItemStack;
                if (activeItemStack.type() != slotData.readType() && slotData.readType() != 0) return activeItemStack;

                activeItemStack = new ItemStack(activeItemStack.type(), (byte) (activeItemStack.amount()-1));
                slotData.set(activeItemStack.type(), (byte) (slotData.readAmount()+1));

                if (activeItemStack.amount() == 0) activeItemStack = null;
                return activeItemStack;
            }

            if (slotData.container.isEmptyAt(slotData.index)) {
                slotData.set(activeItemStack.type(), activeItemStack.amount());

                return null;
            } else if (slotData.readType() == activeItemStack.type() && slotData.readAmount() < 64) {
                int maxCanBeApplied = 64-slotData.container.readAmount(slotData.index);
                int beingApplied = Math.min(activeItemStack.amount(), maxCanBeApplied);
                int newInvVal = slotData.readAmount() + beingApplied;

                slotData.set(activeItemStack.type(), (byte) newInvVal);
                if (beingApplied == activeItemStack.amount()) {
                    activeItemStack = null;
                } else {
                    activeItemStack = new ItemStack(activeItemStack.type(), (byte) (activeItemStack.amount() - beingApplied));
                }

                return activeItemStack;
            } else if (slotData.readType() != activeItemStack.type() || slotData.readAmount() == 64) {
                int oldType = slotData.readType(); int oldAmount = slotData.readAmount();

                slotData.set(activeItemStack.type(), activeItemStack.amount());
                return new ItemStack((short) oldType, (byte) oldAmount);
            }
        }

        return activeItemStack;
    }
}

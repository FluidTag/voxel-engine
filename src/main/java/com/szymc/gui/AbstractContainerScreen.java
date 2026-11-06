package com.szymc.gui;

import com.szymc.voxel_engine.App;
import com.szymc.voxel_engine.ContainerData;
import com.szymc.voxel_engine.UIRenderer;

import java.util.List;

import static org.lwjgl.glfw.GLFW.*;

public abstract class AbstractContainerScreen<T extends AbstractContainerMenu> extends Screen {
    protected int topPos;
    protected int leftPos;
    protected final T menu;
    private int lastIndexClicked;
    private ContainerData lastContainerClicked;
    private long lastClick;

    public record InventoryActiveItem(ContainerData container, int index, short type, byte amount) {}
    private InventoryActiveItem activeInventoryDrag = null;

    public AbstractContainerScreen(int width, int height, T menu) {
        super(width, height);
        this.menu = menu;
        this.topPos = 0;
        this.leftPos = 0;
    }

    private void setActiveInventoryDrag(InventoryActiveItem itemData, boolean isLeftClick, boolean isOutputOnly) {
        if (itemData == null) return;
        System.out.println(isOutputOnly);
        // Container sets automatically handle <= 0 amounts

        if (!itemData.container.isEmptyAt(itemData.index) && activeInventoryDrag == null) {
            if (!isLeftClick) {
                if (itemData.amount <= 1) return;

                int half = itemData.amount/2;
                itemData.container.set(itemData.index, itemData.type, itemData.amount-half);
                activeInventoryDrag = new InventoryActiveItem(itemData.container, itemData.index, itemData.type, (byte) half);
                return;
            }

            activeInventoryDrag = itemData;
            itemData.container.set(itemData.index, 0, 0);
            return;
        }

        if (activeInventoryDrag != null && !isOutputOnly) {
            if (!isLeftClick) {
                if (activeInventoryDrag.amount <= 1 || itemData.amount == 64) return;
                if (activeInventoryDrag.type != itemData.type && itemData.type != 0) return;

                activeInventoryDrag = new InventoryActiveItem(activeInventoryDrag.container, activeInventoryDrag.index, activeInventoryDrag.type, (byte) (activeInventoryDrag.amount-1));
                itemData.container.set(itemData.index, activeInventoryDrag.type, itemData.amount+1);

                if (activeInventoryDrag.amount == 0) activeInventoryDrag = null;
                return;
            }

            if (itemData.container.isEmptyAt(itemData.index)) {
                itemData.container.set(itemData.index, activeInventoryDrag.type, activeInventoryDrag.amount);
                activeInventoryDrag = null;

                return;
            } else if (itemData.container.readType(itemData.index) == activeInventoryDrag.type && itemData.container.readAmount(itemData.index) < 64) {
                int maxCanBeApplied = 64-itemData.container.readAmount(itemData.index);
                int beingApplied = Math.min(activeInventoryDrag.amount, maxCanBeApplied);
                int newInvVal = itemData.container.readAmount(itemData.index) + beingApplied;

                itemData.container.set(itemData.index, activeInventoryDrag.type, newInvVal);
                if (beingApplied == activeInventoryDrag.amount) {
                    activeInventoryDrag = null;
                } else {
                    activeInventoryDrag = new InventoryActiveItem(activeInventoryDrag.container, activeInventoryDrag.index, activeInventoryDrag.type, (byte) (activeInventoryDrag.amount - beingApplied));
                }

                return;
            } else if (itemData.container.readType(itemData.index) != activeInventoryDrag.type || itemData.container.readAmount(itemData.index) == 64) {
                int oldType = itemData.container.readType(itemData.index); int oldAmount = itemData.container.readAmount(itemData.index);

                itemData.container.set(itemData.index, activeInventoryDrag.type, activeInventoryDrag.amount);
                activeInventoryDrag = new InventoryActiveItem(itemData.container, itemData.index, (short) oldType, (byte) oldAmount);
                return;
            }
        }

        if (activeInventoryDrag != null && isOutputOnly && activeInventoryDrag.type == itemData.type && activeInventoryDrag.amount+itemData.amount <= 64) {
            activeInventoryDrag = new InventoryActiveItem(activeInventoryDrag.container, activeInventoryDrag.index, activeInventoryDrag.type, (byte) (activeInventoryDrag.amount+itemData.amount));
        }

        // More not shown..
    }

    protected InventoryActiveItem doubleClickTransfer(InventoryActiveItem itemData, InventoryActiveItem currentActiveItem) {
        int amountTaken = currentActiveItem == null ? itemData.amount : currentActiveItem.amount;
        short gatherType = currentActiveItem == null ? itemData.type : currentActiveItem.type;
        if (gatherType == 0) return null;

        if (currentActiveItem == null) {
            itemData.container.set(itemData.index, 0, 0);
        }

        for (int i = 0; i < itemData.container.getSize(); i++) {
            if (currentActiveItem == null && i == itemData.index) continue;

            if (itemData.container.readType(i) == gatherType && itemData.container.readAmount(i) != 64) {
                int maxCanTake = 64-amountTaken;
                int beingTaken = Math.min(itemData.container.readAmount(i), maxCanTake);
                itemData.container.set(i, itemData.container.readType(i), itemData.container.readAmount(i)-beingTaken);

                amountTaken += beingTaken;
                if (amountTaken == 64) break;
            }
        }

        if (amountTaken < 64) {
            for (int i = 0; i < itemData.container.getSize(); i++) {
                if (currentActiveItem == null && i == itemData.index) continue;

                if (itemData.container.readType(i) == gatherType && itemData.container.readAmount(i) == 64) {
                    int maxCanTake = 64-amountTaken;
                    int beingTaken = Math.min(itemData.container.readAmount(i), maxCanTake);
                    itemData.container.set(i, itemData.container.readType(i), itemData.container.readAmount(i)-beingTaken);

                    amountTaken += beingTaken;
                    if (amountTaken == 64) break;
                }
            }
        }

        currentActiveItem = new InventoryActiveItem(
                currentActiveItem == null ? itemData.container : currentActiveItem.container,
                currentActiveItem == null ? itemData.index : currentActiveItem.index, gatherType, (byte) amountTaken);

        return currentActiveItem;
    }

    protected void shiftClickTransfer(InventoryActiveItem itemData) {
        if (activeInventoryDrag != null) return;

        ContainerData otherContainer = null;
        for (ContainerData cont : menu.getContainersIncluded()) {
            if (cont != itemData.container) {
                otherContainer = cont;
                break;
            }
        }

        if (otherContainer == null) return;
        int amountRemaining = itemData.amount;
        for (int i = otherContainer.getSize(); i >= 0; i--) {
            if (otherContainer.readType(i) == itemData.type) {
                int maxCanBeApplied = 64-otherContainer.readAmount(i);
                int beingApplied = Math.min(amountRemaining, maxCanBeApplied);

                otherContainer.set(i, itemData.type, otherContainer.readAmount(i)+beingApplied);
                amountRemaining-=beingApplied;
            }
        }

        if (amountRemaining > 0) {
            for (int i = otherContainer.getSize(); i >= 0; i--) {
                if (otherContainer.isEmptyAt(i)) {
                    otherContainer.set(i, itemData.type, amountRemaining);
                    amountRemaining-=amountRemaining;
                    break;
                }
            }
        }

        itemData.container.set(itemData.index, itemData.type, amountRemaining);
    }

    // Able to be overwritten for additional capabilities on an operation
    protected void onContainerOperation(InventoryActiveItem clicked, InventoryActiveItem currentlyHeld) {}

    @Override
    public void render(UIRenderer graphics, int mouseX, int mouseY) {
        this.renderBg(graphics);

        List<Slot> slots = menu.getSlots();
        for (Slot s : slots) {
            int xPos = this.leftPos + s.anchorX + s.x;
            int yPos = this.topPos + s.anchorY + s.y;
            graphics.beginUiRendering(App.WINDOW_WIDTH, App.WINDOW_HEIGHT);
            graphics.drawRect(xPos, yPos, 64, 64, 0.55f, 0.55f, 0.55f, 1f); // Bg
            graphics.drawRect(xPos + 2, yPos + 2, 64-4, 64-4, 0.35f, 0.35f, 0.35f, 1f); // Main

            // Content
            if (!s.container.isEmptyAt(s.index)) {
                graphics.drawIcon(s.container.readType(s.index), xPos, yPos, 64, 64);
                graphics.beginTextRendering(App.WINDOW_WIDTH, App.WINDOW_HEIGHT);
                graphics.renderFont(Integer.toString(s.container.readAmount(s.index)), xPos + 64-4, yPos + 64-4, UIRenderer.TextAlignment.RIGHT);
            }
        }

        if (activeInventoryDrag != null) {
            graphics.beginUiRendering(App.WINDOW_WIDTH, App.WINDOW_HEIGHT);
            graphics.drawIcon(activeInventoryDrag.type, mouseX-32, mouseY-32, 64, 64);
            graphics.beginTextRendering(App.WINDOW_WIDTH, App.WINDOW_HEIGHT);
            graphics.renderFont(Integer.toString(activeInventoryDrag.amount), mouseX-32+64-4, mouseY-32+64-4, UIRenderer.TextAlignment.RIGHT);
        }

        graphics.end();
    }

    protected abstract void renderBg(UIRenderer graphics);

    @Override
    public boolean mouseClick(int mouseX, int mouseY, int button, int mods) {
        if (super.mouseClick(mouseX, mouseY, button, mods)) {
            return true;
        }

        long elapsed = (System.nanoTime() - lastClick) / 1_000_000;
        lastClick = System.nanoTime();

        for (int i = 0; i < menu.getSlots().size(); i++) {
            // Check if hovering over
            Slot slot = menu.getSlots().get(i);
            if (mouseX >= leftPos + slot.anchorX + slot.x && mouseX <= leftPos + slot.anchorX + slot.x + slot.size
            && mouseY >= topPos + slot.anchorY + slot.y && mouseY <= topPos + slot.anchorY + slot.y + slot.size) {
                boolean isDoubleClick = (lastContainerClicked == slot.container && lastIndexClicked == slot.index && elapsed < 164);
                InventoryActiveItem itemData = new InventoryActiveItem(slot.container, slot.index, slot.container.readType(slot.index), slot.container.readAmount(slot.index));

                boolean isLeftClick = button == GLFW_MOUSE_BUTTON_1;
                boolean isShiftClick = (mods & GLFW_MOD_SHIFT) != 0;
                InventoryActiveItem oldActiveItem = activeInventoryDrag;

                if (isShiftClick && activeInventoryDrag == null) {
                    shiftClickTransfer(itemData);
                } else if (isDoubleClick && isLeftClick) {
                    activeInventoryDrag = doubleClickTransfer(itemData, activeInventoryDrag);
                } else {
                    setActiveInventoryDrag(itemData, isLeftClick, slot.isOutputOnly);
                }

                onContainerOperation(itemData, oldActiveItem);

                lastContainerClicked = slot.container;
                lastIndexClicked = slot.index;
                return true;
            }
        };

        return false;
    }
}

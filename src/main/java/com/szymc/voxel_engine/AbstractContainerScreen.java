package com.szymc.voxel_engine;

import java.util.List;
import static org.lwjgl.glfw.GLFW.*;

public abstract class AbstractContainerScreen<T extends AbstractContainerMenu> extends Screen {
    protected int topPos;
    protected int leftPos;
    protected final T menu;

    public record InventoryActiveItem(ContainerData container, int index, short type, byte amount) {}
    private InventoryActiveItem activeInventoryDrag = null;

    public AbstractContainerScreen(int width, int height, T menu) {
        super(width, height);
        this.menu = menu;
        this.topPos = 0;
        this.leftPos = 0;
    }

    public void setActiveInventoryDrag(InventoryActiveItem itemData, boolean isLeftClick, boolean isShiftClick, boolean isDoubleClick) {
        if (!itemData.container.isEmptyAt(itemData.index) && activeInventoryDrag == null) {
            activeInventoryDrag = itemData;
            itemData.container.set(itemData.index, 0, 0);
            return;
        }

        if (activeInventoryDrag != null) {
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
            } else if (itemData.container.readType(itemData.index) != activeInventoryDrag.type) {
                int oldType = itemData.container.readType(itemData.index); int oldAmount = itemData.container.readAmount(itemData.index);

                itemData.container.set(itemData.index, activeInventoryDrag.type, activeInventoryDrag.amount);
                activeInventoryDrag = new InventoryActiveItem(itemData.container, itemData.index, (short) oldType, (byte) oldAmount);
                return;
            }
        }

        // More not shown..
    }

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

        for (int i = 0; i < menu.getSlots().size(); i++) {
            // Check if hovering over
            Slot slot = menu.getSlots().get(i);
            if (mouseX >= leftPos + slot.anchorX + slot.x && mouseX <= leftPos + slot.anchorX + slot.x + slot.size
            && mouseY >= topPos + slot.anchorY + slot.y && mouseY <= topPos + slot.anchorY + slot.y + slot.size) {
                setActiveInventoryDrag(new InventoryActiveItem(slot.container, slot.index, slot.container.readType(slot.index), slot.container.readAmount(slot.index)), button == GLFW_MOUSE_BUTTON_LEFT, (mods & GLFW_MOD_SHIFT) != 0, false);
                return true;
            }
        };

        return false;
    }
}

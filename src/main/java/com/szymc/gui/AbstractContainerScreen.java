package com.szymc.gui;

import com.szymc.voxel_engine.App;
import com.szymc.voxel_engine.ContainerData;
import com.szymc.voxel_engine.PlayerCharacter;
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

    private ItemStack activeInventoryDrag;

    public AbstractContainerScreen(int width, int height, T menu, PlayerCharacter player) {
        super(width, height, player);
        this.menu = menu;
        this.topPos = 0;
        this.leftPos = 0;
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
            graphics.drawIcon(activeInventoryDrag.type(), mouseX-32, mouseY-32, 64, 64);
            graphics.beginTextRendering(App.WINDOW_WIDTH, App.WINDOW_HEIGHT);
            graphics.renderFont(Integer.toString(activeInventoryDrag.amount()), mouseX-32+64-4, mouseY-32+64-4, UIRenderer.TextAlignment.RIGHT);
        }

        graphics.end();
    }

    protected abstract void renderBg(UIRenderer graphics);

    @Override
    public void closeScreen() {
        if (activeInventoryDrag != null) {
            this.player.directDropItem(activeInventoryDrag.type(), activeInventoryDrag.amount());
            this.activeInventoryDrag = null;
        }
    }

    @Override
    public void openScreen() {}

    @Override
    public boolean keyPress(int mouseX, int mouseY, int key, int mods) {
        if (key == GLFW_KEY_Q && (mods & GLFW_MOD_CONTROL) != 0) {
            for (int i = 0; i < menu.getSlots().size(); i++) {
                Slot slot = menu.getSlots().get(i);
                if (mouseX >= leftPos + slot.anchorX + slot.x && mouseX <= leftPos + slot.anchorX + slot.x + slot.size
                        && mouseY >= topPos + slot.anchorY + slot.y && mouseY <= topPos + slot.anchorY + slot.y + slot.size) {
                    this.player.dropItem(slot.container, slot.index, slot.readAmount());
                }
            }

        }

        return false;
    }

    @Override
    public boolean mouseClick(int mouseX, int mouseY, int button, int mods) {
        if (super.mouseClick(mouseX, mouseY, button, mods)) {
            return true;
        }

        // Check if it is an out of container right click drop
        if ((mouseX < this.leftPos || mouseX > this.leftPos + this.width) || (mouseY < this.topPos || mouseY > this.topPos + this.height)) {
            if (activeInventoryDrag != null && button == GLFW_MOUSE_BUTTON_RIGHT) {
                this.player.directDropItem(activeInventoryDrag.type(), 1);
                this.activeInventoryDrag = activeInventoryDrag.amount() > 1 ? new ItemStack(activeInventoryDrag.type(), (byte) (activeInventoryDrag.amount()-1)) : null;

            } else if (activeInventoryDrag != null && button == GLFW_MOUSE_BUTTON_LEFT) {
                this.player.directDropItem(activeInventoryDrag.type(), activeInventoryDrag.amount());
                this.activeInventoryDrag = null;
            }
        }

        long elapsed = (System.nanoTime() - lastClick) / 1_000_000;
        lastClick = System.nanoTime();

        for (int i = 0; i < menu.getSlots().size(); i++) {
            // Check if hovering over
            Slot slot = menu.getSlots().get(i);
            if (mouseX >= leftPos + slot.anchorX + slot.x && mouseX <= leftPos + slot.anchorX + slot.x + slot.size
            && mouseY >= topPos + slot.anchorY + slot.y && mouseY <= topPos + slot.anchorY + slot.y + slot.size) {
                boolean isDoubleClick = (lastContainerClicked == slot.container && lastIndexClicked == slot.index && elapsed < 164);
                boolean isLeftClick = button == GLFW_MOUSE_BUTTON_1;
                boolean isShiftClick = (mods & GLFW_MOD_SHIFT) != 0;
                ClickContext clickContext = new ClickContext(isLeftClick, isShiftClick);

                if (isShiftClick) {
                    slot.onPickup(clickContext);
                    menu.shiftClickTransfer(slot);
                } else if (isDoubleClick && isLeftClick && slot.canQuickCollect()) {
                    activeInventoryDrag = menu.doubleClickTransfer(slot, activeInventoryDrag, clickContext);
                } else {
                    //System.out.println("Before: " + activeInventoryDrag);
                    activeInventoryDrag = menu.getNewActiveItemStack(slot, activeInventoryDrag, clickContext);
                    //System.out.println("After: " + activeInventoryDrag);
                }

                lastContainerClicked = slot.container;
                lastIndexClicked = slot.index;
                return true;
            }
        };

        return false;
    }
}

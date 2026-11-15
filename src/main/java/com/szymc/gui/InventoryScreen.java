package com.szymc.gui;

import com.szymc.voxel_engine.*;

public class InventoryScreen extends AbstractContainerScreen<InventoryMenu> {
    private final static int padding = 16;

    private InventoryScreen(InventoryMenu inventoryMenu, PlayerCharacter player) {
        super(9*64, 4*64 + 4*64, inventoryMenu, player);

        this.topPos = (App.WINDOW_HEIGHT - this.height)/2;
        this.leftPos = (App.WINDOW_WIDTH - this.width)/2;
    }

    public static InventoryScreen create(PlayerCharacter player) {
        InventoryMenu menu = new InventoryMenu(player.getInventory());

        return new InventoryScreen(menu, player);
    }

    @Override
    protected void renderBg(UIRenderer graphics) {
        graphics.beginUiRendering(App.WINDOW_WIDTH, App.WINDOW_HEIGHT);
        graphics.drawRect(this.leftPos-(padding/2), this.topPos-(padding/2), this.width+padding, this.height+padding, 0.6f, 0.6f, 0.6f, 1f);
        graphics.end();
    }

    @Override
    public void init() {}

    @Override
    public void removal() {}
}

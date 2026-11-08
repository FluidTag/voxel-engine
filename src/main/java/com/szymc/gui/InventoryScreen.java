package com.szymc.gui;

import com.szymc.voxel_engine.App;
import com.szymc.voxel_engine.ContainerData;
import com.szymc.voxel_engine.Texture;
import com.szymc.voxel_engine.UIRenderer;

public class InventoryScreen extends AbstractContainerScreen<InventoryMenu> {
    private final static int padding = 16;
    private final InventoryMenu menu;
    private final ContainerData playerInv;

    private InventoryScreen(ContainerData playerInv, InventoryMenu menu) {
        super(9*64, 4*64 + 4*64, menu);

        this.playerInv = playerInv;
        this.menu = menu;
        this.topPos = (App.WINDOW_HEIGHT - this.height)/2;
        this.leftPos = (App.WINDOW_WIDTH - this.width)/2;
    }

    public static InventoryScreen create(ContainerData playerInv) {
        InventoryMenu menu = new InventoryMenu(playerInv);
        return new InventoryScreen(playerInv, menu);
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

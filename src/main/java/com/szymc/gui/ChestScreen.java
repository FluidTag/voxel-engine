package com.szymc.gui;

import com.szymc.voxel_engine.App;
import com.szymc.voxel_engine.ContainerData;
import com.szymc.voxel_engine.UIRenderer;

public class ChestScreen extends AbstractContainerScreen<ChestMenu> {
    private ContainerData playerInv;
    private ContainerData chestInv;
    private final static int padding = 16;

    private ChestScreen(ContainerData playerInv, ContainerData chestInv, ChestMenu menu) {
        super(9*64, 9*64, menu);
        this.topPos = (App.WINDOW_HEIGHT - this.height)/2;
        this.leftPos = (App.WINDOW_WIDTH - this.width)/2;
        this.playerInv = playerInv;
        this.chestInv = chestInv;
    }

    public static ChestScreen create(ContainerData invData, ContainerData chestData) {
        ChestMenu menu = new ChestMenu(invData, chestData);
        return new ChestScreen(invData, chestData, menu);
    }

    @Override
    protected void renderBg(UIRenderer graphics) {
        graphics.beginUiRendering(App.WINDOW_WIDTH, App.WINDOW_HEIGHT);
        graphics.drawRect(this.leftPos-(padding/2), this.topPos-(padding/2), this.width+padding, this.height+padding, 0.6f, 0.6f, 0.6f, 1f);
        graphics.end();
    }

    @Override
    public void init() {

    }

    @Override
    public void removal() {

    }
}

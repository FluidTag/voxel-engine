package com.szymc.gui;

import com.szymc.voxel_engine.App;
import com.szymc.voxel_engine.ContainerData;
import com.szymc.voxel_engine.UIRenderer;

public class TableCraftScreen extends AbstractContainerScreen<TableCraftMenu> {
    private final static int padding = 16;
    private final TableCraftMenu menu;

    private TableCraftScreen(TableCraftMenu menu) {
        super(9*64, 4*64 + 4*64, menu);
        this.menu = menu;
        this.topPos = (App.WINDOW_HEIGHT - this.height)/2;
        this.leftPos = (App.WINDOW_WIDTH - this.width)/2;
    }

    public static TableCraftScreen create(ContainerData playerInv) {
        TableCraftMenu menu = new TableCraftMenu(playerInv);
        return new TableCraftScreen(menu);
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

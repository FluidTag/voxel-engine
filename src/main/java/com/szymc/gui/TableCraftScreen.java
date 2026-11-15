package com.szymc.gui;

import com.szymc.voxel_engine.App;
import com.szymc.voxel_engine.ContainerData;
import com.szymc.voxel_engine.PlayerCharacter;
import com.szymc.voxel_engine.UIRenderer;

public class TableCraftScreen extends AbstractContainerScreen<TableCraftMenu> {
    private final static int padding = 16;

    private TableCraftScreen(TableCraftMenu menu, PlayerCharacter character) {
        super(9*64, 4*64 + 4*64, menu, character);
        this.topPos = (App.WINDOW_HEIGHT - this.height)/2;
        this.leftPos = (App.WINDOW_WIDTH - this.width)/2;
    }

    public static TableCraftScreen create(PlayerCharacter player) {
        TableCraftMenu menu = new TableCraftMenu(player.getInventory());

        return new TableCraftScreen(menu, player);
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

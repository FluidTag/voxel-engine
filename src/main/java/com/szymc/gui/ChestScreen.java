package com.szymc.gui;

import com.szymc.voxel_engine.App;
import com.szymc.voxel_engine.ContainerData;
import com.szymc.voxel_engine.PlayerCharacter;
import com.szymc.voxel_engine.UIRenderer;

public class ChestScreen extends AbstractContainerScreen<ChestMenu> {
    private final static int padding = 16;

    private ChestScreen(ChestMenu chestMenu, PlayerCharacter character) {
        super(9*64, 9*64, chestMenu, character);
        this.topPos = (App.WINDOW_HEIGHT - this.height)/2;
        this.leftPos = (App.WINDOW_WIDTH - this.width)/2;
    }

    public static ChestScreen create(PlayerCharacter player, ContainerData chestInv) {
        ContainerData playerInv = player.getInventory();
        ChestMenu chestMenu = new ChestMenu(playerInv, chestInv);

        return new ChestScreen(chestMenu, player);
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

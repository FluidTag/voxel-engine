package com.szymc.voxel_engine;

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
    }

    @Override
    public void shiftClickTransfer(InventoryActiveItem itemData) {
        int startSearch = itemData.index() < 9 ? 9 : 0;
        int endSearch = itemData.index() < 9 ? 35 : 8;

        int amountRemaining = itemData.amount();
        for (int i = startSearch; i <= endSearch; i++) {
            if (itemData.container().readType(i) == itemData.type()) {
                int maxCanBeApplied = 64-itemData.container().readAmount(i);
                int beingApplied = Math.min(amountRemaining, maxCanBeApplied);

                itemData.container().set(i, itemData.type(), itemData.container().readAmount(i)+beingApplied);
                amountRemaining-=beingApplied;
            }
        }

        if (amountRemaining > 0) {
            for (int i = startSearch; i <= endSearch; i++) {
                if (itemData.container().isEmptyAt(i)) {
                    itemData.container().set(i, itemData.type(), amountRemaining);
                    amountRemaining-=amountRemaining;
                    break;
                }
            }
        }

        itemData.container().set(itemData.index(), itemData.type(), amountRemaining);
    }

    @Override
    public void init() {}

    @Override
    public void removal() {}
}

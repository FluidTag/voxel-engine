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
        int endSearch = itemData.index() < 9 ? 36 : 8;

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

    private final static int[][] passes = {
            {9, 35, 0},
            {0, 8,  0},
            {9, 35, 1},
            {0, 8,  1}
    };

    @Override
    public InventoryActiveItem doubleClickTransfer(InventoryActiveItem itemData, InventoryActiveItem currentActiveItem) {
        int amountTaken = currentActiveItem == null ? itemData.amount() : currentActiveItem.amount();
        short gatherType = currentActiveItem == null ? itemData.type() : currentActiveItem.type();
        if (gatherType == 0) return null;

        if (currentActiveItem == null) {
            itemData.container().set(itemData.index(), 0, 0);
        }

        for (int p = 0; p < passes.length; p++) {
            int[] dat = passes[p];
            int startInd = dat[0];
            int endInd = dat[1];
            boolean fullStacksOnly = dat[2] == 1;
            boolean endLoop = false;

            for (int i = startInd; i <= endInd; i++) {
                if (itemData.container().readType(i) == gatherType) {
                    if (!fullStacksOnly && itemData.container().readAmount(i) == 64) continue;

                    int maxCanTake = 64-amountTaken;
                    int beingTaken = Math.min(itemData.container().readAmount(i), maxCanTake);
                    itemData.container().set(i, itemData.container().readType(i), itemData.container().readAmount(i)-beingTaken);

                    amountTaken += beingTaken;
                    if (amountTaken == 64) {endLoop = true; break;}
                }
            }

            if (endLoop) break;
        }

        currentActiveItem = new InventoryActiveItem(
                currentActiveItem == null ? itemData.container() : currentActiveItem.container(),
                currentActiveItem == null ? itemData.index() : currentActiveItem.index(), gatherType, (byte) amountTaken);

        return currentActiveItem;
    }

    @Override
    public void init() {}

    @Override
    public void removal() {}
}

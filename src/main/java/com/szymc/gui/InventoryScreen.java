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
    }

    public String extractCraftInput(short[] iTypes, byte[] iAmounts, int indOffset, int boundXind, int boundYind) {
        StringBuilder result = new StringBuilder();
        int minX = 999; int maxX = -999;
        int minY = 999; int maxY = -999;
        boolean isEmpty = true;

        //System.out.println(Arrays.toString(iTypes));
        //System.out.println("Range/Last index: " + boundXind + ", " + boundYind);
        for (int y = 0; y <= boundYind; y++) {
            for (int x = 0; x <= boundXind; x++) {
                byte ind = (byte) (indOffset + y*(boundXind+1)+x);
                short itemType = iTypes[ind];
                //System.out.printf("ind: %d (%d, %d) : item %d%n", ind, x, y, itemType);
                if (itemType != 0) {
                    isEmpty = false;
                    minX = Math.min(x, minX);
                    maxX = Math.max(x, maxX);
                    minY = Math.min(y, minY);
                    maxY = Math.max(y, maxY);
                }
            }
        }

        if (isEmpty) return null;

        for (int y = minY; y <= maxY; y++) {
            for (int x = minX; x <= maxX; x++) {
                byte ind = (byte) (indOffset + y*(boundXind+1)+x);
                short itemType = iTypes[ind];

                result.append(itemType);
                result.append('.');
            }
            result.append('/');
        }

        //System.out.printf("xMin: %d, xMax: %d, yMin: %d, yMax: %d\n", minX, maxX, minY, maxY);
        //System.out.println(result);

        return result.toString();
    }

    @Override
    protected void onContainerOperation(InventoryActiveItem itemData, InventoryActiveItem currentActiveItem) {
        String crafted = extractCraftInput(playerInv.getRawTypes(), playerInv.getRawAmounts(), 36, 1, 1);
        short matchesRecipe = Texture.craftingRecipes.getShort(crafted);
        byte block = (byte) (matchesRecipe & 0xFF);
        byte amount = (byte) ((matchesRecipe >>> 8) & 0xFF);

        playerInv.set(40, block, amount);
        if (itemData.index() == 40 && amount > 0 && (currentActiveItem == null || currentActiveItem.type() == itemData.type())) {
            for (int i = 36; i < 40; i++) {
                if (!playerInv.isEmptyAt(i)) playerInv.decrementIndex(i);
            }

            onContainerOperation(new InventoryActiveItem(itemData.container(), 0, (short) 0, (byte) 0), null); // Dummy call to recheck craft state
        }
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
            {9, 39, 0},
            {0, 8,  0},
            {9, 39, 1},
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

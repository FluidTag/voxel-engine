package com.szymc.gui;

import com.szymc.voxel_engine.ContainerData;
import com.szymc.voxel_engine.Texture;

public class CraftingContext {
    private String extractCraftInput(short[] iTypes, byte[] iAmounts, int indOffset, int boundXind, int boundYind) {
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

    private ContainerData craftingInventory;
    private final int width;
    private final int height;

    public CraftingContext(int width, int height) {
        craftingInventory = new ContainerData(1 + width*height);
        this.width = width;
        this.height = height;

        craftingInventory.addListener(change -> update(change.index()));
    }

    public ContainerData getCraftingInventory() {
        return this.craftingInventory;
    }

    private void update(int index) {
        //if (index == 0) return;

        String recipe = extractCraftInput(craftingInventory.getRawTypes(), craftingInventory.getRawAmounts(), 1, width-1, height-1);
        short result = Texture.craftingRecipes.getShort(recipe);
        short type = (short) (result & 0xFF);
        byte amount = (byte) ((result >>> 8) & 0xFF);

        craftingInventory.silentSet(0, type, amount);
    }

    public boolean canCraft() {
        update(-1);
        return craftingInventory.readType(0) != 0;
    }

    public byte craft(boolean makeMaximum) {
        if (!canCraft()) return 0;

        int beingMade = 1;
        if (makeMaximum) {
            beingMade = Integer.MAX_VALUE;
            for (int i = 1; i < craftingInventory.getSize(); i++) {
                if (!craftingInventory.isEmptyAt(i)) beingMade = Math.min(craftingInventory.readAmount(i), beingMade);
            }
        }

        for (int i = 1; i < craftingInventory.getSize(); i++) {
            if (!craftingInventory.isEmptyAt(i)) craftingInventory.silentSet(i, craftingInventory.readType(i), craftingInventory.readAmount(i)-beingMade);
        }

        byte initialRecipeAmount = craftingInventory.readAmount(0);

        update(-1);
        return (byte) (initialRecipeAmount * beingMade);
    }
}

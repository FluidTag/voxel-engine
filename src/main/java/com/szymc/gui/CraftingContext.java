package com.szymc.gui;

import com.szymc.voxel_engine.ContainerData;

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
    public CraftingContext(int width, int height) {
        craftingInventory = new ContainerData(1 + width*height);

        craftingInventory.addListener(change -> update(change.index()));
    }

    private void update(int index) {

    }
}

package com.szymc.voxel_engine;

import java.util.ArrayList;
import java.util.List;

public abstract class Screen {
    int width;
    int height;

    List<UIRenderable> renderables = new ArrayList<>();
    List<GuiEventListener> children = new ArrayList<>();

    public Screen(int width, int height) {
        this.width = width;
        this.height = height;
    }

    public abstract void init(); // initial
    public abstract void removal(); // cleanup

    public boolean mouseClick(int mouseX, int mouseY, int button, int mods) {
        for (int i = children.size()-1; i >= 0; i--) {
            if (children.get(i).mouseButtonClick(mouseX, mouseY, button, mods)) {
                return true;
            }
        }

        return false;
    }

    public void render(UIRenderer graphics, int mouseX, int mouseY) {}
}
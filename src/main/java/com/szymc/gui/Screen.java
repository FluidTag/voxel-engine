package com.szymc.gui;

import com.szymc.voxel_engine.PlayerCharacter;
import com.szymc.voxel_engine.UIRenderer;

import java.util.ArrayList;
import java.util.List;

public abstract class Screen {
    int width;
    int height;

    List<UIRenderable> renderables = new ArrayList<>();
    List<GuiEventListener> children = new ArrayList<>();

    public PlayerCharacter player;

    public Screen(int width, int height, PlayerCharacter player) {
        this.width = width;
        this.height = height;
        this.player = player;
    }

    public abstract void closeScreen();
    public abstract void openScreen();

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

    public boolean keyPress(int mouseX, int mouseY, int key, int mods) {
        for (int i = children.size()-1; i >= 0; i--) {
            if (children.get(i).keyPress(mouseX, mouseY, key, mods)) {
                return true;
            }
        }

        return false;
    }

    public void render(UIRenderer graphics, int mouseX, int mouseY) {}
}
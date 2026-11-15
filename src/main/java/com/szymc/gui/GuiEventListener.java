package com.szymc.gui;

public interface GuiEventListener {
    boolean mouseButtonClick(int mouseX, int mouseY, int button, int mods);
    boolean keyPress(int mouseX, int mouseY, int key, int mods);
}

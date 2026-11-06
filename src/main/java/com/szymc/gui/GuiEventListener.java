package com.szymc.gui;

public interface GuiEventListener {
    boolean mouseButtonClick(int screenX, int screenY, int button, int mods);
    boolean mouseHover(int screenX, int screenY);
}

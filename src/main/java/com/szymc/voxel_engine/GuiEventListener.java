package com.szymc.voxel_engine;

public interface GuiEventListener {
    boolean mouseButtonClick(int screenX, int screenY, int button, int mods);
    boolean mouseHover(int screenX, int screenY);
}

package com.szymc.voxel_engine;

import org.joml.Vector3f;

public abstract class Entity {
    public static int entitiesCreated = 0;

    public Vector3f position = new Vector3f();
    public Vector3f velocity = new Vector3f();
    public int entityId;
    public boolean onGround = false;
}

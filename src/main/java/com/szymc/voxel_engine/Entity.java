package com.szymc.voxel_engine;

import org.joml.Vector3f;

import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

public abstract class Entity {
    public static int entitiesCreated = 0;

    public int createdAtTick;
    public Vector3f position = new Vector3f();
    public Vector3f renderPosition = new Vector3f();
    public Vector3f previousPosition = new Vector3f();
    public Vector3f velocity = new Vector3f();
    public UUID entityId;
    public boolean onGround = false;
    public ChunkColumn currentChunk;

    public Entity(int currentTick) {
        //System.out.println("Entity made at tick " + currentTick);
        this.createdAtTick = currentTick;
        this.entityId = generateUUID();
    }

    public static UUID generateUUID() {
        long time = System.currentTimeMillis();
        ThreadLocalRandom random = ThreadLocalRandom.current();
        long randA = random.nextLong() & 0xFFFL;
        long randB = random.nextLong();

        long msb = (time << 16) | (0x7000L) | randA;
        long lsb = (randB & 0x3FFFFFFFFFFFFFFFL) | 0x8000000000000000L;

        return new UUID(msb, lsb);
    }
}

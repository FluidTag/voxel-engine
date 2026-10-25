package com.szymc.voxel_engine;

import it.unimi.dsi.fastutil.floats.FloatArrayList;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import org.joml.Vector3f;

import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

import static org.lwjgl.opengl.GL11.GL_FLOAT;
import static org.lwjgl.opengl.GL11.GL_INT;
import static org.lwjgl.opengl.GL15.*;
import static org.lwjgl.opengl.GL15.GL_ELEMENT_ARRAY_BUFFER;
import static org.lwjgl.opengl.GL20.glEnableVertexAttribArray;
import static org.lwjgl.opengl.GL20.glVertexAttribPointer;
import static org.lwjgl.opengl.GL30.*;
import static org.lwjgl.opengl.GL30.glBindVertexArray;

class MeshDrawCommand {
    public int indexCount;
    public long byteOffset;
    public int baseVertex;
}

public abstract class Entity {
    public int createdAtTick;
    public Vector3f position = new Vector3f();
    public Vector3f renderPosition = new Vector3f();
    public Vector3f previousPosition = new Vector3f();
    public Vector3f velocity = new Vector3f();
    public UUID entityId;
    public boolean onGround = false;
    public byte lastLightLevel = 0;
    public ChunkColumn currentChunk;

    public MeshDrawCommand itemMesh;
    private static int vao, vbo, ebo;
    public static int getVao() {return vao;}

    private static int currentVertexOffset = 0; // Total vertices uploaded so far
    private static int currentIndexOffset = 0;  // Total indices uploaded so far
    private final static int VERTEX_SIZE = 3*Float.BYTES + Integer.BYTES;
    protected static MeshDrawCommand[] meshCache = new MeshDrawCommand[256 + 256*16]; // First 256 is items, next 256 is block-entities each with [Default, 3*2 axis rotations, 4 horizontal rotations] (10 reserved now)

    public static void initializeEABCache() {
        vao = glGenVertexArrays();
        glBindVertexArray(vao);

        vbo = glGenBuffers();
        glBindBuffer(GL_ARRAY_BUFFER, vbo);
        glBufferData(GL_ARRAY_BUFFER, 2*1024*1024, GL_STATIC_DRAW);

        ebo = glGenBuffers();
        glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, ebo);
        glBufferData(GL_ELEMENT_ARRAY_BUFFER, 1024*1024, GL_STATIC_DRAW);

        int stride = (3*Float.BYTES) + (1*Integer.BYTES);
        glVertexAttribPointer(0, 3, GL_FLOAT, false, stride, 0L);
        glVertexAttribIPointer(1, 1, GL_INT, stride, 3*Float.BYTES);
        glEnableVertexAttribArray(0);
        glEnableVertexAttribArray(1);

        glBindVertexArray(0);
    }
    protected static MeshDrawCommand generateEAODrawCommand(float[] vertices, int[] indices, int numVertices, int numIndices) {
        glBindBuffer(GL_ARRAY_BUFFER, vbo);
        long offset = (long) currentVertexOffset *VERTEX_SIZE;
        glBufferSubData(GL_ARRAY_BUFFER, offset, vertices);

        glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, ebo);
        long eboByteOffset = (long) currentIndexOffset * Integer.BYTES;
        glBufferSubData(GL_ELEMENT_ARRAY_BUFFER, eboByteOffset, indices);

        MeshDrawCommand cmd = new MeshDrawCommand();
        cmd.indexCount = numIndices;
        cmd.byteOffset = eboByteOffset;
        cmd.baseVertex = currentVertexOffset;

        currentVertexOffset += numVertices;
        currentIndexOffset += numIndices;

        return cmd;
    }
    protected static void cacheDrawCommand(int cacheIndex, MeshDrawCommand command) {
        meshCache[cacheIndex] = command;
    }

    // General/Universal Mesh Making Methods
    protected static void addQuad(
            FloatArrayList vBuffer,
            IntArrayList iBuffer,
            float x1, float y1, float z1,
            float x2, float y2, float z2,
            float x3, float y3, float z3,
            float x4, float y4, float z4,
            int blockTypeAndState, // U width, V height
            boolean backFace, int axis, byte packedAO
    ) {

        int addedVerts = vBuffer.size()/4;
        int texId;

        short coreBlock = (short) (blockTypeAndState & 0xFFFF);
        short modifiers = (short) ((blockTypeAndState >>> 16) & 0xFFFF);
        int rotationType = (modifiers & 0x3);
        int coreRotationParameters = (modifiers >>> 2) & 0x3;
        int axisTypeBackfaceData = (modifiers >>> 4) & 0x1; // axis backface extra data

        BLOCK_FACE faceToTexLookup;
        BLOCK_FACE defaultFace;
        defaultFace = switch (axis) {
            case 0 -> backFace ? BLOCK_FACE.WEST : BLOCK_FACE.EAST; // X
            case 1 -> backFace ? BLOCK_FACE.TOP : BLOCK_FACE.BOTTOM; // Y
            case 2 -> backFace ? BLOCK_FACE.SOUTH : BLOCK_FACE.NORTH; // Z
            default -> throw new IllegalArgumentException("Invalid axis value: " + axis);
        };

        if (rotationType == 0 || (rotationType == 1 && coreRotationParameters == 1) || (rotationType == 2 && axis == 1)) { // No rotation or axis rotation, core parameter y-axis
            faceToTexLookup = defaultFace;
            if (axisTypeBackfaceData != 0) faceToTexLookup = faceToTexLookup.getOpposite();
        } else if (rotationType == 1) { // Remap standard texture mapping for rotation
            faceToTexLookup = switch (coreRotationParameters) {
                case 0 -> switch (axis) {  // Rotated on X axis
                    case 0 -> backFace ? BLOCK_FACE.TOP : BLOCK_FACE.BOTTOM; // X
                    case 1 -> backFace ? BLOCK_FACE.SOUTH : BLOCK_FACE.NORTH; // Y
                    case 2 -> backFace ? BLOCK_FACE.WEST : BLOCK_FACE.EAST; // Z
                    default -> throw new IllegalArgumentException("Invalid axis value: " + axis);
                };

                case 2 -> switch (axis) { // Rotated on Z axis
                    case 0 -> backFace ? BLOCK_FACE.SOUTH : BLOCK_FACE.NORTH; // X
                    case 1 -> backFace ? BLOCK_FACE.WEST : BLOCK_FACE.EAST; // Y
                    case 2 -> backFace ? BLOCK_FACE.TOP : BLOCK_FACE.BOTTOM; // Z
                    default -> throw new IllegalArgumentException("Invalid axis value: " + axis);
                };

                default -> throw new IllegalArgumentException("Invalid rotational axis: " + coreRotationParameters);
            };
        } else if (rotationType == 2) {
            faceToTexLookup = (coreRotationParameters == 0 || coreRotationParameters == 2)
                    ? (coreRotationParameters == 0 ? defaultFace : defaultFace.getOpposite())
                    : (coreRotationParameters == 1 ? defaultFace.getRotated90HorizontalFace() : defaultFace.getOpposite().getRotated90HorizontalFace());
        } else throw new IllegalArgumentException("Rotation of type " + rotationType + " not supported");

        texId = Texture.getTextureIndex(coreBlock, faceToTexLookup);

        int ao1 = packedAO & 0x3;
        int ao2 = (packedAO >> 2) & 0x3;
        int ao3 = (packedAO >> 4) & 0x3;
        int ao4 = (packedAO >> 6) & 0x3;

        if (axis == 2) {
            int vert1b = (texId & 0xFF) | ((16 & 0x3F) << 8) | ((16 & 0x3F) << 14) | ((ao1 & 0x3)) << 20;
            int vert2b = (texId & 0xFF) | ((0 & 0x3F) << 8) | ((16 & 0x3F) << 14) | ((ao2 & 0x3) << 20);
            int vert3b = (texId & 0xFF) | ((16 & 0x3F) << 8) | ((0 & 0x3F) << 14) | ((ao3 & 0x3) << 20);
            int vert4b = (texId & 0xFF) | ((0 & 0x3F) << 8) | ((0 & 0x3F) << 14) | ((ao4 & 0x3) << 20);

            vBuffer.add(x1); vBuffer.add(y1); vBuffer.add(z1);
            vBuffer.add(Float.intBitsToFloat(vert1b));

            vBuffer.add(x2); vBuffer.add(y2); vBuffer.add(z2);
            vBuffer.add(Float.intBitsToFloat(vert2b));

            vBuffer.add(x3); vBuffer.add(y3); vBuffer.add(z3);
            vBuffer.add(Float.intBitsToFloat(vert3b));

            vBuffer.add(x4); vBuffer.add(y4); vBuffer.add(z4);
            vBuffer.add(Float.intBitsToFloat(vert4b));
        } else {
            int vert1b = (texId & 0xFF) | ((0 & 0x3F) << 8) | ((16 & 0x3F) << 14) | ((ao1 & 0x3) << 20);
            int vert2b = (texId & 0xFF) | ((0 & 0x3F) << 8) | ((0 & 0x3F) << 14) | ((ao2 & 0x3) << 20);
            int vert3b = (texId & 0xFF) | ((16 & 0x3F) << 8) | ((16 & 0x3F) << 14) | ((ao3 & 0x3) << 20);
            int vert4b = (texId & 0xFF) | ((16 & 0x3F) << 8) | ((0 & 0x3F) << 14) | ((ao4 & 0x3) << 20);

            vBuffer.add(x1); vBuffer.add(y1); vBuffer.add(z1);
            vBuffer.add(Float.intBitsToFloat(vert1b));

            vBuffer.add(x2); vBuffer.add(y2); vBuffer.add(z2);
            vBuffer.add(Float.intBitsToFloat(vert2b));

            vBuffer.add(x3); vBuffer.add(y3); vBuffer.add(z3);
            vBuffer.add(Float.intBitsToFloat(vert3b));

            vBuffer.add(x4); vBuffer.add(y4); vBuffer.add(z4);
            vBuffer.add(Float.intBitsToFloat(vert4b));
        }

        if (backFace) {
            iBuffer.add(addedVerts + 0); iBuffer.add(addedVerts + 2); iBuffer.add(addedVerts + 1);
            iBuffer.add(addedVerts + 2); iBuffer.add(addedVerts + 3);iBuffer.add(addedVerts + 1);
        } else {
            iBuffer.add(addedVerts + 0); iBuffer.add(addedVerts + 1); iBuffer.add(addedVerts + 2);
            iBuffer.add(addedVerts + 2); iBuffer.add(addedVerts + 1); iBuffer.add(addedVerts + 3);
        }
    }

    protected static MeshDrawCommand buildBlockMesh(int fullBlockType, float size) {
        FloatArrayList vBuffer = new FloatArrayList(4*4);
        IntArrayList iBuffer = new IntArrayList(4*4*3);

        // 0xFF sets AO level to 3 on all 4 corners (vAoFactor = 1.0)
        byte fullAO = (byte) 0xFF;

        // size. TOP (Y = size, Axis size, backFace = true)
        addQuad(vBuffer, iBuffer, 0,size,0, size,size,0, 0,size,size, size,size,size, fullBlockType, true, 1, fullAO);

        // 2. BOTTOM (Y = 0, Axis size, backFace = false)
        addQuad(vBuffer, iBuffer, 0,0,0, size,0,0, 0,0,size, size,0,size, fullBlockType, false, 1, fullAO);

        // 3. WEST (X = 0, Axis 0, backFace = true)
        addQuad(vBuffer, iBuffer, 0,0,0, 0,size,0, 0,0,size, 0,size,size, fullBlockType, true, 0, fullAO);

        // 4. EAST (X = size, Axis 0, backFace = false)
        addQuad(vBuffer, iBuffer, size,0,0, size,size,0, size,0,size, size,size,size, fullBlockType, false, 0, fullAO);

        // 5. NORTH (Z = 0, Axis 2, backFace = true)
        addQuad(vBuffer, iBuffer, 0,0,0, size,0,0, 0,size,0, size,size,0, fullBlockType, true, 2, fullAO);

        // 6. SOUTH (Z = size, Axis 2, backFace = false)
        addQuad(vBuffer, iBuffer, 0,0,size, size,0,size, 0,size,size, size,size,size, fullBlockType, false, 2, fullAO);

        return generateEAODrawCommand(vBuffer.toFloatArray(), iBuffer.toIntArray(), vBuffer.size()/4, iBuffer.size());
    }

    public Entity(int currentTick) {
        //System.out.println("Entity made at tick " + currentTick);
        this.createdAtTick = currentTick;
        this.entityId = generateUUID();
    }

    @Override
    public String toString() {
        return String.format("Entity Type: %s", this.getClass());
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

package com.szymc.voxel_engine;
import com.szymc.localShaders.FontShader;
import com.szymc.localShaders.UIShader;
import com.szymc.localShaders.WorldShader;
import it.unimi.dsi.fastutil.floats.FloatArrayList;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import org.joml.Matrix4f;
import org.lwjgl.BufferUtils;
import org.lwjgl.stb.*;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL13.*;
import static org.lwjgl.opengl.GL15.*;
import static org.lwjgl.opengl.GL20.*;
import static org.lwjgl.opengl.GL30.*;
import static org.lwjgl.system.MemoryStack.stackPush;

public class UIRenderer {
    private final int uiVao;
    private final int mainProgramId;
    private final int locProjection, locTransform, locColorTint, locUseTexture, locRect, locUvTransform, locTexture;
    private int atlasId;
    private final FloatBuffer matrixBuffer = BufferUtils.createFloatBuffer(16);
    private Texture primaryBlockTextures;
    private final static int atlasSize = 1024; // Use power of 2

    private static void addQuad(
            IntArrayList vBuffer,
            IntArrayList iBuffer,
            int x1, int y1, int z1,
            int x2, int y2, int z2,
            int x3, int y3, int z3,
            int x4, int y4, int z4,
            int width, int height, byte blockType, // U width, V height
            boolean backFace, int axis, byte packedAO, boolean flipQuad, boolean downscale
    ) {

        int addedVerts = vBuffer.size()/2;
        int texId = 0;

        if (axis == 1) texId = Texture.getTextureIndex(blockType, backFace ? BLOCK_FACE.TOP : BLOCK_FACE.BOTTOM); // Y
        if (axis == 0) texId = Texture.getTextureIndex(blockType, backFace ? BLOCK_FACE.WEST : BLOCK_FACE.EAST); // X
        if (axis == 2) texId = Texture.getTextureIndex(blockType, backFace ? BLOCK_FACE.SOUTH : BLOCK_FACE.NORTH); // Z
        //System.out.println(blockType + ": " + texId + ": On Axis " + axis + ", Backface? " + backFace);
        int ao1 = packedAO & 0x3;
        int ao2 = (packedAO >> 2) & 0x3;
        int ao3 = (packedAO >> 4) & 0x3;
        int ao4 = (packedAO >> 6) & 0x3;
        byte scaleFlag = (byte) (downscale ? 1 : 0);

        if (axis == 2) {
            int vert1a = (x1 & 0x1FF) | ((y1 & 0x1FF) << 9) | ((z1 & 0x1FF) << 18);
            int vert1b = (texId & 0xFF) | ((height & 0x3F) << 8) | ((width & 0x3F) << 14) | ((ao1 & 0x3)) << 20 | (scaleFlag << 22);

            int vert2a = (x2 & 0x1FF) | ((y2 & 0x1FF) << 9) | ((z2 & 0x1FF) << 18); // Position
            int vert2b = (texId & 0xFF) | ((0 & 0x3F) << 8) | ((width & 0x3F) << 14) | ((ao2 & 0x3) << 20) | (scaleFlag << 22);

            int vert3a = (x3 & 0x1FF) | ((y3 & 0x1FF) << 9) | ((z3 & 0x1FF) << 18); // Position
            int vert3b = (texId & 0xFF) | ((height & 0x3F) << 8) | ((0 & 0x3F) << 14) | ((ao3 & 0x3) << 20) | (scaleFlag << 22);

            int vert4a = (x4 & 0x1FF) | ((y4 & 0x1FF) << 9) | ((z4 & 0x1FF) << 18); // Position
            int vert4b = (texId & 0xFF) | ((0 & 0x3F) << 8) | ((0 & 0x3F) << 14) | ((ao4 & 0x3) << 20) | (scaleFlag << 22);

            vBuffer.add(vert1a);
            vBuffer.add(vert1b);
            vBuffer.add(vert2a);
            vBuffer.add(vert2b);
            vBuffer.add(vert3a);
            vBuffer.add(vert3b);
            vBuffer.add(vert4a);
            vBuffer.add(vert4b);
        } else {
            int vert1a = (x1 & 0x1FF) | ((y1 & 0x1FF) << 9) | ((z1 & 0x1FF) << 18);
            int vert1b = (texId & 0xFF) | ((0 & 0x3F) << 8) | ((height & 0x3F) << 14) | ((ao1 & 0x3) << 20) | (scaleFlag << 22);

            int vert2a = (x2 & 0x1FF) | ((y2 & 0x1FF) << 9) | ((z2 & 0x1FF) << 18); // Position
            int vert2b = (texId & 0xFF) | ((0 & 0x3F) << 8) | ((0 & 0x3F) << 14) | ((ao2 & 0x3) << 20) | (scaleFlag << 22);

            int vert3a = (x3 & 0x1FF) | ((y3 & 0x1FF) << 9) | ((z3 & 0x1FF) << 18); // Position
            int vert3b = (texId & 0xFF) | ((width & 0x3F) << 8) | ((height & 0x3F) << 14) | ((ao3 & 0x3) << 20) | (scaleFlag << 22);

            int vert4a = (x4 & 0x1FF) | ((y4 & 0x1FF) << 9) | ((z4 & 0x1FF) << 18); // Position
            int vert4b = (texId & 0xFF) | ((width & 0x3F) << 8) | ((0 & 0x3F) << 14) | ((ao4 & 0x3) << 20) | (scaleFlag << 22);

            vBuffer.add(vert1a);
            vBuffer.add(vert1b);
            vBuffer.add(vert2a);
            vBuffer.add(vert2b);
            vBuffer.add(vert3a);
            vBuffer.add(vert3b);
            vBuffer.add(vert4a);
            vBuffer.add(vert4b);
        }

        if (backFace) {
            if (flipQuad) {
                // Slanted the other way for backface: 0-3-1 and 0-2-3
                iBuffer.add(addedVerts + 0);
                iBuffer.add(addedVerts + 3);
                iBuffer.add(addedVerts + 1);

                iBuffer.add(addedVerts + 0);
                iBuffer.add(addedVerts + 2);
                iBuffer.add(addedVerts + 3);
            } else {
                // Standard back face winding: 0-2-1 and 2-3-1
                iBuffer.add(addedVerts + 0);
                iBuffer.add(addedVerts + 2);
                iBuffer.add(addedVerts + 1);

                iBuffer.add(addedVerts + 2);
                iBuffer.add(addedVerts + 3);
                iBuffer.add(addedVerts + 1);
            }
        } else {
            if (flipQuad) {
                // Slanted the other way for frontface: 0-1-3 and 0-3-2
                iBuffer.add(addedVerts + 0);
                iBuffer.add(addedVerts + 1);
                iBuffer.add(addedVerts + 3);

                iBuffer.add(addedVerts + 0);
                iBuffer.add(addedVerts + 3);
                iBuffer.add(addedVerts + 2);
            } else {
                // Standard front face winding: 0-1-2 and 2-1-3
                iBuffer.add(addedVerts + 0);
                iBuffer.add(addedVerts + 1);
                iBuffer.add(addedVerts + 2);

                iBuffer.add(addedVerts + 2);
                iBuffer.add(addedVerts + 1);
                iBuffer.add(addedVerts + 3);
            }
        }
    }

    private static Mesh buildBlockMesh(byte blockType) {
        IntArrayList vBuffer = new IntArrayList(48); // 24 vertices * 2 ints
        IntArrayList iBuffer = new IntArrayList(36); // 6 faces * 6 indices

        // 0xFF sets AO level to 3 on all 4 corners (vAoFactor = 1.0)
        byte fullAO = (byte) 0xFF;

        // 1. TOP (Y = 1, Axis 1, backFace = true)
        addQuad(vBuffer, iBuffer, 0,1,0, 1,1,0, 0,1,1, 1,1,1, 1, 1, blockType, true, 1, fullAO, false, false);

        // 2. BOTTOM (Y = 0, Axis 1, backFace = false)
        addQuad(vBuffer, iBuffer, 0,0,0, 1,0,0, 0,0,1, 1,0,1, 1, 1, blockType, false, 1, fullAO, false, false);

        // 3. WEST (X = 0, Axis 0, backFace = true)
        addQuad(vBuffer, iBuffer, 0,0,0, 0,1,0, 0,0,1, 0,1,1, 1, 1, blockType, true, 0, fullAO, false, false);

        // 4. EAST (X = 1, Axis 0, backFace = false)
        addQuad(vBuffer, iBuffer, 1,0,0, 1,1,0, 1,0,1, 1,1,1, 1, 1, blockType, false, 0, fullAO, false, false);

        // 5. NORTH (Z = 0, Axis 2, backFace = true)
        addQuad(vBuffer, iBuffer, 0,0,0, 1,0,0, 0,1,0, 1,1,0, 1, 1, blockType, true, 2, fullAO, false, false);

        // 6. SOUTH (Z = 1, Axis 2, backFace = false)
        addQuad(vBuffer, iBuffer, 0,0,1, 1,0,1, 0,1,1, 1,1,1, 1, 1, blockType, false, 2, fullAO, false, false);

        return new Mesh(vBuffer.toIntArray(), iBuffer.toIntArray(), vBuffer.size(), iBuffer.size());
    }

    private static final WorldShader cubeShader = new WorldShader();
    private static void bakeBlockMesh(byte blockType, int slotX, int slotY, int blockTextureId) {
        int x = slotX*64;
        int y = atlasSize-((slotY+1)*64);

        glViewport(x, y, 64, 64);
        glEnable(GL_SCISSOR_TEST);
        glScissor(x, y, 64, 64);
        glClearColor(0.0f, 0.0f, 0.0f, 0.0f);
        glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);

        glEnable(GL_DEPTH_TEST);
        glDepthFunc(GL_LESS);
        glEnable(GL_CULL_FACE);
        glCullFace(GL_BACK);

        Matrix4f projection = new Matrix4f().ortho(-1.0f, 1.0f, 1.0f, -1.0f, 0.01f, 10.0f);
        Matrix4f view = new Matrix4f().lookAt(0, 0, 3.0f, 0, 0, 0, 0, 1.0f, 0.0f);
        Matrix4f model = new Matrix4f()
                .scale(0.75f)
                .rotateX((float)Math.toRadians(-30.0f))
                .rotateY((float)Math.toRadians(45.0f))
                .translate(-0.5f, -0.5f, -0.5f); // Center origin

        glActiveTexture(GL_TEXTURE0);
        glBindTexture(GL_TEXTURE_2D_ARRAY, blockTextureId);
        try (MemoryStack stack = stackPush()) {
            FloatBuffer matrixBuffer = stack.mallocFloat(16);
            cubeShader.start();
            cubeShader.setCamera(projection, view, matrixBuffer);
            cubeShader.setModel(model, matrixBuffer);

            // Render cube
            Mesh cube = buildBlockMesh(blockType);
            cube.render();
            cube.cleanup();
        }

        glDisable(GL_SCISSOR_TEST);
        glDisable(GL_DEPTH_TEST);
        glDisable(GL_CULL_FACE);
    }

    private static ByteBuffer upscaleImage(ByteBuffer src16) {
        src16.rewind();
        ByteBuffer dst32 = MemoryUtil.memAlloc(32*32*4);

        for (int y = 0; y < 16; y++) {
            for (int x = 0; x < 16; x++) {
                int srcIndex = (y * 16 + x) * 4;
                byte r = src16.get(srcIndex);
                byte g = src16.get(srcIndex+1);
                byte b = src16.get(srcIndex+2);
                byte a = src16.get(srcIndex+3);

                for (int dy = 0; dy < 2; dy++) {
                    for (int dx = 0; dx < 2; dx++) {
                        int destX = x*2 + dx;
                        int destY = y*2 + dy;
                        int destIndex = (destY*32 + destX) * 4;

                        dst32.put(destIndex, r);
                        dst32.put(destIndex+1, g);
                        dst32.put(destIndex+2, b);
                        dst32.put(destIndex+3, a);
                    }
                }
            }
        }

        dst32.rewind();
        return dst32;
    }

    private static void xTextureDirectUpload(byte blockType, int slotX, int slotY, int atlasId, Texture blockTextures) {
        int xOffset = (64-32)/2;
        int yOffset = (64-32)/2;

        int x = slotX*64 + xOffset;
        int y = atlasSize-((slotY+1)*64) + yOffset;
        glActiveTexture(GL_TEXTURE0);
        glBindTexture(GL_TEXTURE_2D_ARRAY, blockTextures.getId());

        ByteBuffer pixelBuffer = blockTextures.getLayer(Texture.getTextureIndex(blockType, BLOCK_FACE.NORTH)); // Any face should be sufficient for now
        pixelBuffer = upscaleImage(pixelBuffer);

        pixelBuffer.rewind();
        glPixelStorei(GL_UNPACK_ALIGNMENT, 1);
        glBindTexture(GL_TEXTURE_2D, atlasId);
        glTexSubImage2D(GL_TEXTURE_2D, 0, x, y, 32, 32, GL_RGBA, GL_UNSIGNED_BYTE, pixelBuffer);
        glPixelStorei(GL_UNPACK_ALIGNMENT, 4);
        STBImage.stbi_image_free(pixelBuffer);
    }

    private static void uploadIcon(String path, int slotX, int slotY, int atlasId) {
        int offsetX = (64-32)/2;
        int offsetY = (64-32)/2;

        int x = slotX*64 + offsetX;
        int y = atlasSize-((slotY+1)*64) + offsetY;

        ByteBuffer pixelBuffer;
        try (MemoryStack stack = MemoryStack.stackPush()) {
            IntBuffer width = stack.mallocInt(1);
            IntBuffer height = stack.mallocInt(1);
            IntBuffer channels = stack.mallocInt(1);

            pixelBuffer = STBImage.stbi_load(path, width, height, channels, 4);
            if (pixelBuffer == null) {
                System.err.println("Failed to load icon: " + STBImage.stbi_failure_reason());
                return;
            }
            pixelBuffer = upscaleImage(pixelBuffer);

            glPixelStorei(GL_UNPACK_ALIGNMENT, 1);
            glBindTexture(GL_TEXTURE_2D, atlasId);
            glTexSubImage2D(GL_TEXTURE_2D, 0, x, y, 32, 32, GL_RGBA, GL_UNSIGNED_BYTE, pixelBuffer);
            glPixelStorei(GL_UNPACK_ALIGNMENT, 4);

            STBImage.stbi_image_free(pixelBuffer);
        }
    }
    Matrix4f screenOrtho = null;
    public void setScreenDimensions(int windowWidth, int windowHeight) {
        screenOrtho = new Matrix4f().ortho2D(0, windowWidth, windowHeight, 0);
    }

    public UIRenderer(Texture blockTextures) {
        this.mainProgramId = compileUIShaders();

        locProjection = glGetUniformLocation(mainProgramId, "projection");
        locTransform = glGetUniformLocation(mainProgramId, "transform");
        locColorTint = glGetUniformLocation(mainProgramId, "colorTint");
        locUseTexture = glGetUniformLocation(mainProgramId, "useTexture");
        locTexture = glGetUniformLocation(mainProgramId, "uiTexture");
        locRect = glGetUniformLocation(mainProgramId, "u_rect");
        locUvTransform = glGetUniformLocation(mainProgramId, "u_uvTransform");
        primaryBlockTextures = blockTextures;

        float[] vertices = {
                0.0f, 0.0f, 0.0f, 0.0f,
                1.0f, 0.0f, 1.0f, 0.0f,
                1.0f, 1.0f, 1.0f, 1.0f,
                0.0f, 1.0f, 0.0f, 1.0f
        };
        int[] indices = {0, 1, 2, 2, 3, 0};

        uiVao = glGenVertexArrays();
        glBindVertexArray(uiVao);

        int vbo = glGenBuffers();
        glBindBuffer(GL_ARRAY_BUFFER, vbo);
        glBufferData(GL_ARRAY_BUFFER, vertices, GL_STATIC_DRAW);

        int ebo = glGenBuffers();
        glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, ebo);
        glBufferData(GL_ELEMENT_ARRAY_BUFFER, indices, GL_STATIC_DRAW);

        glEnableVertexAttribArray(0);
        glVertexAttribPointer(0, 2, GL_FLOAT, false, 4 * Float.BYTES, 0);
        glEnableVertexAttribArray(1);
        glVertexAttribPointer(1, 2, GL_FLOAT, false, 4 * Float.BYTES, 2 * Float.BYTES);

        glBindVertexArray(0);
        glBindBuffer(GL_ARRAY_BUFFER, 0);
        glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, 0);

        int fbo = glGenFramebuffers();
        glBindFramebuffer(GL_FRAMEBUFFER, fbo);

        int textureId = glGenTextures();
        glBindTexture(GL_TEXTURE_2D, textureId);
        atlasId = textureId;

        glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA8, atlasSize, atlasSize, 0, GL_RGBA, GL_UNSIGNED_BYTE, (ByteBuffer)null);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_NEAREST);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_NEAREST);

        int renderBuffer = glGenRenderbuffers();
        glBindRenderbuffer(GL_RENDERBUFFER, renderBuffer);
        glRenderbufferStorage(GL_RENDERBUFFER, GL_DEPTH24_STENCIL8, atlasSize, atlasSize);

        glFramebufferRenderbuffer(GL_FRAMEBUFFER, GL_DEPTH_ATTACHMENT, GL_RENDERBUFFER, renderBuffer);
        glFramebufferTexture2D(GL_FRAMEBUFFER, GL_COLOR_ATTACHMENT0, GL_TEXTURE_2D, textureId, 0);

        if (glCheckFramebufferStatus(GL_FRAMEBUFFER) != GL_FRAMEBUFFER_COMPLETE) {
            System.err.println("Atlas Framebuffer is not complete!");
        }

        int x = 0;
        int y = 0;
        for (int i = 0; i <= 47; i++) {
            if (Texture.itemTexturePaths[i] != null) {
                uploadIcon(Texture.itemTexturePaths[i], x, y, atlasId);
            } else if (Texture.isXShapedBlock[i]) {
                xTextureDirectUpload((byte)i, x, y, atlasId, primaryBlockTextures);
            } else {
                bakeBlockMesh((byte)i, x, y, primaryBlockTextures.getId());
            }

            x++;
            if (x==16) {
                x = 0;
                y++;
            }
        }


        glBindFramebuffer(GL_FRAMEBUFFER, 0);
        glViewport(0, 0, 1600, 900);
    }

    public void beginUiRendering(int windowWidth, int windowHeight) {
        glDisable(GL_DEPTH_TEST);
        glDisable(GL_CULL_FACE);
        glEnable(GL_BLEND);
        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);

        glUseProgram(mainProgramId);
        glUniform1i(locTexture, 2);
        glActiveTexture(GL_TEXTURE2);

        matrixBuffer.clear();
        glUniformMatrix4fv(locProjection, false, screenOrtho.get(matrixBuffer));

        glBindVertexArray(uiVao);
    }

    public void beginTextRendering(int windowWidth, int windowHeight) {
        glDisable(GL_DEPTH_TEST);
        glDisable(GL_CULL_FACE);
        glEnable(GL_BLEND);
        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);

        glUseProgram(fontShader.getProgramID());
        glUniform1i(fontShader.fontTex_loc, 5);
        glActiveTexture(GL_TEXTURE5);

        matrixBuffer.clear();
        glUniformMatrix4fv(fontShader.proj_loc, false, screenOrtho.get(matrixBuffer));
    }

    public void drawIcon(byte blockId, float screenX, float screenY, float width, float height) {
        int tileX = blockId%16;
        int tileY = blockId/16;
        glUniform1i(locUseTexture, 1);
        glUniform4f(locTransform, screenX, screenY, width, height);
        glUniform4f(locColorTint, 1f, 1f, 1f, 1f);

        float tileSize = 64;
        float xScale = 64f/atlasSize;
        float yScale = 64f/atlasSize;

        float pixelX = tileX * tileSize;
        float pixelY = atlasSize - ((tileY+1)*tileSize);
        float xOffset = pixelX / atlasSize;
        float yOffset = pixelY / atlasSize;

        glUniform4f(locUvTransform, xOffset, yOffset, xScale, yScale);
        glActiveTexture(GL_TEXTURE2);
        glBindTexture(GL_TEXTURE_2D, atlasId);

        glDrawElements(GL_TRIANGLES, 6, GL_UNSIGNED_INT, 0);
    }

    public void drawTexture(int textureId, float x, float y, float width, float height) {
        glUniform1i(locUseTexture, 2);
        glUniform4f(locTransform, x, y, width, height);
        glUniform4f(locColorTint, 1.0f, 1.0f, 1.0f, 1.0f);

        glActiveTexture(GL_TEXTURE2);
        glBindTexture(GL_TEXTURE_2D, textureId);

        glDrawElements(GL_TRIANGLES, 6, GL_UNSIGNED_INT, 0);
    }

    public void drawRect(float x, float y, float width, float height, float r, float g, float b, float a) {
        glUniform1i(locUseTexture, 0);
        glUniform4f(locTransform, x, y, width, height);
        glUniform4f(locColorTint, r, g, b, a);
        glUniform4f(locRect, x, y, width, height);

        glDrawElements(GL_TRIANGLES, 6, GL_UNSIGNED_INT, 0);
    }

    private STBTTBakedChar.Buffer cdata;
    private int fontAtlasId;
    public FontShader fontShader = new FontShader();
    private int[] pAscent;
    private int[] pDescent;
    private int[] pLineGap;
    private STBTTFontinfo fontInfo;

    public void loadFont(String resourcePath) throws IOException {
        try (InputStream is = UIRenderer.class.getResourceAsStream(resourcePath)) {
            if (is == null) {
                throw new IOException("Font file not found: " + resourcePath);
            }

            byte[] bytes = is.readAllBytes();
            ByteBuffer ttfBuffer = BufferUtils.createByteBuffer(bytes.length);
            ttfBuffer.put(bytes);
            ttfBuffer.flip();

            fontInfo = STBTTFontinfo.create();
            boolean success = STBTruetype.stbtt_InitFont(fontInfo, ttfBuffer, 0);
            if (!success) {
                throw new RuntimeException("Failed to initialize STB TTF font info!");
            }

            pAscent = new int[1];
            pDescent = new int[1];
            pLineGap = new int[1];

            STBTruetype.stbtt_GetFontVMetrics(fontInfo, pAscent, pDescent, pLineGap);
            float fontSizePx = 32.0f;
            float scale = STBTruetype.stbtt_ScaleForPixelHeight(fontInfo, fontSizePx);

            float scaledAscent = pAscent[0]*scale;
            float scaledDescent = pDescent[0]*scale;
            float scaledLineGap = pLineGap[0]*scale;

            int atlasWidth = 512;
            int atlasHeight = 512;
            float fontSize = 32.0f;

            ByteBuffer bitmapBuffer = MemoryUtil.memAlloc(atlasWidth*atlasHeight);
            cdata = STBTTBakedChar.malloc(95);
            int result = STBTruetype.stbtt_BakeFontBitmap(
                    ttfBuffer, fontSize, bitmapBuffer, atlasWidth, atlasHeight, 32, cdata
            );

            if (result <= 0) {
                throw new RuntimeException("Font atlas too small for specified font size!");
            }

            fontAtlasId = glGenTextures();
            fontShader.start();
            int texLoc = glGetUniformLocation(fontShader.getProgramID(), "fontTexture");
            glUniform1i(texLoc, 5);
            fontShader.stop();

            glActiveTexture(GL_TEXTURE5);
            glBindTexture(GL_TEXTURE_2D, fontAtlasId);
            glPixelStorei(GL_UNPACK_ALIGNMENT, 1);
            glTexImage2D(GL_TEXTURE_2D, 0, GL_RED, atlasWidth, atlasHeight, 0, GL_RED, GL_UNSIGNED_BYTE, bitmapBuffer);

            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_NEAREST);
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_NEAREST);

            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE);
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE);

            glPixelStorei(GL_UNPACK_ALIGNMENT, 4);
            MemoryUtil.memFree(bitmapBuffer);
            glActiveTexture(GL_TEXTURE0);
        }
    }

    float[] xpos = new float[1];
    float[] ypos = new float[1];
    STBTTAlignedQuad quad = STBTTAlignedQuad.malloc();

    private void addVertex(FloatArrayList verts, float x, float y, float s, float t) {
        verts.add(x);
        verts.add(y);
        verts.add(s);
        verts.add(t);
    }

    private int fontVao;
    private int fontVbo;
    public void prepareFontRendering() {
        fontVao = glGenVertexArrays();
        fontVbo = glGenBuffers();

        glBindVertexArray(fontVao);
        glBindBuffer(GL_ARRAY_BUFFER, fontVbo);

        long maxBufferSize = 1000 * 6 * 4 * Float.BYTES;
        glBufferData(GL_ARRAY_BUFFER, maxBufferSize, GL_DYNAMIC_DRAW);

        glVertexAttribPointer(0, 2, GL_FLOAT, false, 4*Float.BYTES, 0);
        glEnableVertexAttribArray(0);
        glVertexAttribPointer(1, 2, GL_FLOAT, false, 4*Float.BYTES, 2*Float.BYTES);
        glEnableVertexAttribArray(1);

        glBindVertexArray(0);
    }

    public void setFontColor(float r, float g, float b, float a) {
        fontShader.start();
        glUniform4f(fontShader.color_loc, r, g, b, a);
        fontShader.stop();
    }

    enum TextAlignment {
        LEFT,
        CENTER,
        RIGHT
    }

    public void renderFont(String text, int x, int y, TextAlignment alignment) {
        int totalWidth = 0;
        int xOffset = 0;
        if (alignment != TextAlignment.LEFT) {
            float[] dummyX = new float[] { 0.0f };
            float[] dummyY = new float[] { 0.0f };

            for (int i = 0; i < text.length(); i++) {
                char c = text.charAt(i);
                if (c < 32 || c > 126) continue;

                STBTruetype.stbtt_GetBakedQuad(cdata, 512, 512, c - 32, dummyX, dummyY, quad, true);
            }

            totalWidth = (int)quad.x1();
        }

        xpos[0] = x;
        ypos[0] = y;

        if (alignment == TextAlignment.CENTER) {
            xpos[0] -= (float) totalWidth /2;
        } else if (alignment == TextAlignment.RIGHT) {
            xpos[0] -= (float) totalWidth;
        }

        FloatArrayList verts = new FloatArrayList(4*6*text.length()); // Max possible allocated
        int charsAdded = 0;
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (c < 32 || c > 126) continue;
            STBTruetype.stbtt_GetBakedQuad(cdata, 512, 512, c-32, xpos, ypos, quad, true);
            addVertex(verts, quad.x0(), quad.y0(), quad.s0(), quad.t0());
            // Bottom-Left
            addVertex(verts, quad.x0(), quad.y1(), quad.s0(), quad.t1());
            // Bottom-Right
            addVertex(verts, quad.x1(), quad.y1(), quad.s1(), quad.t1());

            // Top-Left
            addVertex(verts, quad.x0(), quad.y0(), quad.s0(), quad.t0());
            // Bottom-Right
            addVertex(verts, quad.x1(), quad.y1(), quad.s1(), quad.t1());
            // Top-Right
            addVertex(verts, quad.x1(), quad.y0(), quad.s1(), quad.t0());
            charsAdded++;
        }

        glBindVertexArray(fontVao);
        glBindBuffer(GL_ARRAY_BUFFER, fontVbo);

        // Note: Engine renderer must enable font shader before calling this method
        try (MemoryStack stack = MemoryStack.stackPush()) {
            FloatBuffer bufferedElements = stack.mallocFloat(charsAdded * 6 * 4);
            bufferedElements.put(verts.elements(), 0, charsAdded * 6 * 4);
            bufferedElements.flip();

            glBufferSubData(GL_ARRAY_BUFFER, 0, bufferedElements);
            glDrawArrays(GL_TRIANGLES, 0, verts.size()/4);
        }
    }

    public void end() {
        glBindVertexArray(0);

        glActiveTexture(GL_TEXTURE2);
        glBindTexture(GL_TEXTURE_2D, 0);
        glActiveTexture(GL_TEXTURE0);

        glUseProgram(0);
        glEnable(GL_DEPTH_TEST);
        glEnable(GL_CULL_FACE);
    }

    public int compileUIShaders() {
        UIShader shader = new UIShader();
        return shader.getProgramID();
    }
}
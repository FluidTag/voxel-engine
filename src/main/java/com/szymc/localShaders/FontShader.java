package com.szymc.localShaders;
import java.nio.FloatBuffer;
import org.lwjgl.opengl.GL;
import org.lwjgl.opengl.GL11;

import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL13.GL_TEXTURE0;
import static org.lwjgl.opengl.GL13.glActiveTexture;
import static org.lwjgl.system.MemoryUtil.NULL;
import org.lwjgl.system.MemoryStack;

import com.szymc.voxel_engine.Texture;

import static org.lwjgl.system.MemoryStack.*;

import static org.lwjgl.opengl.GL15.*; // VBO functions (glGenBuffers)
import static org.lwjgl.opengl.GL20.*; // Shader/Attribute functions (glVertexAttribPointer)
import static org.lwjgl.opengl.GL30.*; // VAO functions (glGenVertexArrays)

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.FloatBuffer;
import java.util.stream.Collectors;

import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.joml.FrustumIntersection;
import org.joml.Matrix4f;

public class FontShader extends Shader {
    public int proj_loc, fontTex_loc, color_loc;
    public FontShader() {
        super("/shaders/font.vert", "/shaders/font.frag");

        proj_loc = glGetUniformLocation(this.getProgramID(), "projection");
        fontTex_loc = glGetUniformLocation(this.getProgramID(), "fontTexture");
        color_loc = glGetUniformLocation(this.getProgramID(), "fontColor");
    }
}

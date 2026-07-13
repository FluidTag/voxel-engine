package com.szymc.voxel_engine;
import com.szymc.localShaders.OutlineShader;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL;
import org.lwjgl.opengl.GL11;


import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL13.GL_TEXTURE0;
import static org.lwjgl.opengl.GL13.glActiveTexture;
import static org.lwjgl.system.MemoryUtil.NULL;
import org.lwjgl.system.MemoryStack;


import com.szymc.localShaders.DebugShader;
import com.szymc.localShaders.WorldShader;


import static org.lwjgl.system.MemoryStack.*;


import static org.lwjgl.opengl.GL15.*; // VBO functions (glGenBuffers)
import static org.lwjgl.opengl.GL20.*; // Shader/Attribute functions (glVertexAttribPointer)
import static org.lwjgl.opengl.GL30.*; // VAO functions (glGenVertexArrays)


import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.stream.Collectors;


import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.joml.FrustumIntersection;


public class Engine {
	private Camera camera;
	private World worldScene;
	private WorldShader mainShader;
	private DebugManager debugger;
	private OutlineShader outlineShader;
	private Matrix4f outlineLoc;
	private BlockOutline outline;
	private UIRenderer uiRenderer;
	private int crosshairTexture;

	public void removeOutlineLoc() {
		this.outlineLoc = null;
	}

	public void setOutlineLoc(int x, int y, int z) {
		this.outlineLoc = new Matrix4f().translation(x, y, z);
	}

	public Engine(World world, Camera camera) {
		this.worldScene = world;
		this.camera = camera;

		this.debugger = new DebugManager(world, camera);
		this.mainShader = new WorldShader();

		this.uiRenderer = new UIRenderer();
		this.crosshairTexture = Texture.loadTexturePath("src/main/resources/ui/crosshair.png");

		this.outlineShader = new OutlineShader();
		outlineShader.start();
		outlineShader.setColor(0.5f, 1.0f, 0.5f);
		outlineShader.stop();
		this.outline = new BlockOutline();

		//glPolygonMode(GL_FRONT_AND_BACK, GL_LINE);
		//glLineWidth(2);
		//glEnable(GL11.GL_BLEND);
	}

	private Vector3f tempModel = new Vector3f();
	private Matrix4f modelVec = new Matrix4f();

	public void render() {
		glClearColor(0.5f, 0.6f, 0.8f, 1.0f); // Sky Blue
		glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);

		try (MemoryStack stack = stackPush()) {
			mainShader.start();

			FloatBuffer matrixBuffer = stack.mallocFloat(16);
			mainShader.setCamera(camera.getProjectionMatrix(), camera.getViewMatrix(), matrixBuffer);

			Matrix4f view = camera.getViewMatrix();
			camera.updateFrustum(view);

			glDisable(GL_BLEND);
			glDepthMask(true);

			//debugger.renderDebug(matrixBuffer);
			mainShader.start();

			for (ChunkColumn chunk : worldScene.getRendered().values()) {
				if (chunk == null) continue;

				if (!camera.frustumInt.testAab(chunk.getWorldX()*32, 0, chunk.getWorldZ()*32, chunk.getWorldX()*32+32, 256, chunk.getWorldZ()*32+32)) {
					continue;
				}

				for (int s = 0; s < 16; s++) {
					ChunkSection section = chunk.getSection(s);
					if (section == null) continue;
					if (section.getMesh() == null) continue;

					int minX = section.getWorldX();
					int minY = section.getWorldY();
					int minZ = section.getWorldZ();
					int maxX = section.getWorldX() + 32;
					int maxY = section.getWorldY() + 16;
					int maxZ = section.getWorldZ() + 32;


					if (!camera.frustumInt.testAab(minX, minY, minZ, maxX, maxY, maxZ)) {
						continue;
					}

					float worldX = minX;
					float worldY = minY;
					float worldZ = minZ;

					tempModel.set(worldX, worldY, worldZ);
					modelVec.translation(tempModel);
					mainShader.setModel(modelVec, matrixBuffer);

					section.getMesh().render();
				}
			}

			if (outlineLoc != null) {
				outlineShader.start();
				outlineShader.setCamera(camera.getProjectionMatrix(), camera.getViewMatrix(), matrixBuffer);
				outlineShader.setModel(this.outlineLoc, matrixBuffer);

				glBindVertexArray(outline.getVao());
				glDepthFunc(GL_LEQUAL);

				glLineWidth(4f);
				glDrawElements(GL_LINES, 24, GL_UNSIGNED_INT, 0L);

				// Clean up states
				glDepthFunc(GL_LESS);
				// ------------------------------

				outlineShader.stop();
				glBindVertexArray(0);

				// Reactivate main shader for the upcoming water rendering loop
				mainShader.start();
				mainShader.setCamera(camera.getProjectionMatrix(), camera.getViewMatrix(), matrixBuffer);
			}

			glEnable(GL_BLEND);
			glDepthMask(false);
			for (ChunkColumn chunk : worldScene.getRendered().values()) {
				if (chunk == null) continue;

				if (!camera.frustumInt.testAab(chunk.getWorldX()*32, 0, chunk.getWorldZ()*32, chunk.getWorldX()*32+32, 256, chunk.getWorldZ()*32+32)) {
					continue;
				}

				for (int s = 0; s < 16; s++) {
					ChunkSection section = chunk.getSection(s);
					if (section == null) continue;
					if (section.getWaterMesh() == null) continue;

					int minX = section.getWorldX();
					int minY = section.getWorldY();
					int minZ = section.getWorldZ();
					int maxX = section.getWorldX() + 32;
					int maxY = section.getWorldY() + 16;
					int maxZ = section.getWorldZ() + 32;


					if (!camera.frustumInt.testAab(minX, minY, minZ, maxX, maxY, maxZ)) {
						continue;
					}

					float worldX = minX;
					float worldY = minY;
					float worldZ = minZ;

					tempModel.set(worldX, worldY, worldZ);
					modelVec.translation(tempModel);
					mainShader.setModel(modelVec, matrixBuffer);

					section.getWaterMesh().render();
				}
			}

			mainShader.stop();

			uiRenderer.begin(1600, 900);
			float crossX = (1600/2.0f) - 8.0f;
			float crossY = (900/2.0f) - 8.0f;

			uiRenderer.drawTexture(crosshairTexture, crossX, crossY, 16, 16);
			uiRenderer.end();

			glDepthMask(true);
		}
	}
}
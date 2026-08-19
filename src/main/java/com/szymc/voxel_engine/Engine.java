package com.szymc.voxel_engine;
import com.szymc.localShaders.EntityShader;
import com.szymc.localShaders.OutlineShader;


import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL32.*;
import org.lwjgl.system.MemoryStack;


import com.szymc.localShaders.WorldShader;


import static org.lwjgl.system.MemoryStack.*;


import static org.lwjgl.opengl.GL30.*; // VAO functions (glGenVertexArrays)


import java.io.IOException;
import java.nio.FloatBuffer;


import org.joml.Matrix4f;
import org.joml.Vector3f;


public class Engine {
	private Camera camera;
	private World worldScene;
	private WorldShader mainShader;
	private EntityShader entityShader;
	private DebugManager debugger;
	private PlayerCharacter player;
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

	public void setPlayer(PlayerCharacter player) {
		this.player = player;
	}
	public static boolean wireframeMode = false;
	public Engine(World world, Camera camera) {
		this.worldScene = world;
		this.camera = camera;

		this.debugger = new DebugManager(world, camera);
		this.mainShader = new WorldShader();
		this.entityShader = new EntityShader(mainShader.getTexture());
		Texture.readBlockJson("gameData/blocks.json");
		this.uiRenderer = new UIRenderer(mainShader.getTexture());
		this.crosshairTexture = Texture.loadTexturePath("src/main/resources/ui/crosshair.png");

		this.outlineShader = new OutlineShader();
		outlineShader.start();
		outlineShader.setColor(0f, 0f, 0f);
		outlineShader.stop();
		this.outline = new BlockOutline();

		uiRenderer.setScreenDimensions(1600, 900);
		uiRenderer.setFontColor(1.0f, 1.0f, 1.0f, 1.0f);
		EntityItem.setBlockTextures(mainShader.getTexture());
		EntityItem.generateEaoCache();

		try {
			uiRenderer.loadFont("/fonts/mainFont.ttf");
			uiRenderer.prepareFontRendering();
		} catch (IOException e) {
			e.printStackTrace();
		}

		//glPolygonMode(GL_FRONT_AND_BACK, GL_LINE);
		//glLineWidth(2);
		//glEnable(GL11.GL_BLEND);
	}

	private Vector3f tempModel = new Vector3f();
	private Matrix4f modelVec = new Matrix4f();

	public void render() {
		glClearColor(0.5f, 0.6f, 0.8f, 1.0f); // Sky Blue
		glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);
		if (wireframeMode) glPolygonMode(GL_FRONT_AND_BACK, GL_LINE); else glPolygonMode(GL_FRONT_AND_BACK, GL_FILL);

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

			mainShader.stop();
			entityShader.start();
			entityShader.setCamera(camera.getProjectionMatrix(), camera.getViewMatrix(), matrixBuffer);

			glBindVertexArray(EntityItem.getVao());
			for (Entity entity : worldScene.getEntities().values()) {
				tempModel.set(entity.position.x, entity.position.y, entity.position.z);
				modelVec.translation(tempModel);
				entityShader.setModel(modelVec, matrixBuffer);

				if (entity.getClass() == EntityItem.class) {
					EntityItem item = (EntityItem)entity;
					glDrawElementsBaseVertex(GL_TRIANGLES, item.itemMesh.indexCount, GL_UNSIGNED_INT, item.itemMesh.byteOffset, item.itemMesh.baseVertex);
				}
			}

			entityShader.stop();
			mainShader.start();

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

			uiRenderer.beginUiRendering(1600, 900);
			float crossX = (1600/2.0f) - 8.0f;
			float crossY = (900/2.0f) - 8.0f;

			uiRenderer.drawTexture(crosshairTexture, crossX, crossY, 16, 16);
			int slotSize = 64;
			int offsetX = (int)((1600/2.0f)-(slotSize*4.5f));
			uiRenderer.drawRect(offsetX-2, 820-2, slotSize*9 + 4, slotSize + 4, 0.7f, 0.7f, 0.7f, 0.8f);
			byte[] inventory = player.getInventory();

			for (int i = 0; i < 9; i++) {
				float color = player.currentHotbarSlot == i ? 0.45f : 0.2f;
				uiRenderer.drawRect(offsetX + (slotSize*i) + 2, 820+2, slotSize-4, slotSize-4, color, color, color, 0.6f);
				byte item = inventory[i];
				if (item != 0) uiRenderer.drawIcon(item, offsetX + (slotSize*i), 820, slotSize, slotSize);
			}

			uiRenderer.beginTextRendering(1600, 900);

			for (byte i = 0; i < 9; i++) {
				if (player.readInventoryType(i) != 0) {
					uiRenderer.renderFont(Integer.toString(player.readInventoryAmount(i)), offsetX + (slotSize*i) + (slotSize-4), 820-4+slotSize, UIRenderer.TextAlignment.RIGHT);
				}
			}

			//uiRenderer.renderFont("Hello World, Text rendering has been added successfully! 1234567890", 100, 100);
			uiRenderer.end();

			glDepthMask(true);
		}
	}
}
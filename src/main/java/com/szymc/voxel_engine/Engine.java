package com.szymc.voxel_engine;
import com.szymc.localShaders.EntityShader;
import com.szymc.localShaders.OutlineShader;


import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL32.*;
import org.lwjgl.system.MemoryStack;


import com.szymc.localShaders.WorldShader;


import static org.lwjgl.system.MemoryStack.*;


import java.io.IOException;
import java.nio.FloatBuffer;
import java.util.Objects;


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

	// Engine is begging for a refactor bro, fix ts, maybe consider moving the ui to another class

	public record InventoryActiveItem(byte item, byte itemAmount, int ogSlotX, int ogSlotY, int inventoryIndex) {}
	private InventoryActiveItem activeInventoryDrag = null;

	public void requestDropInvIndex(byte invIndex, int decrementAmount) {
		byte type = player.readInventoryType(invIndex);
		byte amount = player.readInventoryAmount(invIndex);
		int clampedNewAmount = Math.max(0, amount - decrementAmount);
		if (type == 0) return;

		player.setInventorySlot(invIndex, (amount-decrementAmount > 0 ? type : (byte)0), (byte) clampedNewAmount);

		Camera cam = player.getPlayerCamera();
		Vector3f lookDir = cam.getLookUnitNormal().normalize();

		for (int i = 0; i < decrementAmount; i++) {
			EntityItem createdEntity = worldScene.spawnNewItemEntity(type, 0, 0, 0, true);

			createdEntity.position.set(cam.cameraPos.x + lookDir.x*2, cam.cameraPos.y + lookDir.y*2, cam.cameraPos.z + lookDir.z*2 );
		}
	}

	public void requestDropOfGuiDraggedItem() {
		if (activeInventoryDrag == null) return;
		byte inventoryIndex = (byte) activeInventoryDrag.inventoryIndex;
		requestDropInvIndex(inventoryIndex, activeInventoryDrag.itemAmount);

		activeInventoryDrag = null;
	}

	public void setActiveInventoryDrag(InventoryActiveItem itemData, boolean isLeftClick) {
		if (itemData == null) {
			activeInventoryDrag = null;
			return;
		}

		if (activeInventoryDrag == null && !isLeftClick && itemData.item != 0) {
			activeInventoryDrag = new InventoryActiveItem(itemData.item, (byte) (itemData.itemAmount/2), itemData.ogSlotX, itemData.ogSlotY, -1);

			player.setInventorySlot((byte) itemData.inventoryIndex, itemData.item, (byte) (itemData.itemAmount/2));
			return;
		}

		if (activeInventoryDrag != null && isLeftClick) {
			if (itemData.inventoryIndex == activeInventoryDrag.inventoryIndex) {
				player.setInventorySlot((byte)activeInventoryDrag.inventoryIndex, activeInventoryDrag.item, activeInventoryDrag.itemAmount);
				activeInventoryDrag = null;
				return;
			}

			if (itemData.item == 0 || (activeInventoryDrag.item == itemData.item && itemData.itemAmount < 64)) {
				byte currentInvAmount = player.readInventoryAmount((byte)itemData.inventoryIndex);
				int maxAmount = 64-currentInvAmount;
				int beingApplied = Math.min(activeInventoryDrag.itemAmount, maxAmount);
				int leftOver = activeInventoryDrag.itemAmount-beingApplied;

				if (activeInventoryDrag.inventoryIndex != -1) player.setInventorySlot((byte) activeInventoryDrag.inventoryIndex, (byte)0, (byte)0);
				player.setInventorySlot((byte) itemData.inventoryIndex, activeInventoryDrag.item, (byte) (currentInvAmount+beingApplied));

				if (leftOver > 0) {
					activeInventoryDrag = new InventoryActiveItem(activeInventoryDrag.item, (byte)leftOver, activeInventoryDrag.ogSlotX, activeInventoryDrag.ogSlotY, activeInventoryDrag.inventoryIndex);
				} else activeInventoryDrag = null;
			} else if (activeInventoryDrag.item != itemData.item || itemData.itemAmount == 64) {
				if (activeInventoryDrag.inventoryIndex != -1) player.setInventorySlot((byte) activeInventoryDrag.inventoryIndex, itemData.item, itemData.itemAmount);
				player.setInventorySlot((byte) itemData.inventoryIndex, activeInventoryDrag.item, activeInventoryDrag.itemAmount);

				activeInventoryDrag = new InventoryActiveItem(itemData.item, itemData.itemAmount, activeInventoryDrag.ogSlotX, activeInventoryDrag.ogSlotY, activeInventoryDrag.inventoryIndex);
			}

			return;
		}

		if (itemData != null && itemData.item != 0 && isLeftClick) this.activeInventoryDrag = itemData;
	}

	private int mouseX, mouseY;
	public void setMousePosition(int x, int y) {
		this.mouseX = x;
		this.mouseY = y;
	}

	public void removeOutlineLoc() {
		this.outlineLoc = null;
		this.currentlyMiningState = null;
	}

	public void setOutlineLoc(int x, int y, int z) {
		this.outlineLoc = new Matrix4f().translation(x, y, z);
		byte blockAt = worldScene.getLoadedChunkAtPos(x>>5, z>>5).getBlockInChunk(x&31, y, z&31);
		boolean isNewBlock = currentlyMiningState != null && (x != currentlyMiningState.wx || y != currentlyMiningState.wy || z != currentlyMiningState.wz);
		if (isNewBlock || (currentlyMiningState == null && isLeftMouseHeld)) {
			currentlyMiningState = new BlockMineState(x, y, z, Texture.hardnessLevels[blockAt], Texture.hardnessLevels[blockAt]);
		}
    }

	public void setPlayer(PlayerCharacter player) {
		this.player = player;
	}
	public static boolean wireframeMode = false;
    public static final class BlockMineState {
        public int wx; public int wy; public int wz;
		public float health; public float maxHealth;

        public BlockMineState(int wx, int wy, int wz, float health, float maxHealth) {
            this.wx = wx;
            this.wy = wy;
            this.wz = wz;
            this.health = health;
            this.maxHealth = maxHealth;
        }
    }

	private BlockMineState currentlyMiningState = null;
	private boolean isLeftMouseHeld = false;
	public void startMining(int wx, int wy, int wz) {
		byte blockAt = worldScene.getLoadedChunkAtPos(wx>>5, wz>>5).getBlockInChunk(wx&31, wy, wz&31);
		currentlyMiningState = new BlockMineState(wx, wy, wz, Texture.hardnessLevels[blockAt], Texture.hardnessLevels[blockAt]);
		isLeftMouseHeld = true;
	}

	public void leftMouseHeldTick() {
		if (currentlyMiningState != null) {
			currentlyMiningState.health -= 0.085f;

			if (currentlyMiningState.health < 0) {
				ChunkColumn chunk = worldScene.getLoadedChunkAtPos(currentlyMiningState.wx>>5, currentlyMiningState.wz>>5);
				int x = currentlyMiningState.wx; int y = currentlyMiningState.wy; int z = currentlyMiningState.wz;
				byte block = chunk.getBlockInChunk(x&31, y, z&31);
				chunk.setBlockInChunk(x & 31, y, z & 31, Blocks.AIR);
				worldScene.spawnNewItemEntity(block, x, y, z, false);
				chunk.setSectionDirty(y >> 4);

				worldScene.updateChunk(currentlyMiningState.wx>>5, y, currentlyMiningState.wz>>5, x&31, z&31, false, block);
				currentlyMiningState = null;
			}
		}
	}

	public void mouseReleased() {
		currentlyMiningState = null;
		isLeftMouseHeld = false;
	}

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

		uiRenderer.setScreenDimensions(App.WINDOW_WIDTH, App.WINDOW_HEIGHT);
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
		glLineWidth(2);
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
				tempModel.set(entity.renderPosition.x, entity.renderPosition.y, entity.renderPosition.z);
				modelVec.translation(tempModel);
				entityShader.setModel(modelVec, matrixBuffer);

				if (entity.getClass() == EntityItem.class) {
					EntityItem item = (EntityItem)entity;
					glDrawElementsBaseVertex(GL_TRIANGLES, item.itemMesh.indexCount, GL_UNSIGNED_INT, item.itemMesh.byteOffset, item.itemMesh.baseVertex);
				}
			}

			entityShader.stop();
			mainShader.start();

			glEnable(GL_BLEND);
			glDepthMask(true);
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

                    tempModel.set((float) minX, (float) minY, (float) minZ);
					modelVec.translation(tempModel);
					mainShader.setModel(modelVec, matrixBuffer);

					section.getWaterMesh().render();
				}
			}

			if (outlineLoc != null) {
				outlineShader.start();
				outlineShader.setCamera(camera.getProjectionMatrix(), camera.getViewMatrix(), matrixBuffer);
				outlineShader.setModel(this.outlineLoc, matrixBuffer);

				glBindVertexArray(outline.getLineVao());
				glDepthFunc(GL_LEQUAL);

				glLineWidth(4f);
				outlineShader.setIsRenderingFace(false);
				glDrawElements(GL_LINES, 24, GL_UNSIGNED_INT, 0L);

				if (currentlyMiningState != null) {
					int texId = Texture.breakStages[(int) (7 - (currentlyMiningState.health/currentlyMiningState.maxHealth)*7)];

					outlineShader.setLocal_breakTexId(texId);
					outlineShader.setIsRenderingFace(true);

					glActiveTexture(GL_TEXTURE0);
					glBindVertexArray(outline.getTriangleVao());
					glDepthMask(false);
					glDrawElements(GL_TRIANGLES, 36, GL_UNSIGNED_INT, 0L);
					glDepthMask(true);
				}

				// Clean up states
				glDepthFunc(GL_LESS);
				// ------------------------------

				outlineShader.stop();
				glBindVertexArray(0);

				// Reactivate main shader for the upcoming water rendering loop
				mainShader.start();
				mainShader.setCamera(camera.getProjectionMatrix(), camera.getViewMatrix(), matrixBuffer);
			}

			mainShader.stop();

			uiRenderer.beginUiRendering(App.WINDOW_WIDTH, App.WINDOW_HEIGHT);
			float crossX = (App.WINDOW_WIDTH/2.0f) - 8.0f;
			float crossY = (App.WINDOW_HEIGHT/2.0f) - 8.0f;

			uiRenderer.drawTexture(crosshairTexture, crossX, crossY, 16, 16);
			int slotSize = 64;
			int offsetX = (int)((App.WINDOW_WIDTH/2.0f)-(slotSize*4.5f));
			uiRenderer.drawRect(offsetX-2, App.WINDOW_HEIGHT - 80-2, slotSize*9 + 4, slotSize + 4, 0.7f, 0.7f, 0.7f, 0.8f);
			byte[] inventory = player.getInventory();

			for (int i = 0; i < 9; i++) {
				float color = player.currentHotbarSlot == i ? 0.45f : 0.2f;
				uiRenderer.drawRect(offsetX + (slotSize*i) + 2, App.WINDOW_HEIGHT - 80+2, slotSize-4, slotSize-4, color, color, color, 0.6f);
				byte item = inventory[i];
				if (item != 0) uiRenderer.drawIcon(item, offsetX + (slotSize*i), App.WINDOW_HEIGHT - 80, slotSize, slotSize);
			}

			uiRenderer.beginTextRendering(App.WINDOW_WIDTH, App.WINDOW_HEIGHT);
			for (byte i = 0; i < 9; i++) {
				if (player.readInventoryType(i) != 0) {
					uiRenderer.renderFont(Integer.toString(player.readInventoryAmount(i)), offsetX + (slotSize*i) + (slotSize-4), App.WINDOW_HEIGHT - 80-4+slotSize, UIRenderer.TextAlignment.RIGHT);
				}
			}

			//////////

			if (player.getPlayerGuiInventoryActive()) {
				uiRenderer.beginUiRendering(App.WINDOW_WIDTH, App.WINDOW_HEIGHT);
				int topAreaSize = 270;
				float invPosY = (float) App.WINDOW_HEIGHT / 2 - (float) (slotSize * 4 + 2 + topAreaSize)/2;
				int hotbarGap = 12;

				uiRenderer.drawRect(0, 0, App.WINDOW_WIDTH, App.WINDOW_HEIGHT, 0f, 0f, 0f, 0.5f);
				uiRenderer.drawRect(offsetX - 2, invPosY - 2, slotSize * 9 + 4, slotSize * 4 + 4 + hotbarGap + topAreaSize, 0.7f, 0.7f, 0.7f, 1.0f);

				for (int iy = 0; iy < 4; iy++) {
					for (int ix = 0; ix < 9; ix++) {
						uiRenderer.drawRect(offsetX + (slotSize * ix) + 2, invPosY + (slotSize * iy) + 2 + topAreaSize + (iy == 3 ? hotbarGap : 0), slotSize - 4, slotSize - 4, 0.5f, 0.5f, 0.5f, 1.0f);
						int localInvIndex = (3-iy)*9 + ix;
						byte item = inventory[localInvIndex];
						if (item != 0 && !(activeInventoryDrag != null && activeInventoryDrag.inventoryIndex == localInvIndex)) uiRenderer.drawIcon(item, offsetX + (slotSize*ix) + 2, invPosY + (slotSize * iy) + 2 + topAreaSize + (iy ==3 ? hotbarGap : 0), slotSize-4, slotSize-4);
					}
				}

				uiRenderer.beginTextRendering(App.WINDOW_WIDTH, App.WINDOW_HEIGHT);
				for (int iy = 0; iy < 4; iy++) {
					for (int ix = 0; ix < 9; ix++) {
						int localInvIndex = (3-iy)*9 + ix;
						byte amount = player.readInventoryAmount((byte) localInvIndex);

						if (amount != 0 && !(activeInventoryDrag != null && activeInventoryDrag.inventoryIndex == localInvIndex)) {
							uiRenderer.renderFont(Integer.toString(amount), offsetX + (slotSize*ix) + (slotSize-4), (int) (invPosY + (slotSize * iy) + (slotSize-4) + topAreaSize + (iy ==3 ? hotbarGap : 0)), UIRenderer.TextAlignment.RIGHT);
						};
					}
				}
			}

			if (activeInventoryDrag != null) {
				int adjMouseX = (int) (mouseX - (float)(slotSize-4)/2);
				int adjMouseY = (int) (mouseY - (float)(slotSize-4)/2);

				uiRenderer.beginUiRendering(App.WINDOW_WIDTH, App.WINDOW_HEIGHT);
				uiRenderer.drawIcon(activeInventoryDrag.item, adjMouseX, adjMouseY, slotSize-4, slotSize-4);
				uiRenderer.beginTextRendering(App.WINDOW_WIDTH, App.WINDOW_HEIGHT);
				uiRenderer.renderFont(Integer.toString(activeInventoryDrag.itemAmount), adjMouseX + (slotSize-4), adjMouseY + (slotSize-4), UIRenderer.TextAlignment.RIGHT);
			}

			uiRenderer.end();

			glDepthMask(true);
		}
	}
}
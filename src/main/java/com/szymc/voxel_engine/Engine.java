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

	public record InventoryActiveItem(byte item, byte itemAmount, int ogSlotX, int ogSlotY, int inventoryIndex, byte[] typeSource, byte[] amountSource) {}
	private InventoryActiveItem activeInventoryDrag = null;
	public boolean inCraftingTable = true;
	public void flushCraftingGui() {
		byte[] cTypes = player.getCraftingInv();
		byte[] cAmounts = player.getCraftingAmounts();
		byte[] iTypes = player.getInventory();
		byte[] iAmounts = player.getInventoryAmounts();

		for (int slot = 0; slot < (inCraftingTable ? 9 : 4); slot++) {
			if (!inCraftingTable) {
				cTypes = iTypes;
				cAmounts = iAmounts;
			}

			int cAccessOffset = inCraftingTable ? 0 : 36;

			if (cTypes[cAccessOffset + slot] != 0) {
				int inventoryIndex = 0;
				while (cAmounts[cAccessOffset + slot] > 0) {
					if (iAmounts[inventoryIndex] == 0 || iTypes[inventoryIndex] == cTypes[cAccessOffset + slot]) {
						int maxAbleToApply = 64-iAmounts[inventoryIndex];
						int beingApplied = Math.min(cAmounts[cAccessOffset + slot], maxAbleToApply);
						iTypes[inventoryIndex] = cTypes[cAccessOffset + slot];
						cAmounts[cAccessOffset + slot] -= (byte) beingApplied;
						iAmounts[inventoryIndex] += (byte) beingApplied;

						if (cAmounts[cAccessOffset + slot] <= 0) cTypes[cAccessOffset + slot] = (byte)0;
					}

					if (inventoryIndex >= 36) {
						requestDropInvIndex((byte)slot, cTypes, cAmounts, cAmounts[cAccessOffset + slot]);
						break;
					}

					inventoryIndex++;
				}
			}
		}
	}

	// Can be called directly for non drag through keyboard (q, ctrl+q) drops
	public void requestDropInvIndex(byte invIndex, byte[] typeSource, byte[] amountSource, int decrementAmount) {
		byte type = activeInventoryDrag != null ? activeInventoryDrag.item : typeSource[invIndex];
		if (type == 0) return;

		byte amount = activeInventoryDrag != null ? activeInventoryDrag.itemAmount : amountSource[invIndex];
		int clampedNewAmount = Math.max(0, amount - decrementAmount);

		if (invIndex != -1) setInventorySlot(typeSource, amountSource, invIndex, (amount-decrementAmount > 0 ? type : (byte)0), (byte)clampedNewAmount);

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
		System.out.println(activeInventoryDrag);
		requestDropInvIndex(inventoryIndex, activeInventoryDrag.typeSource, activeInventoryDrag.amountSource, activeInventoryDrag.itemAmount);

		activeInventoryDrag = null;
	}

	private static void setInventorySlot(byte[] typeSource, byte[] amountSource, byte ind, byte type, byte amount) {
		typeSource[ind] = type;
		amountSource[ind] = amount;
	}

	private void updateCraftResult() {
		int tabSize = inCraftingTable ? 2 : 1;
		byte resultInd = (byte) (inCraftingTable ? 9 : 40);
		byte[] relevantCtype = inCraftingTable ? player.getCraftingInv() : player.getInventory();
		byte[] relevantCamount = inCraftingTable ? player.getCraftingAmounts() : player.getInventoryAmounts();

		String tableInput = extractCraftInput(relevantCtype, relevantCamount, inCraftingTable ? 0 : 36, tabSize, tabSize);
		if (tableInput == null) {
			setInventorySlot(relevantCtype, relevantCamount, resultInd, (byte)0, (byte)0);
			return;
		}
		System.out.println("passed check here");
		tableInput = tableInput.trim();

		short result = Texture.craftingRecipes.getShort(tableInput);
		byte blockIdMade = (byte) (result & 0xFF);
		byte amount = (byte) ((result >>> 8) & 0xFF);

		if (result != 0) {
			setInventorySlot(relevantCtype, relevantCamount, resultInd, blockIdMade, amount);
		} else {
			setInventorySlot(relevantCtype, relevantCamount, resultInd, (byte)0, (byte)0);
		}
	}

	public void setActiveInventoryDrag(InventoryActiveItem itemData, boolean isLeftClick, boolean isShiftClick) {
		if (itemData == null) {
			activeInventoryDrag = null;
			return;
		} else if (activeInventoryDrag != null && ((itemData.inventoryIndex == 40 && itemData.typeSource == player.getInventory()) || (itemData.inventoryIndex == 9 && itemData.typeSource == player.getCraftingInv()))) {
			if (activeInventoryDrag.item == itemData.item) {
				int availableToBeCrafted = -1;
				for (byte i = (byte) (inCraftingTable ? 0 : 36); i < (inCraftingTable ? 9 : 40); i++) {
					availableToBeCrafted = Math.max(itemData.amountSource[i], availableToBeCrafted);
				}

				if (availableToBeCrafted == 1 || isShiftClick) setInventorySlot(itemData.typeSource, itemData.amountSource, (byte) (inCraftingTable ? 9 : 40), (byte)0, (byte)0);
				availableToBeCrafted = Math.min(availableToBeCrafted, isShiftClick ? 64 : 1); // clamp

				for (byte i = (byte) (inCraftingTable ? 0 : 36); i < (inCraftingTable ? 9 : 40); i++) {
					setInventorySlot(itemData.typeSource, itemData.amountSource, i, itemData.typeSource[i], (byte) (itemData.amountSource[i]-availableToBeCrafted));
					if (itemData.amountSource[i] <= 0) {
						itemData.typeSource[i] = 0;
						itemData.amountSource[i] = 0;
					}
				}

				this.activeInventoryDrag = new InventoryActiveItem(itemData.item, (byte) (activeInventoryDrag.itemAmount + itemData.itemAmount*availableToBeCrafted), itemData.ogSlotX, itemData.ogSlotY, itemData.inventoryIndex, itemData.typeSource, itemData.amountSource);
				return;
			}
			return;
		}

		if (activeInventoryDrag == null && !isLeftClick && itemData.item != 0 && !((itemData.inventoryIndex == 40 && itemData.typeSource == player.getInventory()) || (itemData.inventoryIndex == 9 && itemData.typeSource == player.getCraftingInv()))) {
			if (itemData.itemAmount <= 1) return;
			int amountTaken = itemData.itemAmount/2;
			int remaining = itemData.itemAmount-amountTaken;

			activeInventoryDrag = new InventoryActiveItem(itemData.item, (byte) amountTaken, itemData.ogSlotX, itemData.ogSlotY, -1, itemData.typeSource, itemData.amountSource);
			setInventorySlot(itemData.typeSource, itemData.amountSource, (byte)itemData.inventoryIndex, itemData.item, (byte) remaining);

			return;
		} else if (activeInventoryDrag != null && !isLeftClick && (itemData.item == activeInventoryDrag.item || itemData.item == 0)) {
			if (itemData.itemAmount == 64) return;

			byte savedType = activeInventoryDrag.item;
			if (activeInventoryDrag.itemAmount > 1) {
				activeInventoryDrag = new InventoryActiveItem(activeInventoryDrag.item, (byte) (activeInventoryDrag.itemAmount-1), activeInventoryDrag.ogSlotX, activeInventoryDrag.ogSlotY, activeInventoryDrag.inventoryIndex, activeInventoryDrag.typeSource, activeInventoryDrag.amountSource);
			} else {
				activeInventoryDrag = null;
			}

			setInventorySlot(itemData.typeSource, itemData.amountSource, (byte)itemData.inventoryIndex, savedType, (byte) (itemData.itemAmount+1));
			updateCraftResult();
			return;
		}

		if (activeInventoryDrag != null && isLeftClick) {
			if (itemData.inventoryIndex == activeInventoryDrag.inventoryIndex) {
				setInventorySlot(activeInventoryDrag.typeSource, activeInventoryDrag.amountSource, (byte)activeInventoryDrag.inventoryIndex, activeInventoryDrag.item, activeInventoryDrag.itemAmount);
				activeInventoryDrag = null;
				return;
			}

			if (itemData.item == 0 || (activeInventoryDrag.item == itemData.item && itemData.itemAmount < 64)) {
				byte currentInvAmount = itemData.itemAmount;
				int maxAmount = 64-currentInvAmount;
				int beingApplied = Math.min(activeInventoryDrag.itemAmount, maxAmount);
				int leftOver = activeInventoryDrag.itemAmount-beingApplied;

				setInventorySlot(itemData.typeSource, itemData.amountSource, (byte) itemData.inventoryIndex, activeInventoryDrag.item, (byte)(currentInvAmount+beingApplied));

				if (leftOver > 0) {
					activeInventoryDrag = new InventoryActiveItem(activeInventoryDrag.item, (byte)leftOver, activeInventoryDrag.ogSlotX, activeInventoryDrag.ogSlotY, activeInventoryDrag.inventoryIndex, activeInventoryDrag.typeSource, activeInventoryDrag.amountSource);
				} else activeInventoryDrag = null;
			} else if ((activeInventoryDrag.item != itemData.item || itemData.itemAmount == 64) && !((activeInventoryDrag.inventoryIndex == 40 && activeInventoryDrag.typeSource == player.getInventory()) || (activeInventoryDrag.inventoryIndex == 9 && activeInventoryDrag.typeSource == player.getCraftingInv()))) {
				setInventorySlot(itemData.typeSource, itemData.amountSource, (byte) itemData.inventoryIndex, activeInventoryDrag.item, activeInventoryDrag.itemAmount);

				activeInventoryDrag = new InventoryActiveItem(itemData.item, itemData.itemAmount, activeInventoryDrag.ogSlotX, activeInventoryDrag.ogSlotY, activeInventoryDrag.inventoryIndex, activeInventoryDrag.typeSource, activeInventoryDrag.amountSource);
			}

			updateCraftResult();
			return;
		}

		if (itemData != null && itemData.item != 0 && isLeftClick) {
			if ((itemData.inventoryIndex == 40 && itemData.typeSource == player.getInventory()) || (itemData.inventoryIndex == 9 && itemData.typeSource == player.getCraftingInv())) {
				int availableToBeCrafted = 999;
				for (byte i = (byte) (inCraftingTable ? 0 : 36); i < (inCraftingTable ? 9 : 40); i++) {
					if (itemData.amountSource[i] > 0) availableToBeCrafted = Math.min(itemData.amountSource[i], availableToBeCrafted);
				}

				if (availableToBeCrafted == 1 || isShiftClick) setInventorySlot(itemData.typeSource, itemData.amountSource, (byte) (inCraftingTable ? 9 : 40), (byte)0, (byte)0);
				availableToBeCrafted = Math.min(availableToBeCrafted, isShiftClick ? 64 : 1); // clamp

				for (byte i = (byte) (inCraftingTable ? 0 : 36); i < (inCraftingTable ? 9 : 40); i++) {
					setInventorySlot(itemData.typeSource, itemData.amountSource, i, itemData.typeSource[i], (byte) (itemData.amountSource[i]-availableToBeCrafted));
					if (itemData.amountSource[i] <= 0) {
						itemData.typeSource[i] = 0;
						itemData.amountSource[i] = 0;
					}
				}

				this.activeInventoryDrag = new InventoryActiveItem(itemData.item, (byte) (itemData.itemAmount*availableToBeCrafted), itemData.ogSlotX, itemData.ogSlotY, itemData.inventoryIndex, itemData.typeSource, itemData.amountSource);
				return;
			}

			this.activeInventoryDrag = new InventoryActiveItem(itemData.item, itemData.itemAmount, itemData.ogSlotX, itemData.ogSlotY, -1, itemData.typeSource, itemData.amountSource);
			setInventorySlot(itemData.typeSource, itemData.amountSource, (byte) itemData.inventoryIndex, (byte)0, (byte)0);
		};
	}

	public String extractCraftInput(byte[] iTypes, byte[] iAmounts, int indOffset, int boundXind, int boundYind) {
		StringBuilder result = new StringBuilder();
		int minX = 999; int maxX = -999;
		int minY = 999; int maxY = -999;
		boolean isEmpty = true;

		//System.out.println(Arrays.toString(iTypes));
		//System.out.println("Range/Last index: " + boundXind + ", " + boundYind);
		for (int y = boundYind; y >= 0; y--) {
			for (int x = 0; x < boundXind+1; x++) {
				byte ind = (byte) (indOffset + x*(boundXind+1)+y);
				byte itemType = iTypes[ind];

				if (itemType != 0) {
					isEmpty = false;
					minX = Math.min(x, minX);
					maxX = Math.max(x, maxX);
					minY = Math.min(y, minY);
					maxY = Math.max(y, maxY);
				}
			}
		}

		if (isEmpty) return null;

		for (int y = maxY; y >= minY; y--) {
			for (int x = minX; x < maxX+1; x++) {
				byte ind = (byte) (indOffset + x*(boundXind+1)+y);
				byte itemType = iTypes[ind];

				result.append(itemType);
				result.append('.');
			}
			result.append('/');
		}

		System.out.printf("xMin: %d, xMax: %d, yMin: %d, yMax: %d\n", minX, maxX, minY, maxY);
		System.out.println(result);

		return result.toString();
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
		Texture.readInCraftingJson("gameData/craftingRecipes.json");
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

				int xPos = (int) Math.floor(entity.position.x);
				int yPos = (int) Math.floor(entity.position.y);
				int zPos = (int) Math.floor(entity.position.z);

				ChunkColumn eChunk = worldScene.getLoadedChunkAtPos(xPos>>5, zPos>>5);
				byte skyLevel = eChunk.getSkylight(xPos&31, yPos, zPos&31);
				byte blockLevel = eChunk.getBlockLight(xPos&31, yPos, zPos&31);

				entityShader.setLightLevel((byte) Math.max(skyLevel, blockLevel));

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
						float slotX = offsetX + (slotSize * ix) + 2;
						float slotY = invPosY + (slotSize * iy) + 2 + topAreaSize + (iy == 3 ? hotbarGap : 0);

						uiRenderer.drawRect(slotX, slotY, slotSize - 4, slotSize - 4, 0.5f, 0.5f, 0.5f, 1.0f);
						int ind = (3-iy)*9 + ix;

						if (inventory[ind] != 0) uiRenderer.drawIcon(inventory[ind], slotX, slotY, slotSize-4, slotSize-4);
					}
				}

				uiRenderer.beginTextRendering(App.WINDOW_WIDTH, App.WINDOW_HEIGHT);
				for (int iy = 0; iy < 4; iy++) {
					for (int ix = 0; ix < 9; ix++) {
						int slotX = offsetX + (slotSize * ix) + 2;
						int slotY = (int) (invPosY + (slotSize * iy) + 2 + topAreaSize + (iy == 3 ? hotbarGap : 0));

						int ind = (3-iy)*9 + ix;
						byte amount = player.readInventoryAmount((byte) ind);

						if (amount != 0) {
							uiRenderer.renderFont(Integer.toString(amount), slotX+slotSize-4, slotY+slotSize-4, UIRenderer.TextAlignment.RIGHT);
						};
					}
				}

				if (!inCraftingTable) {
					int craftXpos = offsetX-2 + (slotSize * 9 + 4) - 2*slotSize - 155;
					int craftYpos = (int) (invPosY + 65);

					uiRenderer.beginUiRendering(App.WINDOW_WIDTH, App.WINDOW_HEIGHT);
					uiRenderer.drawRect(craftXpos, craftYpos, 2*slotSize+4, 2*slotSize, 0.6f, 0.6f, 0.6f, 1.0f);
					int resultSlotX = craftXpos+2*slotSize+75;
					int resultSlotY = (int) (craftYpos + slotSize / 2f);

					uiRenderer.drawRect(resultSlotX, resultSlotY, slotSize, slotSize, 0.5f, 0.5f, 0.5f, 1.0f);

					for (int x = 0; x <= 1; x++) {
						for (int y = 0; y <= 1; y++) {
							int subSlotPosX = craftXpos + slotSize*x + 2;
							int subSlotPosY = craftYpos + slotSize*y + 2;
							byte index = (byte) (36 + x*2 + (1-y));

							uiRenderer.drawRect(subSlotPosX, subSlotPosY, slotSize-4, slotSize-4, 0.5f, 0.5f, 0.5f, 1.0f);
							if (player.readInventoryType(index) != 0) {
								uiRenderer.drawIcon(player.readInventoryType(index), subSlotPosX, subSlotPosY, slotSize-4, slotSize-4);
							}
						}
					}

					if (player.readInventoryType((byte)40) != 0) {
						uiRenderer.drawIcon(player.readInventoryType((byte)40), resultSlotX+2, resultSlotY+2, slotSize-4, slotSize-4);
					}

					uiRenderer.beginTextRendering(App.WINDOW_WIDTH, App.WINDOW_HEIGHT);
					for (int x = 0; x <= 1; x++) {
						for (int y = 0; y <= 1; y++) {
							int subSlotPosX = craftXpos + slotSize*x + 2;
							int subSlotPosY = craftYpos + slotSize*y + 2;
							byte index = (byte) (36 + x*2 + (1-y));

							if (player.readInventoryType(index) != 0) {
								uiRenderer.renderFont(Integer.toString(player.readInventoryAmount(index)), subSlotPosX+slotSize-4, subSlotPosY+slotSize-4, UIRenderer.TextAlignment.RIGHT);
							}
						}
					}

					if (player.readInventoryType((byte)40) != 0) {
						uiRenderer.renderFont(Integer.toString(player.readInventoryAmount((byte)40)), resultSlotX+slotSize-4, resultSlotY+slotSize-4, UIRenderer.TextAlignment.RIGHT);
					}
				} else {
					int craftTableXpos = offsetX-2 + (slotSize*9 + 4)/2 - (3*slotSize)/2;
					int craftTableYpos = (int) (invPosY + topAreaSize/2f - (3*slotSize)/2f);
					int resultX = craftTableXpos + 4*slotSize;
					int resultY = craftTableYpos + (3*slotSize)/2 - slotSize/2;

					uiRenderer.beginUiRendering(App.WINDOW_WIDTH, App.WINDOW_HEIGHT);
					uiRenderer.drawRect(craftTableXpos, craftTableYpos, 3*slotSize, 3*slotSize, 0.5f, 0.5f, 0.5f, 1.0f);
					uiRenderer.drawRect(resultX, resultY, slotSize, slotSize, 0.8f, 0.8f, 0.8f, 1.0f);

					byte[] cTypes = player.getCraftingInv();
					byte[] cAmounts = player.getCraftingAmounts();

					for (int y = 2; y >= 0; y--) {
						for (int x = 0; x < 3; x++) {
							int slotX = craftTableXpos + x*slotSize + 2;
							int slotY = craftTableYpos + y*slotSize + 2;
							int ind = x*3+y;

							uiRenderer.drawRect(slotX, slotY, slotSize-4, slotSize-4, 0.4f, 0.4f, 0.4f, 1.0f);
							if (cTypes[ind] != 0) uiRenderer.drawIcon(cTypes[ind], slotX, slotY, slotSize-4, slotSize-4);
						}
					}

					if (cTypes[9] != 0) {
						uiRenderer.drawIcon(cTypes[9], resultX+2, resultY+2, slotSize-4, slotSize-4);
					}

					uiRenderer.beginTextRendering(App.WINDOW_WIDTH, App.WINDOW_HEIGHT);
					for (int y = 2; y >= 0; y--) {
						for (int x = 0; x < 3; x++) {
							int slotX = craftTableXpos + x*slotSize + 2;
							int slotY = craftTableYpos + y*slotSize + 2;
							int ind = x*3+y;

							if (cTypes[ind] != 0) uiRenderer.renderFont(Integer.toString(cAmounts[ind]), slotX+slotSize-4, slotY+slotSize-4, UIRenderer.TextAlignment.RIGHT);
						}
					}

					if (cTypes[9] != 0) {
						uiRenderer.renderFont(Integer.toString(cAmounts[9]), resultX+slotSize-4, resultY+slotSize-4, UIRenderer.TextAlignment.RIGHT);
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
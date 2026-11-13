package com.szymc.voxel_engine;
import com.szymc.gui.Screen;
import com.szymc.localShaders.EntityShader;
import com.szymc.localShaders.OutlineShader;


import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL32.*;
import static org.lwjgl.opengl.GL46.*;

import it.unimi.dsi.fastutil.BigArrays;
import it.unimi.dsi.fastutil.longs.Long2ObjectMaps;
import org.lwjgl.BufferUtils;
import org.lwjgl.system.MemoryStack;


import com.szymc.localShaders.WorldShader;


import static org.lwjgl.system.MemoryStack.*;


import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.util.*;


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
	private Screen activeGuiScreen;

	public void setActiveGuiScreen(Screen screen) {
		if (activeGuiScreen != null) {
			activeGuiScreen.removal();
			activeGuiScreen = null;
		}

		if (screen == null) return;

		screen.init();
		activeGuiScreen = screen;
	}

	public Screen getActiveGuiScreen() {
		return this.activeGuiScreen;
	}

	public void handleGuiMouseClick(int mouseX, int mouseY, int button, int mods) {
		if (activeGuiScreen != null) {
			activeGuiScreen.mouseClick(mouseX, mouseY, button, mods);
		}
	}

	// Engine is begging for a refactor bro, fix ts, maybe consider moving the ui to another class

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
		short blockAt = worldScene.getLoadedChunkAtPos(x>>5, z>>5).getBlockInChunk(x&31, y, z&31);
		boolean isNewBlock = currentlyMiningState != null && (x != currentlyMiningState.wx || y != currentlyMiningState.wy || z != currentlyMiningState.wz);
		if (isNewBlock || (currentlyMiningState == null && isLeftMouseHeld)) {
			currentlyMiningState = new BlockMineState(x, y, z, Texture.hardnessLevels[blockAt], Texture.hardnessLevels[blockAt], blockAt);
		}
    }

	public void setPlayer(PlayerCharacter player) {
		this.player = player;
	}

	public static boolean wireframeMode = false;
    public static final class BlockMineState {
        public int wx; public int wy; public int wz;
		public float health; public float maxHealth; public short block;

        public BlockMineState(int wx, int wy, int wz, float health, float maxHealth, short block) {
            this.wx = wx;
            this.wy = wy;
            this.wz = wz;
            this.health = health;
            this.maxHealth = maxHealth;
			this.block = block;
        }
    }

	private BlockMineState currentlyMiningState = null;
	private boolean isLeftMouseHeld = false;
	public void startMining(int wx, int wy, int wz) {
		short blockAt = worldScene.getLoadedChunkAtPos(wx>>5, wz>>5).getBlockInChunk(wx&31, wy, wz&31);
		currentlyMiningState = new BlockMineState(wx, wy, wz, Texture.hardnessLevels[blockAt], Texture.hardnessLevels[blockAt], blockAt);
		isLeftMouseHeld = true;
	}

	public void leftMouseHeldTick() {
		if (currentlyMiningState != null) {
			short equipped = player.getInventory().readType(player.currentHotbarSlot);
			float strength = 0.085f * ((Texture.toolType[equipped] != 0 && Texture.toolType[equipped] == Texture.idealToolType[currentlyMiningState.block]) ? Texture.toolTargetedDamage[equipped] : 1);

			currentlyMiningState.health -= strength;

			if (currentlyMiningState.health < 0) {
				ChunkColumn chunk = worldScene.getLoadedChunkAtPos(currentlyMiningState.wx>>5, currentlyMiningState.wz>>5);
				int x = currentlyMiningState.wx; int y = currentlyMiningState.wy; int z = currentlyMiningState.wz;
				short block = chunk.getBlockInChunk(x&31, y, z&31);
				chunk.setBlockInChunk(x & 31, y, z & 31, Blocks.AIR, (short) 0);
				worldScene.spawnNewItemEntity(Texture.customBlockDrop[block] == 0 ? block : Texture.customBlockDrop[block], x, y, z, false);
				chunk.setSectionDirty(y >> 4);
				chunk.playerModification();

				worldScene.updateChunk(currentlyMiningState.wx>>5, y, currentlyMiningState.wz>>5, x&31, z&31, false, block);
				currentlyMiningState = null;
			}
		}
	}

	public void mouseReleased() {
		currentlyMiningState = null;
		isLeftMouseHeld = false;
	}

	private MeshArena meshArena;
	public Engine(World world, Camera camera, MeshArena meshArena) {
		this.worldScene = world;
		this.camera = camera;
		this.meshArena = meshArena;

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
		Entity.initializeEABCache();
		EntityItem.generateStaticItemMeshes();
		BlockEntity.generateBlockEntityMeshes();

		try {
			uiRenderer.loadFont("/fonts/mainFont.ttf");
			uiRenderer.prepareFontRendering();
		} catch (IOException e) {
			e.printStackTrace();
		}

		//glPolygonMode(GL_FRONT_AND_BACK, GL_LINE);
		glLineWidth(2);
		//glEnable(GL11.GL_BLEND);

		createCPUBuffers(10000);
		createDrawBuffers(10000);
	}

	private Vector3f tempModel = new Vector3f();
	private Matrix4f modelVec = new Matrix4f();

	private record IndirectDrawCommand(
			int count,
			int instanceCount,
			int firstIndex,
			int baseVertex,
			int baseInstance
	) {
		public static final int SIZE_BYTES = 5 * Integer.BYTES;
	}

	public static IndirectDrawCommand from(SectionMeshDrawCommand mesh) {
		return new IndirectDrawCommand(
				(int) mesh.indexRange.count(),
				1,
				(int) (mesh.indexRange.valueByteOffset() / Integer.BYTES),
				(int) (mesh.vertexRange.valueByteOffset() / 8),
				0
		);
	}

	private final class MeshPageBatch {
		public final int pageIndex;

		public final List<IndirectDrawCommand> commands =
				new ArrayList<>();

		public final List<Matrix4f> models =
				new ArrayList<>();

		public MeshPageBatch(int pageIndex) {
			this.pageIndex = pageIndex;
		}

		public void clear() {
			commands.clear();
			models.clear();
		}

		public void add(
				SectionMeshDrawCommand mesh,
				Matrix4f model
		) {
			commands.add(from(mesh));
			models.add(new Matrix4f(model));
		}
	}

	private ByteBuffer commandData;
	private FloatBuffer modelData;
	private void createCPUBuffers(int maxDraws) {
		commandData = BufferUtils.createByteBuffer(maxDraws * IndirectDrawCommand.SIZE_BYTES);

		modelData = BufferUtils.createFloatBuffer(maxDraws * 16);
	}

	private int indirectBuffer;
	private int modelBuffer;

	private void createDrawBuffers(int maxDraws) {
		indirectBuffer = glGenBuffers();
		glBindBuffer(GL_DRAW_INDIRECT_BUFFER, indirectBuffer);
		glBufferData(GL_DRAW_INDIRECT_BUFFER, (long)maxDraws * IndirectDrawCommand.SIZE_BYTES, GL_STREAM_DRAW);

		modelBuffer = glGenBuffers();
		glBindBuffer(GL_SHADER_STORAGE_BUFFER, modelBuffer);
		glBufferData(GL_SHADER_STORAGE_BUFFER, (long)maxDraws * 16 * Float.BYTES, GL_STREAM_DRAW);

		glBindBuffer(GL_DRAW_INDIRECT_BUFFER, 0);
		glBindBuffer(GL_SHADER_STORAGE_BUFFER, 0);
	}

	private void uploadBatch(MeshPageBatch batch) {
		commandData.clear();
		modelData.clear();

		for (int i = 0; i < batch.commands.size(); i++) {
			IndirectDrawCommand cmd = batch.commands.get(i);

			commandData.putInt(cmd.count());
			commandData.putInt(cmd.instanceCount());
			commandData.putInt(cmd.firstIndex());
			commandData.putInt(cmd.baseVertex());
			commandData.putInt(cmd.baseInstance());

			batch.models.get(i).get(modelData);
			modelData.position(modelData.position() + 16);
		}

		commandData.flip();
		modelData.flip();

		glBindBuffer(GL_DRAW_INDIRECT_BUFFER, indirectBuffer);
		glBufferSubData(GL_DRAW_INDIRECT_BUFFER, 0, commandData);

		glBindBuffer(GL_SHADER_STORAGE_BUFFER, modelBuffer);
		glBufferSubData(GL_SHADER_STORAGE_BUFFER, 0, modelData);

		glBindBufferBase(GL_SHADER_STORAGE_BUFFER, 0, modelBuffer);
	}

	private Map<Integer, MeshPageBatch> batches = new HashMap<>();

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
			glEnable(GL_DEPTH_TEST);
			glEnable(GL_CULL_FACE);

			debugger.renderDebug(matrixBuffer);
			mainShader.start();

			for (MeshPageBatch batch : batches.values()) {
				batch.clear();
			}

			Long2ObjectMaps.fastForEach(worldScene.getRendered(), entry -> {
				ChunkColumn chunk = entry.getValue();
				if (chunk == null) return;

				if (!camera.frustumInt.testAab(chunk.getWorldX()*32, 0, chunk.getWorldZ()*32, chunk.getWorldX()*32+32, 256, chunk.getWorldZ()*32+32)) {
					return;
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

					tempModel.set((float) minX, (float) minY, (float) minZ);
					modelVec.translation(tempModel);

					SectionMeshDrawCommand m = section.getMesh();
					MeshPageBatch batch =  batches.computeIfAbsent(m.pageIndex, MeshPageBatch::new);

					batch.add(m, new Matrix4f().translation(minX, minY, minZ));
				}
			});

			for (MeshPageBatch batch : batches.values()) {
				if (batch.commands.isEmpty()) continue;
				MeshPage page = meshArena.getPage(batch.pageIndex);
				uploadBatch(batch);
				glBindVertexArray(page.getVao());

				glMultiDrawElementsIndirect(
						GL_TRIANGLES,
						GL_UNSIGNED_INT,
						0L,
						batch.commands.size(),
						0
				);

				glBindVertexArray(0);
				glBindBuffer(GL_DRAW_INDIRECT_BUFFER, 0);
				glBindBuffer(GL_SHADER_STORAGE_BUFFER, 0);
			}

			mainShader.stop();
			entityShader.start();
			entityShader.setCamera(camera.getProjectionMatrix(), camera.getViewMatrix(), matrixBuffer);

			glBindVertexArray(Entity.getVao());
			for (Map.Entry<UUID, Entity> entry : worldScene.getEntities().entrySet()) {
				Entity entity = entry.getValue();

				tempModel.set(entity.renderPosition.x, entity.renderPosition.y, entity.renderPosition.z);
				modelVec.translation(tempModel);
				entityShader.setModel(modelVec, matrixBuffer);

				int xPos = (int) Math.floor(entity.position.x);
				int yPos = (int) Math.floor(entity.position.y);
				int zPos = (int) Math.floor(entity.position.z);

				ChunkColumn eChunk = worldScene.getLoadedChunkAtPos(xPos>>5, zPos>>5);
				if (eChunk == null) continue;
				boolean hasLightData = eChunk.state.isAtleast(ChunkColumn.ChunkState.LIGHT);

				if (yPos < 0) continue;

				if (!(entity instanceof BlockEntity)) {
					if (hasLightData) {
						byte skyLevel = eChunk.getSkylight(xPos & 31, yPos, zPos & 31);
						byte blockLevel = eChunk.getBlockLight(xPos & 31, yPos, zPos & 31);
						byte maxLevel = (byte) Math.max(skyLevel, blockLevel);
						entity.lastLightLevel = maxLevel;
						entityShader.setLightLevel(maxLevel);
					} else {
						entityShader.setLightLevel(entity.lastLightLevel);
					}
				} else {
					entityShader.setLightLevel((byte) 15);
				}

				if (entity.itemMesh == null) {
					System.out.println(entity);
					throw new NullPointerException("Entity itemMesh is null; cannot render frame.");
				}

				glDrawElementsBaseVertex(GL_TRIANGLES, entity.itemMesh.indexCount, GL_UNSIGNED_INT, entity.itemMesh.byteOffset, entity.itemMesh.baseVertex);
			}

			entityShader.stop();
			mainShader.start();

			glEnable(GL_BLEND);
			glDepthMask(true);

//			Long2ObjectMaps.fastForEach(worldScene.getRendered(), entry -> {
//				ChunkColumn chunk = entry.getValue();
//				if (chunk == null) return;
//
//				if (!camera.frustumInt.testAab(chunk.getWorldX()*32, 0, chunk.getWorldZ()*32, chunk.getWorldX()*32+32, 256, chunk.getWorldZ()*32+32)) {
//					return;
//				}
//
//				for (int s = 0; s < 16; s++) {
//					ChunkSection section = chunk.getSection(s);
//					if (section == null) continue;
//					if (section.getWaterMesh() == null) continue;
//
//					int minX = section.getWorldX();
//					int minY = section.getWorldY();
//					int minZ = section.getWorldZ();
//					int maxX = section.getWorldX() + 32;
//					int maxY = section.getWorldY() + 16;
//					int maxZ = section.getWorldZ() + 32;
//
//
//					if (!camera.frustumInt.testAab(minX, minY, minZ, maxX, maxY, maxZ)) {
//						continue;
//					}
//
//					tempModel.set((float) minX, (float) minY, (float) minZ);
//					modelVec.translation(tempModel);
//					mainShader.setModel(modelVec, matrixBuffer);
//
//					SectionMeshDrawCommand m = section.getWaterMesh();
//					MeshPage page = meshArena.getPage(m.pageIndex);
//					glBindVertexArray(page.getVao());
//
//					int baseVertex = (int) (
//							m.vertexRange.valueByteOffset() / (2L * Integer.BYTES)
//					);
//
//					glDrawElementsBaseVertex(GL_TRIANGLES, (int)m.indexRange.count(), GL_UNSIGNED_INT, m.indexRange.valueByteOffset(), baseVertex);
//				}
//			});

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

			uiRenderer.beginUiRendering(App.WINDOW_WIDTH, App.WINDOW_HEIGHT);
			float crossX = (App.WINDOW_WIDTH/2.0f) - 8.0f;
			float crossY = (App.WINDOW_HEIGHT/2.0f) - 8.0f;

			uiRenderer.drawTexture(crosshairTexture, crossX, crossY, 16, 16);
			int slotSize = 64;
			int offsetX = (int)((App.WINDOW_WIDTH/2.0f)-(slotSize*4.5f));
			uiRenderer.drawRect(offsetX-2, App.WINDOW_HEIGHT - 80-2, slotSize*9 + 4, slotSize + 4, 0.7f, 0.7f, 0.7f, 0.8f);
			short[] inventory = player.getInventory().getRawTypes();

			for (int i = 0; i < 9; i++) {
				float color = player.currentHotbarSlot == i ? 0.45f : 0.2f;
				uiRenderer.drawRect(offsetX + (slotSize*i) + 2, App.WINDOW_HEIGHT - 80+2, slotSize-4, slotSize-4, color, color, color, 0.6f);
				short item = inventory[i];
				if (item != 0) uiRenderer.drawIcon(item, offsetX + (slotSize*i), App.WINDOW_HEIGHT - 80, slotSize, slotSize);
			}

			uiRenderer.beginTextRendering(App.WINDOW_WIDTH, App.WINDOW_HEIGHT);
			for (byte i = 0; i < 9; i++) {
				if (player.getInventory().readType(i) != 0) {
					uiRenderer.renderFont(Integer.toString(player.getInventory().readAmount(i)), offsetX + (slotSize*i) + (slotSize-4), App.WINDOW_HEIGHT - 80-4+slotSize, UIRenderer.TextAlignment.RIGHT);
				}
			}

			if (activeGuiScreen != null) {
				activeGuiScreen.render(uiRenderer, mouseX, mouseY);
			}

			glDepthMask(true);
		}
	}
}
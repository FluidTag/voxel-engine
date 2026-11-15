package com.szymc.voxel_engine;
import static com.szymc.voxel_engine.FreeRangeMeshAllocator.AllocatorSegment;

public class SectionMeshDrawCommand {
	public AllocatorSegment vertexRange;
	public AllocatorSegment indexRange;
	public int pageIndex;
	public MeshPage.PageFence lastUseFence;
	private MeshPage page;

	public SectionMeshDrawCommand(AllocatorSegment vertexRange, AllocatorSegment indexRange, MeshPage pageUsed) {
		this.vertexRange = vertexRange;
		this.indexRange = indexRange;
		this.pageIndex = pageUsed.getPageIndex();
		this.page = pageUsed;
		this.lastUseFence = null;
	}

	public void markUsed(MeshPage.PageFence newFence) {
		if (newFence == lastUseFence) {
			return;
		}

		if (newFence != null) {
			newFence.retain();
		}

		if (lastUseFence != null) {
			page.releaseFenceReference(lastUseFence);
		}

		this.lastUseFence = newFence;
	}

	public void clearLastUsedFence() {
		if (lastUseFence != null) {
			page.releaseFenceReference(lastUseFence);
			lastUseFence = null;
		}
	}
}
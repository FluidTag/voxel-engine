package com.szymc.voxel_engine;
import static com.szymc.voxel_engine.FreeRangeMeshAllocator.AllocatorSegment;

public class SectionMeshDrawCommand {
	public AllocatorSegment vertexRange;
	public AllocatorSegment indexRange;
	public int pageIndex;

	public SectionMeshDrawCommand(AllocatorSegment vertexRange, AllocatorSegment indexRange, int pageIndex) {
		this.vertexRange = vertexRange;
		this.indexRange = indexRange;
		this.pageIndex = pageIndex;
	}
}
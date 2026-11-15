package com.szymc.voxel_engine;
import java.nio.IntBuffer;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL15.*; // VBO functions (glGenBuffers)
import static org.lwjgl.opengl.GL20.*; // Shader/Attribute functions (glVertexAttribPointer)
import static org.lwjgl.opengl.GL30.*; // VAO functions (glGenVertexArrays)
import static org.lwjgl.opengl.GL44.*;
import static com.szymc.voxel_engine.FreeRangeMeshAllocator.AllocatorSegment;

public class MeshPage {
    private int vao, vbo, ebo;
    private final int pageIndex;
    private final int chunksAllowed;
    private PageFence lastFenceUsed = null;
    public class PageFence {
        public long fenceId;
        public int references = 0;
        public PageFence() {
            this.fenceId = glFenceSync(GL_SYNC_GPU_COMMANDS_COMPLETE, 0);

            if (fenceId == 0L) {
                throw new IllegalStateException("Failed to create GPU fence");
            }
        }

        public void retain() {
            references++;
        }
    };

    private record RetiredMesh(AllocatorSegment vertexRange, AllocatorSegment indexRange, PageFence fence) {};

    private FreeRangeMeshAllocator vertexSpaceAllocator;
    private FreeRangeMeshAllocator indexSpaceAllocator;

    private IntBuffer mappedVBOByteBuffer, mappedEBOByteBuffer;

    public MeshPage(int pageIndex, int chunksAllowed) {
        this.pageIndex = pageIndex;
        this.chunksAllowed = chunksAllowed;

        int allocationByteSize = (chunksAllowed * (32*32*32)) * 2 * Integer.BYTES; // For the entire range
        this.vertexSpaceAllocator = new FreeRangeMeshAllocator(allocationByteSize, 8);
        this.indexSpaceAllocator = new FreeRangeMeshAllocator(allocationByteSize, 4);
        System.out.println("Page " + pageIndex + " meshPage created.");

        vao = glGenVertexArrays();
        glBindVertexArray(vao);

        vbo = glGenBuffers();
        int flags = GL_MAP_WRITE_BIT | GL_MAP_PERSISTENT_BIT | GL_MAP_COHERENT_BIT;
        glBindBuffer(GL_ARRAY_BUFFER, vbo);
        glBufferStorage(GL_ARRAY_BUFFER, allocationByteSize, flags);
        this.mappedVBOByteBuffer = glMapBufferRange(GL_ARRAY_BUFFER, 0, allocationByteSize, flags, null).asIntBuffer();

        ebo = glGenBuffers();
        glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, ebo);
        glBufferStorage(GL_ELEMENT_ARRAY_BUFFER, allocationByteSize, flags);
        this.mappedEBOByteBuffer = glMapBufferRange(GL_ELEMENT_ARRAY_BUFFER, 0, allocationByteSize, flags, null).asIntBuffer();

        glVertexAttribIPointer(0, 2, GL_INT, 2*Integer.BYTES, 0);
        glEnableVertexAttribArray(0);

        glBindVertexArray(0);
    }

    public PageFence getLastFenceUsed() {
        return this.lastFenceUsed;
    }

    public void setLastFence() {
        PageFence previous = this.lastFenceUsed;
        this.lastFenceUsed = new PageFence();

        if (previous != null && previous.references == 0) {
            glDeleteSync(previous.fenceId);
        }
    }

    private List<RetiredMesh> retireList = new ArrayList<>();
    public void removeChunk(AllocatorSegment vSegment, AllocatorSegment iSegment, PageFence fenceUsed) {
        retireAllocation(vSegment, iSegment, fenceUsed);
    }

    public void releaseFenceReference(PageFence fence) {
        fence.references--;

        if (fence.references < 0) {
            throw new IllegalStateException("Negative fence reference count");
        }

        if (fence.references == 0 && fence != this.lastFenceUsed) {
            glDeleteSync(fence.fenceId);
        }
    }

    public boolean isFenceSignaled(PageFence fence) {
        if (fence == null) return true;

        int state = glClientWaitSync(fence.fenceId, 0, 0L);
        if (state == GL_ALREADY_SIGNALED || state == GL_CONDITION_SATISFIED) {
            return true;
        }

        if (state == GL_TIMEOUT_EXPIRED) {
            return false;
        }

        if (state == GL_WAIT_FAILED) {
            int error = glGetError();

            throw new IllegalStateException(
                    "GPU fence wait failed. Fence ID: " +
                            fence.fenceId +
                            ", GL error: 0x" +
                            Integer.toHexString(error)
            );
        }

        throw new IllegalStateException(
                "Unexpected glClientWaitSync result: " + state
        );
    }

    public void flushRetireList() {
        Iterator<RetiredMesh> it = retireList.iterator();

        while (it.hasNext()) {
            RetiredMesh m = it.next();
            PageFence fence = m.fence;

            if (isFenceSignaled(fence)) {
                vertexSpaceAllocator.freeAllocation(m.vertexRange);
                indexSpaceAllocator.freeAllocation(m.indexRange);

                releaseFenceReference(fence);
                it.remove();
            }
        }
    }

    private void retireAllocation(AllocatorSegment vSegment, AllocatorSegment iSegment, PageFence fenceUsed) {
        if (fenceUsed == null) {
            vertexSpaceAllocator.freeAllocation(vSegment);
            indexSpaceAllocator.freeAllocation(iSegment);
            return;
        }

        fenceUsed.references++;
        retireList.add(new RetiredMesh(vSegment, iSegment, fenceUsed));
    }

    public SectionMeshDrawCommand updateChunk(AllocatorSegment vSegment, AllocatorSegment iSegment, PageFence lastSectionUsedFence, int[] vertices, int[] indices, int vCount, int iCount) {
        // request new range only gives a new range from strictly free memory and does not free old segment
        // resizeAllocationInPlace modifies the range of potentially being used data

        AllocatorSegment givenVSegment = vertexSpaceAllocator.requestNewRange(vSegment, vCount * Integer.BYTES, vCount);
        AllocatorSegment givenISegment = indexSpaceAllocator.requestNewRange(iSegment, iCount * Integer.BYTES, iCount);
        if (givenVSegment != null && givenISegment != null) {
            retireAllocation(vSegment, iSegment, lastSectionUsedFence);
        } else {
            if (givenVSegment != null) vertexSpaceAllocator.freeAllocation(givenVSegment);
            if (givenISegment != null) indexSpaceAllocator.freeAllocation(givenISegment);
        }

        if ((givenVSegment == null || givenISegment == null) && isFenceSignaled(lastSectionUsedFence)) {
            boolean canResizeVertex = vertexSpaceAllocator.canResizeInPlace(vSegment, vCount * Integer.BYTES);
            boolean canResizeIndex = indexSpaceAllocator.canResizeInPlace(iSegment, iCount * Integer.BYTES);

            if (canResizeVertex && canResizeIndex) {
                //System.out.println("Successful resize");
                givenVSegment = vertexSpaceAllocator.resizeAllocationInPlace(vSegment, vCount * Integer.BYTES, vCount);
                givenISegment = indexSpaceAllocator.resizeAllocationInPlace(iSegment, iCount * Integer.BYTES, iCount);
            }
        }

        if (givenVSegment == null || givenISegment == null) {
            retireAllocation(vSegment, iSegment, lastSectionUsedFence);
            return null;
        }

        mappedVBOByteBuffer.position((int) (givenVSegment.valueByteOffset() / Integer.BYTES));
        mappedVBOByteBuffer.put(vertices, 0, vCount);

        mappedEBOByteBuffer.position((int) (givenISegment.valueByteOffset()) / Integer.BYTES);
        mappedEBOByteBuffer.put(indices, 0, iCount);

        return new SectionMeshDrawCommand(givenVSegment, givenISegment, this);
    }

    public SectionMeshDrawCommand addChunk(int[] vertices, int[] indices, int vCount, int iCount) {
        if (!vertexSpaceAllocator.canInsert(vCount*Integer.BYTES) || !indexSpaceAllocator.canInsert(iCount*Integer.BYTES)) {
            return null;
        }

        AllocatorSegment givenVSegment = vertexSpaceAllocator.insert(vCount * Integer.BYTES, vCount);
        AllocatorSegment givenISegment = indexSpaceAllocator.insert(iCount * Integer.BYTES, iCount);

        mappedVBOByteBuffer.position((int) (givenVSegment.valueByteOffset() / Integer.BYTES));
        mappedVBOByteBuffer.put(vertices, 0, vCount);

        mappedEBOByteBuffer.position((int) (givenISegment.valueByteOffset()) / Integer.BYTES);
        mappedEBOByteBuffer.put(indices, 0, iCount);

        return new SectionMeshDrawCommand(givenVSegment, givenISegment, this);
    }

    public String debugData() {
        return String.format("Vertex Allocator: Total Free: %d bytes%nLargest Free Byte Range Size: %d bytes%nAmount of Free Ranges: %d%n%nIndex Allocator: Total Free: %d bytes%nLargest Free Byte Range Size %d bytes%nAmount of free ranges: %d%n", vertexSpaceAllocator.getTotalFreeSpace(), vertexSpaceAllocator.getLargestFreeRange(), vertexSpaceAllocator.getNumberOfFreeSpaces(), indexSpaceAllocator.getTotalFreeSpace(), indexSpaceAllocator.getLargestFreeRange(), indexSpaceAllocator.getNumberOfFreeSpaces());
    }

    public int getVao() {
        return this.vao;
    }

    public int getPageIndex() {
        return this.pageIndex;
    }
}

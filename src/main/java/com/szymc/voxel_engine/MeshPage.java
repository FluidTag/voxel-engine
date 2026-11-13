package com.szymc.voxel_engine;
import java.nio.IntBuffer;

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

    public void removeChunk(AllocatorSegment vSegment, AllocatorSegment iSegment) {
        vertexSpaceAllocator.freeAllocation(vSegment);
        indexSpaceAllocator.freeAllocation(iSegment);
    }

    public SectionMeshDrawCommand updateChunk(AllocatorSegment vSegment, AllocatorSegment iSegment, int[] vertices, int[] indices, int vCount, int iCount) {
        // Allocators return null if cannot handle update, if successful it already frees the old range and gives the newly allocated segment
        AllocatorSegment givenVSegment = vertexSpaceAllocator.requestNewRange(vSegment, vCount * Integer.BYTES, vCount);
        AllocatorSegment givenISegment = indexSpaceAllocator.requestNewRange(iSegment, iCount * Integer.BYTES, iCount);

        // Note this is correct rollback behavior, returning null means it is going to remove this mesh data from this page and add it into another
        if (givenVSegment == null || givenISegment == null) {
            if (givenVSegment != null) vertexSpaceAllocator.freeAllocation(givenVSegment); else vertexSpaceAllocator.freeAllocation(vSegment);
            if (givenISegment != null) indexSpaceAllocator.freeAllocation(givenISegment); else indexSpaceAllocator.freeAllocation(iSegment);

            return null;
        }

        mappedVBOByteBuffer.position((int) (givenVSegment.valueByteOffset() / Integer.BYTES));
        mappedVBOByteBuffer.put(vertices, 0, vCount);

        mappedEBOByteBuffer.position((int) (givenISegment.valueByteOffset()) / Integer.BYTES);
        mappedEBOByteBuffer.put(indices, 0, iCount);

        return new SectionMeshDrawCommand(givenVSegment, givenISegment, this.pageIndex);
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

        return new SectionMeshDrawCommand(givenVSegment, givenISegment, this.pageIndex);
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

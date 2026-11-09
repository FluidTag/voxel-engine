package com.szymc.voxel_engine;

import java.util.ArrayList;
import java.util.List;

public class MeshArena {
    private List<MeshPage> pages;
    private int count = 0;
    private MeshPage addPage() {
        MeshPage pg = new MeshPage(count++, 200);
        pages.add(pg);
        return pg;
    }

    public MeshPage getPage(int index) {
        return pages.get(index);
    }

    public MeshArena() {
        this.pages = new ArrayList<>(1000);
        addPage();
    }

    public SectionMeshDrawCommand updateChunk(SectionMeshDrawCommand prevDat, int[] vertices, int[] indices, int vCount, int iCount) {
        MeshPage pg = pages.get(prevDat.pageIndex);
        SectionMeshDrawCommand updatedResult = pg.updateChunk(prevDat.vertexRange, prevDat.indexRange, vertices, indices, vCount, iCount);

        if (updatedResult == null) {
            for (int i = 0; i < pages.size(); i++) {
                SectionMeshDrawCommand subResult = pages.get(i).addChunk(vertices, indices, vCount, iCount);
                if (subResult != null) {
                    subResult.pageIndex = i;
                    return subResult;
                }
            }

            MeshPage createdPg = addPage();
            updatedResult = createdPg.addChunk(vertices, indices, vCount, iCount);
            updatedResult.pageIndex = createdPg.getPageIndex();
        }

        return updatedResult;
    }

    // Try adding in the pages, if none, make a new one
    public SectionMeshDrawCommand addChunk(int[] vertices, int[] indices, int vCount, int iCount) {
        if (vCount == 0) return null;

        for (int i = 0; i < pages.size(); i++) {
            MeshPage pg = pages.get(i);
            SectionMeshDrawCommand result = pg.addChunk(vertices, indices, vCount, iCount);

            if (result != null) return result;
        }

        MeshPage created = addPage();
        return created.addChunk(vertices, indices, vCount, iCount);
    }
}

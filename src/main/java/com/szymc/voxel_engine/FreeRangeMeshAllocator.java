package com.szymc.voxel_engine;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.ListIterator;

public class FreeRangeMeshAllocator {
    public record AllocatorSegment(boolean isFree, long valueByteOffset, long valueByteAllocationSize, long count) {};
    private final ArrayList<AllocatorSegment> rangeAllocations; // FREE SEGMENTS ONLY
    private final static int EXPAND_SEGMENT_BYTE_VALUE = 500;
    private final static int MINIMUM_USEFUL_SEGMENT_BYTE_SIZE = 10000;

    private int alignment;
    public FreeRangeMeshAllocator(int initialMaxFreeSpaceBytes, int alignment) {
        this.rangeAllocations = new ArrayList<>();
        this.alignment = alignment;
        rangeAllocations.add(new AllocatorSegment(true, 0, initialMaxFreeSpaceBytes, -1));
    }

    public boolean canInsert(int sizeOfBytesNeeded) {
        sizeOfBytesNeeded = (sizeOfBytesNeeded + alignment - 1) & ~(alignment - 1);;

        for (AllocatorSegment segment : rangeAllocations) {
            if (segment.valueByteAllocationSize >= sizeOfBytesNeeded) {
                return true;
            }
        }

        return false;
    }

    public AllocatorSegment insert(int sizeOfBytesNeeded, int count) {
        if (sizeOfBytesNeeded <= 0) throw new IllegalArgumentException("sizeOfBytesNeeded needs to be greater than zero");

        ListIterator<AllocatorSegment> it = rangeAllocations.listIterator();
        AllocatorSegment givenSegment = null;
        sizeOfBytesNeeded = (sizeOfBytesNeeded + alignment - 1) & ~(alignment - 1);;

        while (it.hasNext()) {
            AllocatorSegment segment = it.next();
            long availableSize = segment.valueByteAllocationSize;

            if (availableSize >= sizeOfBytesNeeded) {
                // Remove original segment
                it.remove();

                // Only free segments stored in the allocator, given segments are stored by the caller (MeshPage)
                givenSegment = new AllocatorSegment(false, segment.valueByteOffset, sizeOfBytesNeeded, count);

                if (availableSize > sizeOfBytesNeeded) {
                    // Check if we can combine any free spaces, any empty space in-between is considered occupied and cannot be joined
                    AllocatorSegment next = it.hasNext() ? rangeAllocations.get(it.nextIndex()) : null;

                    long startOfFreeSpace = segment.valueByteOffset + sizeOfBytesNeeded;
                    long freeSpaceAllocated = segment.valueByteAllocationSize - sizeOfBytesNeeded;

                    if (next != null && startOfFreeSpace + freeSpaceAllocated == next.valueByteOffset) {
                        freeSpaceAllocated += next.valueByteAllocationSize;
                        it.next();
                        it.remove();
                    }

                    AllocatorSegment createdSplitSegment = new AllocatorSegment(
                            true,
                            startOfFreeSpace,
                            freeSpaceAllocated,
                            -1
                    );

                    it.add(createdSplitSegment);
                }

                break;
            }
        }

        return givenSegment;
    }

    // LIST MUST BE SORTED ON BYTE OFFSET BEFORE CALL
    private int findByteOffsetIndexGreaterThan(long byteOffsetTarget) {
        int low = 0;
        int high = rangeAllocations.size()-1;
        int indexResult = -1;

        while (low <= high) {
            int mid = low + (high-low) / 2;

            if (rangeAllocations.get(mid).valueByteOffset > byteOffsetTarget) {
                indexResult = mid;
                high = mid-1;
            } else {
                low = mid+1;
            }
        }

        return indexResult;
    }

    private int findByteOffsetIndexLessThan(long byteOffsetTarget) {
        int low = 0;
        int high = rangeAllocations.size()-1;
        int indexResult = -1;

        while (low <= high) {
            int mid = low + (high-low) / 2;

            if (rangeAllocations.get(mid).valueByteOffset < byteOffsetTarget) {
                indexResult = mid;
                low = mid+1;
            } else {
                high = mid-1;
            }
        }

        return indexResult;
    }

    public void freeAllocation(AllocatorSegment segment) {
        int greaterIndex = findByteOffsetIndexGreaterThan(segment.valueByteOffset);
        int insertionIndex = greaterIndex != -1 ? greaterIndex : rangeAllocations.size();

        long startOfFreedSpace = segment.valueByteOffset;
        long allocationSize = segment.valueByteAllocationSize;

        int previousIndex = findByteOffsetIndexLessThan(segment.valueByteOffset);
        AllocatorSegment prev = previousIndex != -1 ? rangeAllocations.get(previousIndex) : null;
        AllocatorSegment next = greaterIndex != -1 ? rangeAllocations.get(greaterIndex) : null;

        if (prev != null && prev.valueByteOffset + prev.valueByteAllocationSize == startOfFreedSpace) {
            startOfFreedSpace = prev.valueByteOffset;
            allocationSize += prev.valueByteAllocationSize;
            rangeAllocations.remove(previousIndex);
            insertionIndex--;
        }

        if (next != null && startOfFreedSpace + allocationSize == next.valueByteOffset) {
            allocationSize += next.valueByteAllocationSize;
            rangeAllocations.remove(insertionIndex);
        }

        rangeAllocations.add(insertionIndex, new AllocatorSegment(true, startOfFreedSpace, allocationSize, -1));
    }

    public AllocatorSegment requestNewRange(AllocatorSegment oldAllocatedSegment, int requestedSizeOfBytes, int newIntCount) {
        requestedSizeOfBytes =
                (requestedSizeOfBytes + alignment - 1) & ~(alignment - 1);

        if (requestedSizeOfBytes <= 0) throw new IllegalArgumentException("sizeOfBytesNeeded needs to be greater than zero");

        if (requestedSizeOfBytes == oldAllocatedSegment.valueByteAllocationSize) return new AllocatorSegment(false, oldAllocatedSegment.valueByteOffset, oldAllocatedSegment.valueByteAllocationSize, newIntCount);
        if (requestedSizeOfBytes < oldAllocatedSegment.valueByteAllocationSize) {
            // Simply free the excess space, check for combining of this free space to the right and return the new smaller segment
            // Since the list is sorted and index is found before any modifications, it is fine to modify here
            if (oldAllocatedSegment.valueByteAllocationSize - requestedSizeOfBytes < MINIMUM_USEFUL_SEGMENT_BYTE_SIZE) return new AllocatorSegment(false, oldAllocatedSegment.valueByteOffset, oldAllocatedSegment.valueByteAllocationSize, newIntCount);

            int indexOfGreaterSegment = findByteOffsetIndexGreaterThan(oldAllocatedSegment.valueByteOffset);
            AllocatorSegment next = indexOfGreaterSegment != -1 ? rangeAllocations.get(indexOfGreaterSegment) : null;

            long startOfFreedSpace = oldAllocatedSegment.valueByteOffset + requestedSizeOfBytes;
            long baseFreedSpace = oldAllocatedSegment.valueByteAllocationSize - requestedSizeOfBytes;
            int insertionIndex = indexOfGreaterSegment != -1 ? indexOfGreaterSegment : rangeAllocations.size();

            if (next != null && oldAllocatedSegment.valueByteOffset + oldAllocatedSegment.valueByteAllocationSize == next.valueByteOffset) {
                rangeAllocations.remove(next);
                rangeAllocations.add(insertionIndex, new AllocatorSegment(true, startOfFreedSpace, baseFreedSpace + next.valueByteAllocationSize, -1));
            } else {
                rangeAllocations.add(insertionIndex, new AllocatorSegment(true, startOfFreedSpace, baseFreedSpace, -1));
            }

            return new AllocatorSegment(false, oldAllocatedSegment.valueByteOffset, requestedSizeOfBytes, newIntCount);
        }

        // First search for any range simply big enough
        // Ill give them like 500? extra bytes than they need for expansion room
        ListIterator<AllocatorSegment> it = rangeAllocations.listIterator();
        while (it.hasNext()) {
            AllocatorSegment segment = it.next();
            if (segment.valueByteAllocationSize >= requestedSizeOfBytes) {
                long extraSpace = segment.valueByteAllocationSize - requestedSizeOfBytes;
                long extraTaken = Math.min(extraSpace, EXPAND_SEGMENT_BYTE_VALUE);
                extraTaken -= (extraTaken % alignment);

                long allocationSize = requestedSizeOfBytes+extraTaken;
                long remainder = segment.valueByteAllocationSize - allocationSize;

                it.remove();
                if (remainder > MINIMUM_USEFUL_SEGMENT_BYTE_SIZE) {
                    it.add(new AllocatorSegment(true, segment.valueByteOffset + allocationSize, remainder, -1));
                } else {
                    allocationSize += remainder;
                }

                AllocatorSegment givenSegment = new AllocatorSegment(false, segment.valueByteOffset, allocationSize, newIntCount);

                // Release old allocated segment that is elsewhere
                // Coalesce old release to left and right
                freeAllocation(oldAllocatedSegment);

                return givenSegment;
            }
        }

        // Then Ill try to merge the old space with previous or next
        int greaterFreeIndex = findByteOffsetIndexGreaterThan(oldAllocatedSegment.valueByteOffset);
        int lessFreeIndex = findByteOffsetIndexLessThan(oldAllocatedSegment.valueByteOffset);

        long createdByteStart = oldAllocatedSegment.valueByteOffset;
        long allocatedSize = oldAllocatedSegment.valueByteAllocationSize;

        // If the index is -1, the last free-range is the last free-range in the allocation list
        AllocatorSegment prev = lessFreeIndex != -1 ? rangeAllocations.get(lessFreeIndex) : null;
        AllocatorSegment next = greaterFreeIndex != -1 ? rangeAllocations.get(greaterFreeIndex) : null;
        boolean usedPrev = false, usedNext = false;

        if (next != null && createdByteStart + allocatedSize == next.valueByteOffset) {
            allocatedSize += next.valueByteAllocationSize;
            usedNext = true;
        }

        if (allocatedSize < requestedSizeOfBytes && prev != null && prev.valueByteOffset + prev.valueByteAllocationSize == createdByteStart) {
            createdByteStart = prev.valueByteOffset;
            allocatedSize += prev.valueByteAllocationSize;
            usedPrev = true;
        }

        // One at least must have been consumed since the equal condition was checked at the start
        // requested size is gaurenteed to be greater than the old size, so for this to be true either left, right or both must have been consumed
        if (allocatedSize >= requestedSizeOfBytes) {
            // Take only whats needed plus some extra if possible
            long extraSpace = allocatedSize - requestedSizeOfBytes;
            long extraTaken = Math.min(extraSpace, EXPAND_SEGMENT_BYTE_VALUE);
            long beingTaken = requestedSizeOfBytes + extraTaken - (extraTaken % alignment);

            long remainder = allocatedSize - beingTaken;

            int insertionIndex = (greaterFreeIndex != -1 ? greaterFreeIndex : rangeAllocations.size()) - (usedPrev ? 1 : 0);

            if (usedPrev) rangeAllocations.remove(prev);
            if (usedNext) rangeAllocations.remove(next);

            if (remainder > MINIMUM_USEFUL_SEGMENT_BYTE_SIZE) {
                rangeAllocations.add(insertionIndex, new AllocatorSegment(true, createdByteStart+beingTaken, remainder, -1));
            } else {
                beingTaken += remainder;
            }

            return new AllocatorSegment(false, createdByteStart, beingTaken, newIntCount);
        }

        return null;
    }

    public void logSanityCheck() {
        for (AllocatorSegment s1 : rangeAllocations) {
            for (AllocatorSegment s2 : rangeAllocations) {
                if (s1 == s2) continue;

                // Check for overlapping
                if ((s1.valueByteOffset < s2.valueByteOffset + s2.valueByteAllocationSize) &&
                        (s1.valueByteOffset + s1.valueByteAllocationSize > s2.valueByteOffset)) {
                    System.out.println(s1);
                    System.out.println(s2);
                }
            }
        }
    }

    public long getTotalFreeSpace() {
        long total = 0;

        for (AllocatorSegment segment : rangeAllocations) {
            total += segment.valueByteAllocationSize();
        }

        return total;
    }

    public int getNumberOfFreeSpaces() {
        return rangeAllocations.size();
    }

    public long getLargestFreeRange() {
        long largest = 0;

        for (AllocatorSegment segment : rangeAllocations) {
            largest = Math.max(largest, segment.valueByteAllocationSize());
        }

        return largest;
    }
}

package com.szymc.voxel_engine;

public class PaletteContainer {
    private byte[] palette = new byte[0];
    private long[] blockData = new long[256];
    private final static ThreadLocal<byte[]> threadByteBuffer = ThreadLocal.withInitial(() -> new byte[32*16*32]); // To be used as temporary read only outside of this function

    public byte readBlock(int x, int y, int z) {
        int index = y*32*32 + z*32 + x;
        int bitWidth = calculateBitCount(palette.length);

        return palette[(int)(readRawIndex(blockData, bitWidth, index))];
    }

    public void writeBlock(int x, int y, int z, byte block) {
        int currentPalettePos = -1;
        for (int i = 0; i < palette.length; i++) {
            if (palette[i] == block) {
                currentPalettePos = i;
                break;
            }
        }

        if (currentPalettePos == -1) {
            byte[] newPalette = new byte[palette.length+1];
            newPalette[palette.length] = block;
            currentPalettePos = palette.length;
            System.arraycopy(palette, 0, newPalette, 0, palette.length);
            palette = newPalette;

            int newBitWidth = calculateBitCount(newPalette.length);
            int datLength = ((32*16*32)*newBitWidth) / 64;
            if (datLength != blockData.length) {
                long[] oldBlockData = blockData;
                blockData = new long[datLength];

                for (int i = 0; i < 32 * 16 * 32; i++) {
                    long val = readRawIndex(oldBlockData, newBitWidth-1, i);
                    writeRawIndex(blockData, newBitWidth, i, val);
                }
            }
        }

        int bitWidth = calculateBitCount(palette.length);
        int index = y*32*32 + z*32 + x;
        writeRawIndex(blockData, bitWidth, index, currentPalettePos);
    }

    public byte[] toByteArray() {
        int bitWidth = calculateBitCount(palette.length);
        byte[] buffer = threadByteBuffer.get();
        int currentLongIndex = 0;
        int bitOffset = 0;
        for (int i = 0; i < (32*16*32); i++) {
            long mask = (1L << bitWidth) - 1L;
            if (bitOffset + bitWidth <= 64) {
                buffer[i] = palette[(int)((blockData[currentLongIndex] >>> bitOffset) & mask)];
                bitOffset += bitWidth;

                if (bitOffset == 64) {
                    currentLongIndex++;
                    bitOffset = 0;
                }

                continue;
            }

            int bits1 = 64-bitOffset;
            long part1 = (blockData[currentLongIndex] >>> bitOffset);
            long part2 = (blockData[currentLongIndex+1] << bits1);
            buffer[i] = palette[(int)((part1 | part2) & mask)];
            currentLongIndex++;
            bitOffset = bitWidth-bits1;
        }

        return buffer;
    }

    private void writeRawIndex(long[] data, int bitWidth, int index, long value) {
        int startBit = index*bitWidth;
        int mainLong = startBit/64;
        int offset = startBit%64;

        long mask = (1L << bitWidth) - 1L;

        if (offset + bitWidth <= 64) {
            data[mainLong] = (data[mainLong] &~ (mask << offset)) | (value << offset);
            return;
        }

        int bits1 = 64 - offset;
        data[mainLong] = (data[mainLong] &~ (mask << offset)) | (value << offset);
        data[mainLong+1] = (data[mainLong+1] &~ (mask >>> bits1)) | (value >>> bits1);
    }

    private long readRawIndex(long[] data, int bitWidth, int index) {
        int startBit = index*bitWidth;
        int mainLong = startBit/64;
        int offset = startBit%64;

        long mask = (1L << bitWidth) - 1L;
        if (offset + bitWidth <= 64) {
            return (data[mainLong] >>> offset) & mask;
        }

        int bits1 = 64-offset;
        long part1 = (data[mainLong] >>> offset);
        long part2 = (data[mainLong+1] << bits1);
        return (part1 | part2) & mask;
    }

    private int calculateBitCount(int uniqueValues) {
        if (uniqueValues <= 1) return 1;
        int bits = 0;
        int capacity = 1;
        while (capacity < uniqueValues) {
            bits++;
            capacity <<= 1;
        }

        return bits;
    }
}

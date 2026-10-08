package com.szymc.voxel_engine;

import java.nio.ByteBuffer;
import java.nio.LongBuffer;

public class PaletteContainer {
    private byte[] palette = new byte[0];
    private long[] blockData = new long[256];
    private final static ThreadLocal<byte[]> threadByteBuffer = ThreadLocal.withInitial(() -> new byte[32*16*32]); // To be used as temporary read only outside of this function
    private int bitWidth;

    public int getBitWidth() {
        return this.bitWidth;
    }

    public byte readBlock(int x, int y, int z) {
        int index = y*32*32 + z*32 + x;
        if (bitWidth == 0) return 0;
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

        bitWidth = calculateBitCount(palette.length);
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
        return 32 - Integer.numberOfLeadingZeros(uniqueValues - 1);
    }

    public byte[] serialize() {
        ByteBuffer byteBuffer = ByteBuffer.allocate(1 + 64 + 7 + (32*16*32));

        byteBuffer.put((byte) palette.length);
        byteBuffer.put(palette);
        byteBuffer.put(new byte[64 - palette.length]);
        byteBuffer.put(new byte[7]);
        byteBuffer.asLongBuffer().put(blockData);
        byteBuffer.position(byteBuffer.position() + (32 * 16 * 32));

        return byteBuffer.array();
    }

    public PaletteContainer() {}

    private PaletteContainer(byte[] palette, byte[] data) {
        this.palette = palette;
        this.bitWidth = calculateBitCount(palette.length);

        if (this.bitWidth == 0) {
            this.blockData = new long[0];
            return;
        }

        // Calculate how many longs are active based on bitWidth
        int requiredLongs = ((32 * 16 * 32) * this.bitWidth) / 64;

        ByteBuffer byteBuffer = ByteBuffer.wrap(data);
        LongBuffer longBuffer = byteBuffer.asLongBuffer();

        this.blockData = new long[requiredLongs];
        longBuffer.get(this.blockData, 0, requiredLongs); // Only read what's active
    }

    public static PaletteContainer deserialize(byte[] palette, byte[] data) {
        return new PaletteContainer(palette, data);
    }
}

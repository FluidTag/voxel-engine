package com.szymc.voxel_engine;

public class ContainerData {
    private short[] typeSource;
    private byte[] amountSource;
    private int size;

    public short[] getRawTypes() {
        return this.typeSource;
    }

    public byte[] getRawAmounts() {
        return this.amountSource;
    }

    public int getSize() {
        return this.size;
    }

    public ContainerData(int size) {
        this.typeSource = new short[size];
        this.amountSource = new byte[size];
        this.size = size;
    }

    public void set(int index, int type, int amount) {
        this.typeSource[index] = (short) type;
        this.amountSource[index] = (byte) amount;
    }

    public void decrementIndex(int index) {
        this.amountSource[index]--;
        if (this.amountSource[index] <= 0) {
            this.typeSource[index] = 0;
        }
    }

    public short readType(int index) {
        return this.typeSource[index];
    }

    public byte readAmount(int index) {
        return this.amountSource[index];
    }

    public boolean isEmptyAt(int index) {
        return this.typeSource[index] == 0;
    }
}
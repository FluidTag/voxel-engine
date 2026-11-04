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

    public ContainerData(short[] typeSource, byte[] amountSource) {
        if (typeSource == null || amountSource == null) throw new IllegalArgumentException("Type or amount source must not be null");
        if (typeSource.length != amountSource.length) throw new IllegalArgumentException("Type and amount source arrays must have equal length.");

        this.typeSource = typeSource;
        this.amountSource = amountSource;
        this.size = typeSource.length;
    }

    public void set(int index, int type, int amount) {
        this.typeSource[index] = (short) type;
        this.amountSource[index] = (byte) amount;

        if (this.amountSource[index] <= 0) {
            this.typeSource[index] = 0;
            this.amountSource[index] = 0;
        }
    }

    public void incrementIndex(int index) {
        this.amountSource[index]++;
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
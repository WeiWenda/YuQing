//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by Fernflower decompiler)
//

package de.bwaldvogel.liblinear;

final class DoubleArrayPointer {
    private final double[] _array;
    private int _offset;

    public void setOffset(int offset) {
        if(offset >= 0 && offset < this._array.length) {
            this._offset = offset;
        } else {
            throw new IllegalArgumentException("offset must be between 0 and the length of the array");
        }
    }

    public DoubleArrayPointer(double[] array, int offset) {
        this._array = array;
        this.setOffset(offset);
    }

    public double get(int index) {
        return this._array[this._offset + index];
    }

    public void set(int index, double value) {
        this._array[this._offset + index] = value;
    }
}

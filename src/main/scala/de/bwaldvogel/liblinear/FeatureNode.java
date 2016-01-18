//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by Fernflower decompiler)
//

package de.bwaldvogel.liblinear;

public class FeatureNode implements Feature {
    public final int index;
    public double value;

    public FeatureNode(int index, double value) {
        if(index < 0) {
            throw new IllegalArgumentException("index must be >= 0");
        } else {
            this.index = index;
            this.value = value;
        }
    }

    public int getIndex() {
        return this.index;
    }

    public double getValue() {
        return this.value;
    }

    public void setValue(double value) {
        this.value = value;
    }

    public int hashCode() {
        boolean prime = true;
        byte result = 1;
        int result1 = 31 * result + this.index;
        long temp = Double.doubleToLongBits(this.value);
        result1 = 31 * result1 + (int)(temp ^ temp >>> 32);
        return result1;
    }

    public boolean equals(Object obj) {
        if(this == obj) {
            return true;
        } else if(obj == null) {
            return false;
        } else if(this.getClass() != obj.getClass()) {
            return false;
        } else {
            FeatureNode other = (FeatureNode)obj;
            return this.index != other.index?false:Double.doubleToLongBits(this.value) == Double.doubleToLongBits(other.value);
        }
    }

    public String toString() {
        return "FeatureNode(idx=" + this.index + ", value=" + this.value + ")";
    }
}

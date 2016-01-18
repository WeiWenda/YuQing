//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by Fernflower decompiler)
//

package de.bwaldvogel.liblinear;

import java.io.File;
import java.io.IOException;
import java.io.Reader;
import java.io.Serializable;
import java.io.Writer;
import java.util.Arrays;

public final class Model implements Serializable {
    private static final long serialVersionUID = -6456047576741854834L;
    double bias;
    int[] label;
    int nr_class;
    int nr_feature;
    SolverType solverType;
    double[] w;

    public Model() {
    }

    public int getNrClass() {
        return this.nr_class;
    }

    public int getNrFeature() {
        return this.nr_feature;
    }

    public int[] getLabels() {
        return Linear.copyOf(this.label, this.nr_class);
    }

    public double[] getFeatureWeights() {
        return Linear.copyOf(this.w, this.w.length);
    }

    public boolean isProbabilityModel() {
        return this.solverType.isLogisticRegressionSolver();
    }

    public double getBias() {
        return this.bias;
    }

    public String toString() {
        StringBuilder sb = new StringBuilder("Model");
        sb.append(" bias=").append(this.bias);
        sb.append(" nr_class=").append(this.nr_class);
        sb.append(" nr_feature=").append(this.nr_feature);
        sb.append(" solverType=").append(this.solverType);
        return sb.toString();
    }

    public int hashCode() {
        boolean prime = true;
        byte result = 1;
        long temp = Double.doubleToLongBits(this.bias);
        int result1 = 31 * result + (int)(temp ^ temp >>> 32);
        result1 = 31 * result1 + Arrays.hashCode(this.label);
        result1 = 31 * result1 + this.nr_class;
        result1 = 31 * result1 + this.nr_feature;
        result1 = 31 * result1 + (this.solverType == null?0:this.solverType.hashCode());
        result1 = 31 * result1 + Arrays.hashCode(this.w);
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
            Model other = (Model)obj;
            if(Double.doubleToLongBits(this.bias) != Double.doubleToLongBits(other.bias)) {
                return false;
            } else if(!Arrays.equals(this.label, other.label)) {
                return false;
            } else if(this.nr_class != other.nr_class) {
                return false;
            } else if(this.nr_feature != other.nr_feature) {
                return false;
            } else {
                if(this.solverType == null) {
                    if(other.solverType != null) {
                        return false;
                    }
                } else if(!this.solverType.equals(other.solverType)) {
                    return false;
                }

                return equals(this.w, other.w);
            }
        }
    }

    protected static boolean equals(double[] a, double[] a2) {
        if(a == a2) {
            return true;
        } else if(a != null && a2 != null) {
            int length = a.length;
            if(a2.length != length) {
                return false;
            } else {
                for(int i = 0; i < length; ++i) {
                    if(a[i] != a2[i]) {
                        return false;
                    }
                }

                return true;
            }
        } else {
            return false;
        }
    }

    public void save(File file) throws IOException {
        Linear.saveModel(file, this);
    }

    public void save(Writer writer) throws IOException {
        Linear.saveModel(writer, this);
    }

    public static Model load(String file) throws IOException {
        return Linear.loadModel(file);
    }

    public static Model load(Reader inputReader) throws IOException {
        return Linear.loadModel(inputReader);
    }
}

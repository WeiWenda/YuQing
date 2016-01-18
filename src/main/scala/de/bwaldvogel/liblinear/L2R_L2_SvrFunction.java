//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by Fernflower decompiler)
//

package de.bwaldvogel.liblinear;

public class L2R_L2_SvrFunction extends L2R_L2_SvcFunction {
    private double p;

    public L2R_L2_SvrFunction(Problem prob, double[] C, double p) {
        super(prob, C);
        this.p = p;
    }

    public double fun(double[] w) {
        double f = 0.0D;
        double[] y = this.prob.y;
        int l = this.prob.l;
        int w_size = this.get_nr_variable();
        this.Xv(w, this.z);

        int i;
        for(i = 0; i < w_size; ++i) {
            f += w[i] * w[i];
        }

        f /= 2.0D;

        for(i = 0; i < l; ++i) {
            double d = this.z[i] - y[i];
            if(d < -this.p) {
                f += this.C[i] * (d + this.p) * (d + this.p);
            } else if(d > this.p) {
                f += this.C[i] * (d - this.p) * (d - this.p);
            }
        }

        return f;
    }

    public void grad(double[] w, double[] g) {
        double[] y = this.prob.y;
        int l = this.prob.l;
        int w_size = this.get_nr_variable();
        this.sizeI = 0;

        int i;
        for(i = 0; i < l; ++i) {
            double d = this.z[i] - y[i];
            if(d < -this.p) {
                this.z[this.sizeI] = this.C[i] * (d + this.p);
                this.I[this.sizeI] = i;
                ++this.sizeI;
            } else if(d > this.p) {
                this.z[this.sizeI] = this.C[i] * (d - this.p);
                this.I[this.sizeI] = i;
                ++this.sizeI;
            }
        }

        this.subXTv(this.z, g);

        for(i = 0; i < w_size; ++i) {
            g[i] = w[i] + 2.0D * g[i];
        }

    }
}

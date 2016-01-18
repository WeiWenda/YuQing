//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by Fernflower decompiler)
//

package de.bwaldvogel.liblinear;

class L2R_LrFunction implements Function {
    private final double[] C;
    private final double[] z;
    private final double[] D;
    private final Problem prob;

    public L2R_LrFunction(Problem prob, double[] C) {
        int l = prob.l;
        this.prob = prob;
        this.z = new double[l];
        this.D = new double[l];
        this.C = C;
    }

    private void Xv(double[] v, double[] Xv) {
        for(int i = 0; i < this.prob.l; ++i) {
            Xv[i] = 0.0D;
            Feature[] var7;
            int var6 = (var7 = this.prob.x[i]).length;

            for(int var5 = 0; var5 < var6; ++var5) {
                Feature s = var7[var5];
                Xv[i] += v[s.getIndex() - 1] * s.getValue();
            }
        }

    }

    private void XTv(double[] v, double[] XTv) {
        int l = this.prob.l;
        int w_size = this.get_nr_variable();
        Feature[][] x = this.prob.x;

        int i;
        for(i = 0; i < w_size; ++i) {
            XTv[i] = 0.0D;
        }

        for(i = 0; i < l; ++i) {
            Feature[] var10;
            int var9 = (var10 = x[i]).length;

            for(int var8 = 0; var8 < var9; ++var8) {
                Feature s = var10[var8];
                int var10001 = s.getIndex() - 1;
                XTv[var10001] += v[i] * s.getValue();
            }
        }

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
            double yz = y[i] * this.z[i];
            if(yz >= 0.0D) {
                f += this.C[i] * Math.log(1.0D + Math.exp(-yz));
            } else {
                f += this.C[i] * (-yz + Math.log(1.0D + Math.exp(yz)));
            }
        }

        return f;
    }

    public void grad(double[] w, double[] g) {
        double[] y = this.prob.y;
        int l = this.prob.l;
        int w_size = this.get_nr_variable();

        int i;
        for(i = 0; i < l; ++i) {
            this.z[i] = 1.0D / (1.0D + Math.exp(-y[i] * this.z[i]));
            this.D[i] = this.z[i] * (1.0D - this.z[i]);
            this.z[i] = this.C[i] * (this.z[i] - 1.0D) * y[i];
        }

        this.XTv(this.z, g);

        for(i = 0; i < w_size; ++i) {
            g[i] += w[i];
        }

    }

    public void Hv(double[] s, double[] Hs) {
        int l = this.prob.l;
        int w_size = this.get_nr_variable();
        double[] wa = new double[l];
        this.Xv(s, wa);

        int i;
        for(i = 0; i < l; ++i) {
            wa[i] = this.C[i] * this.D[i] * wa[i];
        }

        this.XTv(wa, Hs);

        for(i = 0; i < w_size; ++i) {
            Hs[i] += s[i];
        }

    }

    public int get_nr_variable() {
        return this.prob.n;
    }
}

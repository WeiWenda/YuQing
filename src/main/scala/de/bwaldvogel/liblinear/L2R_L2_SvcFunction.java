//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by Fernflower decompiler)
//

package de.bwaldvogel.liblinear;

class L2R_L2_SvcFunction implements Function {
    protected final Problem prob;
    protected final double[] C;
    protected final int[] I;
    protected final double[] z;
    protected int sizeI;

    public L2R_L2_SvcFunction(Problem prob, double[] C) {
        int l = prob.l;
        this.prob = prob;
        this.z = new double[l];
        this.I = new int[l];
        this.C = C;
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
            this.z[i] = y[i] * this.z[i];
            double d = 1.0D - this.z[i];
            if(d > 0.0D) {
                f += this.C[i] * d * d;
            }
        }

        return f;
    }

    public int get_nr_variable() {
        return this.prob.n;
    }

    public void grad(double[] w, double[] g) {
        double[] y = this.prob.y;
        int l = this.prob.l;
        int w_size = this.get_nr_variable();
        this.sizeI = 0;

        int i;
        for(i = 0; i < l; ++i) {
            if(this.z[i] < 1.0D) {
                this.z[this.sizeI] = this.C[i] * y[i] * (this.z[i] - 1.0D);
                this.I[this.sizeI] = i;
                ++this.sizeI;
            }
        }

        this.subXTv(this.z, g);

        for(i = 0; i < w_size; ++i) {
            g[i] = w[i] + 2.0D * g[i];
        }

    }

    public void Hv(double[] s, double[] Hs) {
        int w_size = this.get_nr_variable();
        double[] wa = new double[this.sizeI];
        this.subXv(s, wa);

        int i;
        for(i = 0; i < this.sizeI; ++i) {
            wa[i] = this.C[this.I[i]] * wa[i];
        }

        this.subXTv(wa, Hs);

        for(i = 0; i < w_size; ++i) {
            Hs[i] = s[i] + 2.0D * Hs[i];
        }

    }

    protected void subXTv(double[] v, double[] XTv) {
        int w_size = this.get_nr_variable();

        int i;
        for(i = 0; i < w_size; ++i) {
            XTv[i] = 0.0D;
        }

        for(i = 0; i < this.sizeI; ++i) {
            Feature[] var8;
            int var7 = (var8 = this.prob.x[this.I[i]]).length;

            for(int var6 = 0; var6 < var7; ++var6) {
                Feature s = var8[var6];
                int var10001 = s.getIndex() - 1;
                XTv[var10001] += v[i] * s.getValue();
            }
        }

    }

    private void subXv(double[] v, double[] Xv) {
        for(int i = 0; i < this.sizeI; ++i) {
            Xv[i] = 0.0D;
            Feature[] var7;
            int var6 = (var7 = this.prob.x[this.I[i]]).length;

            for(int var5 = 0; var5 < var6; ++var5) {
                Feature s = var7[var5];
                Xv[i] += v[s.getIndex() - 1] * s.getValue();
            }
        }

    }

    protected void Xv(double[] v, double[] Xv) {
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
}

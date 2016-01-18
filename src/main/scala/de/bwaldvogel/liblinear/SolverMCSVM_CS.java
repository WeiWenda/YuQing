//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by Fernflower decompiler)
//

package de.bwaldvogel.liblinear;

import de.bwaldvogel.liblinear.ArraySorter;

class SolverMCSVM_CS {
    private final double[] B;
    private final double[] C;
    private final double eps;
    private final double[] G;
    private final int max_iter;
    private final int w_size;
    private final int l;
    private final int nr_class;
    private final Problem prob;

    public SolverMCSVM_CS(Problem prob, int nr_class, double[] C) {
        this(prob, nr_class, C, 0.1D);
    }

    public SolverMCSVM_CS(Problem prob, int nr_class, double[] C, double eps) {
        this(prob, nr_class, C, eps, 100000);
    }

    public SolverMCSVM_CS(Problem prob, int nr_class, double[] weighted_C, double eps, int max_iter) {
        this.w_size = prob.n;
        this.l = prob.l;
        this.nr_class = nr_class;
        this.eps = eps;
        this.max_iter = max_iter;
        this.prob = prob;
        this.C = weighted_C;
        this.B = new double[nr_class];
        this.G = new double[nr_class];
    }

    private int GETI(int i) {
        return (int)this.prob.y[i];
    }

    private boolean be_shrunk(int i, int m, int yi, double alpha_i, double minG) {
        double bound = 0.0D;
        if(m == yi) {
            bound = this.C[this.GETI(i)];
        }

        return alpha_i == bound && this.G[m] < minG;
    }

    public void solve(double[] w) {
        int iter = 0;
        double[] alpha = new double[this.l * this.nr_class];
        double[] alpha_new = new double[this.nr_class];
        int[] index = new int[this.l];
        double[] QD = new double[this.l];
        int[] d_ind = new int[this.nr_class];
        double[] d_val = new double[this.nr_class];
        int[] alpha_index = new int[this.nr_class * this.l];
        int[] y_index = new int[this.l];
        int active_size = this.l;
        int[] active_size_i = new int[this.l];
        double eps_shrink = Math.max(10.0D * this.eps, 1.0D);
        boolean start_from_all = true;

        int i;
        for(i = 0; i < this.l * this.nr_class; ++i) {
            alpha[i] = 0.0D;
        }

        for(i = 0; i < this.w_size * this.nr_class; ++i) {
            w[i] = 0.0D;
        }

        int m;
        double nSV;
        for(i = 0; i < this.l; index[i] = i++) {
            for(m = 0; m < this.nr_class; alpha_index[i * this.nr_class + m] = m++) {
                ;
            }

            QD[i] = 0.0D;
            Feature[] var22;
            int v = (var22 = this.prob.x[i]).length;

            for(int alpha_index_i = 0; alpha_index_i < v; ++alpha_index_i) {
                Feature alpha_i = var22[alpha_index_i];
                nSV = alpha_i.getValue();
                QD[i] += nSV * nSV;
            }

            active_size_i[i] = this.nr_class;
            y_index[i] = (int)this.prob.y[i];
        }

        DoubleArrayPointer var35 = new DoubleArrayPointer(alpha, 0);
        IntArrayPointer var36 = new IntArrayPointer(alpha_index, 0);

        double var38;
        int var37;
        while(iter < this.max_iter) {
            var38 = -1.0D / 0.0;

            for(i = 0; i < active_size; ++i) {
                var37 = i + Linear.random.nextInt(active_size - i);
                Linear.swap(index, i, var37);
            }

            for(int s = 0; s < active_size; ++s) {
                i = index[s];
                nSV = QD[i];
                var35.setOffset(i * this.nr_class);
                var36.setOffset(i * this.nr_class);
                if(nSV > 0.0D) {
                    for(m = 0; m < active_size_i[i]; ++m) {
                        this.G[m] = 1.0D;
                    }

                    if(y_index[i] < active_size_i[i]) {
                        this.G[y_index[i]] = 0.0D;
                    }

                    Feature[] var28;
                    int maxG = (var28 = this.prob.x[i]).length;

                    int nz_d;
                    for(int var26 = 0; var26 < maxG; ++var26) {
                        Feature minG = var28[var26];
                        nz_d = (minG.getIndex() - 1) * this.nr_class;

                        for(m = 0; m < active_size_i[i]; ++m) {
                            this.G[m] += w[nz_d + var36.get(m)] * minG.getValue();
                        }
                    }

                    double var39 = 1.0D / 0.0;
                    double var40 = -1.0D / 0.0;

                    for(m = 0; m < active_size_i[i]; ++m) {
                        if(var35.get(var36.get(m)) < 0.0D && this.G[m] < var39) {
                            var39 = this.G[m];
                        }

                        if(this.G[m] > var40) {
                            var40 = this.G[m];
                        }
                    }

                    if(y_index[i] < active_size_i[i] && var35.get((int)this.prob.y[i]) < this.C[this.GETI(i)] && this.G[y_index[i]] < var39) {
                        var39 = this.G[y_index[i]];
                    }

                    for(m = 0; m < active_size_i[i]; ++m) {
                        if(this.be_shrunk(i, m, y_index[i], var35.get(var36.get(m)), var39)) {
                            --active_size_i[i];

                            while(active_size_i[i] > m) {
                                if(!this.be_shrunk(i, active_size_i[i], y_index[i], var35.get(var36.get(active_size_i[i])), var39)) {
                                    Linear.swap(var36, m, active_size_i[i]);
                                    Linear.swap(this.G, m, active_size_i[i]);
                                    if(y_index[i] == active_size_i[i]) {
                                        y_index[i] = m;
                                    } else if(y_index[i] == m) {
                                        y_index[i] = active_size_i[i];
                                    }
                                    break;
                                }

                                --active_size_i[i];
                            }
                        }
                    }

                    if(active_size_i[i] <= 1) {
                        --active_size;
                        Linear.swap(index, s, active_size);
                        --s;
                    } else if(var40 - var39 > 1.0E-12D) {
                        var38 = Math.max(var40 - var39, var38);

                        for(m = 0; m < active_size_i[i]; ++m) {
                            this.B[m] = this.G[m] - nSV * var35.get(var36.get(m));
                        }

                        this.solve_sub_problem(nSV, y_index[i], this.C[this.GETI(i)], active_size_i[i], alpha_new);
                        nz_d = 0;

                        for(m = 0; m < active_size_i[i]; ++m) {
                            double xi = alpha_new[m] - var35.get(var36.get(m));
                            var35.set(var36.get(m), alpha_new[m]);
                            if(Math.abs(xi) >= 1.0E-12D) {
                                d_ind[nz_d] = var36.get(m);
                                d_val[nz_d] = xi;
                                ++nz_d;
                            }
                        }

                        Feature[] var33;
                        int var32 = (var33 = this.prob.x[i]).length;

                        for(int var31 = 0; var31 < var32; ++var31) {
                            Feature var41 = var33[var31];
                            int w_offset = (var41.getIndex() - 1) * this.nr_class;

                            for(m = 0; m < nz_d; ++m) {
                                w[w_offset + d_ind[m]] += d_val[m] * var41.getValue();
                            }
                        }
                    }
                }
            }

            ++iter;
            if(iter % 10 == 0) {
                Linear.info(".");
            }

            if(var38 >= eps_shrink) {
                start_from_all = false;
            } else {
                if(var38 < this.eps && start_from_all) {
                    break;
                }

                active_size = this.l;

                for(i = 0; i < this.l; ++i) {
                    active_size_i[i] = this.nr_class;
                }

                Linear.info("*");
                eps_shrink = Math.max(eps_shrink / 2.0D, this.eps);
                start_from_all = true;
            }
        }

        Linear.info("%noptimization finished, #iter = %d%n", new Object[]{Integer.valueOf(iter)});
        if(iter >= this.max_iter) {
            Linear.info("%nWARNING: reaching max number of iterations%n");
        }

        var38 = 0.0D;
        var37 = 0;

        for(i = 0; i < this.w_size * this.nr_class; ++i) {
            var38 += w[i] * w[i];
        }

        var38 *= 0.5D;

        for(i = 0; i < this.l * this.nr_class; ++i) {
            var38 += alpha[i];
            if(Math.abs(alpha[i]) > 0.0D) {
                ++var37;
            }
        }

        for(i = 0; i < this.l; ++i) {
            var38 -= alpha[i * this.nr_class + (int)this.prob.y[i]];
        }

        Linear.info("Objective value = %f%n", new Object[]{Double.valueOf(var38)});
        Linear.info("nSV = %d%n", new Object[]{Integer.valueOf(var37)});
    }

    private void solve_sub_problem(double A_i, int yi, double C_yi, int active_i, double[] alpha_new) {
        assert active_i <= this.B.length;

        double[] D = Linear.copyOf(this.B, active_i);
        if(yi < active_i) {
            D[yi] += A_i * C_yi;
        }

        ArraySorter.reversedMergesort(D);
        double beta = D[0] - A_i * C_yi;

        int r;
        for(r = 1; r < active_i && beta < (double)r * D[r]; ++r) {
            beta += D[r];
        }

        beta /= (double)r;

        for(r = 0; r < active_i; ++r) {
            if(r == yi) {
                alpha_new[r] = Math.min(C_yi, (beta - this.B[r]) / A_i);
            } else {
                alpha_new[r] = Math.min(0.0D, (beta - this.B[r]) / A_i);
            }
        }

    }
}

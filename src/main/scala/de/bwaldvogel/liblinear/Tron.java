//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by Fernflower decompiler)
//

package de.bwaldvogel.liblinear;

class Tron {
    private final Function fun_obj;
    private final double eps;
    private final int max_iter;

    public Tron(Function fun_obj) {
        this(fun_obj, 0.1D);
    }

    public Tron(Function fun_obj, double eps) {
        this(fun_obj, eps, 1000);
    }

    public Tron(Function fun_obj, double eps, int max_iter) {
        this.fun_obj = fun_obj;
        this.eps = eps;
        this.max_iter = max_iter;
    }

    void tron(double[] w) {
        double eta0 = 1.0E-4D;
        double eta1 = 0.25D;
        double eta2 = 0.75D;
        double sigma1 = 0.25D;
        double sigma2 = 0.5D;
        double sigma3 = 4.0D;
        int n = this.fun_obj.get_nr_variable();
        double one = 1.0D;
        boolean search = true;
        boolean iter = true;
        double[] s = new double[n];
        double[] r = new double[n];
        double[] w_new = new double[n];
        double[] g = new double[n];

        for(int i = 0; i < n; ++i) {
            w[i] = 0.0D;
        }

        double f = this.fun_obj.fun(w);
        this.fun_obj.grad(w, g);
        double delta = euclideanNorm(g);
        double gnorm1 = delta;
        double gnorm = delta;
        if(delta <= this.eps * delta) {
            search = false;
        }

        int var45 = 1;

        while(var45 <= this.max_iter && search) {
            int cg_iter = this.trcg(delta, g, s, r);
            System.arraycopy(w, 0, w_new, 0, n);
            daxpy(one, s, w_new);
            double gs = dot(g, s);
            double prered = -0.5D * (gs - dot(s, r));
            double fnew = this.fun_obj.fun(w_new);
            double actred = f - fnew;
            double snorm = euclideanNorm(s);
            if(var45 == 1) {
                delta = Math.min(delta, snorm);
            }

            double alpha;
            if(fnew - f - gs <= 0.0D) {
                alpha = sigma3;
            } else {
                alpha = Math.max(sigma1, -0.5D * (gs / (fnew - f - gs)));
            }

            if(actred < eta0 * prered) {
                delta = Math.min(Math.max(alpha, sigma1) * snorm, sigma2 * delta);
            } else if(actred < eta1 * prered) {
                delta = Math.max(sigma1 * delta, Math.min(alpha * snorm, sigma2 * delta));
            } else if(actred < eta2 * prered) {
                delta = Math.max(sigma1 * delta, Math.min(alpha * snorm, sigma3 * delta));
            } else {
                delta = Math.max(delta, Math.min(alpha * snorm, sigma3 * delta));
            }

            Linear.info("iter %2d act %5.3e pre %5.3e delta %5.3e f %5.3e |g| %5.3e CG %3d%n", new Object[]{Integer.valueOf(var45), Double.valueOf(actred), Double.valueOf(prered), Double.valueOf(delta), Double.valueOf(f), Double.valueOf(gnorm), Integer.valueOf(cg_iter)});
            if(actred > eta0 * prered) {
                ++var45;
                System.arraycopy(w_new, 0, w, 0, n);
                f = fnew;
                this.fun_obj.grad(w, g);
                gnorm = euclideanNorm(g);
                if(gnorm <= this.eps * gnorm1) {
                    break;
                }
            }

            if(f < -1.0E32D) {
                Linear.info("WARNING: f < -1.0e+32%n");
                break;
            }

            if(Math.abs(actred) <= 0.0D && prered <= 0.0D) {
                Linear.info("WARNING: actred and prered <= 0%n");
                break;
            }

            if(Math.abs(actred) <= 1.0E-12D * Math.abs(f) && Math.abs(prered) <= 1.0E-12D * Math.abs(f)) {
                Linear.info("WARNING: actred and prered too small%n");
                break;
            }
        }

    }

    private int trcg(double delta, double[] g, double[] s, double[] r) {
        int n = this.fun_obj.get_nr_variable();
        double one = 1.0D;
        double[] d = new double[n];
        double[] Hd = new double[n];

        int cg_iter;
        for(cg_iter = 0; cg_iter < n; ++cg_iter) {
            s[cg_iter] = 0.0D;
            r[cg_iter] = -g[cg_iter];
            d[cg_iter] = r[cg_iter];
        }

        double cgtol = 0.1D * euclideanNorm(g);
        cg_iter = 0;

        double rnewTrnew;
        for(double rTr = dot(r, r); euclideanNorm(r) > cgtol; rTr = rnewTrnew) {
            ++cg_iter;
            this.fun_obj.Hv(d, Hd);
            double alpha = rTr / dot(d, Hd);
            daxpy(alpha, d, s);
            double beta;
            if(euclideanNorm(s) > delta) {
                Linear.info("cg reaches trust region boundary%n");
                alpha = -alpha;
                daxpy(alpha, d, s);
                beta = dot(s, d);
                double sts = dot(s, s);
                double dtd = dot(d, d);
                double dsq = delta * delta;
                double rad = Math.sqrt(beta * beta + dtd * (dsq - sts));
                if(beta >= 0.0D) {
                    alpha = (dsq - sts) / (beta + rad);
                } else {
                    alpha = (rad - beta) / dtd;
                }

                daxpy(alpha, d, s);
                alpha = -alpha;
                daxpy(alpha, Hd, r);
                break;
            }

            alpha = -alpha;
            daxpy(alpha, Hd, r);
            rnewTrnew = dot(r, r);
            beta = rnewTrnew / rTr;
            scale(beta, d);
            daxpy(one, r, d);
        }

        return cg_iter;
    }

    private static void daxpy(double constant, double[] vector1, double[] vector2) {
        if(constant != 0.0D) {
            assert vector1.length == vector2.length;

            for(int i = 0; i < vector1.length; ++i) {
                vector2[i] += constant * vector1[i];
            }

        }
    }

    private static double dot(double[] vector1, double[] vector2) {
        double product = 0.0D;

        assert vector1.length == vector2.length;

        for(int i = 0; i < vector1.length; ++i) {
            product += vector1[i] * vector2[i];
        }

        return product;
    }

    private static double euclideanNorm(double[] vector) {
        int n = vector.length;
        if(n < 1) {
            return 0.0D;
        } else if(n == 1) {
            return Math.abs(vector[0]);
        } else {
            double scale = 0.0D;
            double sum = 1.0D;

            for(int i = 0; i < n; ++i) {
                if(vector[i] != 0.0D) {
                    double abs = Math.abs(vector[i]);
                    double t;
                    if(scale < abs) {
                        t = scale / abs;
                        sum = 1.0D + sum * t * t;
                        scale = abs;
                    } else {
                        t = abs / scale;
                        sum += t * t;
                    }
                }
            }

            return scale * Math.sqrt(sum);
        }
    }

    private static void scale(double constant, double[] vector) {
        if(constant != 1.0D) {
            for(int i = 0; i < vector.length; ++i) {
                vector[i] *= constant;
            }

        }
    }
}

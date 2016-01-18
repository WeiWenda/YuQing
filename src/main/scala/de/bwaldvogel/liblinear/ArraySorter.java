//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by Fernflower decompiler)
//

package de.bwaldvogel.liblinear;

final class ArraySorter {
    ArraySorter() {
    }

    public static void reversedMergesort(double[] a) {
        reversedMergesort(a, 0, a.length);
    }

    private static void reversedMergesort(double[] x, int off, int len) {
        int m;
        int v;
        if(len < 7) {
            for(m = off; m < len + off; ++m) {
                for(v = m; v > off && x[v - 1] < x[v]; --v) {
                    swap(x, v, v - 1);
                }
            }

        } else {
            m = off + (len >> 1);
            int a;
            if(len > 7) {
                v = off;
                int n = off + len - 1;
                if(len > 40) {
                    a = len / 8;
                    v = med3(x, off, off + a, off + 2 * a);
                    m = med3(x, m - a, m, m + a);
                    n = med3(x, n - 2 * a, n - a, n);
                }

                m = med3(x, v, m, n);
            }

            double var12 = x[m];
            a = off;
            int b = off;
            int c = off + len - 1;
            int d = c;

            while(true) {
                while(b > c || x[b] < var12) {
                    for(; c >= b && x[c] <= var12; --c) {
                        if(x[c] == var12) {
                            swap(x, c, d--);
                        }
                    }

                    if(b > c) {
                        int n1 = off + len;
                        int s = Math.min(a - off, b - a);
                        vecswap(x, off, b - s, s);
                        s = Math.min(d - c, n1 - d - 1);
                        vecswap(x, b, n1 - s, s);
                        if((s = b - a) > 1) {
                            reversedMergesort(x, off, s);
                        }

                        if((s = d - c) > 1) {
                            reversedMergesort(x, n1 - s, s);
                        }

                        return;
                    }

                    swap(x, b++, c--);
                }

                if(x[b] == var12) {
                    swap(x, a++, b);
                }

                ++b;
            }
        }
    }

    private static void swap(double[] x, int a, int b) {
        double t = x[a];
        x[a] = x[b];
        x[b] = t;
    }

    private static void vecswap(double[] x, int a, int b, int n) {
        for(int i = 0; i < n; ++b) {
            swap(x, a, b);
            ++i;
            ++a;
        }

    }

    private static int med3(double[] x, int a, int b, int c) {
        return x[a] < x[b]?(x[b] < x[c]?b:(x[a] < x[c]?c:a)):(x[b] > x[c]?b:(x[a] > x[c]?c:a));
    }
}

//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by Fernflower decompiler)
//

package de.bwaldvogel.liblinear;

interface Function {
    double fun(double[] var1);

    void grad(double[] var1, double[] var2);

    void Hv(double[] var1, double[] var2);

    int get_nr_variable();
}

package org.apache.lib;

/**
 * Created by liumeng on 26.12.15.
 */

// origin: tron.h
interface Function {

    double fun(double[] w);

    void grad(double[] w, double[] g);

    void Hv(double[] s, double[] Hs);

    int get_nr_variable();
}


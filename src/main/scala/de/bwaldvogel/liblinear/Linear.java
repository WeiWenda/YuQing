//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by Fernflower decompiler)
//

package de.bwaldvogel.liblinear;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.Closeable;
import java.io.EOFException;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintStream;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.Charset;
import java.util.Formatter;
import java.util.Locale;
import java.util.Random;
import java.util.regex.Pattern;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;

public class Linear {
    static final Charset FILE_CHARSET = Charset.forName("ISO-8859-1");
    static final Locale DEFAULT_LOCALE;
    private static Object OUTPUT_MUTEX;
    private static PrintStream DEBUG_OUTPUT;
    private static final long DEFAULT_RANDOM_SEED = 0L;
    static Random random;

    static {
        DEFAULT_LOCALE = Locale.ENGLISH;
        OUTPUT_MUTEX = new Object();
        DEBUG_OUTPUT = System.out;
        random = new Random(0L);
    }

    public Linear() {
    }

    public static void crossValidation(Problem prob, Parameter param, int nr_fold, double[] target) {
        int[] fold_start = new int[nr_fold + 1];
        int l = prob.l;
        int[] perm = new int[l];

        int i;
        for(i = 0; i < l; perm[i] = i++) {
            ;
        }

        int begin;
        for(i = 0; i < l; ++i) {
            begin = i + random.nextInt(l - i);
            swap(perm, i, begin);
        }

        for(i = 0; i <= nr_fold; ++i) {
            fold_start[i] = i * l / nr_fold;
        }

        for(i = 0; i < nr_fold; ++i) {
            begin = fold_start[i];
            int end = fold_start[i + 1];
            Problem subprob = new Problem();
            subprob.bias = prob.bias;
            subprob.n = prob.n;
            subprob.l = l - (end - begin);
            subprob.x = new Feature[subprob.l][];
            subprob.y = new double[subprob.l];
            int k = 0;

            int j;
            for(j = 0; j < begin; ++j) {
                subprob.x[k] = prob.x[perm[j]];
                subprob.y[k] = prob.y[perm[j]];
                ++k;
            }

            for(j = end; j < l; ++j) {
                subprob.x[k] = prob.x[perm[j]];
                subprob.y[k] = prob.y[perm[j]];
                ++k;
            }

            Model submodel = train(subprob, param);

            for(j = begin; j < end; ++j) {
                target[perm[j]] = predict(submodel, prob.x[perm[j]]);
            }
        }

    }

    private static GroupClassesReturn groupClasses(Problem prob, int[] perm) {
        int l = prob.l;
        int max_nr_class = 16;
        int nr_class = 0;
        int[] label = new int[max_nr_class];
        int[] count = new int[max_nr_class];
        int[] data_label = new int[l];

        int i;
        for(i = 0; i < l; ++i) {
            int start = (int)prob.y[i];

            int j;
            for(j = 0; j < nr_class; ++j) {
                if(start == label[j]) {
                    ++count[j];
                    break;
                }
            }

            data_label[i] = j;
            if(j == nr_class) {
                if(nr_class == max_nr_class) {
                    max_nr_class *= 2;
                    label = copyOf(label, max_nr_class);
                    count = copyOf(count, max_nr_class);
                }

                label[nr_class] = start;
                count[nr_class] = 1;
                ++nr_class;
            }
        }

        int[] var11 = new int[nr_class];
        var11[0] = 0;

        for(i = 1; i < nr_class; ++i) {
            var11[i] = var11[i - 1] + count[i - 1];
        }

        for(i = 0; i < l; ++i) {
            perm[var11[data_label[i]]] = i;
            ++var11[data_label[i]];
        }

        var11[0] = 0;

        for(i = 1; i < nr_class; ++i) {
            var11[i] = var11[i - 1] + count[i - 1];
        }

        return new GroupClassesReturn(nr_class, label, var11, count);
    }

    static void info(String message) {
        Object var1 = OUTPUT_MUTEX;
        synchronized(OUTPUT_MUTEX) {
            if(DEBUG_OUTPUT != null) {
                DEBUG_OUTPUT.printf(message, new Object[0]);
                DEBUG_OUTPUT.flush();
            }
        }
    }

    static void info(String format, Object... args) {
        Object var2 = OUTPUT_MUTEX;
        synchronized(OUTPUT_MUTEX) {
            if(DEBUG_OUTPUT != null) {
                DEBUG_OUTPUT.printf(format, args);
                DEBUG_OUTPUT.flush();
            }
        }
    }

    static double atof(String s) {
        if(s != null && s.length() >= 1) {
            double d = Double.parseDouble(s);
            if(!Double.isNaN(d) && !Double.isInfinite(d)) {
                return d;
            } else {
                throw new IllegalArgumentException("NaN or Infinity in input: " + s);
            }
        } else {
            throw new IllegalArgumentException("Can\'t convert empty string to integer");
        }
    }

    static int atoi(String s) throws NumberFormatException {
        if(s != null && s.length() >= 1) {
            if(s.charAt(0) == 43) {
                s = s.substring(1);
            }

            return Integer.parseInt(s);
        } else {
            throw new IllegalArgumentException("Can\'t convert empty string to integer");
        }
    }

    public static double[] copyOf(double[] original, int newLength) {
        double[] copy = new double[newLength];
        System.arraycopy(original, 0, copy, 0, Math.min(original.length, newLength));
        return copy;
    }

    public static int[] copyOf(int[] original, int newLength) {
        int[] copy = new int[newLength];
        System.arraycopy(original, 0, copy, 0, Math.min(original.length, newLength));
        return copy;
    }

    public static Model loadModel(Reader inputReader) throws IOException {
        Model model = new Model();
        model.label = null;
        Pattern whitespace = Pattern.compile("\\s+");
        BufferedReader reader = null;
        if(inputReader instanceof BufferedReader) {
            reader = (BufferedReader)inputReader;
        } else {
            reader = new BufferedReader(inputReader);
        }

        String line = null;

        int nr_w;
        while((line = reader.readLine()) != null) {
            String[] w_size = whitespace.split(line);
            if(w_size[0].equals("solver_type")) {
                SolverType var13 = SolverType.valueOf(w_size[1]);
                if(var13 == null) {
                    throw new RuntimeException("unknown solver type");
                }

                model.solverType = var13;
            } else if(w_size[0].equals("nr_class")) {
                model.nr_class = atoi(w_size[1]);
                Integer.parseInt(w_size[1]);
            } else if(w_size[0].equals("nr_feature")) {
                model.nr_feature = atoi(w_size[1]);
            } else if(w_size[0].equals("bias")) {
                model.bias = atof(w_size[1]);
            } else {
                if(w_size[0].equals("w")) {
                    break;
                }

                if(!w_size[0].equals("label")) {
                    throw new RuntimeException("unknown text in model file: [" + line + "]");
                }

                model.label = new int[model.nr_class];

                for(nr_w = 0; nr_w < model.nr_class; ++nr_w) {
                    model.label[nr_w] = atoi(w_size[nr_w + 1]);
                }
            }
        }

        int var12 = model.nr_feature;
        if(model.bias >= 0.0D) {
            ++var12;
        }

        nr_w = model.nr_class;
        if(model.nr_class == 2 && model.solverType != SolverType.MCSVM_CS) {
            nr_w = 1;
        }

        model.w = new double[var12 * nr_w];
        int[] buffer = new int[128];

        for(int i = 0; i < var12; ++i) {
            for(int j = 0; j < nr_w; ++j) {
                int b = 0;

                while(true) {
                    int ch = reader.read();
                    if(ch == -1) {
                        throw new EOFException("unexpected EOF");
                    }

                    if(ch == 32) {
                        model.w[i * nr_w + j] = atof(new String(buffer, 0, b));
                        break;
                    }

                    buffer[b++] = ch;
                }
            }
        }

        return model;
    }

    public static Model loadModel(String modelFile) throws IOException {
        Configuration conf = new Configuration();
        conf.addResource(new Path("projectFile/core-site.xml"));
        conf.addResource(new Path("projectFile/hdfs-site.xml"));
        FileSystem hdfs = FileSystem.get(conf);
        BufferedReader inputReader = new BufferedReader(new InputStreamReader(hdfs.open(new Path(modelFile)), FILE_CHARSET));

        Model var3;
        try {
            var3 = loadModel((Reader)inputReader);
        } finally {
            inputReader.close();
        }

        return var3;
    }

    static void closeQuietly(Closeable c) {
        if(c != null) {
            try {
                c.close();
            } catch (Throwable var2) {
                ;
            }

        }
    }

    public static double predict(Model model, Feature[] x) {
        double[] dec_values = new double[model.nr_class];
        return predictValues(model, x, dec_values);
    }

    public static double predictProbability(Model model, Feature[] x, double[] prob_estimates) throws IllegalArgumentException {
        int nr_w;
        int sum;
        if(!model.isProbabilityModel()) {
            StringBuilder var10 = new StringBuilder("probability output is only supported for logistic regression");
            var10.append(". This is currently only supported by the following solvers: ");
            nr_w = 0;
            SolverType[] var8;
            sum = (var8 = SolverType.values()).length;

            for(int var6 = 0; var6 < sum; ++var6) {
                SolverType var11 = var8[var6];
                if(var11.isLogisticRegressionSolver()) {
                    if(nr_w++ > 0) {
                        var10.append(", ");
                    }

                    var10.append(var11.name());
                }
            }

            throw new IllegalArgumentException(var10.toString());
        } else {
            int nr_class = model.nr_class;
            if(nr_class == 2) {
                nr_w = 1;
            } else {
                nr_w = nr_class;
            }

            double label = predictValues(model, x, prob_estimates);

            for(sum = 0; sum < nr_w; ++sum) {
                prob_estimates[sum] = 1.0D / (1.0D + Math.exp(-prob_estimates[sum]));
            }

            if(nr_class == 2) {
                prob_estimates[1] = 1.0D - prob_estimates[0];
            } else {
                double var12 = 0.0D;

                int i;
                for(i = 0; i < nr_class; ++i) {
                    var12 += prob_estimates[i];
                }

                for(i = 0; i < nr_class; ++i) {
                    prob_estimates[i] /= var12;
                }
            }

            return label;
        }
    }

    public static double predictValues(Model model, Feature[] x, double[] dec_values) {
        int n;
        if(model.bias >= 0.0D) {
            n = model.nr_feature + 1;
        } else {
            n = model.nr_feature;
        }

        double[] w = model.w;
        int nr_w;
        if(model.nr_class == 2 && model.solverType != SolverType.MCSVM_CS) {
            nr_w = 1;
        } else {
            nr_w = model.nr_class;
        }

        int dec_max_idx;
        for(dec_max_idx = 0; dec_max_idx < nr_w; ++dec_max_idx) {
            dec_values[dec_max_idx] = 0.0D;
        }

        Feature[] var9 = x;
        int var8 = x.length;

        int i;
        for(i = 0; i < var8; ++i) {
            Feature var12 = var9[i];
            int idx = var12.getIndex();
            if(idx <= n) {
                for(int i1 = 0; i1 < nr_w; ++i1) {
                    dec_values[i1] += w[(idx - 1) * nr_w + i1] * var12.getValue();
                }
            }
        }

        if(model.nr_class == 2) {
            if(model.solverType.isSupportVectorRegression()) {
                return dec_values[0];
            } else {
                return (double)(dec_values[0] > 0.0D?model.label[0]:model.label[1]);
            }
        } else {
            dec_max_idx = 0;

            for(i = 1; i < model.nr_class; ++i) {
                if(dec_values[i] > dec_values[dec_max_idx]) {
                    dec_max_idx = i;
                }
            }

            return (double)model.label[dec_max_idx];
        }
    }

    static void printf(Formatter formatter, String format, Object... args) throws IOException {
        formatter.format(format, args);
        IOException ioException = formatter.ioException();
        if(ioException != null) {
            throw ioException;
        }
    }

    public static void saveModel(Writer modelOutput, Model model) throws IOException {
        int nr_feature = model.nr_feature;
        int w_size = nr_feature;
        if(model.bias >= 0.0D) {
            w_size = nr_feature + 1;
        }

        int nr_w = model.nr_class;
        if(model.nr_class == 2 && model.solverType != SolverType.MCSVM_CS) {
            nr_w = 1;
        }

        Formatter formatter = new Formatter(modelOutput, DEFAULT_LOCALE);

        try {
            printf(formatter, "solver_type %s\n", new Object[]{model.solverType.name()});
            printf(formatter, "nr_class %d\n", new Object[]{Integer.valueOf(model.nr_class)});
            int ioException;
            if(model.label != null) {
                printf(formatter, "label", new Object[0]);

                for(ioException = 0; ioException < model.nr_class; ++ioException) {
                    printf(formatter, " %d", new Object[]{Integer.valueOf(model.label[ioException])});
                }

                printf(formatter, "\n", new Object[0]);
            }

            printf(formatter, "nr_feature %d\n", new Object[]{Integer.valueOf(nr_feature)});
            printf(formatter, "bias %.16g\n", new Object[]{Double.valueOf(model.bias)});
            printf(formatter, "w\n", new Object[0]);

            for(ioException = 0; ioException < w_size; ++ioException) {
                for(int j = 0; j < nr_w; ++j) {
                    double value = model.w[ioException * nr_w + j];
                    if(value == 0.0D) {
                        printf(formatter, "%d ", new Object[]{Integer.valueOf(0)});
                    } else {
                        printf(formatter, "%.16g ", new Object[]{Double.valueOf(value)});
                    }
                }

                printf(formatter, "\n", new Object[0]);
            }

            formatter.flush();
            IOException var13 = formatter.ioException();
            if(var13 != null) {
                throw var13;
            }
        } finally {
            formatter.close();
        }

    }

    public static void saveModel(File modelFile, Model model) throws IOException {
        BufferedWriter modelOutput = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(modelFile), FILE_CHARSET));
        saveModel((Writer)modelOutput, model);
    }

    private static int GETI(byte[] y, int i) {
        return y[i] + 1;
    }

    private static void solve_l2r_l1l2_svc(Problem prob, double[] w, double eps, double Cp, double Cn, SolverType solver_type) {
        int l = prob.l;
        int w_size = prob.n;
        int iter = 0;
        double[] QD = new double[l];
        short max_iter = 1000;
        int[] index = new int[l];
        double[] alpha = new double[l];
        byte[] y = new byte[l];
        int active_size = l;
        double PGmax_old = 1.0D / 0.0;
        double PGmin_old = -1.0D / 0.0;
        double[] diag = new double[]{0.5D / Cn, 0.0D, 0.5D / Cp};
        double[] upper_bound = new double[]{1.0D / 0.0, 0.0D, 1.0D / 0.0};
        if(solver_type == SolverType.L2R_L1LOSS_SVC_DUAL) {
            diag[0] = 0.0D;
            diag[2] = 0.0D;
            upper_bound[0] = Cn;
            upper_bound[2] = Cp;
        }

        int i;
        for(i = 0; i < l; ++i) {
            if(prob.y[i] > 0.0D) {
                y[i] = 1;
            } else {
                y[i] = -1;
            }
        }

        for(i = 0; i < l; ++i) {
            alpha[i] = 0.0D;
        }

        for(i = 0; i < w_size; ++i) {
            w[i] = 0.0D;
        }

        int nSV;
        int var10001;
        for(i = 0; i < l; index[i] = i++) {
            QD[i] = diag[GETI(y, i)];
            Feature[] xi;
            nSV = (xi = prob.x[i]).length;

            for(int alpha_old = 0; alpha_old < nSV; ++alpha_old) {
                Feature v = xi[alpha_old];
                double val = v.getValue();
                QD[i] += val * val;
                var10001 = v.getIndex() - 1;
                w[var10001] += (double)y[i] * alpha[i] * val;
            }
        }

        while(iter < max_iter) {
            double PGmax_new = -1.0D / 0.0;
            double PGmin_new = 1.0D / 0.0;

            for(i = 0; i < active_size; ++i) {
                int var47 = i + random.nextInt(active_size - i);
                swap(index, i, var47);
            }

            for(int s = 0; s < active_size; ++s) {
                i = index[s];
                double G = 0.0D;
                byte var46 = y[i];
                Feature[] var53;
                int var49 = (var53 = prob.x[i]).length;

                for(nSV = 0; nSV < var49; ++nSV) {
                    Feature var51 = var53[nSV];
                    G += w[var51.getIndex() - 1] * var51.getValue();
                }

                G = G * (double)var46 - 1.0D;
                double C = upper_bound[GETI(y, i)];
                G += alpha[i] * diag[GETI(y, i)];
                double PG = 0.0D;
                if(alpha[i] == 0.0D) {
                    if(G > PGmax_old) {
                        --active_size;
                        swap(index, s, active_size);
                        --s;
                        continue;
                    }

                    if(G < 0.0D) {
                        PG = G;
                    }
                } else if(alpha[i] == C) {
                    if(G < PGmin_old) {
                        --active_size;
                        swap(index, s, active_size);
                        --s;
                        continue;
                    }

                    if(G > 0.0D) {
                        PG = G;
                    }
                } else {
                    PG = G;
                }

                PGmax_new = Math.max(PGmax_new, PG);
                PGmin_new = Math.min(PGmin_new, PG);
                if(Math.abs(PG) > 1.0E-12D) {
                    double var50 = alpha[i];
                    alpha[i] = Math.min(Math.max(alpha[i] - G / QD[i], 0.0D), C);
                    double d = (alpha[i] - var50) * (double)var46;
                    Feature[] var44;
                    int var43 = (var44 = prob.x[i]).length;

                    for(int var52 = 0; var52 < var43; ++var52) {
                        Feature var48 = var44[var52];
                        var10001 = var48.getIndex() - 1;
                        w[var10001] += d * var48.getValue();
                    }
                }
            }

            ++iter;
            if(iter % 10 == 0) {
                info(".");
            }

            if(PGmax_new - PGmin_new <= eps) {
                if(active_size == l) {
                    break;
                }

                active_size = l;
                info("*");
                PGmax_old = 1.0D / 0.0;
                PGmin_old = -1.0D / 0.0;
            } else {
                PGmax_old = PGmax_new;
                PGmin_old = PGmin_new;
                if(PGmax_new <= 0.0D) {
                    PGmax_old = 1.0D / 0.0;
                }

                if(PGmin_new >= 0.0D) {
                    PGmin_old = -1.0D / 0.0;
                }
            }
        }

        info("%noptimization finished, #iter = %d%n", new Object[]{Integer.valueOf(iter)});
        if(iter >= max_iter) {
            info("%nWARNING: reaching max number of iterations%nUsing -s 2 may be faster (also see FAQ)%n%n");
        }

        double var45 = 0.0D;
        nSV = 0;

        for(i = 0; i < w_size; ++i) {
            var45 += w[i] * w[i];
        }

        for(i = 0; i < l; ++i) {
            var45 += alpha[i] * (alpha[i] * diag[GETI(y, i)] - 2.0D);
            if(alpha[i] > 0.0D) {
                ++nSV;
            }
        }

        info("Objective value = %g%n", new Object[]{Double.valueOf(var45 / 2.0D)});
        info("nSV = %d%n", new Object[]{Integer.valueOf(nSV)});
    }

    private static int GETI_SVR(int i) {
        return 0;
    }

    private static void solve_l2r_l1l2_svr(Problem prob, double[] w, Parameter param) {
        int l = prob.l;
        double C = param.C;
        double p = param.p;
        int w_size = prob.n;
        double eps = param.eps;
        int iter = 0;
        short max_iter = 1000;
        int active_size = l;
        int[] index = new int[l];
        double Gmax_old = 1.0D / 0.0;
        double Gnorm1_init = 0.0D;
        double[] beta = new double[l];
        double[] QD = new double[l];
        double[] y = prob.y;
        double[] lambda = new double[]{0.5D / C};
        double[] upper_bound = new double[]{1.0D / 0.0};
        if(param.solverType == SolverType.L2R_L1LOSS_SVR_DUAL) {
            lambda[0] = 0.0D;
            upper_bound[0] = C;
        }

        int i;
        for(i = 0; i < l; ++i) {
            beta[i] = 0.0D;
        }

        for(i = 0; i < w_size; ++i) {
            w[i] = 0.0D;
        }

        int nSV;
        Feature[] var39;
        Feature v;
        int var37;
        double violation;
        int var10001;
        for(i = 0; i < l; index[i] = i++) {
            QD[i] = 0.0D;
            nSV = (var39 = prob.x[i]).length;

            for(var37 = 0; var37 < nSV; ++var37) {
                v = var39[var37];
                violation = v.getValue();
                QD[i] += violation * violation;
                var10001 = v.getIndex() - 1;
                w[var10001] += beta[i] * violation;
            }
        }

        double var50;
        while(iter < max_iter) {
            double Gmax_new = 0.0D;
            double Gnorm1_new = 0.0D;

            for(i = 0; i < active_size; ++i) {
                int var48 = i + random.nextInt(active_size - i);
                swap(index, i, var48);
            }

            for(int s = 0; s < active_size; ++s) {
                i = index[s];
                double G = -y[i] + lambda[GETI_SVR(i)] * beta[i];
                double H = QD[i] + lambda[GETI_SVR(i)];
                nSV = (var39 = prob.x[i]).length;

                for(var37 = 0; var37 < nSV; ++var37) {
                    v = var39[var37];
                    int var51 = v.getIndex() - 1;
                    double val = v.getValue();
                    G += val * w[var51];
                }

                var50 = G + p;
                double var49 = G - p;
                violation = 0.0D;
                if(beta[i] == 0.0D) {
                    if(var50 < 0.0D) {
                        violation = -var50;
                    } else if(var49 > 0.0D) {
                        violation = var49;
                    } else if(var50 > Gmax_old && var49 < -Gmax_old) {
                        --active_size;
                        swap(index, s, active_size);
                        --s;
                        continue;
                    }
                } else if(beta[i] >= upper_bound[GETI_SVR(i)]) {
                    if(var50 > 0.0D) {
                        violation = var50;
                    } else if(var50 < -Gmax_old) {
                        --active_size;
                        swap(index, s, active_size);
                        --s;
                        continue;
                    }
                } else if(beta[i] <= -upper_bound[GETI_SVR(i)]) {
                    if(var49 < 0.0D) {
                        violation = -var49;
                    } else if(var49 > Gmax_old) {
                        --active_size;
                        swap(index, s, active_size);
                        --s;
                        continue;
                    }
                } else if(beta[i] > 0.0D) {
                    violation = Math.abs(var50);
                } else {
                    violation = Math.abs(var49);
                }

                Gmax_new = Math.max(Gmax_new, violation);
                Gnorm1_new += violation;
                double d;
                if(var50 < H * beta[i]) {
                    d = -var50 / H;
                } else if(var49 > H * beta[i]) {
                    d = -var49 / H;
                } else {
                    d = -beta[i];
                }

                if(Math.abs(d) >= 1.0E-12D) {
                    double beta_old = beta[i];
                    beta[i] = Math.min(Math.max(beta[i] + d, -upper_bound[GETI_SVR(i)]), upper_bound[GETI_SVR(i)]);
                    d = beta[i] - beta_old;
                    if(d != 0.0D) {
                        Feature[] var47;
                        int var46 = (var47 = prob.x[i]).length;

                        for(int var45 = 0; var45 < var46; ++var45) {
                            Feature xi = var47[var45];
                            var10001 = xi.getIndex() - 1;
                            w[var10001] += d * xi.getValue();
                        }
                    }
                }
            }

            if(iter == 0) {
                Gnorm1_init = Gnorm1_new;
            }

            ++iter;
            if(iter % 10 == 0) {
                info(".");
            }

            if(Gnorm1_new <= eps * Gnorm1_init) {
                if(active_size == l) {
                    break;
                }

                active_size = l;
                info("*");
                Gmax_old = 1.0D / 0.0;
            } else {
                Gmax_old = Gmax_new;
            }
        }

        info("%noptimization finished, #iter = %d%n", new Object[]{Integer.valueOf(iter)});
        if(iter >= max_iter) {
            info("%nWARNING: reaching max number of iterations%nUsing -s 11 may be faster%n%n");
        }

        var50 = 0.0D;
        nSV = 0;

        for(i = 0; i < w_size; ++i) {
            var50 += w[i] * w[i];
        }

        var50 *= 0.5D;

        for(i = 0; i < l; ++i) {
            var50 += p * Math.abs(beta[i]) - y[i] * beta[i] + 0.5D * lambda[GETI_SVR(i)] * beta[i] * beta[i];
            if(beta[i] != 0.0D) {
                ++nSV;
            }
        }

        info("Objective value = %g%n", new Object[]{Double.valueOf(var50)});
        info("nSV = %d%n", new Object[]{Integer.valueOf(nSV)});
    }

    private static void solve_l2r_lr_dual(Problem prob, double[] w, double eps, double Cp, double Cn) {
        int l = prob.l;
        int w_size = prob.n;
        int iter = 0;
        double[] xTx = new double[l];
        short max_iter = 1000;
        int[] index = new int[l];
        double[] alpha = new double[2 * l];
        byte[] y = new byte[l];
        byte max_inner_iter = 100;
        double innereps = 0.01D;
        double innereps_min = Math.min(1.0E-8D, eps);
        double[] upper_bound = new double[]{Cn, 0.0D, Cp};

        int i;
        for(i = 0; i < l; ++i) {
            if(prob.y[i] > 0.0D) {
                y[i] = 1;
            } else {
                y[i] = -1;
            }
        }

        for(i = 0; i < l; ++i) {
            alpha[2 * i] = Math.min(0.001D * upper_bound[GETI(y, i)], 1.0E-8D);
            alpha[2 * i + 1] = upper_bound[GETI(y, i)] - alpha[2 * i];
        }

        for(i = 0; i < w_size; ++i) {
            w[i] = 0.0D;
        }

        double C;
        int var10001;
        for(i = 0; i < l; index[i] = i++) {
            xTx[i] = 0.0D;
            Feature[] yi;
            int var26 = (yi = prob.x[i]).length;

            for(int Gmax = 0; Gmax < var26; ++Gmax) {
                Feature v = yi[Gmax];
                C = v.getValue();
                xTx[i] += C * C;
                var10001 = v.getIndex() - 1;
                w[var10001] += (double)y[i] * alpha[2 * i] * C;
            }
        }

        while(iter < max_iter) {
            int var56;
            for(i = 0; i < l; ++i) {
                var56 = i + random.nextInt(l - i);
                swap(index, i, var56);
            }

            var56 = 0;
            double var55 = 0.0D;

            for(int s = 0; s < l; ++s) {
                i = index[s];
                byte var57 = y[i];
                C = upper_bound[GETI(y, i)];
                double ywTx = 0.0D;
                double xisq = xTx[i];
                Feature[] var37;
                int b = (var37 = prob.x[i]).length;

                for(int var35 = 0; var35 < b; ++var35) {
                    Feature a = var37[var35];
                    ywTx += w[a.getIndex() - 1] * a.getValue();
                }

                ywTx *= (double)y[i];
                double var59 = xisq;
                double var58 = ywTx;
                int ind1 = 2 * i;
                int ind2 = 2 * i + 1;
                byte sign = 1;
                if(0.5D * xisq * (alpha[ind2] - alpha[ind1]) + ywTx < 0.0D) {
                    ind1 = 2 * i + 1;
                    ind2 = 2 * i;
                    sign = -1;
                }

                double alpha_old = alpha[ind1];
                double z = alpha_old;
                if(C - alpha_old < 0.5D * C) {
                    z = alpha_old * 0.1D;
                }

                double gp = xisq * (z - alpha_old) + (double)sign * ywTx + Math.log(z / (C - z));
                var55 = Math.max(var55, Math.abs(gp));
                double eta = 0.1D;

                int inner_iter;
                for(inner_iter = 0; inner_iter <= max_inner_iter && Math.abs(gp) >= innereps; ++inner_iter) {
                    double xi = var59 + C / (C - z) / z;
                    double tmpz = z - gp / xi;
                    if(tmpz <= 0.0D) {
                        z *= 0.1D;
                    } else {
                        z = tmpz;
                    }

                    gp = var59 * (z - alpha_old) + (double)sign * var58 + Math.log(z / (C - z));
                    ++var56;
                }

                if(inner_iter > 0) {
                    alpha[ind1] = z;
                    alpha[ind2] = C - z;
                    Feature[] var53;
                    int var61 = (var53 = prob.x[i]).length;

                    for(int var51 = 0; var51 < var61; ++var51) {
                        Feature var60 = var53[var51];
                        var10001 = var60.getIndex() - 1;
                        w[var10001] += (double)sign * (z - alpha_old) * (double)var57 * var60.getValue();
                    }
                }
            }

            ++iter;
            if(iter % 10 == 0) {
                info(".");
            }

            if(var55 < eps) {
                break;
            }

            if(var56 <= l / 10) {
                innereps = Math.max(innereps_min, 0.1D * innereps);
            }
        }

        info("%noptimization finished, #iter = %d%n", new Object[]{Integer.valueOf(iter)});
        if(iter >= max_iter) {
            info("%nWARNING: reaching max number of iterations%nUsing -s 0 may be faster (also see FAQ)%n%n");
        }

        double var54 = 0.0D;

        for(i = 0; i < w_size; ++i) {
            var54 += w[i] * w[i];
        }

        var54 *= 0.5D;

        for(i = 0; i < l; ++i) {
            var54 += alpha[2 * i] * Math.log(alpha[2 * i]) + alpha[2 * i + 1] * Math.log(alpha[2 * i + 1]) - upper_bound[GETI(y, i)] * Math.log(upper_bound[GETI(y, i)]);
        }

        info("Objective value = %g%n", new Object[]{Double.valueOf(var54)});
    }

    private static void solve_l1r_l2_svc(Problem prob_col, double[] w, double eps, double Cp, double Cn) {
        int l = prob_col.l;
        int w_size = prob_col.n;
        int iter = 0;
        short max_iter = 1000;
        int active_size = w_size;
        byte max_num_linesearch = 20;
        double sigma = 0.01D;
        double Gmax_old = 1.0D / 0.0;
        double Gnorm1_init = 0.0D;
        double loss_old = 0.0D;
        int[] index = new int[w_size];
        byte[] y = new byte[l];
        double[] b = new double[l];
        double[] xj_sq = new double[w_size];
        double[] C = new double[]{Cn, 0.0D, Cp};

        int j;
        for(j = 0; j < w_size; ++j) {
            w[j] = 0.0D;
        }

        for(j = 0; j < l; ++j) {
            b[j] = 1.0D;
            if(prob_col.y[j] > 0.0D) {
                y[j] = 1;
            } else {
                y[j] = -1;
            }
        }

        Feature v;
        int violation;
        Feature[] x;
        int nnz;
        int var52;
        double val;
        for(j = 0; j < w_size; ++j) {
            index[j] = j;
            xj_sq[j] = 0.0D;
            nnz = (x = prob_col.x[j]).length;

            for(var52 = 0; var52 < nnz; ++var52) {
                v = x[var52];
                violation = v.getIndex() - 1;
                v.setValue(v.getValue() * (double)y[violation]);
                val = v.getValue();
                b[violation] -= w[j] * val;
                xj_sq[j] += C[GETI(y, violation)] * val * val;
            }
        }

        double var68;
        while(iter < max_iter) {
            double Gmax_new = 0.0D;
            double Gnorm1_new = 0.0D;

            for(j = 0; j < active_size; ++j) {
                int var67 = j + random.nextInt(active_size - j);
                swap(index, var67, j);
            }

            for(int s = 0; s < active_size; ++s) {
                j = index[s];
                double G_loss = 0.0D;
                double H = 0.0D;
                nnz = (x = prob_col.x[j]).length;

                for(var52 = 0; var52 < nnz; ++var52) {
                    v = x[var52];
                    violation = v.getIndex() - 1;
                    if(b[violation] > 0.0D) {
                        val = v.getValue();
                        double tmp = C[GETI(y, violation)] * val;
                        G_loss -= tmp * b[violation];
                        H += tmp * val;
                    }
                }

                G_loss *= 2.0D;
                H *= 2.0D;
                H = Math.max(H, 1.0E-12D);
                var68 = G_loss + 1.0D;
                double var72 = G_loss - 1.0D;
                double var70 = 0.0D;
                if(w[j] == 0.0D) {
                    if(var68 < 0.0D) {
                        var70 = -var68;
                    } else if(var72 > 0.0D) {
                        var70 = var72;
                    } else if(var68 > Gmax_old / (double)l && var72 < -Gmax_old / (double)l) {
                        --active_size;
                        swap(index, s, active_size);
                        --s;
                        continue;
                    }
                } else if(w[j] > 0.0D) {
                    var70 = Math.abs(var68);
                } else {
                    var70 = Math.abs(var72);
                }

                Gmax_new = Math.max(Gmax_new, var70);
                Gnorm1_new += var70;
                double d;
                if(var68 < H * w[j]) {
                    d = -var68 / H;
                } else if(var72 > H * w[j]) {
                    d = -var72 / H;
                } else {
                    d = -w[j];
                }

                if(Math.abs(d) >= 1.0E-12D) {
                    double delta = Math.abs(w[j] + d) - Math.abs(w[j]) + G_loss * d;
                    double d_old = 0.0D;

                    int var10001;
                    int num_linesearch;
                    int var62;
                    label217:
                    for(num_linesearch = 0; num_linesearch < max_num_linesearch; ++num_linesearch) {
                        double d_diff = d_old - d;
                        double cond = Math.abs(w[j] + d) - Math.abs(w[j]) - sigma * delta;
                        double appxcond = xj_sq[j] * d * d + G_loss * d + cond;
                        Feature[] var63;
                        int x1;
                        Feature i;
                        if(appxcond <= 0.0D) {
                            var62 = (var63 = prob_col.x[j]).length;
                            x1 = 0;

                            while(true) {
                                if(x1 >= var62) {
                                    break label217;
                                }

                                i = var63[x1];
                                var10001 = i.getIndex() - 1;
                                b[var10001] += d_diff * i.getValue();
                                ++x1;
                            }
                        }

                        double loss_new;
                        int ind;
                        double b_new;
                        if(num_linesearch == 0) {
                            loss_old = 0.0D;
                            loss_new = 0.0D;
                            var62 = (var63 = prob_col.x[j]).length;

                            for(x1 = 0; x1 < var62; ++x1) {
                                i = var63[x1];
                                ind = i.getIndex() - 1;
                                if(b[ind] > 0.0D) {
                                    loss_old += C[GETI(y, ind)] * b[ind] * b[ind];
                                }

                                b_new = b[ind] + d_diff * i.getValue();
                                b[ind] = b_new;
                                if(b_new > 0.0D) {
                                    loss_new += C[GETI(y, ind)] * b_new * b_new;
                                }
                            }
                        } else {
                            loss_new = 0.0D;
                            var62 = (var63 = prob_col.x[j]).length;

                            for(x1 = 0; x1 < var62; ++x1) {
                                i = var63[x1];
                                ind = i.getIndex() - 1;
                                b_new = b[ind] + d_diff * i.getValue();
                                b[ind] = b_new;
                                if(b_new > 0.0D) {
                                    loss_new += C[GETI(y, ind)] * b_new * b_new;
                                }
                            }
                        }

                        cond = cond + loss_new - loss_old;
                        if(cond <= 0.0D) {
                            break;
                        }

                        d_old = d;
                        d *= 0.5D;
                        delta *= 0.5D;
                    }

                    w[j] += d;
                    if(num_linesearch >= max_num_linesearch) {
                        info("#");

                        int var74;
                        for(var74 = 0; var74 < l; ++var74) {
                            b[var74] = 1.0D;
                        }

                        for(var74 = 0; var74 < w_size; ++var74) {
                            if(w[var74] != 0.0D) {
                                Feature[] var77;
                                int var76 = (var77 = prob_col.x[var74]).length;

                                for(var62 = 0; var62 < var76; ++var62) {
                                    Feature var75 = var77[var62];
                                    var10001 = var75.getIndex() - 1;
                                    b[var10001] -= w[var74] * var75.getValue();
                                }
                            }
                        }
                    }
                }
            }

            if(iter == 0) {
                Gnorm1_init = Gnorm1_new;
            }

            ++iter;
            if(iter % 10 == 0) {
                info(".");
            }

            if(Gmax_new <= eps * Gnorm1_init) {
                if(active_size == w_size) {
                    break;
                }

                active_size = w_size;
                info("*");
                Gmax_old = 1.0D / 0.0;
            } else {
                Gmax_old = Gmax_new;
            }
        }

        info("%noptimization finished, #iter = %d%n", new Object[]{Integer.valueOf(iter)});
        if(iter >= max_iter) {
            info("%nWARNING: reaching max number of iterations%n");
        }

        var68 = 0.0D;
        nnz = 0;

        for(j = 0; j < w_size; ++j) {
            Feature[] var71;
            int var69 = (var71 = prob_col.x[j]).length;

            for(violation = 0; violation < var69; ++violation) {
                Feature var73 = var71[violation];
                var73.setValue(var73.getValue() * prob_col.y[var73.getIndex() - 1]);
            }

            if(w[j] != 0.0D) {
                var68 += Math.abs(w[j]);
                ++nnz;
            }
        }

        for(j = 0; j < l; ++j) {
            if(b[j] > 0.0D) {
                var68 += C[GETI(y, j)] * b[j] * b[j];
            }
        }

        info("Objective value = %g%n", new Object[]{Double.valueOf(var68)});
        info("#nonzeros/#features = %d/%d%n", new Object[]{Integer.valueOf(nnz), Integer.valueOf(w_size)});
    }

    private static void solve_l1r_lr(Problem prob_col, double[] w, double eps, double Cp, double Cn) {
        int l = prob_col.l;
        int w_size = prob_col.n;
        int newton_iter = 0;
        boolean iter = false;
        byte max_newton_iter = 100;
        short max_iter = 1000;
        byte max_num_linesearch = 20;
        double nu = 1.0E-12D;
        double inner_eps = 1.0D;
        double sigma = 0.01D;
        double Gnorm1_init = 0.0D;
        double Gmax_old = 1.0D / 0.0;
        double QP_Gmax_old = 1.0D / 0.0;
        int[] index = new int[w_size];
        byte[] y = new byte[l];
        double[] Hdiag = new double[w_size];
        double[] Grad = new double[w_size];
        double[] wpd = new double[w_size];
        double[] xjneg_sum = new double[w_size];
        double[] xTd = new double[l];
        double[] exp_wTx = new double[l];
        double[] exp_wTx_new = new double[l];
        double[] tau = new double[l];
        double[] D = new double[l];
        double[] C = new double[]{Cn, 0.0D, Cp};

        int j;
        for(j = 0; j < w_size; ++j) {
            w[j] = 0.0D;
        }

        for(j = 0; j < l; ++j) {
            if(prob_col.y[j] > 0.0D) {
                y[j] = 1;
            } else {
                y[j] = -1;
            }

            exp_wTx[j] = 0.0D;
        }

        double w_norm = 0.0D;

        int i;
        int nnz;
        Feature[] var70;
        int violation;
        Feature v;
        for(j = 0; j < w_size; ++j) {
            w_norm += Math.abs(w[j]);
            wpd[j] = w[j];
            index[j] = j;
            xjneg_sum[j] = 0.0D;
            nnz = (var70 = prob_col.x[j]).length;

            for(i = 0; i < nnz; ++i) {
                v = var70[i];
                violation = v.getIndex() - 1;
                double val = v.getValue();
                exp_wTx[violation] += w[j] * val;
                if(y[violation] == -1) {
                    xjneg_sum[j] += C[GETI(y, violation)] * val;
                }
            }
        }

        double var80;
        for(j = 0; j < l; ++j) {
            exp_wTx[j] = Math.exp(exp_wTx[j]);
            var80 = 1.0D / (1.0D + exp_wTx[j]);
            tau[j] = C[GETI(y, j)] * var80;
            D[j] = C[GETI(y, j)] * exp_wTx[j] * var80 * var80;
        }

        while(newton_iter < max_newton_iter) {
            double Gmax_new = 0.0D;
            double Gnorm1_new = 0.0D;
            int active_size = w_size;

            int s;
            Feature var85;
            int var84;
            double var86;
            Feature[] var83;
            double var82;
            for(s = 0; s < active_size; ++s) {
                j = index[s];
                Hdiag[j] = nu;
                Grad[j] = 0.0D;
                var80 = 0.0D;
                violation = (var83 = prob_col.x[j]).length;

                for(var84 = 0; var84 < violation; ++var84) {
                    var85 = var83[var84];
                    int x = var85.getIndex() - 1;
                    Hdiag[j] += var85.getValue() * var85.getValue() * D[x];
                    var80 += var85.getValue() * tau[x];
                }

                Grad[j] = -var80 + xjneg_sum[j];
                var86 = Grad[j] + 1.0D;
                var82 = Grad[j] - 1.0D;
                double var81 = 0.0D;
                if(w[j] == 0.0D) {
                    if(var86 < 0.0D) {
                        var81 = -var86;
                    } else if(var82 > 0.0D) {
                        var81 = var82;
                    } else if(var86 > Gmax_old / (double)l && var82 < -Gmax_old / (double)l) {
                        --active_size;
                        swap(index, s, active_size);
                        --s;
                        continue;
                    }
                } else if(w[j] > 0.0D) {
                    var81 = Math.abs(var86);
                } else {
                    var81 = Math.abs(var82);
                }

                Gmax_new = Math.max(Gmax_new, var81);
                Gnorm1_new += var81;
            }

            if(newton_iter == 0) {
                Gnorm1_init = Gnorm1_new;
            }

            if(Gnorm1_new <= eps * Gnorm1_init) {
                break;
            }

            int var78 = 0;
            QP_Gmax_old = 1.0D / 0.0;
            int QP_active_size = active_size;

            int var79;
            for(var79 = 0; var79 < l; ++var79) {
                xTd[var79] = 0.0D;
            }

            while(var78 < max_iter) {
                double QP_Gmax_new = 0.0D;
                double QP_Gnorm1_new = 0.0D;

                for(j = 0; j < QP_active_size; ++j) {
                    var79 = random.nextInt(QP_active_size - j);
                    swap(index, var79, j);
                }

                for(s = 0; s < QP_active_size; ++s) {
                    j = index[s];
                    double H = Hdiag[j];
                    double G = Grad[j] + (wpd[j] - w[j]) * nu;
                    nnz = (var70 = prob_col.x[j]).length;

                    for(i = 0; i < nnz; ++i) {
                        v = var70[i];
                        violation = v.getIndex() - 1;
                        G += v.getValue() * D[violation] * xTd[violation];
                    }

                    var80 = G + 1.0D;
                    var86 = G - 1.0D;
                    var82 = 0.0D;
                    if(wpd[j] == 0.0D) {
                        if(var80 < 0.0D) {
                            var82 = -var80;
                        } else if(var86 > 0.0D) {
                            var82 = var86;
                        } else if(var80 > QP_Gmax_old / (double)l && var86 < -QP_Gmax_old / (double)l) {
                            --QP_active_size;
                            swap(index, s, QP_active_size);
                            --s;
                            continue;
                        }
                    } else if(wpd[j] > 0.0D) {
                        var82 = Math.abs(var80);
                    } else {
                        var82 = Math.abs(var86);
                    }

                    QP_Gmax_new = Math.max(QP_Gmax_new, var82);
                    QP_Gnorm1_new += var82;
                    double z;
                    if(var80 < H * wpd[j]) {
                        z = -var80 / H;
                    } else if(var86 > H * wpd[j]) {
                        z = -var86 / H;
                    } else {
                        z = -wpd[j];
                    }

                    if(Math.abs(z) >= 1.0E-12D) {
                        z = Math.min(Math.max(z, -10.0D), 10.0D);
                        wpd[j] += z;
                        Feature[] var76;
                        int var75 = (var76 = prob_col.x[j]).length;

                        for(int var74 = 0; var74 < var75; ++var74) {
                            Feature var87 = var76[var74];
                            int ind = var87.getIndex() - 1;
                            xTd[ind] += var87.getValue() * z;
                        }
                    }
                }

                ++var78;
                if(QP_Gnorm1_new <= inner_eps * Gnorm1_init) {
                    if(QP_active_size == active_size) {
                        break;
                    }

                    QP_active_size = active_size;
                    QP_Gmax_old = 1.0D / 0.0;
                } else {
                    QP_Gmax_old = QP_Gmax_new;
                }
            }

            if(var78 >= max_iter) {
                info("WARNING: reaching max number of inner iterations%n");
            }

            double delta = 0.0D;
            double w_norm_new = 0.0D;

            for(j = 0; j < w_size; ++j) {
                delta += Grad[j] * (wpd[j] - w[j]);
                if(wpd[j] != 0.0D) {
                    w_norm_new += Math.abs(wpd[j]);
                }
            }

            delta += w_norm_new - w_norm;
            double negsum_xTd = 0.0D;

            for(var79 = 0; var79 < l; ++var79) {
                if(y[var79] == -1) {
                    negsum_xTd += C[GETI(y, var79)] * xTd[var79];
                }
            }

            label254:
            for(var79 = 0; var79 < max_num_linesearch; ++var79) {
                double cond = w_norm_new - w_norm + negsum_xTd - sigma * delta;

                for(i = 0; i < l; ++i) {
                    var86 = Math.exp(xTd[i]);
                    exp_wTx_new[i] = exp_wTx[i] * var86;
                    cond += C[GETI(y, i)] * Math.log((1.0D + exp_wTx_new[i]) / (var86 + exp_wTx_new[i]));
                }

                if(cond <= 0.0D) {
                    w_norm = w_norm_new;

                    for(j = 0; j < w_size; ++j) {
                        w[j] = wpd[j];
                    }

                    i = 0;

                    while(true) {
                        if(i >= l) {
                            break label254;
                        }

                        exp_wTx[i] = exp_wTx_new[i];
                        var86 = 1.0D / (1.0D + exp_wTx[i]);
                        tau[i] = C[GETI(y, i)] * var86;
                        D[i] = C[GETI(y, i)] * exp_wTx[i] * var86 * var86;
                        ++i;
                    }
                }

                w_norm_new = 0.0D;

                for(j = 0; j < w_size; ++j) {
                    wpd[j] = (w[j] + wpd[j]) * 0.5D;
                    if(wpd[j] != 0.0D) {
                        w_norm_new += Math.abs(wpd[j]);
                    }
                }

                delta *= 0.5D;
                negsum_xTd *= 0.5D;

                for(i = 0; i < l; ++i) {
                    xTd[i] *= 0.5D;
                }
            }

            if(var79 >= max_num_linesearch) {
                for(i = 0; i < l; ++i) {
                    exp_wTx[i] = 0.0D;
                }

                for(i = 0; i < w_size; ++i) {
                    if(w[i] != 0.0D) {
                        violation = (var83 = prob_col.x[i]).length;

                        for(var84 = 0; var84 < violation; ++var84) {
                            var85 = var83[var84];
                            int var10001 = var85.getIndex() - 1;
                            exp_wTx[var10001] += w[i] * var85.getValue();
                        }
                    }
                }

                for(i = 0; i < l; ++i) {
                    exp_wTx[i] = Math.exp(exp_wTx[i]);
                }
            }

            if(var78 == 1) {
                inner_eps *= 0.25D;
            }

            ++newton_iter;
            Gmax_old = Gmax_new;
            info("iter %3d  #CD cycles %d%n", new Object[]{Integer.valueOf(newton_iter), Integer.valueOf(var78)});
        }

        info("=========================%n");
        info("optimization finished, #iter = %d%n", new Object[]{Integer.valueOf(newton_iter)});
        if(newton_iter >= max_newton_iter) {
            info("WARNING: reaching max number of iterations%n");
        }

        var80 = 0.0D;
        nnz = 0;

        for(j = 0; j < w_size; ++j) {
            if(w[j] != 0.0D) {
                var80 += Math.abs(w[j]);
                ++nnz;
            }
        }

        for(j = 0; j < l; ++j) {
            if(y[j] == 1) {
                var80 += C[GETI(y, j)] * Math.log(1.0D + 1.0D / exp_wTx[j]);
            } else {
                var80 += C[GETI(y, j)] * Math.log(1.0D + exp_wTx[j]);
            }
        }

        info("Objective value = %g%n", new Object[]{Double.valueOf(var80)});
        info("#nonzeros/#features = %d/%d%n", new Object[]{Integer.valueOf(nnz), Integer.valueOf(w_size)});
    }

    static Problem transpose(Problem prob) {
        int l = prob.l;
        int n = prob.n;
        int[] col_ptr = new int[n + 1];
        Problem prob_col = new Problem();
        prob_col.l = l;
        prob_col.n = n;
        prob_col.y = new double[l];
        prob_col.x = new Feature[n][];

        int i;
        for(i = 0; i < l; ++i) {
            prob_col.y[i] = prob.y[i];
        }

        int index;
        for(i = 0; i < l; ++i) {
            Feature[] var9;
            index = (var9 = prob.x[i]).length;

            for(int x = 0; x < index; ++x) {
                Feature j = var9[x];
                ++col_ptr[j.getIndex()];
            }
        }

        for(i = 0; i < n; ++i) {
            prob_col.x[i] = new Feature[col_ptr[i + 1]];
            col_ptr[i] = 0;
        }

        for(i = 0; i < l; ++i) {
            for(int var10 = 0; var10 < prob.x[i].length; ++var10) {
                Feature var11 = prob.x[i][var10];
                index = var11.getIndex() - 1;
                prob_col.x[index][col_ptr[index]] = new FeatureNode(i + 1, var11.getValue());
                ++col_ptr[index];
            }
        }

        return prob_col;
    }

    static void swap(double[] array, int idxA, int idxB) {
        double temp = array[idxA];
        array[idxA] = array[idxB];
        array[idxB] = temp;
    }

    static void swap(int[] array, int idxA, int idxB) {
        int temp = array[idxA];
        array[idxA] = array[idxB];
        array[idxB] = temp;
    }

    static void swap(IntArrayPointer array, int idxA, int idxB) {
        int temp = array.get(idxA);
        array.set(idxA, array.get(idxB));
        array.set(idxB, temp);
    }

    public static Model train(Problem prob, Parameter param) {
        if(prob == null) {
            throw new IllegalArgumentException("problem must not be null");
        } else if(param == null) {
            throw new IllegalArgumentException("parameter must not be null");
        } else if(prob.n == 0) {
            throw new IllegalArgumentException("problem has zero features");
        } else if(prob.l == 0) {
            throw new IllegalArgumentException("problem has zero instances");
        } else {
            Feature[][] model = prob.x;
            int w_size = prob.x.length;

            int n;
            int nr_class;
            for(n = 0; n < w_size; ++n) {
                Feature[] l = model[n];
                int perm = 0;
                Feature[] start = l;
                int label = l.length;

                for(nr_class = 0; nr_class < label; ++nr_class) {
                    Feature rv = start[nr_class];
                    if(rv.getIndex() <= perm) {
                        throw new IllegalArgumentException("feature nodes must be sorted by index in ascending order");
                    }

                    perm = rv.getIndex();
                }
            }

            int var21 = prob.l;
            n = prob.n;
            w_size = prob.n;
            Model var22 = new Model();
            if(prob.bias >= 0.0D) {
                var22.nr_feature = n - 1;
            } else {
                var22.nr_feature = n;
            }

            var22.solverType = param.solverType;
            var22.bias = prob.bias;
            if(param.solverType != SolverType.L2R_L2LOSS_SVR && param.solverType != SolverType.L2R_L1LOSS_SVR_DUAL && param.solverType != SolverType.L2R_L2LOSS_SVR_DUAL) {
                int[] var23 = new int[var21];
                GroupClassesReturn var24 = groupClasses(prob, var23);
                nr_class = var24.nr_class;
                int[] var25 = var24.label;
                int[] var26 = var24.start;
                int[] count = var24.count;
                checkProblemSize(n, nr_class);
                var22.nr_class = nr_class;
                var22.label = new int[nr_class];

                for(int weighted_C = 0; weighted_C < nr_class; ++weighted_C) {
                    var22.label[weighted_C] = var25[weighted_C];
                }

                double[] var27 = new double[nr_class];

                int x;
                for(x = 0; x < nr_class; ++x) {
                    var27[x] = param.C;
                }

                int sub_prob;
                for(x = 0; x < param.getNumWeights(); ++x) {
                    for(sub_prob = 0; sub_prob < nr_class && param.weightLabel[x] != var25[sub_prob]; ++sub_prob) {
                        ;
                    }

                    if(sub_prob == nr_class) {
                        throw new IllegalArgumentException("class label " + param.weightLabel[x] + " specified in weight is not found");
                    }

                    var27[sub_prob] *= param.weight[x];
                }

                Feature[][] var29 = new Feature[var21][];

                for(sub_prob = 0; sub_prob < var21; ++sub_prob) {
                    var29[sub_prob] = prob.x[var23[sub_prob]];
                }

                Problem var28 = new Problem();
                var28.l = var21;
                var28.n = n;
                var28.x = new Feature[var28.l][];
                var28.y = new double[var28.l];

                int w;
                for(w = 0; w < var28.l; ++w) {
                    var28.x[w] = var29[w];
                }

                int i;
                if(param.solverType == SolverType.MCSVM_CS) {
                    var22.w = new double[n * nr_class];

                    for(w = 0; w < nr_class; ++w) {
                        for(i = var26[w]; i < var26[w] + count[w]; ++i) {
                            var28.y[i] = (double)w;
                        }
                    }

                    SolverMCSVM_CS var30 = new SolverMCSVM_CS(var28, nr_class, var27, param.eps);
                    var30.solve(var22.w);
                } else if(nr_class == 2) {
                    var22.w = new double[w_size];
                    w = var26[0] + count[0];

                    for(i = 0; i < w; ++i) {
                        var28.y[i] = 1.0D;
                    }

                    while(i < var28.l) {
                        var28.y[i] = -1.0D;
                        ++i;
                    }

                    train_one(var28, param, var22.w, var27[0], var27[1]);
                } else {
                    var22.w = new double[w_size * nr_class];
                    double[] var31 = new double[w_size];

                    for(i = 0; i < nr_class; ++i) {
                        int si = var26[i];
                        int ei = si + count[i];

                        int k;
                        for(k = 0; k < si; ++k) {
                            var28.y[k] = -1.0D;
                        }

                        while(k < ei) {
                            var28.y[k] = 1.0D;
                            ++k;
                        }

                        while(k < var28.l) {
                            var28.y[k] = -1.0D;
                            ++k;
                        }

                        train_one(var28, param, var31, var27[i], param.C);

                        for(int j = 0; j < n; ++j) {
                            var22.w[j * nr_class + i] = var31[j];
                        }
                    }
                }
            } else {
                var22.w = new double[w_size];
                var22.nr_class = 2;
                var22.label = null;
                checkProblemSize(n, var22.nr_class);
                train_one(prob, param, var22.w, 0.0D, 0.0D);
            }

            return var22;
        }
    }

    private static void checkProblemSize(int n, int nr_class) {
        if(n >= 2147483647 / nr_class || n * nr_class < 0) {
            throw new IllegalArgumentException("\'number of classes\' * \'number of instances\' is too large: " + nr_class + "*" + n);
        }
    }

    private static void train_one(Problem prob, Parameter param, double[] w, double Cp, double Cn) {
        double eps = param.eps;
        int pos = 0;

        int neg;
        for(neg = 0; neg < prob.l; ++neg) {
            if(prob.y[neg] > 0.0D) {
                ++pos;
            }
        }

        neg = prob.l - pos;
        double primal_solver_tol = eps * (double)Math.max(Math.min(pos, neg), 1) / (double)prob.l;
        L2R_L2_SvrFunction fun_obj = null;
        double[] C;
        int tron_obj;
        Tron var19;
        Problem var18;
        switch(param.solverType.ordinal()) {
            case 1:
                C = new double[prob.l];

                for(tron_obj = 0; tron_obj < prob.l; ++tron_obj) {
                    if(prob.y[tron_obj] > 0.0D) {
                        C[tron_obj] = Cp;
                    } else {
                        C[tron_obj] = Cn;
                    }
                }

                L2R_LrFunction var17 = new L2R_LrFunction(prob, C);
                var19 = new Tron(var17, primal_solver_tol);
                var19.tron(w);
                break;
            case 2:
                solve_l2r_l1l2_svc(prob, w, eps, Cp, Cn, SolverType.L2R_L2LOSS_SVC_DUAL);
                break;
            case 3:
                C = new double[prob.l];

                for(tron_obj = 0; tron_obj < prob.l; ++tron_obj) {
                    if(prob.y[tron_obj] > 0.0D) {
                        C[tron_obj] = Cp;
                    } else {
                        C[tron_obj] = Cn;
                    }
                }

                L2R_L2_SvcFunction var16 = new L2R_L2_SvcFunction(prob, C);
                var19 = new Tron(var16, primal_solver_tol);
                var19.tron(w);
                break;
            case 4:
                solve_l2r_l1l2_svc(prob, w, eps, Cp, Cn, SolverType.L2R_L1LOSS_SVC_DUAL);
                break;
            case 5:
            default:
                throw new IllegalStateException("unknown solver type: " + param.solverType);
            case 6:
                var18 = transpose(prob);
                solve_l1r_l2_svc(var18, w, primal_solver_tol, Cp, Cn);
                break;
            case 7:
                var18 = transpose(prob);
                solve_l1r_lr(var18, w, primal_solver_tol, Cp, Cn);
                break;
            case 8:
                solve_l2r_lr_dual(prob, w, eps, Cp, Cn);
                break;
            case 9:
                C = new double[prob.l];

                for(tron_obj = 0; tron_obj < prob.l; ++tron_obj) {
                    C[tron_obj] = param.C;
                }

                fun_obj = new L2R_L2_SvrFunction(prob, C, param.p);
                var19 = new Tron(fun_obj, param.eps);
                var19.tron(w);
                break;
            case 10:
            case 11:
                solve_l2r_l1l2_svr(prob, w, param);
        }

    }

    public static void disableDebugOutput() {
        setDebugOutput((PrintStream)null);
    }

    public static void enableDebugOutput() {
        setDebugOutput(System.out);
    }

    public static void setDebugOutput(PrintStream debugOutput) {
        Object var1 = OUTPUT_MUTEX;
        synchronized(OUTPUT_MUTEX) {
            DEBUG_OUTPUT = debugOutput;
        }
    }

    public static void resetRandom() {
        random = new Random(0L);
    }

    private static class GroupClassesReturn {
        final int[] count;
        final int[] label;
        final int nr_class;
        final int[] start;

        GroupClassesReturn(int nr_class, int[] label, int[] start, int[] count) {
            this.nr_class = nr_class;
            this.label = label;
            this.start = start;
            this.count = count;
        }
    }
}

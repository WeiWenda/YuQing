//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by Fernflower decompiler)
//

package de.bwaldvogel.liblinear;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Formatter;
import java.util.NoSuchElementException;
import java.util.StringTokenizer;
import java.util.regex.Pattern;

public class Predict {
    private static boolean flag_predict_probability = false;
    private static final Pattern COLON = Pattern.compile(":");

    public Predict() {
    }

    static void doPredict(BufferedReader reader, Writer writer, Model model) throws IOException {
        int correct = 0;
        int total = 0;
        double error = 0.0D;
        double sump = 0.0D;
        double sumt = 0.0D;
        double sumpp = 0.0D;
        double sumtt = 0.0D;
        double sumpt = 0.0D;
        int nr_class = model.getNrClass();
        double[] prob_estimates = null;
        int nr_feature = model.getNrFeature();
        int n;
        if(model.bias >= 0.0D) {
            n = nr_feature + 1;
        } else {
            n = nr_feature;
        }

        if(flag_predict_probability && !model.isProbabilityModel()) {
            throw new IllegalArgumentException("probability output is only supported for logistic regression");
        } else {
            Formatter out = new Formatter(writer);
            if(flag_predict_probability) {
                int[] line = model.getLabels();
                prob_estimates = new double[nr_class];
                Linear.printf(out, "labels", new Object[0]);

                for(int x = 0; x < nr_class; ++x) {
                    Linear.printf(out, " %d", new Object[]{Integer.valueOf(line[x])});
                }

                Linear.printf(out, "\n", new Object[0]);
            }

            for(String var34 = null; (var34 = reader.readLine()) != null; ++total) {
                ArrayList var35 = new ArrayList();
                StringTokenizer st = new StringTokenizer(var34, " \t\n");

                double target_label;
                try {
                    String nodes = st.nextToken();
                    target_label = Linear.atof(nodes);
                } catch (NoSuchElementException var33) {
                    throw new RuntimeException("Wrong input format at line " + (total + 1), var33);
                }

                while(st.hasMoreTokens()) {
                    String[] var39 = COLON.split(st.nextToken(), 2);
                    if(var39 == null || var39.length < 2) {
                        throw new RuntimeException("Wrong input format at line " + (total + 1));
                    }

                    try {
                        int predict_label = Linear.atoi(var39[0]);
                        double val = Linear.atof(var39[1]);
                        if(predict_label <= nr_feature) {
                            FeatureNode node = new FeatureNode(predict_label, val);
                            var35.add(node);
                        }
                    } catch (NumberFormatException var32) {
                        throw new RuntimeException("Wrong input format at line " + (total + 1), var32);
                    }
                }

                if(model.bias >= 0.0D) {
                    FeatureNode var37 = new FeatureNode(n, model.bias);
                    var35.add(var37);
                }

                Feature[] var36 = new Feature[var35.size()];
                var36 = (Feature[])var35.toArray(var36);
                double var38;
                if(flag_predict_probability) {
                    assert prob_estimates != null;

                    var38 = Linear.predictProbability(model, var36, prob_estimates);
                    Linear.printf(out, "%g", new Object[]{Double.valueOf(var38)});

                    for(int j = 0; j < model.nr_class; ++j) {
                        Linear.printf(out, " %g", new Object[]{Double.valueOf(prob_estimates[j])});
                    }

                    Linear.printf(out, "\n", new Object[0]);
                } else {
                    var38 = Linear.predict(model, var36);
                    Linear.printf(out, "%g\n", new Object[]{Double.valueOf(var38)});
                }

                if(var38 == target_label) {
                    ++correct;
                }

                error += (var38 - target_label) * (var38 - target_label);
                sump += var38;
                sumt += target_label;
                sumpp += var38 * var38;
                sumtt += target_label * target_label;
                sumpt += var38 * target_label;
            }

            if(model.solverType.isSupportVectorRegression()) {
                Linear.info("Mean squared error = %g (regression)%n", new Object[]{Double.valueOf(error / (double)total)});
                Linear.info("Squared correlation coefficient = %g (regression)%n", new Object[]{Double.valueOf(((double)total * sumpt - sump * sumt) * ((double)total * sumpt - sump * sumt) / (((double)total * sumpp - sump * sump) * ((double)total * sumtt - sumt * sumt)))});
            }

        }
    }

    private static void exit_with_help() {
        System.out.printf("Usage: predict [options] test_file model_file output_file%noptions:%n-b probability_estimates: whether to output probability estimates, 0 or 1 (default 0); currently for logistic regression only%n-q quiet mode (no outputs)%n", new Object[0]);
        System.exit(1);
    }

    public static void main(String[] args) throws IOException {
        int i;
        for(i = 0; i < args.length && args[i].charAt(0) == 45; ++i) {
            ++i;
            switch(args[i - 1].charAt(1)) {
            case 'b':
                try {
                    flag_predict_probability = Linear.atoi(args[i]) != 0;
                } catch (NumberFormatException var9) {
                    exit_with_help();
                }
                break;
            case 'q':
                --i;
                Linear.disableDebugOutput();
                break;
            default:
                System.err.printf("unknown option: -%d%n", new Object[]{Character.valueOf(args[i - 1].charAt(1))});
                exit_with_help();
            }
        }

        if(i >= args.length || args.length <= i + 2) {
            exit_with_help();
        }

        BufferedReader reader = null;
        BufferedWriter writer = null;

        Configuration conf = new Configuration();
        conf.addResource(new Path("projectFile/core-site.xml"));
        conf.addResource(new Path("projectFile/hdfs-site.xml"));

        try {
            FileSystem hdfs = FileSystem.get(conf);
            reader = new BufferedReader(new InputStreamReader(hdfs.open(new Path(args[i])), Linear.FILE_CHARSET));
            writer = new BufferedWriter(new OutputStreamWriter(hdfs.create(new Path(args[i + 2])), Linear.FILE_CHARSET));
            Model model = Linear.loadModel(args[i + 1]);
            doPredict(reader, writer, model);
        } finally {
            Linear.closeQuietly(reader);
            Linear.closeQuietly(writer);
        }

    }
}

package org.apache.lib;

/**
 * Created by liumeng on 26.12.15.
 */


        import static org.apache.lib.Linear.atof;
        import static org.apache.lib.Linear.atoi;
        import static org.apache.lib.Linear.closeQuietly;
        import static org.apache.lib.Linear.printf;
        import static org.apache.lib.Linear.info;

        import org.apache.hadoop.conf.Configuration;
        import org.apache.hadoop.fs.FileSystem;
        import org.apache.hadoop.fs.Path;

        import java.io.BufferedReader;
        import java.io.BufferedWriter;
        import java.io.FileNotFoundException;
        import java.io.IOException;
        import java.io.InputStreamReader;
        import java.io.OutputStreamWriter;
        import java.util.ArrayList;
        import java.util.Formatter;
        import java.util.List;
        import java.util.NoSuchElementException;
        import java.util.StringTokenizer;
        import java.util.regex.Pattern;

public class Predict {

    private static boolean       flag_predict_probability = false;

    private static final Pattern COLON                    = Pattern.compile(":");

    /**
     * <p><b>Note: The streams are NOT closed</b></p>
     */
    static void doPredict(BufferedReader reader, BufferedWriter writer, Model model) throws IOException {
        int correct = 0;
        int total = 0;
        double error = 0;
        double sump = 0, sumt = 0, sumpp = 0, sumtt = 0, sumpt = 0;

        int nr_class = model.getNrClass();
        double[] prob_estimates = null;
        int n;
        int nr_feature = model.getNrFeature();
        if (model.bias >= 0)
            n = nr_feature + 1;
        else
            n = nr_feature;

        if (flag_predict_probability && !model.isProbabilityModel()) {
            throw new IllegalArgumentException("probability output is only supported for logistic regression");
        }

        Formatter out = new Formatter(writer);

        if (flag_predict_probability) {
            int[] labels = model.getLabels();
            prob_estimates = new double[nr_class];

            printf(out, "labels");
            for (int j = 0; j < nr_class; j++)
                printf(out, " %d", labels[j]);
            printf(out, "\n");
        }


        String line = null;
        while ((line = reader.readLine()) != null) {
            List<Feature> x = new ArrayList<Feature>();
            StringTokenizer st = new StringTokenizer(line, " \t\n");
            double target_label;
            try {
                String label = st.nextToken();
                target_label = atof(label);
            } catch (NoSuchElementException e) {
                throw new RuntimeException("Wrong input format at line " + (total + 1), e);
            }

            while (st.hasMoreTokens()) {
                String[] split = COLON.split(st.nextToken(), 2);
                if (split == null || split.length < 2) {
                    throw new RuntimeException("Wrong input format at line " + (total + 1));
                }

                try {
                    int idx = atoi(split[0]);
                    double val = atof(split[1]);

                    // feature indices larger than those in training are not used
                    if (idx <= nr_feature) {
                        Feature node = new FeatureNode(idx, val);
                        x.add(node);
                    }
                } catch (NumberFormatException e) {
                    throw new RuntimeException("Wrong input format at line " + (total + 1), e);
                }
            }

            if (model.bias >= 0) {
                Feature node = new FeatureNode(n, model.bias);
                x.add(node);
            }

            Feature[] nodes = new Feature[x.size()];
            nodes = x.toArray(nodes);

            double predict_label;

            if (flag_predict_probability) {
                assert prob_estimates != null;
                predict_label = Linear.predictProbability(model, nodes, prob_estimates);
                printf(out, "%g", predict_label);
                for (int j = 0; j < model.nr_class; j++)
                    printf(out, " %g", prob_estimates[j]);
                printf(out, "\n");
            } else {
                predict_label = Linear.predict(model, nodes);
                System.out.println("输出数据");
                System.out.println("输出数据");
                printf(out, "%g\n", predict_label);
            }


            if (predict_label == target_label) {
                ++correct;
            }

            error += (predict_label - target_label) * (predict_label - target_label);
            sump += predict_label;
            sumt += target_label;
            sumpp += predict_label * predict_label;
            sumtt += target_label * target_label;
            sumpt += predict_label * target_label;
            ++total;
        }

        if (model.solverType.isSupportVectorRegression()) //
        {
            info("Mean squared error = %g (regression)%n", error / total);
            info("Squared correlation coefficient = %g (regression)%n", //
                    ((total * sumpt - sump * sumt) * (total * sumpt - sump * sumt)) / ((total * sumpp - sump * sump) * (total * sumtt - sumt * sumt)));
        } else {
            info("Accuracy = %g%% (%d/%d)%n", (double)correct / total * 100, correct, total);
        }
    }

    private static void exit_with_help() {
        System.out.printf("Usage: predict [options] test_file model_file output_file%n" //
                + "options:%n" //
                + "-b probability_estimates: whether to output probability estimates, 0 or 1 (default 0); currently for logistic regression only%n" //
                + "-q quiet mode (no outputs)%n");
        System.exit(1);
    }

    public void ExecPredict(String[] args1) throws IOException{
        int i;
        for (i = 0; i < args1.length; i++) {
            if (args1[i].charAt(0) != '-') break;
            ++i;
            switch (args1[i - 1].charAt(1)) {
                case 'b':
                    try {
                        flag_predict_probability = (atoi(args1[i]) != 0);
                    } catch (NumberFormatException e) {
                        exit_with_help();
                    }
                    break;

                case 'q':
                    i--;
                    Linear.disableDebugOutput();
                    break;

                default:
                    System.err.printf("unknown option: -%d%n", args1[i - 1].charAt(1));
                    exit_with_help();
                    break;
            }
        }
        if (i >= args1.length || args1.length <= i + 2) {
            exit_with_help();
        }

        BufferedReader reader = null;
        BufferedWriter writer = null;
        Configuration conf = new Configuration();
        conf.addResource(new Path("./projectFile/core-site.xml"));
        conf.addResource(new Path("./projectFile/hdfs-site.xml"));

        try {
            String inFile = args1[i];
            String outFile = args1[i + 2];
            FileSystem hdfs = FileSystem.get(conf);
            reader = new BufferedReader(new InputStreamReader(hdfs.open(new Path(inFile)), Linear.FILE_CHARSET));
            writer = new BufferedWriter(new OutputStreamWriter(hdfs.create(new Path(outFile)), Linear.FILE_CHARSET));//, Linear.FILE_CHARSET));
            //System.out.println("创建完writer对象");

            //reader = new BufferedReader(new InputStreamReader(new FileInputStream(args1[i]), Linear.FILE_CHARSET));
            //writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(args1[i + 2]), Linear.FILE_CHARSET));

            Model model;
            try {
                //model = Linear.loadModel(new File(args1[i + 1]));
                model = Linear.loadModel(args1[i + 1]);
                doPredict(reader, writer, model);
                //doPredict(reader, writer, model);
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }

        } catch (FileNotFoundException e1) {
             //TODO Auto-generated catch block
           e1.printStackTrace();
        }
        finally {
            closeQuietly(reader);
            closeQuietly(writer);
        }

    }

    public static void main(String[] args) throws IOException {
        Predict pre=new Predict();
        pre.ExecPredict(args);
    }
}


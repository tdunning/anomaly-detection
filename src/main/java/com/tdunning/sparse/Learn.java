package com.tdunning.sparse;

import com.google.common.collect.Lists;
import com.google.common.io.Resources;
import org.apache.mahout.clustering.streaming.cluster.BallKMeans;
import org.apache.mahout.common.distance.EuclideanDistanceMeasure;
import org.apache.mahout.math.*;
import org.apache.mahout.math.function.Functions;
import org.apache.mahout.math.neighborhood.BruteSearch;
import org.apache.mahout.math.neighborhood.UpdatableSearcher;
import org.apache.mahout.math.random.WeightedThing;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.Formatter;
import java.util.List;

/**
 * Read a bunch of EKG data, chop out windows and cluster the windows.  Then reconstruct the signal and
 * figure out the error.
 */
public class Learn {
    public static void main(String[] args) throws IOException {
        // read the data
        URL x = Resources.getResource("a02.dat");
        double t0 = System.nanoTime() / 1e9;
        Vector trace = Trace.read16b(new File(x.getPath()), 1.0 / 200);
        double t1 = System.nanoTime() / 1e9;
        System.out.printf("Read test data from %s in %.2f s\n", x, t1 - t0);

        final int WINDOW = 32;
        int STEP = 2;
        int SAMPLES = 200000;

        // set up the window vector
        Vector window = new DenseVector(WINDOW);
        for (int i = 0; i < WINDOW; i++) {
            double w = Math.sin(Math.PI * i / (WINDOW - 1.0));
            window.set(i, w * w);
        }

        // window and normalize the data
        t0 = System.nanoTime() / 1e9;
        List<WeightedVector> r = Lists.newArrayList();
        for (int i = 0; i < SAMPLES; i++) {
            int offset = i * STEP;
            WeightedVector row = new WeightedVector(new DenseVector(WINDOW), 1, i);
            row.assign(trace.viewPart(offset, WINDOW));
            row.assign(window, Functions.MULT);
            row.assign(Functions.mult(1 / row.norm(2)));
            r.add(row);
        }
        t1 = System.nanoTime() / 1e9;
        System.out.printf("Windowed data in %.2f s\n", t1 - t0);

        // now cluster the data
        t0 = System.nanoTime() / 1e9;
        BallKMeans km = new BallKMeans(new BruteSearch(new EuclideanDistanceMeasure()), 400, 10);
        UpdatableSearcher clustering = km.cluster(r);
        t1 = System.nanoTime() / 1e9;
        System.out.printf("Clustered in %.2f s\n", t1 - t0);


        // and now dump the clustering results.  This prints one line per cluster centroids, each with WINDOW values
        t0 = System.nanoTime() / 1e9;
        try (Formatter out = new Formatter("dict.tsv")) {
            for (Vector v : clustering) {
                String separator = "";
                for (Vector.Element element : v.all()) {
                    out.format("%s%.3f", separator, element.get());
                    separator = "\t";
                }
                out.format("\n");
            }
        }

        // and the final results.  This output includes the original signal, the reconstructed signal and the error
        // in this algorithm, each window is windowed and we simply look for the nearest cluster signal for that
        // window.  The trick is that we can look at each window independently because of the windowing.  This works
        // because the window before and after the current one will independently approximate the portion of the signal
        // left over after subtracting this window.
        try (Formatter out = new Formatter("trace.tsv")) {
            Matrix rx = new DenseMatrix(WINDOW / 2, 2);
            Vector previous = new DenseVector(WINDOW);
            Vector current = new DenseVector(WINDOW);
            for (int i = 0; i + WINDOW < trace.size(); i += WINDOW / 2) {
                // copy chunk of data to temporary window storage and multiply by window
                WeightedVector row = new WeightedVector(new DenseVector(WINDOW), 1, i);
                row.assign(trace.viewPart(i, WINDOW));
                row.assign(window, Functions.MULT);

                // scale and find nearest dictionary entry
                double scale = row.norm(2);
                row.assign(Functions.mult(1 / scale));

                WeightedThing<Vector> cluster = clustering.search(row, 1).get(0);
                current.assign(cluster.getValue());
                current.assign(Functions.mult(scale));

                // we produce results half a window at a time.  First column is original signal, second is reconstruction
                rx.viewColumn(0).assign(trace.viewPart(i, WINDOW / 2));
                rx.viewColumn(1).assign(previous.viewPart(WINDOW / 2, WINDOW / 2));
                rx.viewColumn(1).assign(current.viewPart(0, WINDOW / 2), Functions.PLUS);
                previous.assign(current);

                for (int j = 0; j < WINDOW / 2; j++) {
                    out.format("%.3f\t%.3f\t%d\n", rx.get(j, 0), rx.get(j, 1), ((WeightedVector) cluster.getValue()).getIndex());
                }
            }
        }
        t1 = System.nanoTime() / 1e9;
        System.out.printf("Output in %.2f s\n", t1 - t0);
    }
}

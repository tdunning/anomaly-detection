package com.tdunning.sparse;

import com.google.common.io.Resources;
import org.apache.mahout.math.DenseMatrix;
import org.apache.mahout.math.DenseVector;
import org.apache.mahout.math.Matrix;
import org.apache.mahout.math.Vector;
import org.apache.mahout.math.function.Functions;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.Formatter;

public class WindowTest {

    @Test
    public void testColumnDetector() throws IOException {
        URL x = Resources.getResource("a02.dat");
        Vector trace = Trace.read16b(new File(x.getPath()), 1.0 / 200);

        final int WINDOW = 32;
        int STEP = (int) (WINDOW / 8.0);
        int SAMPLES = 100000;

        Vector window = new DenseVector(WINDOW);
        for (int i = 0; i < WINDOW; i++) {
            double w = Math.sin(Math.PI * i / (WINDOW - 1.0));
            window.set(i, w * w);
        }

        Matrix m = new DenseMatrix(SAMPLES, WINDOW);
        Formatter out = new Formatter(new File("xx"));
        Vector r = new DenseVector(WINDOW);
        for (int i = 0; i < SAMPLES; i++) {
            int offset = i * STEP;
            r.assign(trace.viewPart(offset, WINDOW));
            r.assign(window, Functions.MULT);
            m.viewRow(i).assign(r);
            String separator = "";
            for (int j = 0; j < WINDOW; j++) {
                out.format("%s%.2f", separator, r.get(j));
                separator = "\t";
            }
            out.format("\n");
        }
        out.close();

        Matrix dict = new DenseMatrix(WINDOW, 300);
        OrthogonalMatchingPursuit codec = new OrthogonalMatchingPursuit(dict.viewPart(0, WINDOW, 0, 100));
        KSvd k1 = new KSvd(m, codec, 1, true);
        KSvd.dump(dict, "dict.1.tsv", null);

        codec = new OrthogonalMatchingPursuit(dict);
        KSvd k2 = new KSvd(m, codec, 3, false);

        KSvd.dump(dict, "dict.2.tsv", null);
    }
}

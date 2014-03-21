package com.tdunning.sparse;

import org.apache.mahout.common.RandomUtils;
import org.apache.mahout.math.*;
import org.apache.mahout.math.function.DoubleFunction;
import org.apache.mahout.math.function.Functions;
import org.apache.mahout.math.random.Normal;
import org.junit.Test;

import java.io.IOException;
import java.util.Random;

import static org.junit.Assert.assertEquals;

public class KSvdTest {
    @Test
    public void testSines() throws IOException {
        Random rand = RandomUtils.getRandom();
        Matrix data = new DenseMatrix(1000, 64);
        for (int i = 0; i < data.rowSize(); i++) {
            double f1 = 1 << rand.nextInt(3);
            double f2 = 1 << rand.nextInt(3);
            while (f1 == f2) {
                f2 = 1 << rand.nextInt(3);
            }
            double m1 = rand.nextDouble();
            double m2 = rand.nextDouble();
            for (int j = 0; j < data.columnSize(); j++) {
                double value = m1 * Math.sin(f1 * j / 64 * Math.PI) + m2 * Math.sin(f2 * j / 64 * Math.PI);
                data.set(i, j, value);
            }


        }
        Matrix dict = new DenseMatrix(data.columnSize(), 10).assign(new Normal());
        for (MatrixSlice row : dict) {
            row.vector().assign(row.vector().normalize());
        }
        OrthogonalMatchingPursuit codec = new OrthogonalMatchingPursuit(dict);
        KSvd k = new KSvd(data, codec, 3, true);
        Matrix encoded = codec.encode(data, 3);

        // the dictionary atoms should be normalized (or zero)
        Vector dx = new DenseVector(k.getDict().transpose().times(k.getDict()).viewDiagonal());
        dx.assign(new DoubleFunction() {
            @Override
            public double apply(double x) {
                return Math.min(Math.abs(x - 1), Math.abs(x));
            }
        }).norm(1);

        assertEquals(0, dx.norm(1), 1e-9);

        // verify that the encoded form is good
        double e1 = encoded.times(k.getDict().transpose()).minus(data).aggregate(Functions.PLUS, Functions.ABS);

        // and make sure that the official decoding agrees
        Matrix decoded = codec.decode(encoded);
        double e2 = decoded.minus(data).aggregate(Functions.PLUS, Functions.ABS);

        System.out.printf("%.4g\t%.4g\n", e1, e2);
        assertEquals(0, e1, 1e-8);
        assertEquals(0, e2, 1e-8);

    }
}

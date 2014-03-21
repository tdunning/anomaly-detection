package com.tdunning.lasso;

import org.apache.mahout.common.RandomUtils;
import org.apache.mahout.math.DenseMatrix;
import org.apache.mahout.math.DenseVector;
import org.apache.mahout.math.Matrix;
import org.apache.mahout.math.Vector;
import org.apache.mahout.math.function.Functions;
import org.junit.Test;

import java.util.Random;

public class LassoTest {
    @Test
    public void testBasics() {
        Matrix x = new DenseMatrix(50, 100).assign(Functions.random());
        Vector beta = new DenseVector(100);
        final Random rnd = RandomUtils.getRandom();
        double sign = 1;
        for (int i = 0; i < 10; i++) {
            beta.set(i, sign * 10 / (i + 1));
            sign *= -1;
        }
        Vector y = x.times(beta);

        Lasso s = new Lasso(x, y, 0.999);
        for (Lasso.Fit m : s.solve(y)) {
            Vector betaHat = m.beta();
            double localMse = y.minus(x.times(betaHat)).norm(2);
            System.out.printf("%.3f\t%.3f\t%.3f\t%.3f\t%.3f\t%.3f\t%.3f\t%.3f\n", m.lambda(), localMse, m.mse(), m.regularizedMse(), betaHat.get(0), betaHat.get(1), betaHat.get(2), betaHat.get(3));
        }
        System.out.printf("done\n");
    }
}

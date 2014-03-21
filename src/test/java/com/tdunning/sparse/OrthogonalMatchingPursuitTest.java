package com.tdunning.sparse;

import org.apache.mahout.common.RandomUtils;
import org.apache.mahout.math.DenseMatrix;
import org.apache.mahout.math.Matrix;
import org.apache.mahout.math.MatrixSlice;
import org.apache.mahout.math.Vector;
import org.apache.mahout.math.function.Functions;
import org.apache.mahout.math.random.Normal;
import org.junit.Assert;
import org.junit.Test;

import java.util.Random;

import static org.junit.Assert.assertEquals;

public class OrthogonalMatchingPursuitTest {
    @Test
    public void testEncode() {
        Random gen = RandomUtils.getRandom();
        Matrix dict = new DenseMatrix(64, 10).assign(new Normal());
        for (MatrixSlice row : dict) {
            row.vector().assign(Functions.div(row.vector().norm(2)));
        }

        Matrix data = new DenseMatrix(100, 64);
        Matrix encoded = new DenseMatrix(100, 10);
        for (MatrixSlice row : data) {
            Vector sp = encoded.viewRow(row.index());
            sp.assign(0);
            sp.set(gen.nextInt(10), gen.nextDouble());
            sp.set(gen.nextInt(10), gen.nextDouble());
            row.vector().assign(dict.times(sp));
        }

        SparseCodec encoder = new OrthogonalMatchingPursuit(dict);
        for (MatrixSlice row : data) {
            Vector enc = encoder.encode(row.vector(), 3);
            assertEquals(0, row.vector().minus(dict.times(enc)).norm(1), 1e-10);
            assertEquals(0, encoded.viewRow(row.index()).minus(enc).norm(1), 1e-10);
        }
    }
}

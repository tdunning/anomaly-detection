package com.tdunning.sparse;

import com.google.common.base.Charsets;
import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import com.google.common.io.Resources;
import org.apache.commons.math3.linear.Array2DRowRealMatrix;
import org.apache.commons.math3.linear.RealMatrix;
import org.apache.mahout.math.DenseMatrix;
import org.apache.mahout.math.Matrix;
import org.apache.mahout.math.MatrixSlice;
import org.apache.mahout.math.Vector;
import org.apache.mahout.math.function.Functions;
import org.junit.Test;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;

import static org.junit.Assert.assertEquals;

public class SVDTest {
    @Test(/*timeout = 2000*/)
    public void testNastyCases() throws IOException, InterruptedException, ExecutionException, TimeoutException {
        for (String f : ImmutableList.of("e02", "e01")) {
            System.out.printf("starting %s\n", f);

            final Matrix m = readTsv(f + ".tsv");
            RealMatrix mx = new Array2DRowRealMatrix(m.rowSize(), m.columnSize());
            for (MatrixSlice row : m) {
                for (Vector.Element element : row.vector().all()) {
                    double x = element.get();
                    if (Double.isNaN(x) || Double.isInfinite(x)) {
                        System.out.printf("%.5f, %d, %d\n", x, row.index(), element.index());
                    }
                    mx.setEntry(row.index(), element.index(), x);
                }
            }
            SVD svd = new SVD(m);
            assertEquals(0, m.minus(svd.getU().times(svd.getS()).times(svd.getV().transpose())).aggregate(Functions.PLUS, Functions.ABS), 1e-10);
            System.out.printf("%s worked\n", f);
        }
    }

    Matrix readTsv(String name) throws IOException {
        Splitter onTab = Splitter.on("\t");
        List<String> lines = Resources.readLines((Resources.getResource(name)), Charsets.UTF_8);
        int rows = lines.size();
        int columns = Iterables.size(onTab.split(lines.get(0)));
        Matrix r = new DenseMatrix(rows, columns);
        int row = 0;
        for (String line : lines) {
            Iterable<String> values = onTab.split(line);
            int column = 0;
            for (String value : values) {
                r.set(row, column, Double.parseDouble(value));
                column++;
            }
            row++;
        }
        return r;
    }
}

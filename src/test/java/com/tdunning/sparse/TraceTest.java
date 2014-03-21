package com.tdunning.sparse;

import com.google.common.io.Resources;
import org.apache.mahout.math.Vector;
import org.junit.Test;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Formatter;

/**
 * Created by tdunning on 1/26/14.
 */
public class TraceTest {
    @Test
    public void testRead212() throws IOException {
        Vector m = Trace.read212(new File(Resources.getResource("cu01.dat").getFile()), -1, 1.0 / 400);
        try (Formatter out = new Formatter(new FileOutputStream("xx.tsv"))) {
            for (Vector.Element row : m.all()) {
                out.format("%.4f\n", row.get());
            }
        }
    }

    @Test
    public void testRead16b() {
    }
}

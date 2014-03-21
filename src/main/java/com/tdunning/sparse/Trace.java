package com.tdunning.sparse;

import com.google.common.base.Preconditions;
import org.apache.mahout.math.DenseMatrix;
import org.apache.mahout.math.DenseVector;
import org.apache.mahout.math.Matrix;
import org.apache.mahout.math.Vector;

import java.io.*;

/**
 * Reads an EKG trace from the format provided by physio.net
 * <p/>
 * This format contains a matrix of 16bit numbers in row major order.
 */
public class Trace {
    public static Matrix read(InputStream in, int cols) throws IOException {
        DataInputStream input = new DataInputStream(in);
        int rows = 1000;
        Matrix data = new DenseMatrix(rows, cols);
        int i = 0;
        while (true) {
            // standard sort of exponential reallocation
            if (i >= rows) {
                int newRows = 2 * rows;
                Matrix mx = new DenseMatrix(newRows, cols);
                mx.viewPart(0, rows, 0, cols).assign(data);
                data = mx;
                rows = newRows;
            }
            for (int j = 0; j < cols; j++) {
                try {
                    data.setQuick(i, j, input.readShort());
                } catch (EOFException e) {
                    if (j != 0) {
                        // ran out on partial row
                        throw e;
                    }
                    // ran out of data ... trim what we got
                    return new DenseMatrix(i, cols).assign(data.viewPart(0, i, 0, cols));
                }
            }
            i++;
        }
    }

    public static Vector read212(File in, int rows, double scale) throws IOException {
        DataInputStream input = new DataInputStream(new BufferedInputStream(new FileInputStream(in), 1 << 20));

        if (rows <= 0) {
            long n = in.length();
            Preconditions.checkArgument(n % 3 == 0, "Input file has to contain an even number of triple byte values");
            rows = (int) (n / 3) * 2;
        }

        Vector data = new DenseVector(rows);
        byte[] buf = new byte[3];
        for (int i = 0; i < rows; ) {
            input.readFully(buf);
            int v = buf[0] & 0xff;
            v += ((buf[1] & 0xf) << 28) >> 20;
            data.setQuick(i, v * scale);
            i++;

            v = ((buf[1] & 0xf0) << 24) >> 20;
            v += buf[2] & 0xff;
            data.setQuick(i, v * scale);
            i++;
        }
        return data;
    }

    public static DenseVector read16b(File in, double scale) throws IOException {
        DataInputStream input = new DataInputStream(new FileInputStream(in));

        int rows = (int) (in.length() / 2);

        DenseVector data = new DenseVector(rows);
        for (int i = 0; i < rows; i++) {
            data.setQuick(i, input.readShort() * scale);
        }
        return data;
    }


}

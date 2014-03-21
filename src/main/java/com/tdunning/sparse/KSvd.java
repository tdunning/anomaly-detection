package com.tdunning.sparse;

import org.apache.mahout.math.DenseMatrix;
import org.apache.mahout.math.Matrix;
import org.apache.mahout.math.MatrixSlice;
import org.apache.mahout.math.Vector;
import org.apache.mahout.math.function.Functions;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.util.Formatter;

/**
 * Learn an over-complete basis using K-svd.
 * <p/>
 * The basic idea is that a set of data points are decoded using a sparse projection.
 * Then each basis vector is updated by finding all of the data points which include
 * this basis vector.  Then these points are reconstructed without the current basis
 * vector and these errors are used to find a new value for the basis vector and
 * its coefficients.
 * <p/>
 * After all basis vectors have been adapted, the data points are decoded again and
 * the process repeats as long as desired.
 */
public class KSvd {
    private final Matrix dict;

    public KSvd(Matrix data, SparseCodec codec, int sparsity, boolean initDictionary) throws IOException {
        // pick random rows of data as initial dictionary entries
        dict = codec.getDictionary();
        if (initDictionary) {
            for (int i = 0; i < dict.columnSize(); i++) {
                // 971 is prime so 971*i mod data.rowSize() is a permutation of 1:dict.columnSize()
                dict.viewColumn(i).assign(data.viewRow((971 * i) % data.rowSize()));
                dict.viewColumn(i).assign(dict.viewColumn(i).normalize());
            }
        }
//        dump(data, "data.tsv", "data");
        for (int i = 0; i < 11; i++) {
            // encode
            Matrix x = codec.encode(data, sparsity);
            System.out.printf("%.2f\n", codec.decode(x).minus(data).aggregate(Functions.PLUS, Functions.ABS));

            // for each atom
            for (int j = 0; j < dict.columnSize(); j++) {
                // find all elements that use this atom
                int n = (int) x.viewColumn(j).aggregate(Functions.PLUS, Functions.chain(Functions.greater(0), Functions.ABS));

                if (n > 0) {
                    // copy those elements together in a matrix
                    int k = 0;
                    Matrix tmpData = new DenseMatrix(n, data.columnSize());
                    for (Vector.Element element : x.viewColumn(j).nonZeroes()) {
                        if (element.get() != 0) {
                            if (k > n) {
                                System.out.printf("k too large (%d > %d)\n", k, n);
                            }
                            tmpData.viewRow(k).assign(data.viewRow(element.index()));
                            k++;
                        }
                    }

                    // do a round trip without this atom to determine what this atom should be
                    codec.viewBasis(j).assign(0);
                    Matrix encoded = codec.encode(tmpData, sparsity);
                    Matrix error = codec.decode(encoded).minus(tmpData);

//                    dump(error, "error.tsv", "error");
//                    dump(encoded, "encoded.tsv", "encoded");

                    SVD quickSVD = new SVD(error);
                    Vector u0 = quickSVD.getU().viewColumn(0);
                    Vector v0 = quickSVD.getV().viewColumn(0);

                    double w = quickSVD.getSingularValues()[0];

                    if (Math.abs(w) > 1e-9) {
                        // the same svd that gives a new atom also tells us the new weights
                        k = 0;
                        codec.viewBasis(j).assign(v0);
                        for (Vector.Element element : x.viewColumn(j).nonZeroes()) {
                            if (element.get() != 0) {
                                element.set(u0.get(k) * w);
                                k++;
                            }
                        }
                    } else {
                        for (Vector.Element element : x.viewColumn(j).nonZeroes()) {
                            if (element.get() != 0) {
                                element.set(0);
                                k++;
                            }
                        }
                    }
                }
                // this encoding step is redundant in the k-SVD algorithm ... keep it for now, however
//                x = codec.encode(data, sparsity);
            }
            System.out.printf("%d\n", i);
//            dump(dict, "dict.tsv", "dict");
        }
    }

    public Matrix getDict() {
        return dict;
    }

    public static void dump(Matrix dict, String pathname, String title) throws IOException {
        try (Formatter out = new Formatter(Files.newOutputStream(new File(pathname).toPath(), StandardOpenOption.CREATE, StandardOpenOption.WRITE))) {
            for (MatrixSlice row : dict) {
                String sep = "";
                for (Vector.Element element : row.all()) {
                    out.format("%s%.3f", sep, element.get());
                    sep = "\t";
                }
                out.format("\n");
            }
        }
    }

}

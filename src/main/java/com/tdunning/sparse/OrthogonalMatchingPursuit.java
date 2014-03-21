package com.tdunning.sparse;

import com.google.common.base.Preconditions;
import org.apache.mahout.math.*;
import org.apache.mahout.math.function.Functions;

/**
 * Given a query vector and a dictionary of basis vector, find a sparse set of basis vectors
 * that pretty closely add up to the query.
 * <p/>
 * The basic Matching Pursuit algorithm works by finding the basis that best explains the query.
 * The residual error is then treated the same way in that the best basis for representing the
 * residual is found.  
 */
public class OrthogonalMatchingPursuit implements SparseCodec {
    private double epsilon = 1e-5;

    // atoms in the dictionary are columns in this matrix
    // column orientation allows matrix multiplication to be used for reconstruction
    private Matrix dictionary;

    /**
     * @param dictionary A dictionary of rows which are the basis vectors.
     */
    public OrthogonalMatchingPursuit(Matrix dictionary) {
        this.dictionary = dictionary;
    }

    /**
     * Returns a sparse weight vector r such that dictionary.times(r) is close to x but
     * r has at most n non-zero elements.
     *
     * @param x The vector to encode
     * @param n The number of atoms from the dictionary we are allowed to use
     * @return r, the sparse weight vector that is the encoding of x
     */
    @Override
    public Vector encode(Vector x, int n) {
        Preconditions.checkArgument(n > 0);
        Preconditions.checkArgument(x.size() == dictionary.rowSize(),
                "Vector is the wrong size for encoding [wanted, got] = ", dictionary.rowSize(), x.size());

        Vector tmp = null;
        int dimension = dictionary.rowSize();

        // this matrix holds the atoms that we use to represent x
        Matrix basis = new DenseMatrix(dimension, n);

        // k remembers where the atoms came from so the final result can be exploded back to full size
        int[] k = new int[n];

        Vector residual = x.clone();
        for (int i = 0; i < n; i++) {
            double error = residual.norm(2);
            Vector scores = dictionary.transpose().times(residual).assign(Functions.ABS);
            k[i] = scores.maxValueIndex();

            if (error < epsilon || Math.abs(scores.get(k[i])) < epsilon) {
                // none of the current basis vectors can help us, might as well return what we have
                if (tmp == null) {
                    tmp = new DenseVector(n);
                }
                break;
            }

            // copy the latest best basis vector into our partial set to keep data together
            basis.viewColumn(i).assign(dictionary.viewColumn(k[i]));

            // we find the residual by least squares solution
            Matrix currentDictionary = basis.viewPart(0, dimension, 0, i + 1);
            tmp = new QRDecomposition(currentDictionary).solve(x);
            residual.assign(x.minus(currentDictionary.times(tmp)));
        }
        assert tmp != null;  // because n > 0

        // our tmp variable has all the non-zeros collected together ... we need to explode them back out
        RandomAccessSparseVector weights = new RandomAccessSparseVector(dictionary.columnSize());
        for (int i = 0; i < Math.min(n, tmp.size()); i++) {
            weights.set(k[i], tmp.get(i));
        }
        return weights;
    }

    /**
     * Encodes the rows of a matrix.  The results R is a matrix such that dictionary.times(R.transpose())
     * is close to data but where rows of R have a limited number of non-zeros.
     *
     * @param data     The matrix of data to encode
     * @param sparsity How many non-zero elements are allowed in each encoding
     * @return A matrix with the same number of rows as data where each row has
     */
    @Override
    public Matrix encode(Matrix data, int sparsity) {
        Preconditions.checkArgument(sparsity > 0);
        Matrix r = new SparseRowMatrix(data.rowSize(), dictionary.columnSize());
        for (int i = 0; i < data.rowSize(); i++) {
            Vector encodedRow = encode(data.viewRow(i), sparsity);
            r.viewRow(i).assign(encodedRow);
        }
        return r;
    }

    @Override
    public Vector decode(Vector encoded) {
        return dictionary.times(encoded);
    }

    @Override
    public Matrix decode(Matrix encoded) {
        return encoded.times(dictionary.transpose());
    }


    @Override
    public Matrix getDictionary() {
        return dictionary;
    }

    @Override
    public Vector viewBasis(int basis) {
        return dictionary.viewColumn(basis);
    }
}

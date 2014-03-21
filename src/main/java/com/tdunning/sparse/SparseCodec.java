package com.tdunning.sparse;

import org.apache.mahout.math.Matrix;
import org.apache.mahout.math.Vector;

/**
 * Created by tdunning on 12/31/13.
 */
public interface SparseCodec {
    Vector encode(Vector x, int n);

    Matrix encode(Matrix data, int sparsity);

    Vector decode(Vector encoded);

    Matrix decode(Matrix encoded);

    Matrix getDictionary();

    Vector viewBasis(int basis);
}

package com.tdunning.lasso;

import com.google.common.collect.AbstractIterator;
import com.google.common.collect.Sets;
import org.apache.mahout.math.*;
import org.apache.mahout.math.function.DoubleDoubleFunction;
import org.apache.mahout.math.function.Functions;
import org.apache.mahout.math.function.VectorFunction;

import java.util.Iterator;
import java.util.Set;

/**
 * Solves in-memory linear systems using L1 and L2 regularization.
 * <p/>
 * Typical usage will have observations in rows of Matrix x and target values in Vector y.  Given
 * a value of alpha (0 gives L2 regularization, 1 gives L1, 0.999 is common), solutions can be had
 * by doing this:
 * <pre>
 *     for (Fit r : new Lasso(x, alpha).solve(y)) {
 *         // use r.predict(Vector newX) here,
 *         // or r.mse()
 *         // or get the actual coefficients with r.beta() and r.beta0()
 *     }
 * </pre>
 * <p/>
 * The approach is taken from http://www.jstatsoft.org/v33/i01/paper
 */
public class Lasso {
    private final Matrix x;
    private final Matrix xt;
    private final Vector scale;
    private final Vector mean;
    private final Set<Integer> skippedColumns;

    private final double alpha;

    public Lasso(Matrix x, Vector y, double alpha) {
        this.alpha = alpha;
        this.x = x;

        // standardize a
        mean = x.aggregateColumns(new VectorFunction() {
            @Override
            public double apply(Vector f) {
                return f.zSum() / f.size();
            }
        });

        skippedColumns = Sets.newHashSet();

        // xt is a sparse matrix which contains a partially standardized x.transpose()
        // the point is to allow fast iteration through scaled columns of x which may
        // be sparse
        scale = new DenseVector(x.columnSize());
        if (x.viewRow(0).isDense()) {
            xt = x.transpose();
        } else {
            xt = new SparseRowMatrix(x.columnSize(), x.rowSize());
        }
        for (int column = 0; column < x.columnSize(); column++) {
            Vector f = x.viewColumn(column);
            double norm = 0;
            double m = mean.get(column);
            for (int i = 0; i < f.size(); i++) {
                double z = f.get(i) - m;
                norm += z * z;
            }
            norm = Math.sqrt(norm);

            if (norm < 1e-12) {
                skippedColumns.add(column);
                scale.set(column, 1);
            } else {
                scale.set(column, norm);
                mean.set(column, mean.get(column) / norm);
                for (int i = 0; i < x.rowSize(); i++) {
                    if (Math.abs(x.get(i, column) / norm) > 1e-12) {
                        xt.set(column, i, x.get(i, column) / norm);
                    }
                }
            }
        }

        // verify that the means of xt are correct
        assert mean.minus(xt.aggregateRows(new VectorFunction() {
            @Override
            public double apply(Vector f) {
                return f.zSum() / f.size();
            }
        })).norm(1) < 1e-12;

        // validate that xt is standardized if you subtract the column means
        assert xt.aggregateRows(new VectorFunction() {
            int j = 0;

            @Override
            public double apply(Vector f) {
                Vector v = f.plus(-mean.get(j++));
                return v.dot(v);
            }
        }).plus(-1).norm(1) < 1e-10;

        // validate that x can be reconstructed from xt using scale
        assert x.minus(xt.transpose().times(new DiagonalMatrix(scale))).aggregate(Functions.PLUS, Functions.ABS) < 1e-12;
    }

    private double maxLambda(Matrix x, Vector y, double alpha) {
        // lambda starts at a value guaranteed to force beta to zero
        double maxLambda = 0;
        for (int column = 0; column < x.columnSize(); column++) {
            @SuppressWarnings("SuspiciousNameCombination")
            double z = Math.abs((xt.viewRow(column).dot(y) - mean.get(column) * y.zSum()) / x.rowSize() / alpha);
            if (maxLambda < z) {
                maxLambda = z;
            }
        }
        return maxLambda;
    }

    /**
     * Solves the entire path of solutions.
     *
     * @return An iterator of Fit structures, one for each successive value of lambda
     */
    public Iterable<Fit> solve(final Vector y) {
        final double maxLambda = maxLambda(x, y, alpha);
        final double minLambda = 0.001 * maxLambda;
        final double lambdaStep = Math.exp(Math.log(maxLambda / minLambda) / 100);
        return internalSolve(maxLambda * lambdaStep, lambdaStep, minLambda, y);
    }

    /**
     * Solves for a particular value of lambda.  Note that the original paper that this class is based on
     * suggests that it may be faster to following the path rather than solving in a single step.
     *
     * @param lambda The regularization constant.
     * @return The Fit for this value of lambda.
     */
    public Fit solve(double lambda, Vector y) {
        double maxLambda = maxLambda(x, y, alpha);
        return internalSolve(maxLambda, maxLambda / lambda, lambda * 0.9999, y).iterator().next();
    }

    private Iterable<Fit> internalSolve(final double start, final double step, final double end, final Vector yValues) {
        return new Iterable<Fit>() {
            @Override
            public Iterator<Fit> iterator() {
                return new AbstractIterator<Fit>() {
                    Vector y = yValues;
                    double lambda = start;
                    double lambdaStep = step;
                    double minLambda = end;
                    Fit previous;

                    {
                        double beta0 = y.zSum() / y.size();

                        // initial residual is y-beta0 since beta starts as zero
                        previous = new Fit(y, start, beta0, new DenseVector(x.columnSize()), new DenseVector(y).plus(-beta0));
                    }

                    @Override
                    protected Fit computeNext() {
                        lambda /= lambdaStep;
                        if (lambda < minLambda) {
                            return endOfData();
                        } else {
                            previous = new Fit(y, lambda, previous.beta0, previous.beta, previous.residual);
                            return previous;
                        }
                    }
                };
            }
        };
    }

    /**
     * Encapsulates a single solution.
     */
    public class Fit {
        private final Vector y;
        private double lambda;
        private double beta0;
        private final Vector beta;
        private final Vector residual;

        private Fit(Vector y, double lambda, double initialBeta0, Vector initialBeta, Vector initialResidual) {
            this.y = y;
            this.lambda = lambda;
            this.beta0 = initialBeta0;
            this.beta = new DenseVector(initialBeta);

            residual = initialResidual;

            int updates = 1;
            while (updates > 0) {
                updates = 0;
                for (int j = 0; j < x.columnSize(); j++) {
                    if (!skippedColumns.contains(j)) {
                        assert residual().minus(residual).norm(1) < 1e-8;
//                        assert Math.abs((xt.viewRow(j).plus(-mean.get(j)).dot(residual)) - (xt.viewRow(j).dot(residual) - mean.get(j) * residual.zSum())) < 1e-10;

                        final double betaJ = beta.get(j);
                        final double newBeta = trim((xt.viewRow(j).dot(residual) - mean.get(j) * residual.zSum()) / x.rowSize() + betaJ, lambda * alpha) / (1 + lambda * (1 - alpha));
                        if (Math.abs(newBeta - betaJ) > 1e-12 && Math.abs((newBeta - betaJ) / Math.max(newBeta, betaJ)) > 1e-6) {
                            updates++;

                            this.beta.set(j, newBeta);
                            residual.assign(residual());
                            double offset = residual.zSum() / residual.size();
                            this.beta0 -= offset;
                            residual.assign(Functions.PLUS, offset);

                            assert residual().minus(residual).norm(1) < 1e-8;
                        }
                    }
                }
            }
        }

        public double predict(Vector xi) {
            double r = 0;
            // unrolled this loop to avoid vector allocation
            for (int i = 0; i < xi.size(); i++) {
                r += xi.get(i) / scale.get(i) * beta.get(i);
            }
            return r - mean.dot(beta) + beta0;
        }

        public Vector predict(Matrix x) {
            Vector r = new DenseVector(x.rowSize());
            for (int i = 0; i < x.rowSize(); i++) {
                r.set(i, predict(x.viewRow(i)));
            }
            return r;
        }

        public double lambda() {
            return lambda;
        }

        public Vector beta() {
            return beta.times(scale);
        }

        public double beta0() {
            return beta0 - mean.dot(beta);
        }

        public Vector residual() {
            return y.minus(predict(x));
        }

        public double mse() {
            return residual().norm(2);
        }

        public double regularizedMse() {
            Vector b = beta();
            return mse() + lambda * ((1 - alpha) * b.dot(b) / 2 + alpha * beta.aggregate(Functions.PLUS, Functions.ABS));
        }
    }

    private static double trim(double z, double gamma) {
        if (z > gamma) {
            return z - gamma;
        } else if (z < -gamma) {
            return z + gamma;
        } else {
            return 0;
        }
    }


    // pass over each variable, perform update
    // unstandardize
}

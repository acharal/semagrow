package eu.semagrow.core.estimator;

import org.openrdf.model.URI;
import org.openrdf.query.algebra.Join;
import org.openrdf.query.algebra.TupleExpr;
import org.openrdf.query.algebra.ValueExpr;


/**
 * Plan Optimizer
 * 
 * <p>Interface for any component that estimates the selectivity of
 * a given expression.</p>
 * 
 * @author Angelos Charalambidis
 */

public interface SelectivityEstimator
{

    /**
     * Estimate the merge selectivity factor {@code f} of a merge, such that
     * merge cardinality = cross product cardinality * {@code f}.
     * @param expr the merge expression
     * @param source a referring data source
     * @return the selectivity factor
     */

	double getJoinSelectivity( Join expr, URI source );


    /**
     * Estimate the merge selectivity factor {@code f} of a merge, such that
     * merge cardinality = cross product cardinality * {@code f}.
     * @param expr the merge expression
     * @return the selectivity factor
     */

    double getJoinSelectivity( Join expr );


    /**
     * Estimates the number of distinct values of a given variable.
     * @param varName the name of the variable
     * @param expr
     * @param source a potential referring data source
     * @return the estimated number of distinct values of a variable.
     */

    double getVarSelectivity( String varName, TupleExpr expr, URI source );

    
    /**
     * Estimates the reduction factor {@code f} of applying {@code condition}
     * to expression {@code expr} executed at {@code source}. That is, the cardinality
     * of the results is:
     * cardinality of executing {@code expr} at {@code source} * {@code f}.
     * @param condition
     * @param expr
     * @param source
     * @return
     */

    double getConditionSelectivity( ValueExpr condition, TupleExpr expr, URI source );

    
    /**
     * Estimates the reduction factor {@code f} of applying {@code condition}
     * to expression {@code expr}. That is, the cardinality of the results is
     * cardinality of results of {@code expr} * {@code f}.
     * @param condition
     * @param expr
     * @return
     */

    double getConditionSelectivity( ValueExpr condition, TupleExpr expr );
}

package org.semagrow.plan;

import org.eclipse.rdf4j.query.BindingSet;
import org.eclipse.rdf4j.query.Dataset;
import org.eclipse.rdf4j.query.algebra.TupleExpr;

import java.util.Optional;

/**
 * Plan Optimizer
 * 
 * <p>Interface for any component that optimizes
 * a query execution plan.</p>
 * 
 * @author Angelos Charalambidis
 */

public interface PlanOptimizer
{

	/**
	 * Returns a Plan from an abstract expression consulting the bindings and dataset
	 * that accompanies the expression.
	 * @param expr the abstract expression that constitute the query
	 * @param bindings the BindingSet populated by the user
	 * @param dataset the datasets used in the query
	 * @return
	 */
	Optional<Plan> getBestPlan(TupleExpr expr, BindingSet bindings, Dataset dataset);

}

package eu.semagrow.core.impl.planner;

import eu.semagrow.core.impl.estimator.CostEstimator;
import eu.semagrow.core.impl.util.BPGCollector;
import eu.semagrow.core.decomposer.QueryDecomposer;
import eu.semagrow.core.estimator.CardinalityEstimator;
import eu.semagrow.core.impl.optimizer.LimitPushDownOptimizer;
import eu.semagrow.core.impl.selector.StaticSourceSelector;
import eu.semagrow.core.impl.optimizer.ExtensionOptimizer;
import eu.semagrow.core.source.SourceSelector;
import eu.semagrow.art.*;

import org.openrdf.query.BindingSet;
import org.openrdf.query.Dataset;
import org.openrdf.query.algebra.TupleExpr;
import org.openrdf.query.algebra.evaluation.QueryOptimizer;
import org.openrdf.query.algebra.evaluation.util.QueryOptimizerList;

import java.util.Collection;


/**
 * Dynamic Programming Query Decomposer
 *
 * <p>Dynamic programming implementation of the
 * eu.semagrow.core.decomposer.QueryDecomposer interface.</p>
 *
 * @author Angelos Charalambidis
 * @author Stasinos Konstantopoulos
 */

public class DPQueryDecomposer implements QueryDecomposer
{

	private org.slf4j.Logger logger =
			org.slf4j.LoggerFactory.getLogger( DPQueryDecomposer.class );

	private CostEstimator costEstimator;
    private CardinalityEstimator cardinalityEstimator;
    private SourceSelector sourceSelector;

    public DPQueryDecomposer(CostEstimator estimator,
                             CardinalityEstimator cardinalityEstimator,
                             SourceSelector selector)
    {
        this.costEstimator = estimator;
        this.sourceSelector = selector;
        this.cardinalityEstimator = cardinalityEstimator;
    }


    /**
     * This method is the entry point to the Semagrow Stack that is called
     * by the HTTP endpoint implementation in eu.semagrow.stack.webapp
     * <p>
     * This methods edits {@code expr} in place to decompose it into the
     * sub-expressions that will be executed at each data source and to
     * annotate it with the execution plan.
     *
	 * @param expr The expression that will be decomposed. It must be an instance of eu.semagrow.core.impl.algebra.QueryRoot
	 * @param dataset
	 * @param bindings
     */

    @Override
    @Loggable
    public void decompose( TupleExpr expr, Dataset dataset, BindingSet bindings )
    {
        /*
         * Identify the Basic Graph Patterns, a partitioning of the AST into
         * BGP sub-trees such that each BGP only uses the operators that the decomposer
         * can handle.
         */

        Collection<TupleExpr> basicGraphPatterns = BPGCollector.process(expr);

        for( TupleExpr bgp : basicGraphPatterns ) {

        	/* creates the context of operation of the decomposer.
        	 * Specifically, collects FILTER statements */
            DecomposerContext ctx = new DecomposerContext( bgp );

            SourceSelector staticSelector = new StaticSourceSelector(sourceSelector.getSources(bgp, dataset, bindings));

        	/* uses the SourceSelector provided in order to identify the
        	 * sub-expressions that can be executed at each data source,
        	 * and annotates with cardinality and selectivity metadata */
            PlanGenerator planGenerator =
            		new PlanGeneratorImpl( ctx, sourceSelector, costEstimator, cardinalityEstimator );

        	/* optimizes the plans generated by the PlanGenerator */
            PlanOptimizer planOptimizer = new DPPlanOptimizer( planGenerator );

        	/* selects the optimal plan  */
            Plan bestPlan = planOptimizer.getBestPlan( bgp, bindings, dataset );

            /* grafts the optimal plan into expr */
            bgp.replaceWith( bestPlan );
        }

        QueryOptimizer opt =  new QueryOptimizerList(
                new ExtensionOptimizer(),
                new LimitPushDownOptimizer());

        opt.optimize(expr, dataset, bindings);
    }


}

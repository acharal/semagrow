package org.semagrow.evaluation.rxjava;

import org.semagrow.plan.Plan;
import org.semagrow.evaluation.QueryExecutor;

import org.semagrow.plan.operators.*;
import org.semagrow.selector.Site;
import org.eclipse.rdf4j.common.iteration.CloseableIteration;
import org.eclipse.rdf4j.model.*;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.query.BindingSet;
import org.eclipse.rdf4j.query.QueryEvaluationException;
import org.eclipse.rdf4j.query.algebra.*;
import org.eclipse.rdf4j.query.algebra.evaluation.TripleSource;
import org.reactivestreams.Publisher;

import rx.Observable;
import rx.RxReactiveStreams;
import rx.schedulers.Schedulers;

import java.util.List;
import java.util.Set;

/**
 * Created by angel on 11/26/14.
 */
public class FederatedEvaluationStrategyImpl extends EvaluationStrategyImpl {

    public QueryExecutor queryExecutor;

    private int batchSize = 10;


    public FederatedEvaluationStrategyImpl(QueryExecutor queryExecutor, final ValueFactory vf) {
        super(new TripleSource() {
            public CloseableIteration<? extends Statement, QueryEvaluationException>
            getStatements(Resource resource, IRI uri, Value value, Resource... resources) throws QueryEvaluationException {
                throw new UnsupportedOperationException("Statement retrieval is not supported");
            }

            public ValueFactory getValueFactory() {
                return vf;
            }
        });
        this.queryExecutor = queryExecutor;
    }

    public FederatedEvaluationStrategyImpl(QueryExecutor queryExecutor) {
        this(queryExecutor, SimpleValueFactory.getInstance());
    }


    public void setBatchSize(int b) {
        batchSize = b;
    }

    public int getBatchSize() {
        return batchSize;
    }

    @Override
    public Observable<BindingSet> evaluateReactiveInternal(TupleExpr expr, BindingSet bindings)
            throws QueryEvaluationException
    {
        if (expr instanceof SourceQuery) {
            return evaluateReactiveInternal((SourceQuery) expr, bindings);
        }
        else if (expr instanceof Join) {
            return evaluateReactiveInternal((Join) expr, bindings);
        }
        else if (expr instanceof Plan) {
            return evaluateReactiveInternal(((Plan) expr).getArg(), bindings);
        }
        else
            return super.evaluateReactiveInternal(expr, bindings);
    }


    @Override
    public Observable<BindingSet> evaluateReactiveInternal(Join expr, BindingSet bindings)
            throws QueryEvaluationException
    {
        if (expr instanceof BindJoin) {
            return evaluateReactiveInternal((BindJoin) expr, bindings);
        }
        else if (expr instanceof HashJoin) {
            return evaluateReactiveInternal((HashJoin) expr, bindings);
        }
        else if (expr instanceof MergeJoin) {
            return evaluateReactiveInternal((MergeJoin) expr, bindings);
        }
        else if (expr == null) {
            throw new IllegalArgumentException("expr must not be null");
        }
        else {
            throw new QueryEvaluationException("Unsupported tuple expr type: " + expr.getClass());
        }
    }

    public Observable<BindingSet> evaluateReactiveInternal(HashJoin expr, BindingSet bindings)
        throws QueryEvaluationException
    {
        Observable<BindingSet> r = evaluateReactiveInternal(expr.getRightArg(), bindings);

        Set<String> joinAttributes = expr.getLeftArg().getBindingNames();
        joinAttributes.retainAll(expr.getRightArg().getBindingNames());

        return evaluateReactiveInternal(expr.getLeftArg(), bindings)
                .toMultimap(b -> bindingSetOps.project(joinAttributes, b), b1 -> b1)
                .flatMap((probe) ->
                    r.concatMap(b -> {
                        if (!probe.containsKey(bindingSetOps.project(joinAttributes, b)))
                            return Observable.empty();
                        else
                            return Observable.from(probe.get(bindingSetOps.project(joinAttributes, b)))
                                             .join(Observable.just(b),
                                                     b1 -> Observable.never(),
                                                     b1 -> Observable.never(),
                                                     bindingSetOps::merge);
                    })
                );
    }

    public  Observable<BindingSet> hashJoin(Observable<BindingSet> left, Observable<BindingSet> right, Set<String> joinAttributes) {
        return left.toMultimap(b -> bindingSetOps.project(joinAttributes, b), b1 -> b1)
                .flatMap((probe) -> right.concatMap(b -> {
                            if (!probe.containsKey(bindingSetOps.project(joinAttributes, b)))
                                return Observable.empty();
                            else
                                return Observable.from(probe.get(bindingSetOps.project(joinAttributes, b)))
                                        .join(Observable.just(b),
                                                b1 -> Observable.never(),
                                                b1 -> Observable.never(),
                                                bindingSetOps::merge);
                        })
                );
    }


    public Observable<BindingSet> evaluateReactiveInternal(BindJoin expr, BindingSet bindings)
        throws QueryEvaluationException
    {
        return this.evaluateReactiveInternal(expr.getLeftArg(), bindings)
                .buffer(getBatchSize())
                .flatMap((b) -> {
                    try {
                        return evaluateReactiveInternal(expr.getRightArg(), b);
                    } catch (Exception e) {
                        return Observable.error(e);
                    }
                });
    }

    public Observable<BindingSet> evaluateReactiveInternal(SourceQuery expr, BindingSet bindings)
            throws QueryEvaluationException
    {
        //return queryExecutor.evaluateReactiveInternal(null, expr.getArg(), bindings)
        return evaluateSourceReactive(expr.getSite(), expr.getArg(), bindings);

    }


    public Observable<BindingSet> evaluateSourceReactive(Site source, TupleExpr expr, BindingSet bindings)
        throws QueryEvaluationException
    {
        Publisher<BindingSet> result = queryExecutor.evaluate(source, expr, bindings);

        return RxReactiveStreams.toObservable(result).subscribeOn(Schedulers.io());
    }

    public Observable<BindingSet> evaluateSourceReactive(Site source, TupleExpr expr, List<BindingSet> bindings)
            throws QueryEvaluationException
    {

        Publisher<BindingSet> result = queryExecutor.evaluate(source, expr, bindings);

        return RxReactiveStreams.toObservable(result).subscribeOn(Schedulers.io());
    }

    public Observable<BindingSet> evaluateReactiveInternal(TupleExpr expr, List<BindingSet> bindingList)
        throws QueryEvaluationException
    {
        if (expr instanceof Plan)
            return evaluateReactiveInternal(((Plan) expr).getArg(), bindingList);
        else if (expr instanceof Union)
            return evaluateReactiveInternal((Union) expr, bindingList);
        else if (expr instanceof SourceQuery)
            return evaluateReactiveInternal((SourceQuery) expr, bindingList);
        else
            return evaluateReactiveDefault(expr, bindingList);
    }

    public Observable<BindingSet> evaluateReactiveInternal(SourceQuery expr, List<BindingSet> bindingList)
        throws QueryEvaluationException
    {
        //return queryExecutor.evaluateReactiveInternal(null, expr.getArg(), bindings)
        return evaluateSourceReactive(expr.getSite(), expr.getArg(), bindingList);
    }

    public Observable<BindingSet> evaluateReactiveDefault(TupleExpr expr, List<BindingSet> bindingList)
        throws QueryEvaluationException
    {
        return Observable.from(bindingList).flatMap(b -> {
            try {
                return evaluateReactiveInternal(expr, b);
            }
            catch (Exception e) {
                return Observable.error(e);
            }
        });
    }

    public Observable<BindingSet> evaluateReactiveInternal(Union expr, List<BindingSet> bindingList)
            throws QueryEvaluationException
    {
        return Observable.just(expr.getLeftArg(), expr.getRightArg())
                .flatMap(e -> { try {
                    return evaluateReactiveInternal(e, bindingList);
                } catch (Exception x) {
                    return Observable.error(x);
                }});
    }

}

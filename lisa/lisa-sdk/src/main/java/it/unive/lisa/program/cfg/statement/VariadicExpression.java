package it.unive.lisa.program.cfg.statement;

import it.unive.lisa.analysis.*;
import it.unive.lisa.analysis.lattices.ExpressionSet;
import it.unive.lisa.interprocedural.InterproceduralAnalysis;
import it.unive.lisa.program.cfg.CFG;
import it.unive.lisa.program.cfg.CodeLocation;
import it.unive.lisa.symbolic.SymbolicExpression;

import javax.annotation.Nonnull;
import java.util.Iterator;
import java.util.Map;
import java.util.NoSuchElementException;

public abstract class VariadicExpression extends NaryExpression {
    private final Map<String, Integer> varargsIndex;

    /**
     * Builds a VariadicExpression happening at the given source location.
     *
     * @param cfg      the cfg that this statement belongs to
     * @param location the location where this statement is defined within the
     *                 program
     */
    protected VariadicExpression(CFG cfg, CodeLocation location, String constructName, Expression[] subExpressions, Map<String, Integer> varargsIndex) {
        super(cfg, location, constructName, subExpressions);
        this.varargsIndex = varargsIndex;
    }

    @Override
    public <A extends AbstractLattice<A>, D extends AbstractDomain<A>> AnalysisState<A> forwardSemanticsAux(
            InterproceduralAnalysis<A, D> interprocedural,
            AnalysisState<A> state,
            ExpressionSet[] params,
            StatementStore<A> expressions)
            throws SemanticException {
        AnalysisState<A> result = state.bottomExecution();
        for (SymbolicExpression[] combination : new CartesianProduct(params)) {
            result = result.lub(
                    fwdVariadicSemantics(interprocedural, state, combination, expressions)
            );
            if (result.isTop()) {
                return result;
            }
        }
        return result;
    }

    public abstract <A extends AbstractLattice<A>, D extends AbstractDomain<A>> AnalysisState<A> fwdVariadicSemantics(InterproceduralAnalysis<A,D> interprocedural, AnalysisState<A> state, SymbolicExpression[] combination, StatementStore<A> expressions) throws SemanticException;

    @Override
    protected int compareSameClass(Statement o) {
        return 0;
    }

    public Map<String, Integer> getVarArgsIndex() {
        return varargsIndex;
    }
    private static final class CartesianProduct implements Iterable<SymbolicExpression[]> {

        private final ExpressionSet[] params;

        public CartesianProduct(ExpressionSet[] params) {
            this.params = params;
        }

        @Override
        @Nonnull
        @SuppressWarnings("unchecked")
        public Iterator<SymbolicExpression[]> iterator() {
            return new Iterator<>() {

                private final Iterator<SymbolicExpression>[] iterators = new Iterator[params.length];
                private final SymbolicExpression[] current = new SymbolicExpression[params.length];
                private boolean hasNext = true;

                {
                    if (params.length == 0) {
                        hasNext = false;
                    } else {
                        for (int i = 0; i < params.length; i++) {
                            iterators[i] = params[i].iterator();
                            if (!iterators[i].hasNext()) {
                                hasNext = false;
                                break;
                            }
                            current[i] = iterators[i].next();
                        }
                    }
                }

                @Override
                public boolean hasNext() {
                    return hasNext;
                }

                @Override
                public SymbolicExpression[] next() {
                    if (!hasNext)
                        throw new NoSuchElementException();

                    SymbolicExpression[] result = current.clone();
                    advance();
                    return result;
                }

                private void advance() {
                    for (int i = params.length - 1; i >= 0; i--) {

                        if (iterators[i].hasNext()) {
                            current[i] = iterators[i].next();
                            for (int j = i + 1; j < params.length; j++) {
                                iterators[j] = params[j].iterator();
                                if (!iterators[j].hasNext()) {
                                    hasNext = false;
                                    return;
                                }
                                current[j] = iterators[j].next();
                            }

                            return;
                        }
                    }

                    hasNext = false;
                }
            };
        }
    }
}

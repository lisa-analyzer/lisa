package it.unive.lisa.symbolic.value;

import it.unive.lisa.analysis.ScopeToken;
import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.analysis.lattices.ExpressionSet;
import it.unive.lisa.program.cfg.CodeLocation;
import it.unive.lisa.program.cfg.ProgramPoint;
import it.unive.lisa.symbolic.ExpressionVisitor;
import it.unive.lisa.symbolic.SymbolicExpression;
import it.unive.lisa.symbolic.value.operator.nary.NaryOperator;
import it.unive.lisa.type.Type;

import javax.annotation.Nonnull;
import java.lang.reflect.Array;
import java.util.*;

public class VariadicExpression extends ValueExpression {
    private final Map<String, Integer> varargsIndex;
    private final SymbolicExpression[] operands;
    private final NaryOperator operator;


    public VariadicExpression(Type staticType, SymbolicExpression[] operands, Map<String, Integer> varargsIndex, NaryOperator operator,  CodeLocation location) {
        super(staticType, location);
        this.operands = operands;
        this.varargsIndex = varargsIndex;
        this.operator = operator;
    }

    public NaryOperator getOperator() {
        return operator;
    }

    public Map<String, Integer> getVarargsIndex() {
        return varargsIndex;
    }

    public SymbolicExpression[] getOperands() {
        return operands;
    }

    public static class Builder {
        private Type staticType;
        private final Map<String, Integer> varargsIndex = new HashMap<>();
        private final List<SymbolicExpression> operands = new ArrayList<>();
        private NaryOperator operator;
        private CodeLocation location;

        public Builder staticType(Type staticType) {
            this.staticType = staticType;
            return this;
        }

        public Builder operator(NaryOperator operator) {
            this.operator = operator;
            return this;
        }

        public Builder location(CodeLocation location) {
            this.location = location;
            return this;
        }

        /**
         * Adds one operand at a time
         */
        public Builder varargsOperand(String name, SymbolicExpression operand) {
            Objects.requireNonNull(operand, "operand must not be null");
            if (this.varargsIndex.get(name) != null) {
                throw new IllegalArgumentException("Duplicate operand: " + name);
            }
            this.operands.add(operand);
            this.varargsIndex.put(name, this.operands.size() - 1);
            return this;
        }

        /**
         * Adds many operands
         */
        public Builder varargsOperands(Map<String, SymbolicExpression> operands) {
            operands.forEach(this::varargsOperand);
            return this;
        }

        public Builder operand(SymbolicExpression operand) {
            this.operands.add(operand);
            return this;
        }
        public VariadicExpression build() {
            Objects.requireNonNull(staticType, "staticType must not be null");
            Objects.requireNonNull(operator, "operator must not be null");
            Objects.requireNonNull(location, "location must not be null");

            if (operands.isEmpty() && varargsIndex.isEmpty())
                throw new IllegalStateException("At least one operand is required");

            return new VariadicExpression(
                    staticType,
                    operands.toArray(new SymbolicExpression[]{}),
                    varargsIndex,
                    operator,
                    location
            );
        }
    }

    @Override
    public SymbolicExpression pushScope(ScopeToken token, ProgramPoint pp) throws SemanticException {
        return null;
    }

    @Override
    public SymbolicExpression popScope(ScopeToken token, ProgramPoint pp) throws SemanticException {
        return null;
    }

    @Override
    public <T> T accept(ExpressionVisitor<T> visitor, Object... params) throws SemanticException {

        T first = operands[0].accept(visitor, params);
        @SuppressWarnings("unchecked")
        T[] newOperands = (T[]) Array.newInstance(first.getClass(), operands.length);
        newOperands[0] = first;

        for (int i = 1; i < operands.length; i++) {
            newOperands[i] = operands[i].accept(visitor, params);
        }

        return visitor.visit(this, newOperands, params);
    }

    @Override
    public String toString() {
        return operator + "(" + operands + ")" + "{" + varargsIndex + "}";
    }

    @Override
    public boolean mightNeedRewriting() {
        return false;
    }

    @Override
    public SymbolicExpression removeTypingExpressions() {
        SymbolicExpression[] newOperands = new SymbolicExpression[operands.length];
        boolean change = false;
        for (int i = 0; i < operands.length; i++) {
            newOperands[i] = operands[i].removeTypingExpressions();
            if (newOperands[i] != operands[i]) {
                change = true;
            }
        }
        if (change) {
            return new VariadicExpression(this.getStaticType(), newOperands, varargsIndex, this.operator, this.getCodeLocation());
        }
        return this;
    }

    @Override
    public SymbolicExpression replace(SymbolicExpression source, SymbolicExpression target) {
        return null;
    }

    public static final class CartesianProduct implements Iterable<SymbolicExpression[]> {

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

                            // reset all to the right
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

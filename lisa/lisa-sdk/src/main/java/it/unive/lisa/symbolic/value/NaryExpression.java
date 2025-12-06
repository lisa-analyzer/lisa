package it.unive.lisa.symbolic.value;

import it.unive.lisa.analysis.ScopeToken;
import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.program.cfg.CodeLocation;
import it.unive.lisa.program.cfg.ProgramPoint;
import it.unive.lisa.symbolic.ExpressionVisitor;
import it.unive.lisa.symbolic.SymbolicExpression;
import it.unive.lisa.symbolic.value.operator.nary.NaryOperator;
import it.unive.lisa.type.Type;

public class NaryExpression extends ValueExpression {

    /**
     * First argument of the expression
     */
    private final SymbolicExpression[] operands;

    /**
     * Operator to apply
     */
    private final NaryOperator operator;

    /**
     * Builds the binary expression.
     *
     * @param staticType the static type of this expression
     * @param operands array of operands
     * @param operator   the operator to apply
     * @param location   the code location of the statement that has generated
     *                       this expression
     */
    public NaryExpression(
            Type staticType,
            SymbolicExpression[] operands,
            NaryOperator operator,
            CodeLocation location) {
        super(staticType, location);
        this.operands = operands;
        this.operator = operator;
    }

    /**
     * Yields the i-th operand of this expression.
     *
     * @return the i-th operand
     */
    public SymbolicExpression getOperand(
            int i) {
        if (i > this.operands.length || i < 0)
            throw new IndexOutOfBoundsException();
        return this.operands[i];
    }
    /**
     * Return the operands of the expression
     *
     * @return the operands
     */
    public SymbolicExpression[] getOperands() {
        return operands;
    }

    /**
     * Yields the operator that is applied to the operands.
     *
     * @return the operator to apply
     */
    public NaryOperator getOperator() {
        return operator;
    }

    @Override
    public SymbolicExpression pushScope(
            ScopeToken token,
            ProgramPoint pp)
            throws SemanticException {
        SymbolicExpression[] aux = new SymbolicExpression[operands.length];

        for (int i = 0; i < operands.length; ++i) {
            aux[i] = operands[i].pushScope(token, pp);
        }

        NaryExpression expr = new NaryExpression(
                getStaticType(),
                aux,
                operator,
                getCodeLocation());
        return expr;
    }

    @Override
    public SymbolicExpression popScope(
            ScopeToken token,
            ProgramPoint pp)
            throws SemanticException {
        SymbolicExpression[] aux = new SymbolicExpression[operands.length];

        for (int i = 0; i < operands.length; ++i) {
            aux[i] = operands[i].pushScope(token, pp);
        }

        return new NaryExpression(
                getStaticType(),
                aux,
                operator,
                getCodeLocation());
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = super.hashCode();
        result = prime * result + ((operator == null) ? 0 : operator.hashCode());

        for (SymbolicExpression expr : operands) {
            result = prime * result + ((expr == null) ? 0 : operator.hashCode());
        }

        return result;
    }

    @Override
    public boolean equals(
            Object obj) {
        if (this == obj)
            return true;
        if (!super.equals(obj))
            return false;
        if (getClass() != obj.getClass())
            return false;
        NaryExpression other = (NaryExpression) obj;

        if (operands.length != other.operands.length)
            return false;

        for (int i = 0; i < operands.length; ++i) {
            if (this.operands[i] == null) {
                if (other.operands[i] != null)
                    return false;
            } else if (!this.operands[i].equals(other.operands[i]))
                return false;
        }
        return operator == other.operator;
    }

    @Override
    public String toString() {
        StringBuilder res = new StringBuilder(operands[0] + " " + operator + "(");
        for (int i = 1; i < operands.length; ++i) {
            res.append(operands[i]);
            if (i != operands.length - 1) {
                res.append(", ");
            }
        }
        res.append(")");
        return res.toString();
    }

    @Override
    public <T> T accept(
            ExpressionVisitor<T> visitor,
            Object... params)
            throws SemanticException {
        @SuppressWarnings("unchecked")
        T[] op = (T[]) new Object[operands.length];

        for (int i = 0; i < operands.length; ++i)
            op[i] = operands[i].accept(visitor, params);

        return visitor.visit(this, op, params);
    }

    @Override
    public boolean mightNeedRewriting() {
        for (SymbolicExpression expr : operands)
            if (expr.mightNeedRewriting())
                return true;

        return false;
    }

    /**
     * Yields a copy of this expression with the given operator.
     *
     * @param operator the operator to apply to the left, middle, and right
     *                     operands
     *
     * @return the copy of this expression with the given operator
     */
    public NaryExpression withOperator(
            NaryOperator operator) {
        return new NaryExpression(getStaticType(), operands, operator, getCodeLocation());
    }

    @Override
    public SymbolicExpression removeTypingExpressions() {
        SymbolicExpression[] aux = new SymbolicExpression[operands.length];

        for (int i = 0; i < operands.length; ++i) {
            aux[i] = operands[i].removeTypingExpressions();
        }

        boolean cond = true;

        for (int i = 0; i < operands.length; ++i) {
            cond = cond && (aux[i] == operands[i]);
        }

        if (cond)
            return this;
        return new NaryExpression(getStaticType(), aux, operator, getCodeLocation());
    }

    @Override
    public SymbolicExpression replace(
            SymbolicExpression source,
            SymbolicExpression target) {
        SymbolicExpression[] aux = new SymbolicExpression[operands.length];

        for (int i = 0; i < operands.length; ++i) {
            aux[i] = operands[i].replace(source, target);
        }

        boolean cond = true;

        for (int i = 0; i < operands.length; ++i) {
            cond = cond && (aux[i] == operands[i]);
        }

        if (cond)
            return this;
        return new NaryExpression(getStaticType(), aux, operator, getCodeLocation());
    }

}

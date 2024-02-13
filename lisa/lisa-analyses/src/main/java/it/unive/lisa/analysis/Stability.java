package it.unive.lisa.analysis;

import it.unive.lisa.analysis.lattices.Satisfiability;
import it.unive.lisa.analysis.nonrelational.value.ValueEnvironment;
import it.unive.lisa.analysis.numeric.Interval;
import it.unive.lisa.analysis.value.ValueDomain;
import it.unive.lisa.program.SyntheticLocation;
import it.unive.lisa.program.cfg.ProgramPoint;
import it.unive.lisa.symbolic.SymbolicExpression;
import it.unive.lisa.symbolic.value.*;
import it.unive.lisa.symbolic.value.operator.AdditionOperator;
import it.unive.lisa.symbolic.value.operator.DivisionOperator;
import it.unive.lisa.symbolic.value.operator.MultiplicationOperator;
import it.unive.lisa.symbolic.value.operator.SubtractionOperator;
import it.unive.lisa.symbolic.value.operator.binary.*;
import it.unive.lisa.symbolic.value.operator.unary.NumericNegation;
import it.unive.lisa.util.representation.StructuredRepresentation;

import java.util.function.Predicate;

public class Stability implements ValueDomain<Stability> {

    /**
     * The abstract top element.
     */
    public static final Stability TOP = new Stability((byte) 0);

    /**
     * The abstract bottom element.
     */
    public static final Stability BOTTOM = new Stability((byte) 1);

    /**
     * The abstract stable element.
     */
    public static final Stability STABLE = new Stability((byte) 2);

    /**
     * The abstract increasing element.
     */
    public static final Stability INC = new Stability((byte) 3);

    /**
     * The abstract decreasing element.
     */
    public static final Stability DEC = new Stability((byte) 4);

    /**
     * The abstract not stable element.
     */
    public static final Stability NON_STABLE = new Stability((byte) 5);

    /**
     * The abstract non-increasing element.
     */
    public static final Stability NON_INC = new Stability((byte) 6);

    /**
     * The abstract non-decreasing element.
     */
    public static final Stability NON_DEC = new Stability((byte) 7);

    private final byte stability;

    private final ValueEnvironment<Interval> intervals;

    public Stability(byte stability){
        this.stability = stability;
        this.intervals = new ValueEnvironment<>(new Interval()).top();
    }

    public Stability(byte stability, ValueEnvironment<Interval> intervals) {
        this.stability = stability;
        this.intervals = intervals;
    }

    @Override
    public boolean lessOrEqual(Stability other) throws SemanticException {
        return false;
    }

    @Override
    public Stability lub(Stability other) throws SemanticException {
        return null;
    }

    @Override
    public Stability top() {
        return TOP;
    }

    @Override
    public Stability bottom() {
        return BOTTOM;
    }

    @Override
    public Stability pushScope(ScopeToken token) throws SemanticException {
        return null;
    }

    @Override
    public Stability popScope(ScopeToken token) throws SemanticException {
        return null;
    }

    private boolean queryToAux(
            BinaryExpression query,
            ProgramPoint pp,
            SemanticOracle oracle)
            throws SemanticException {

        return intervals.satisfies(query, pp, oracle) == Satisfiability.SATISFIED;
    }

    /**
     * Builds BinaryExpression "l operator r"
     *
     * @return the new BinaryExpression
     */
    private BinaryExpression binary(
            BinaryOperator operator,
            SymbolicExpression l,
            SymbolicExpression r,
            ProgramPoint pp){

        return new BinaryExpression(
                pp.getProgram().getTypes().getBooleanType(),
                l,
                r,
                operator,
                SyntheticLocation.INSTANCE);
    }

    /**
     * Builds Constant with value c
     *
     * @return the new Constant
     */
    private Constant constantInt(int c, ProgramPoint pp){
        return new Constant(
                pp.getProgram().getTypes().getIntegerType(),
                c,
                SyntheticLocation.INSTANCE
        );
    }

    private Stability opposite(){
        if (this == TOP || this == BOTTOM || this == STABLE || this == NON_STABLE) return this;
        else if (this == INC) return DEC;
        else if (this == DEC) return INC;
        else if (this == NON_INC) return NON_DEC;
        else if (this == NON_DEC) return NON_INC;

        else return TOP;
    }

    /**
     * Returns a Stability based on query "a > b ?" to auxiliary domain
     *
     * @return INC if a > b, DEC if a < b, STABLE if a == b ...
     */
    private Stability increasingIfGreater(SymbolicExpression a, SymbolicExpression b, ProgramPoint pp, SemanticOracle oracle)
            throws SemanticException {
        if (queryToAux(binary(ComparisonGt.INSTANCE, a, b, pp), pp, oracle)) return INC;
        else if (queryToAux(binary(ComparisonEq.INSTANCE, a, b, pp), pp, oracle)) return STABLE;
        else if (queryToAux(binary(ComparisonLt.INSTANCE, a, b, pp), pp, oracle)) return DEC;
        else if (queryToAux(binary(ComparisonGe.INSTANCE, a, b, pp), pp, oracle)) return NON_DEC;
        else if (queryToAux(binary(ComparisonLe.INSTANCE, a, b, pp), pp, oracle)) return NON_INC;
        else if (queryToAux(binary(ComparisonNe.INSTANCE, a, b, pp), pp, oracle)) return NON_STABLE;
        else return TOP;
    }

    private Stability increasingIfLess(SymbolicExpression a, SymbolicExpression b, ProgramPoint pp, SemanticOracle oracle)
            throws SemanticException {
        //return increasingIfGreater(b, a, pp, oracle);
        return increasingIfGreater(a, b, pp, oracle).opposite();
    }

    private Stability nonDecreasingIfGreater(SymbolicExpression a, SymbolicExpression b, ProgramPoint pp, SemanticOracle oracle)
            throws SemanticException {

        if (queryToAux(binary(ComparisonEq.INSTANCE, a, b, pp), pp, oracle)) return STABLE;

        else if (queryToAux(binary(ComparisonGt.INSTANCE, a, b, pp), pp, oracle)
                || queryToAux(binary(ComparisonGe.INSTANCE, a, b, pp), pp, oracle))
            return NON_DEC;

        else if (queryToAux(binary(ComparisonLt.INSTANCE, a, b, pp), pp, oracle)
                || queryToAux(binary(ComparisonLe.INSTANCE, a, b, pp), pp, oracle))
            return NON_INC;

        else return TOP;
    }

    private Stability nonDecreasingIfLess(SymbolicExpression a, SymbolicExpression b, ProgramPoint pp, SemanticOracle oracle)
            throws SemanticException {
        //return nonDecreasingIfGreaterOrEqual(b, a, pp, oracle);
        return nonDecreasingIfGreater(a, b, pp, oracle).opposite();
    }

    // assume !(a == 0 || a >= 0 || a <= 0)
    private Stability increasingIfBetweenZeroAnd(SymbolicExpression a, SymbolicExpression b, ProgramPoint pp, SemanticOracle oracle)
            throws SemanticException {

        Constant zero = constantInt(0, pp);

        // a == b
        if (queryToAux(binary(ComparisonEq.INSTANCE, a, b, pp), pp, oracle)) return STABLE;

        // 0 < a
        else if (queryToAux(binary(ComparisonGt.INSTANCE, a, zero, pp), pp, oracle)) {
            // a < b
            if (queryToAux(binary(ComparisonLt.INSTANCE, a, b, pp), pp, oracle))
                return INC;
            // a <= b
            else if (queryToAux(binary(ComparisonLe.INSTANCE, a, b, pp), pp, oracle))
                return NON_DEC;
        }

        // a < 0 || a > b
        else if (queryToAux(binary(ComparisonLt.INSTANCE, a, zero, pp), pp, oracle)
                || queryToAux(binary(ComparisonGt.INSTANCE, a, b, pp), pp, oracle))
            return DEC;

        // a >= b
        else if (queryToAux(binary(ComparisonGe.INSTANCE, a, b, pp), pp, oracle))
            return NON_INC;

        // a != b
        else if (queryToAux(binary(ComparisonNe.INSTANCE, a, b, pp), pp, oracle))
            return NON_STABLE;

        return TOP;

    }

    // assume !(a == 0 || a >= 0 || a <= 0)
    private Stability increasingIfOutsideZeroAnd(SymbolicExpression a, SymbolicExpression b, ProgramPoint pp, SemanticOracle oracle)
            throws SemanticException {
        return increasingIfBetweenZeroAnd(a, b, pp, oracle).opposite();
    }

    // assume !(a == 0 || a >= 0 || a <= 0)
    private Stability nonDecreasingIfBetweenZeroAnd(SymbolicExpression a, SymbolicExpression b, ProgramPoint pp, SemanticOracle oracle)
            throws SemanticException {

        Constant zero = constantInt(0, pp);

        // a == b
        if (queryToAux(binary(ComparisonEq.INSTANCE, a, b, pp), pp, oracle)) return STABLE;

        // 0 < a < b || 0 < a <= b
        // a > 0 && (a < b || a <= b)
        if ( queryToAux(binary(ComparisonGt.INSTANCE, a, zero, pp), pp, oracle)
                && (queryToAux(binary(ComparisonLt.INSTANCE, a, b, pp), pp, oracle)
                || queryToAux(binary(ComparisonLe.INSTANCE, a, b, pp), pp, oracle)) )
            return NON_DEC;

        // a < 0 || a > b || a >= b
        else if (queryToAux(binary(ComparisonLt.INSTANCE, a, zero, pp), pp, oracle)
                || queryToAux(binary(ComparisonGt.INSTANCE, a, b, pp), pp, oracle)
                || queryToAux(binary(ComparisonGe.INSTANCE, a, b, pp), pp, oracle))
            return NON_INC;

        return TOP;
    }

    // assume !(a == 0 || a >= 0 || a <= 0)
    private Stability nonDecreasingIfOutsideZeroAnd(SymbolicExpression a, SymbolicExpression b, ProgramPoint pp, SemanticOracle oracle)
            throws SemanticException {
        return nonDecreasingIfBetweenZeroAnd(a, b, pp, oracle).opposite();
    }


    // TO DO: maybe move last return out of the ifs
    @Override
    public Stability assign(Identifier id, ValueExpression expression, ProgramPoint pp, SemanticOracle oracle) throws SemanticException {

        if (expression instanceof UnaryExpression &&
                ((UnaryExpression) expression).getOperator() instanceof NumericNegation)
            return increasingIfLess(id, expression, pp, oracle);

        else if (expression instanceof BinaryExpression) {
            BinaryExpression be = (BinaryExpression) expression;
            BinaryOperator op = be.getOperator();
            SymbolicExpression left = be.getLeft();
            SymbolicExpression right = be.getRight();

            boolean isLeft = id.equals(left);
            boolean isRight = id.equals(right);

            if (isLeft || isRight) {
                SymbolicExpression other = isLeft ? right : left;

                // x = x + other || x = other + x
                if (op instanceof AdditionOperator)
                    return increasingIfGreater(other, constantInt(0, pp), pp, oracle);

                // x = x - other
                else if (op instanceof SubtractionOperator) {
                    if (isLeft) return increasingIfLess(right, constantInt(0, pp), pp, oracle);
                    else return increasingIfLess(id, expression, pp, oracle);
                }

                // x = x * other || x = other * x
                else if (op instanceof MultiplicationOperator) {

                    // id == 0 || other == 1
                    if (queryToAux(binary(ComparisonEq.INSTANCE, id, constantInt(0, pp), pp), pp, oracle)
                            || queryToAux(binary(ComparisonEq.INSTANCE, other, constantInt(1, pp), pp), pp, oracle))
                        return STABLE;

                    // id > 0
                    else if (queryToAux(binary(ComparisonGt.INSTANCE, id, constantInt(0, pp), pp), pp, oracle))
                        return increasingIfGreater(other, constantInt(1, pp), pp, oracle);

                    // id < 0
                    else if (queryToAux(binary(ComparisonLt.INSTANCE, id, constantInt(0, pp), pp), pp, oracle))
                        return increasingIfLess(other, constantInt(1, pp), pp, oracle);

                    // id >= 0
                    else if (queryToAux(binary(ComparisonGe.INSTANCE, id, constantInt(0, pp), pp), pp, oracle))
                        return nonDecreasingIfGreater(other, constantInt(1, pp), pp, oracle);

                    // id <= 0
                    else if (queryToAux(binary(ComparisonLe.INSTANCE, id, constantInt(0, pp), pp), pp, oracle))
                        return nonDecreasingIfLess(other, constantInt(1, pp), pp, oracle);

                    // id != 0 && other != 1
                    else if (queryToAux(binary(ComparisonNe.INSTANCE, id, constantInt(0, pp), pp), pp, oracle)
                            && queryToAux(binary(ComparisonNe.INSTANCE, other, constantInt(1, pp), pp), pp, oracle))
                        return NON_STABLE;

                    else return TOP;    // Q

                }

                // x = x / other
                else if (op instanceof DivisionOperator){
                    if (isLeft) {

                        // id == 0 || other == 1
                        if (queryToAux(binary(ComparisonEq.INSTANCE, id, constantInt(0, pp), pp), pp, oracle)
                                || queryToAux(binary(ComparisonEq.INSTANCE, other, constantInt(1, pp), pp), pp, oracle))
                            return STABLE;

                        // id > 0
                        else if (queryToAux(binary(ComparisonGt.INSTANCE, id, constantInt(0, pp), pp), pp, oracle))
                            return increasingIfBetweenZeroAnd(other, constantInt(1, pp), pp, oracle);

                        // id < 0
                        else if (queryToAux(binary(ComparisonLt.INSTANCE, id, constantInt(0, pp), pp), pp, oracle))
                            return increasingIfOutsideZeroAnd(other, constantInt(1, pp), pp, oracle);

                        // id >= 0
                        else if (queryToAux(binary(ComparisonGe.INSTANCE, id, constantInt(0, pp), pp), pp, oracle))
                            return nonDecreasingIfBetweenZeroAnd(other, constantInt(1, pp), pp, oracle);

                        // id <= 0
                        else if (queryToAux(binary(ComparisonLe.INSTANCE, id, constantInt(0, pp), pp), pp, oracle))
                            return nonDecreasingIfOutsideZeroAnd(other, constantInt(1, pp), pp, oracle);

                        // id != 0 && other != 1
                        else if (queryToAux(binary(ComparisonNe.INSTANCE, id, constantInt(0, pp), pp), pp, oracle)
                                && queryToAux(binary(ComparisonNe.INSTANCE, other, constantInt(1, pp), pp), pp, oracle))
                            return NON_STABLE;

                        else return TOP;
                    }

                    else return increasingIfLess(id, expression, pp, oracle);

                }

                // op is not +, -, * or /
                else return TOP;    // Q
            }

            // x = a OP b
            else return increasingIfLess(id, expression, pp, oracle);
        }

        // not UnaryExpression && not BinaryExpression
        return TOP;
    }

    @Override
    public Stability smallStepSemantics(ValueExpression expression, ProgramPoint pp, SemanticOracle oracle) throws SemanticException {
        return null;
    }

    @Override
    public Stability assume(ValueExpression expression, ProgramPoint src, ProgramPoint dest, SemanticOracle oracle) throws SemanticException {
        return null;
    }

    @Override
    public boolean knowsIdentifier(Identifier id) {
        return false;
    }

    @Override
    public Stability forgetIdentifier(Identifier id) throws SemanticException {
        return null;
    }

    @Override
    public Stability forgetIdentifiersIf(Predicate<Identifier> test) throws SemanticException {
        return null;
    }

    @Override
    public Satisfiability satisfies(ValueExpression expression, ProgramPoint pp, SemanticOracle oracle) throws SemanticException {
        return null;
    }

    @Override
    public StructuredRepresentation representation() {
        return null;
    }
}

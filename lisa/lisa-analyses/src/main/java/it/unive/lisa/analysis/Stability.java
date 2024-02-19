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
import it.unive.lisa.util.representation.ListRepresentation;
import it.unive.lisa.util.representation.StructuredRepresentation;

import java.util.function.Predicate;

public class Stability implements ValueDomain<Stability> {


    private final ValueEnvironment<Interval> intervals;

    private final ValueEnvironment<Trend> trend;

    public Stability() {
        this.intervals = new ValueEnvironment<>(new Interval()).top();
        this.trend = new ValueEnvironment<>(new Trend((byte)0));
    }

    public Stability(ValueEnvironment<Interval> intervals, ValueEnvironment<Trend> trend) {
        this.intervals = intervals;
        this.trend = trend;
    }

    @Override
    public boolean lessOrEqual(Stability other) throws SemanticException {
        return (intervals.lessOrEqual(other.intervals)
                && trend.lessOrEqual(other.trend));
    }

    @Override
    public Stability lub(Stability other) throws SemanticException {
        return new Stability(intervals.lub(other.intervals), trend.lub(other.trend));
    }

    @Override
    public Stability top() {
        return new Stability(intervals.top(), trend.top());
    }

    @Override
    public Stability bottom() {
        return new Stability(intervals.bottom(), trend.bottom());
    }

    @Override
    public Stability pushScope(ScopeToken token) throws SemanticException {
        return new Stability(intervals.pushScope(token), trend.pushScope(token));
    }

    @Override
    public Stability popScope(ScopeToken token) throws SemanticException {
        return new Stability(intervals.popScope(token), trend.popScope(token));
    }

    /**
     * Verifies weather a query expression is satisfied in the Intervals domain
      * @return {@code true} if the expression is satisfied
     */
    private boolean query(
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

    /**
     * Generates a Trend based on the relationship between a and b in the {@code intervals} domain
     * @return {@code INC} if a > b
     */
    private Trend increasingIfGreater(SymbolicExpression a, SymbolicExpression b, ProgramPoint pp, SemanticOracle oracle)
            throws SemanticException {

        return Trend.generateTrendIncIfGt(
                query(binary(ComparisonEq.INSTANCE, a, b, pp), pp, oracle),
                query(binary(ComparisonGt.INSTANCE, a, b, pp), pp, oracle),
                query(binary(ComparisonGe.INSTANCE, a, b, pp), pp, oracle),
                query(binary(ComparisonLt.INSTANCE, a, b, pp), pp, oracle),
                query(binary(ComparisonLe.INSTANCE, a, b, pp), pp, oracle),
                query(binary(ComparisonNe.INSTANCE, a, b, pp), pp, oracle)
        );
    }

    /**
     * Generates a Trend based on the relationship between a and b in the {@code intervals} domain
     * @return {@code INC} if a < b
     */
    private Trend increasingIfLess(SymbolicExpression a, SymbolicExpression b, ProgramPoint pp, SemanticOracle oracle)
            throws SemanticException {
        return increasingIfGreater(a, b, pp, oracle).opposite();
    }

    /**
     * Generates a Trend based on the relationship between a and b in the {@code intervals} domain
     * @return {@code NON_DEC} if a > b
     */
    private Trend nonDecreasingIfGreater(SymbolicExpression a, SymbolicExpression b, ProgramPoint pp, SemanticOracle oracle)
            throws SemanticException {

        return Trend.generateTrendNonDecIfGt(
                query(binary(ComparisonEq.INSTANCE, a, b, pp), pp, oracle),
                query(binary(ComparisonGt.INSTANCE, a, b, pp), pp, oracle),
                query(binary(ComparisonGe.INSTANCE, a, b, pp), pp, oracle),
                query(binary(ComparisonLt.INSTANCE, a, b, pp), pp, oracle),
                query(binary(ComparisonLe.INSTANCE, a, b, pp), pp, oracle),
                query(binary(ComparisonNe.INSTANCE, a, b, pp), pp, oracle)
        );
    }

    /**
     * Generates a Trend based on the relationship between a and b in the {@code intervals} domain
     * @return {@code NON_DEC} if a < b
     */
    private Trend nonDecreasingIfLess(SymbolicExpression a, SymbolicExpression b, ProgramPoint pp, SemanticOracle oracle)
            throws SemanticException {
        return nonDecreasingIfGreater(a, b, pp, oracle).opposite();
    }

    /**
     * Generates a Trend based on the value of {@code a} in the {@code intervals} domain
     * @return {@code INC} if 0 < a < 1
     */
    private Trend increasingIfBetweenZeroAndOne(SymbolicExpression a, ProgramPoint pp, SemanticOracle oracle)
            throws SemanticException {

        Constant zero = constantInt(0, pp);
        Constant one = constantInt(1, pp);

        if (!query(binary(ComparisonNe.INSTANCE, a, zero, pp), pp, oracle))
            return Trend.BOTTOM;

        else return Trend.generateTrendIncIfBetween(
                false,
                query(binary(ComparisonGt.INSTANCE, a, zero, pp), pp, oracle),
                false,
                query(binary(ComparisonLt.INSTANCE, a, zero, pp), pp, oracle),
                false,
                true,
                query(binary(ComparisonEq.INSTANCE, a, one, pp), pp, oracle),
                query(binary(ComparisonGt.INSTANCE, a, one, pp), pp, oracle),
                query(binary(ComparisonGe.INSTANCE, a, one, pp), pp, oracle),
                query(binary(ComparisonLt.INSTANCE, a, one, pp), pp, oracle),
                query(binary(ComparisonLe.INSTANCE, a, one, pp), pp, oracle),
                query(binary(ComparisonNe.INSTANCE, a, one, pp), pp, oracle)
        );
    }

    /**
     * Generates a Trend based on the value of {@code a} in the {@code intervals} domain
     * @return {@code INC} if {@code (a < 0 || a > 1)}
     */
    private Trend increasingIfOutsideZeroAndOne(SymbolicExpression a, ProgramPoint pp, SemanticOracle oracle)
            throws SemanticException{
        return increasingIfBetweenZeroAndOne(a, pp, oracle).opposite();
    }

    /**
     * Generates a Trend based on the value of {@code a} in the {@code intervals} domain
     * @return {@code NON_DEC} if 0 < a < 1
     */
    private Trend nonDecreasingIfBetweenZeroAndOne(SymbolicExpression a, ProgramPoint pp, SemanticOracle oracle)
            throws SemanticException{

        Constant zero = constantInt(0, pp);
        Constant one = constantInt(1, pp);

        if (!query(binary(ComparisonNe.INSTANCE, a, zero, pp), pp, oracle))
            return Trend.BOTTOM;

        else return Trend.generateTrendNonDecIfBetween(
                false,
                query(binary(ComparisonGt.INSTANCE, a, zero, pp), pp, oracle),
                false,
                query(binary(ComparisonLt.INSTANCE, a, zero, pp), pp, oracle),
                false,
                true,

                query(binary(ComparisonEq.INSTANCE, a, one, pp), pp, oracle),
                query(binary(ComparisonGt.INSTANCE, a, one, pp), pp, oracle),
                query(binary(ComparisonGe.INSTANCE, a, one, pp), pp, oracle),
                query(binary(ComparisonLt.INSTANCE, a, one, pp), pp, oracle),
                query(binary(ComparisonLe.INSTANCE, a, one, pp), pp, oracle),
                query(binary(ComparisonNe.INSTANCE, a, one, pp), pp, oracle)
        );

    }

    /**
     * Generates a Trend based on the value of {@code a} in the {@code intervals} domain
     * @return {@code NON_DEC} if {@code (a < 0 || a > 1)}
     */
    private Trend nonDecreasingIfOutsideZeroAndOne(SymbolicExpression a, ProgramPoint pp, SemanticOracle oracle)
            throws SemanticException{
        return nonDecreasingIfBetweenZeroAndOne(a, pp, oracle).opposite();
    }



    @Override
    public Stability assign(Identifier id, ValueExpression expression, ProgramPoint pp, SemanticOracle oracle) throws SemanticException {

        Trend returnTrend = Trend.TOP;

        if (expression instanceof UnaryExpression &&
                ((UnaryExpression) expression).getOperator() instanceof NumericNegation)
            returnTrend = increasingIfLess(id, expression, pp, oracle);

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
                    returnTrend = increasingIfGreater(other, constantInt(0, pp), pp, oracle);

                // x = x - other
                else if (op instanceof SubtractionOperator) {
                    if (isLeft) returnTrend = increasingIfLess(other, constantInt(0, pp), pp, oracle);
                    else returnTrend = increasingIfLess(id, expression, pp, oracle);
                }

                // x = x * other || x = other * x
                else if (op instanceof MultiplicationOperator) {

                    // id == 0 || other == 1
                    if (query(binary(ComparisonEq.INSTANCE, id, constantInt(0, pp), pp), pp, oracle)
                            || query(binary(ComparisonEq.INSTANCE, other, constantInt(1, pp), pp), pp, oracle))
                        returnTrend = Trend.STABLE;

                    // id > 0
                    else if (query(binary(ComparisonGt.INSTANCE, id, constantInt(0, pp), pp), pp, oracle))
                        returnTrend = increasingIfGreater(other, constantInt(1, pp), pp, oracle);

                    // id < 0
                    else if (query(binary(ComparisonLt.INSTANCE, id, constantInt(0, pp), pp), pp, oracle))
                        returnTrend = increasingIfLess(other, constantInt(1, pp), pp, oracle);

                    // id >= 0
                    else if (query(binary(ComparisonGe.INSTANCE, id, constantInt(0, pp), pp), pp, oracle))
                        returnTrend = nonDecreasingIfGreater(other, constantInt(1, pp), pp, oracle);

                    // id <= 0
                    else if (query(binary(ComparisonLe.INSTANCE, id, constantInt(0, pp), pp), pp, oracle))
                        returnTrend = nonDecreasingIfLess(other, constantInt(1, pp), pp, oracle);

                    // id != 0 && other != 1
                    else if (query(binary(ComparisonNe.INSTANCE, id, constantInt(0, pp), pp), pp, oracle)
                            && query(binary(ComparisonNe.INSTANCE, other, constantInt(1, pp), pp), pp, oracle))
                        returnTrend = Trend.NON_STABLE;

                    //else returnTrend = Trend.TOP;
                }

                // x = x / other
                else if (op instanceof DivisionOperator){
                    if (isLeft) {

                        // id == 0 || other == 1
                        if (query(binary(ComparisonEq.INSTANCE, id, constantInt(0, pp), pp), pp, oracle)
                                || query(binary(ComparisonEq.INSTANCE, other, constantInt(1, pp), pp), pp, oracle))
                            returnTrend = Trend.STABLE;

                        // id > 0
                        else if (query(binary(ComparisonGt.INSTANCE, id, constantInt(0, pp), pp), pp, oracle))
                            returnTrend = increasingIfBetweenZeroAndOne(other, pp, oracle);

                        // id < 0
                        else if (query(binary(ComparisonLt.INSTANCE, id, constantInt(0, pp), pp), pp, oracle))
                            returnTrend = increasingIfOutsideZeroAndOne(other, pp, oracle);

                        // id >= 0
                        else if (query(binary(ComparisonGe.INSTANCE, id, constantInt(0, pp), pp), pp, oracle))
                            returnTrend = nonDecreasingIfBetweenZeroAndOne(other, pp, oracle);

                        // id <= 0
                        else if (query(binary(ComparisonLe.INSTANCE, id, constantInt(0, pp), pp), pp, oracle))
                            returnTrend = nonDecreasingIfOutsideZeroAndOne(other, pp, oracle);

                        // id != 0 && other != 1
                        else if (query(binary(ComparisonNe.INSTANCE, id, constantInt(0, pp), pp), pp, oracle)
                                && query(binary(ComparisonNe.INSTANCE, other, constantInt(1, pp), pp), pp, oracle))
                            returnTrend = Trend.NON_STABLE;

                        //else returnTrend = Trend.TOP;
                    }

                    else returnTrend = increasingIfLess(id, expression, pp, oracle);

                }

                // else returnTrend = Trend.TOP;
            }
            else returnTrend = increasingIfLess(id, expression, pp, oracle);
        }

        //else returnTrend = Trend.TOP;

        return new Stability(
                intervals.assign(id, expression, pp, oracle),
                new ValueEnvironment<>(new Trend(returnTrend.getTrend()))); // Q
    }

    @Override
    public Stability smallStepSemantics(ValueExpression expression, ProgramPoint pp, SemanticOracle oracle) throws SemanticException {
        return new Stability(
                intervals.smallStepSemantics(expression, pp, oracle),
                trend.smallStepSemantics(expression, pp, oracle));
    }

    @Override
    public Stability assume(ValueExpression expression, ProgramPoint src, ProgramPoint dest, SemanticOracle oracle) throws SemanticException {
        return new Stability(
                intervals.assume(expression, src, dest, oracle),
                trend.assume(expression, src, dest, oracle));
    }

    @Override
    public boolean knowsIdentifier(Identifier id) {
        return (intervals.knowsIdentifier(id)
                || trend.knowsIdentifier(id));
    }

    @Override
    public Stability forgetIdentifier(Identifier id) throws SemanticException {
        return new Stability(
                intervals.forgetIdentifier(id),
                trend.forgetIdentifier(id));
    }

    @Override
    public Stability forgetIdentifiersIf(Predicate<Identifier> test) throws SemanticException {
        return new Stability(
                intervals.forgetIdentifiersIf(test),
                trend.forgetIdentifiersIf(test));
    }

    @Override
    public Satisfiability satisfies(ValueExpression expression, ProgramPoint pp, SemanticOracle oracle) throws SemanticException {
        return intervals.satisfies(expression, pp, oracle).glb(trend.satisfies(expression, pp, oracle));
    }

    @Override
    public StructuredRepresentation representation() {
        return new ListRepresentation(intervals.representation(), trend.representation());
    }
}

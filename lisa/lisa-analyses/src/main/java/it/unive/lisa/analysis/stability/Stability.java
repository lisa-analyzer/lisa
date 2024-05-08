package it.unive.lisa.analysis.stability;

import it.unive.lisa.analysis.BaseLattice;
import it.unive.lisa.analysis.ScopeToken;
import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.analysis.SemanticOracle;
import it.unive.lisa.analysis.lattices.Satisfiability;
import it.unive.lisa.analysis.nonrelational.value.ValueEnvironment;
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

public class Stability<V extends ValueDomain<V> & BaseLattice<V>> implements BaseLattice<Stability<V>>, ValueDomain<Stability<V>> {

    private final V auxiliaryDomain;

    private final ValueEnvironment<Trend> trend;

    public Stability(V auxiliaryDomain) {
        this.auxiliaryDomain = auxiliaryDomain;
        this.trend = new ValueEnvironment<>(new Trend((byte)2));
    }

    public Stability(V auxiliaryDomain, ValueEnvironment<Trend> trend) {
        this.auxiliaryDomain = auxiliaryDomain;
        this.trend = trend;
    }

    @Override
    public Stability<V> lubAux(Stability<V> other) throws SemanticException {
        V ad = auxiliaryDomain.lub(other.getAuxiliaryDomain());
        ValueEnvironment<Trend> t = trend.lub(other.getTrend());
        if (ad.isBottom() || t.isBottom()) return bottom();
        else
            return new Stability<>(ad, t);
    }

    @Override
    public Stability<V> glbAux(Stability<V> other) throws SemanticException {
        V ad = auxiliaryDomain.glb(other.getAuxiliaryDomain());
        ValueEnvironment<Trend> t = trend.glb(other.getTrend());
        if (ad.isBottom() || t.isBottom()) return bottom();
        else return new Stability<>(ad, t);
    }

    @Override
    public Stability<V> wideningAux(Stability<V> other) throws SemanticException {
        V ad = auxiliaryDomain.widening(other.getAuxiliaryDomain());
        ValueEnvironment<Trend> t = trend.widening(other.getTrend());
        if (ad.isBottom() || t.isBottom()) return bottom();
        else return new Stability<>(ad, t);
    }

    @Override
    public boolean lessOrEqualAux(Stability<V> other) throws SemanticException {
        return (getAuxiliaryDomain().lessOrEqual(other.getAuxiliaryDomain())
                && getTrend().lessOrEqual(other.getTrend()));
    }

    @Override
    public boolean isTop() {
        return (auxiliaryDomain.isTop() && trend.isTop());
    }

    @Override
    public boolean isBottom() {
        return (auxiliaryDomain.isBottom() && trend.isBottom());
    }

    @Override
    public Stability<V> top() {
        return new Stability<>(auxiliaryDomain.top(), trend.top());
    }

    @Override
    public Stability<V> bottom() {
        return new Stability<>(auxiliaryDomain.bottom(), trend.bottom());
    }

    @Override
    public Stability<V> pushScope(ScopeToken token) throws SemanticException {
        return new Stability<>(auxiliaryDomain.pushScope(token), trend.pushScope(token));
    }

    @Override
    public Stability<V> popScope(ScopeToken token) throws SemanticException {
        return new Stability<>(auxiliaryDomain.popScope(token), trend.popScope(token));
    }

    /**
     * Verifies weather a query expression is satisfied in the V domain
      * @return {@code true} if the expression is satisfied
     */
    private boolean query(
            BinaryExpression query,
            ProgramPoint pp,
            SemanticOracle oracle)
            throws SemanticException {

        return auxiliaryDomain.satisfies(query, pp, oracle) == Satisfiability.SATISFIED;
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
     * Generates a Trend based on the relationship between a and b in the {@code auxiliaryDomain} domain
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
     * Generates a Trend based on the relationship between a and b in the {@code auxiliaryDomain} domain
     * @return {@code INC} if a < b
     */
    private Trend increasingIfLess(SymbolicExpression a, SymbolicExpression b, ProgramPoint pp, SemanticOracle oracle)
            throws SemanticException {
        return increasingIfGreater(a, b, pp, oracle).invert();
    }

    /**
     * Generates a Trend based on the relationship between a and b in the {@code auxiliaryDomain} domain
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
     * Generates a Trend based on the relationship between a and b in the {@code auxiliaryDomain} domain
     * @return {@code NON_DEC} if a < b
     */
    private Trend nonDecreasingIfLess(SymbolicExpression a, SymbolicExpression b, ProgramPoint pp, SemanticOracle oracle)
            throws SemanticException {
        return nonDecreasingIfGreater(a, b, pp, oracle).invert();
    }

    /**
     * Generates a Trend based on the value of {@code a} in the {@code auxiliaryDomain} domain
     * @return {@code INC} if 0 < a < 1 || 0 <= a < 1
     */
    private Trend increasingIfBetweenZeroAndOne(SymbolicExpression a, ProgramPoint pp, SemanticOracle oracle)
            throws SemanticException {

        Constant zero = constantInt(0, pp);
        Constant one = constantInt(1, pp);

        return Trend.generateTrendIncIfBetween(
                false,
                query(binary(ComparisonGe.INSTANCE, a, zero, pp), pp, oracle),// Gt -> Ge
                query(binary(ComparisonGe.INSTANCE, a, zero, pp), pp, oracle),
                query(binary(ComparisonLe.INSTANCE, a, zero, pp), pp, oracle),
                query(binary(ComparisonLe.INSTANCE, a, zero, pp), pp, oracle), // Lt -> Le
                query(binary(ComparisonNe.INSTANCE, a, zero, pp), pp, oracle),
                query(binary(ComparisonEq.INSTANCE, a, one, pp), pp, oracle),
                query(binary(ComparisonGt.INSTANCE, a, one, pp), pp, oracle),
                query(binary(ComparisonGe.INSTANCE, a, one, pp), pp, oracle),
                query(binary(ComparisonLt.INSTANCE, a, one, pp), pp, oracle),
                query(binary(ComparisonLe.INSTANCE, a, one, pp), pp, oracle),
                query(binary(ComparisonNe.INSTANCE, a, one, pp), pp, oracle)
        );
    }

    /**
     * Generates a Trend based on the value of {@code a} in the {@code auxiliaryDomain} domain
     * @return {@code INC} if {@code (a < 0 || a > 1)}
     */
    private Trend increasingIfOutsideZeroAndOne(SymbolicExpression a, ProgramPoint pp, SemanticOracle oracle)
            throws SemanticException{
        return increasingIfBetweenZeroAndOne(a, pp, oracle).invert();
    }

    /**
     * Generates a Trend based on the value of {@code a} in the {@code auxiliaryDomain} domain
     * @return {@code NON_DEC} if 0 < a < 1 || 0 <= a < 1
     */
    private Trend nonDecreasingIfBetweenZeroAndOne(SymbolicExpression a, ProgramPoint pp, SemanticOracle oracle)
            throws SemanticException{

        Constant zero = constantInt(0, pp);
        Constant one = constantInt(1, pp);

        return Trend.generateTrendNonDecIfBetween(
                false,
                query(binary(ComparisonGe.INSTANCE, a, zero, pp), pp, oracle),  // Gt -> Ge
                query(binary(ComparisonGe.INSTANCE, a, zero, pp), pp, oracle),
                query(binary(ComparisonLe.INSTANCE, a, zero, pp), pp, oracle),  // Lt -> Le
                query(binary(ComparisonLe.INSTANCE, a, zero, pp), pp, oracle),
                query(binary(ComparisonNe.INSTANCE, a, zero, pp), pp, oracle),

                query(binary(ComparisonEq.INSTANCE, a, one, pp), pp, oracle),
                query(binary(ComparisonGt.INSTANCE, a, one, pp), pp, oracle),
                query(binary(ComparisonGe.INSTANCE, a, one, pp), pp, oracle),
                query(binary(ComparisonLt.INSTANCE, a, one, pp), pp, oracle),
                query(binary(ComparisonLe.INSTANCE, a, one, pp), pp, oracle),
                query(binary(ComparisonNe.INSTANCE, a, one, pp), pp, oracle)
        );

    }

    /**
     * Generates a Trend based on the value of {@code a} in the {@code auxiliaryDomain} domain
     * @return {@code NON_DEC} if {@code (a < 0 || a > 1)}
     */
    private Trend nonDecreasingIfOutsideZeroAndOne(SymbolicExpression a, ProgramPoint pp, SemanticOracle oracle)
            throws SemanticException{
        return nonDecreasingIfBetweenZeroAndOne(a, pp, oracle).invert();
    }



    @Override
    public Stability<V> assign(Identifier id, ValueExpression expression, ProgramPoint pp, SemanticOracle oracle) throws SemanticException {

        if (!trend.knowsIdentifier(id))
            return new Stability<>(
                    auxiliaryDomain.assign(id, expression, pp, oracle),
                    trend.putState(id, Trend.STABLE));

        if (this.isBottom() || auxiliaryDomain.isBottom() || trend.isBottom()) return bottom();

        Trend returnTrend = Trend.TOP;
        //Trend returnTrend = increasingIfLess(id, expression, pp, oracle);

        if ((expression instanceof Constant))
            returnTrend = increasingIfLess(id, expression, pp, oracle);

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

            // x = a / 0
            if (op instanceof DivisionOperator
                    && query(binary(ComparisonEq.INSTANCE, right, constantInt(0, pp), pp), pp, oracle))
                return bottom();

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

        V ad = auxiliaryDomain.assign(id, expression, pp, oracle);
        //ValueEnvironment<Trend> t = trend.putState(id, returnTrend.combine(this.trend.getState(id)));
        ValueEnvironment<Trend> t = trend.putState(id, returnTrend);
        if (ad.isBottom() || t.isBottom()) return bottom();
        else
            return new Stability<>(ad, t);
    }

    @Override
    public Stability<V> smallStepSemantics(ValueExpression expression, ProgramPoint pp, SemanticOracle oracle) throws SemanticException {
        V ad = auxiliaryDomain.smallStepSemantics(expression, pp, oracle);
        if (ad.isBottom()) return bottom();
        else return new Stability<>(ad, trend);
    }

    @Override
    public Stability<V> assume(ValueExpression expression, ProgramPoint src, ProgramPoint dest, SemanticOracle oracle) throws SemanticException {
        V ad = auxiliaryDomain.assume(expression, src, dest, oracle);
        ValueEnvironment<Trend> t = trend.assume(expression, src, dest, oracle);
        if (ad.isBottom() || t.isBottom()) return bottom();
        else return new Stability<>(ad, t);
    }

    @Override
    public boolean knowsIdentifier(Identifier id) {
        return (auxiliaryDomain.knowsIdentifier(id)
                || trend.knowsIdentifier(id));
    }

    @Override
    public Stability<V> forgetIdentifier(Identifier id) throws SemanticException {
        return new Stability<>(
                auxiliaryDomain.forgetIdentifier(id),
                trend.forgetIdentifier(id));
    }

    @Override
    public Stability<V> forgetIdentifiersIf(Predicate<Identifier> test) throws SemanticException {
        return new Stability<>(
                auxiliaryDomain.forgetIdentifiersIf(test),
                trend.forgetIdentifiersIf(test));
    }

    @Override
    public Satisfiability satisfies(ValueExpression expression, ProgramPoint pp, SemanticOracle oracle) throws SemanticException {
        return Satisfiability.UNKNOWN;
        //return auxiliaryDomain.satisfies(expression, pp, oracle).glb(trend.satisfies(expression, pp, oracle));
    }

    @Override
    public StructuredRepresentation representation() {
        return new ListRepresentation(auxiliaryDomain.representation(), trend.representation());
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        Stability<V> other = (Stability<V>) obj;
        return (this.auxiliaryDomain.equals(other.auxiliaryDomain) && this.trend.equals(other.trend));

    }

    @Override
    public int hashCode() {
        return 0;
    }

    @Override
    public String toString() {
        return auxiliaryDomain.representation().toString() + trend.representation().toString();
    }

    public ValueEnvironment<Trend> getTrend() {
        return trend;
    }

    public V getAuxiliaryDomain() {
        return auxiliaryDomain;
    }

}

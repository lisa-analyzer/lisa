package it.unive.lisa.analysis;

import it.unive.lisa.analysis.nonrelational.value.BaseNonRelationalValueDomain;
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

public class Trend implements BaseNonRelationalValueDomain<Trend> {

    /**
     * The abstract top element.
     */
    public static final Trend TOP = new Trend((byte) 0);

    /**
     * The abstract bottom element.
     */
    public static final Trend BOTTOM = new Trend((byte) 1);

    /**
     * The abstract stable element.
     */
    public static final Trend STABLE = new Trend((byte) 2);

    /**
     * The abstract increasing element.
     */
    public static final Trend INC = new Trend((byte) 3);

    /**
     * The abstract decreasing element.
     */
    public static final Trend DEC = new Trend((byte) 4);

    /**
     * The abstract not stable element.
     */
    public static final Trend NON_STABLE = new Trend((byte) 5);

    /**
     * The abstract non-increasing element.
     */
    public static final Trend NON_INC = new Trend((byte) 6);

    /**
     * The abstract non-decreasing element.
     */
    public static final Trend NON_DEC = new Trend((byte) 7);

    private final byte trend;

    public Trend(byte trend){
        this.trend = trend;
    }

    public byte getTrend() {
        return trend;
    }

    @Override
    public Trend lubAux(Trend other) throws SemanticException {

        if (this == other) return this;
        else if (this.lessOrEqual(other)) return other;
        else if (other.lessOrEqual(this)) return this;

        else if ((this == STABLE && other == INC)
                || (this == INC && other == STABLE))
            return NON_DEC;
        else if ((this == STABLE && other == DEC)
                || (this == DEC && other == STABLE))
            return NON_INC;
        else if ((this == INC && other == DEC)
                || (this == DEC && other == INC))
            return NON_STABLE;

        return TOP;
    }

    @Override
    public boolean lessOrEqualAux(Trend other) throws SemanticException {

        if (this == other
                || other == TOP
                || this == BOTTOM
                || (this == STABLE && (other == NON_INC || other == NON_DEC))
                || (this == INC && (other == NON_DEC || other == NON_STABLE))
                || (this == DEC && (other == NON_INC || other == NON_STABLE)))
            return true;

        else return false;
    }

    @Override
    public Trend top() {
        return TOP;
    }

    @Override
    public Trend bottom() {
        return BOTTOM;
    }

    public Trend opposite(){
        if (this == TOP || this == BOTTOM || this == STABLE || this == NON_STABLE) return this;
        else if (this == INC) return DEC;
        else if (this == DEC) return INC;
        else if (this == NON_INC) return NON_DEC;
        else if (this == NON_DEC) return NON_INC;

        else return TOP;
    }

    /**
     * Generates a Trend based on a comparison
     * @return INC if x > a
     */
    public static Trend generateTrendIncIfGt(
            boolean isEqual,
            boolean isGreater,
            boolean isGreaterOrEq,
            boolean isLess,
            boolean isLessOrEq,
            boolean isNotEq) {

        if (isEqual) return STABLE;
        else if (isGreater) return INC;
        else if (isGreaterOrEq) return NON_DEC;
        else if (isLess) return DEC;
        else if (isLessOrEq) return NON_INC;
        else if (isNotEq) return NON_STABLE;

        else return TOP;

    }

    /**
     * Generates a Trend based on a comparison
     * @return NON_DEC if x > a
     */
    public static Trend generateTrendNonDecIfGt(
            boolean isEqual,
            boolean isGreater,
            boolean isGreaterOrEq,
            boolean isLess,
            boolean isLessOrEq,
            boolean isNotEq) {

        if (isEqual) return STABLE;
        else if (isGreater || isGreaterOrEq) return NON_DEC;
        else if (isLess || isLessOrEq) return NON_INC;

        else return TOP;
    }

    /**
     * Generates a Trend based on a comparison
     * @return INC if a < x < b
     */
    public static Trend generateTrendIncIfBetween(
            boolean isEqualA,
            boolean isGreaterA,
            boolean isGreaterOrEqA,
            boolean isLessA,
            boolean isLessOrEqA,
            boolean isNotEqA,
            boolean isEqualB,
            boolean isGreaterB,
            boolean isGreaterOrEqB,
            boolean isLessB,
            boolean isLessOrEqB,
            boolean isNotEqB) {

        if (isEqualA || isEqualB) return STABLE;
        else if (isGreaterA && isLessB) return INC;
        else if ((isGreaterA || isGreaterOrEqA)
                && (isLessB || isLessOrEqB)) return NON_DEC;
        else if (isLessA || isGreaterB) return DEC;
        else if (isLessOrEqA || isGreaterOrEqB) return NON_INC;
        else if (isNotEqA && isNotEqB) return NON_STABLE;

        else return TOP;
    }

    /**
     * Generates a Trend based on a comparison
     * @return INC if a < x < b
     */
    public static Trend generateTrendNonDecIfBetween(
            boolean isEqualA,
            boolean isGreaterA,
            boolean isGreaterOrEqA,
            boolean isLessA,
            boolean isLessOrEqA,
            boolean isNotEqA,
            boolean isEqualB,
            boolean isGreaterB,
            boolean isGreaterOrEqB,
            boolean isLessB,
            boolean isLessOrEqB,
            boolean isNotEqB) {

        if (isEqualA || isEqualB) return STABLE;
        else if ((isGreaterA || isGreaterOrEqA)
                && (isLessB || isLessOrEqB)) return NON_DEC;
        else if (isLessA || isLessOrEqA || isGreaterB || isGreaterOrEqB) return NON_INC;

        else return TOP;
    }

    @Override
    public StructuredRepresentation representation() {
        return null;
    }
}

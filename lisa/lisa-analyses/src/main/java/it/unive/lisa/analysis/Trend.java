package it.unive.lisa.analysis;

import it.unive.lisa.analysis.nonrelational.value.BaseNonRelationalValueDomain;
import it.unive.lisa.util.representation.StringRepresentation;
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
     * The abstract non-decreasing element.
     */
    public static final Trend NON_DEC = new Trend((byte) 5);

    /**
     * The abstract non-increasing element.
     */
    public static final Trend NON_INC = new Trend((byte) 6);

    /**
     * The abstract not stable element.
     */
    public static final Trend NON_STABLE = new Trend((byte) 7);

    private final byte trend;


    public Trend(byte trend){
        this.trend = trend;
    }

    public Trend(){
        this.trend = 0;
    }

    public byte getTrend() {
        return trend;
    }

    public boolean isTop(){
        return this.trend == (byte) 0;
    }

    public boolean isBottom(){
        return this.trend == (byte) 1;
    }

    public boolean isStable(){
        return this.trend == (byte) 2;
    }

    public boolean isInc(){
        return this.trend == (byte) 3;
    }

    public boolean isDec(){
        return this.trend == (byte) 4;
    }

    public boolean isNonDec(){
        return this.trend == (byte) 5;
    }

    public boolean isNonInc(){
        return this.trend == (byte) 6;
    }

    public boolean isNonStable(){
        return this.trend == (byte) 7;
    }

    @Override
    public Trend lubAux(Trend other) throws SemanticException {

        if (this.lessOrEqual(other)) return other;
        else if (other.lessOrEqual(this)) return this;

        else if ((this.isStable() && other.isInc())
                || (this.isInc() && other.isStable()))
            return NON_DEC;
        else if ((this.isStable() && other.isDec())
                || (this.isDec() && other.isStable()))
            return NON_INC;
        else if ((this.isInc() && other.isDec())
                || (this.isDec() && other.isInc()))
            return NON_STABLE;

        return TOP;
    }

    @Override
    public Trend glbAux(Trend other) throws SemanticException {

        if (this.lessOrEqual(other)) return this;
        else if (other.lessOrEqual(this)) return other;

        else if ((this.isNonDec() && other.isNonStable())
                || (this.isNonStable() && other.isNonDec()))
            return INC;
        else if ((this.isNonInc() && other.isNonStable())
                || (this.isNonStable() && other.isNonInc()))
            return DEC;
        else if ((this.isNonDec() && other.isNonInc())
                || (this.isNonInc() && other.isNonDec()))
            return STABLE;

        return BOTTOM;
    }

    @Override
    public boolean lessOrEqualAux(Trend other) throws SemanticException {
        return (this.isStable() && (other.isNonInc() || other.isNonDec()))
                || (this.isInc() && (other.isNonDec() || other.isNonStable()))
                || (this.isDec() && (other.isNonInc() || other.isNonStable()));
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
        if (this.isTop() || this.isBottom() || this.isStable() || this.isNonStable()) return this;
        else if (this.isInc()) return DEC;
        else if (this.isDec()) return INC;
        else if (this.isNonInc()) return NON_DEC;
        else if (this.isNonDec()) return NON_INC;

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
     * @return NON_DEC if a < x < b
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
        if (isBottom())
            return Lattice.bottomRepresentation();
        if (isTop())
            return Lattice.topRepresentation();

        String repr;
        if (this.isStable())
            repr = "stable";
        else if (this.isInc())
            repr = "increasing";
        else if (this.isDec())
            repr = "decreasing";
        else if (this.isNonDec())
            repr = "non-decreasing";
        else if (this.isNonInc())
            repr = "non-increasing";
        else
            repr = "not stable";

        return new StringRepresentation(repr);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        Trend other = (Trend) obj;
        return this.getTrend() == other.getTrend();
    }
}

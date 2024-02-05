package it.unive.lisa.analysis;

import it.unive.lisa.analysis.nonrelational.value.BaseNonRelationalValueDomain;
import it.unive.lisa.analysis.numeric.Sign;
import it.unive.lisa.util.representation.StringRepresentation;
import it.unive.lisa.util.representation.StructuredRepresentation;

public class Stability implements BaseNonRelationalValueDomain<Stability> {

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

    public Stability(byte stability){
        this.stability = stability;
    }

    @Override
    public Stability lubAux(Stability other) throws SemanticException {
        return TOP;
    }

    @Override
    public boolean lessOrEqualAux(Stability other) throws SemanticException {
        return false;
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
    public StructuredRepresentation representation() {
        if (isBottom())
            return Lattice.bottomRepresentation();
        if (isTop())
            return Lattice.topRepresentation();

        String repr;
        if (this == INC)
            repr = "increasing";
        else if (this == DEC)
            repr = "decreasing";
        else if (this == STABLE)
            repr = "stable";
        else if (this == NON_INC)
            repr = "non-increasing";
        else if (this == NON_DEC)
            repr = "non-decreasing";
        else
            repr = "not stable";

        return new StringRepresentation(repr);
    }
}

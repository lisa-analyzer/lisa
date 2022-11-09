package it.unive.lisa.analysis.string;

import it.unive.lisa.analysis.Lattice;
import it.unive.lisa.analysis.SemanticDomain.Satisfiability;
import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.analysis.nonrelational.value.BaseNonRelationalValueDomain;
import it.unive.lisa.analysis.representation.DomainRepresentation;
import it.unive.lisa.analysis.representation.StringRepresentation;
import it.unive.lisa.program.cfg.ProgramPoint;
import it.unive.lisa.symbolic.value.Constant;
import it.unive.lisa.symbolic.value.operator.binary.*;
import it.unive.lisa.symbolic.value.operator.ternary.TernaryOperator;
import it.unive.lisa.symbolic.value.operator.unary.UnaryOperator;

import java.util.Objects;

public class Suffix extends BaseNonRelationalValueDomain<Suffix> {

    private final static Suffix TOP = new Suffix();
    private final static Suffix BOTTOM = new Suffix(null);
    private final String suffix;

    public Suffix() {
        this("");
    }

    public Suffix(String suffix) {
        this.suffix = suffix;
    }

    @Override
    public Suffix lubAux(Suffix other) throws SemanticException {
        String otherSuffix = other.getSuffix();
        StringBuilder result = new StringBuilder();

        int i = suffix.length() - 1;
        int j = otherSuffix.length() - 1;

        while (i >= 0 && j >= 0 &&
                suffix.charAt(i) == otherSuffix.charAt(j)) {
            result.append(suffix.charAt(i--));
            j--;
        }

        if (result.length() != 0)
            return new Suffix(result.reverse().toString());

        else
            return TOP;
    }

    @Override
    public Suffix wideningAux(Suffix other) throws SemanticException {
        return lubAux(other);
    }

    @Override
    public boolean lessOrEqualAux(Suffix other) throws SemanticException {
        if (other.getSuffix().length() <= this.getSuffix().length()) {
            Suffix lub = this.lubAux(other);
            String lubString = lub.getSuffix();

            return lubString.length() == other.getSuffix().length();
        }

        return false;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Suffix suffix1 = (Suffix) o;
        return Objects.equals(suffix, suffix1.suffix);
    }

    @Override
    public int hashCode() {
        return Objects.hash(suffix);
    }

    @Override
    public Suffix top() {
        return TOP;
    }

    @Override
    public Suffix bottom() {
        return BOTTOM;
    }

    @Override
    public boolean isTop() {
        return this.equals(TOP);
    }

    @Override
    public boolean isBottom() {
        return this.equals(BOTTOM);
    }

    @Override
    public DomainRepresentation representation() {
        if (isBottom())
            return Lattice.bottomRepresentation();
        if (isTop())
            return Lattice.topRepresentation();

        return new StringRepresentation(suffix);
    }

    @Override
    public Suffix evalNullConstant(ProgramPoint pp) {
        return TOP;
    }

    @Override
    public Suffix evalNonNullConstant(Constant constant, ProgramPoint pp) {
        if (constant.getValue() instanceof String)
            return new Suffix((String) constant.getValue());

        return TOP;
    }

    @Override
    public Suffix evalUnaryExpression(UnaryOperator operator, Suffix arg, ProgramPoint pp) {
        return TOP;
    }

    @Override
    public Suffix evalBinaryExpression(BinaryOperator operator, Suffix left, Suffix right, ProgramPoint pp) {
        if (operator == StringConcat.INSTANCE) {
            return right;
        } else if (operator == StringContains.INSTANCE ||
                operator == StringEndsWith.INSTANCE ||
                operator == StringEquals.INSTANCE ||
                operator == StringIndexOf.INSTANCE ||
                operator == StringStartsWith.INSTANCE) {
            return TOP;
        }

        return TOP;
    }

    @Override
    public Suffix evalTernaryExpression(TernaryOperator operator, Suffix left, Suffix middle, Suffix right, ProgramPoint pp) {
        return TOP;
    }

    @Override
    public Satisfiability satisfiesBinaryExpression(BinaryOperator operator, Suffix left, Suffix right, ProgramPoint pp) {
        if(left.isTop() || right.isTop())
            return Satisfiability.UNKNOWN;

        if(operator == StringEndsWith.INSTANCE)
            return Satisfiability.SATISFIED;

        return Satisfiability.UNKNOWN;
    }

    @Override
    public Satisfiability satisfiesTernaryExpression(TernaryOperator operator, Suffix left, Suffix middle, Suffix right, ProgramPoint pp) {
        return Satisfiability.UNKNOWN;
    }

    protected String getSuffix() {
        return this.suffix;
    }
}
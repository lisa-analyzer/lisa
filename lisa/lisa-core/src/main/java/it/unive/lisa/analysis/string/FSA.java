package it.unive.lisa.analysis.string;

import it.unive.lisa.analysis.SemanticDomain;
import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.analysis.nonrelational.value.BaseNonRelationalValueDomain;
import it.unive.lisa.analysis.representation.DomainRepresentation;
import it.unive.lisa.program.cfg.ProgramPoint;
import it.unive.lisa.symbolic.value.Constant;
import it.unive.lisa.symbolic.value.operator.binary.BinaryOperator;
import it.unive.lisa.symbolic.value.operator.binary.StringConcat;
import it.unive.lisa.symbolic.value.operator.ternary.TernaryOperator;
import it.unive.lisa.symbolic.value.operator.unary.UnaryOperator;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

public class FSA extends BaseNonRelationalValueDomain<FSA> {
    private final Automaton a;
    private static final FSA TOP = new FSA();
    public static final int WIDENING_TH = 3;

    public FSA() {
        // top
        this.a = new Automaton(null, null);
    }

    private FSA(Automaton a) {
        this.a = a;
    }

    @Override
    protected FSA lubAux(FSA other) throws SemanticException {
        return null;
    }

    @Override
    protected FSA wideningAux(FSA other) throws SemanticException {
        return new FSA(this.a.union(other.a).widening(WIDENING_TH));
    }

    @Override
    protected boolean lessOrEqualAux(FSA other) throws SemanticException {
        return this.a.isContained(other.a);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        FSA fsa = (FSA) o;
        return Objects.equals(a, fsa.a);
    }

    @Override
    public int hashCode() {
        return Objects.hash(a);
    }

    @Override
    public FSA top() {
        return TOP;
    }

    @Override
    public boolean isBottom() {
        return this.a.acceptsEmptyLanguage();
    }

    @Override
    public FSA bottom() {
        Set<State> states = new HashSet<>();
        states.add(new State(true, false));
        return new FSA(new Automaton(states, null));
    }

    @Override
    public DomainRepresentation representation() {
        return null;
    }

    @Override
    protected FSA evalNonNullConstant(Constant constant, ProgramPoint pp) throws SemanticException {
        // TODO: costruttore automaton con stringa
        if(constant.getValue() instanceof String) {
            return new FSA(new Automaton((String) constant.getValue()));
        }
        return top();
    }

    @Override
    protected FSA evalUnaryExpression(UnaryOperator operator, FSA arg, ProgramPoint pp) throws SemanticException {
        // TODO
        return super.evalUnaryExpression(operator, arg, pp);
    }

    @Override
    protected FSA evalBinaryExpression(BinaryOperator operator, FSA left, FSA right, ProgramPoint pp) throws SemanticException {
        if(operator == StringConcat.INSTANCE)
            return new FSA(left.a.concat(right.a));
        return top();
    }

    @Override
    protected FSA evalTernaryExpression(TernaryOperator operator, FSA left, FSA middle, FSA right, ProgramPoint pp) throws SemanticException {
        // TODO
        return super.evalTernaryExpression(operator, left, middle, right, pp);
    }

    @Override
    protected SemanticDomain.Satisfiability satisfiesBinaryExpression(BinaryOperator operator, FSA left, FSA right, ProgramPoint pp) throws SemanticException {
        // TODO
        return super.satisfiesBinaryExpression(operator, left, right, pp);
    }
}

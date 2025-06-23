package it.unive.lisa.analysis.combination.constraints;

import java.util.Set;

import it.unive.lisa.analysis.Lattice;
import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.analysis.SemanticOracle;
import it.unive.lisa.analysis.lattices.Satisfiability;
import it.unive.lisa.analysis.nonrelational.value.BaseNonRelationalValueDomain;
import it.unive.lisa.analysis.nonrelational.value.ValueEnvironment;
import it.unive.lisa.program.cfg.ProgramPoint;
import it.unive.lisa.symbolic.value.BinaryExpression;
import it.unive.lisa.symbolic.value.Constant;
import it.unive.lisa.symbolic.value.TernaryExpression;
import it.unive.lisa.symbolic.value.UnaryExpression;
import it.unive.lisa.symbolic.value.ValueExpression;
import it.unive.lisa.symbolic.value.operator.binary.BinaryOperator;
import it.unive.lisa.symbolic.value.operator.binary.ComparisonEq;
import it.unive.lisa.symbolic.value.operator.binary.ComparisonNe;
import it.unive.lisa.symbolic.value.operator.binary.LogicalOperation;
import it.unive.lisa.symbolic.value.operator.binary.NumericComparison;
import it.unive.lisa.symbolic.value.operator.binary.NumericOperation;
import it.unive.lisa.symbolic.value.operator.binary.StringConcat;
import it.unive.lisa.symbolic.value.operator.binary.StringIndexOf;
import it.unive.lisa.symbolic.value.operator.binary.StringOperation;
import it.unive.lisa.symbolic.value.operator.ternary.StringReplace;
import it.unive.lisa.symbolic.value.operator.ternary.StringSubstring;
import it.unive.lisa.symbolic.value.operator.ternary.TernaryOperator;
import it.unive.lisa.symbolic.value.operator.unary.LogicalNegation;
import it.unive.lisa.symbolic.value.operator.unary.NumericNegation;
import it.unive.lisa.symbolic.value.operator.unary.StringLength;
import it.unive.lisa.symbolic.value.operator.unary.UnaryOperator;
import it.unive.lisa.util.representation.StringRepresentation;
import it.unive.lisa.util.representation.StructuredRepresentation;

public class WholeValueAnalysis<
    N extends WholeValueDomain<N>,
    S extends WholeValueStringDomain<S>,
    B extends WholeValueDomain<B>> 
    implements
    BaseNonRelationalValueDomain<WholeValueAnalysis<N, S, B>> {

    private final N intValue;
	private final S stringValue;
	private final B boolValue;

	/**
	 * Builds an abstract element of this domain.
	 * 
	 * @param intValue    the abstract value for intergers
	 * @param stringValue the abstract value for strings
	 * @param boolValue   the abstract value for booleans
	 */
	public WholeValueAnalysis(
			N intValue,
			S stringValue,
			B boolValue) {
		this.intValue = intValue;
		this.stringValue = stringValue;
		this.boolValue = boolValue;
	}


    @Override
    public WholeValueAnalysis<N, S, B> lubAux(WholeValueAnalysis<N, S, B> other) throws SemanticException {
        return new WholeValueAnalysis<>(
                this.intValue.lub(other.intValue),
                this.stringValue.lub(other.stringValue),
                this.boolValue.lub(other.boolValue));
    }

    @Override
    public boolean lessOrEqualAux(WholeValueAnalysis<N, S, B> other) throws SemanticException {
        return this.intValue.lessOrEqual(other.intValue) &&
               this.stringValue.lessOrEqual(other.stringValue) &&
               this.boolValue.lessOrEqual(other.boolValue);
    }

    @Override
    public WholeValueAnalysis<N, S, B> wideningAux(WholeValueAnalysis<N, S, B> other) throws SemanticException {
        return new WholeValueAnalysis<>(
                this.intValue.widening(other.intValue),
                this.stringValue.widening(other.stringValue),
                this.boolValue.widening(other.boolValue));
    }

	@Override
	public boolean isTop() {
		return intValue.isTop() && stringValue.isTop() && boolValue.isTop();
	}

	@Override
	public WholeValueAnalysis<N, S, B> top() {
		return new WholeValueAnalysis<>(intValue.top(), stringValue.top(), boolValue.top());
	}

	@Override
	public boolean isBottom() {
		return intValue.isBottom() && stringValue.isBottom() && boolValue.isBottom();
	}

	@Override
	public WholeValueAnalysis<N, S, B> bottom() {
		return new WholeValueAnalysis<>(intValue.bottom(), stringValue.bottom(), boolValue.bottom());
	}

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((intValue == null) ? 0 : intValue.hashCode());
        result = prime * result + ((stringValue == null) ? 0 : stringValue.hashCode());
        result = prime * result + ((boolValue == null) ? 0 : boolValue.hashCode());
        return result;
    }


    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        WholeValueAnalysis<?, ?, ?> other = (WholeValueAnalysis<?, ?, ?>) obj;
        if (intValue == null) {
            if (other.intValue != null)
                return false;
        } else if (!intValue.equals(other.intValue))
            return false;
        if (stringValue == null) {
            if (other.stringValue != null)
                return false;
        } else if (!stringValue.equals(other.stringValue))
            return false;
        if (boolValue == null) {
            if (other.boolValue != null)
                return false;
        } else if (!boolValue.equals(other.boolValue))
            return false;
        return true;
    }

    @Override
	public StructuredRepresentation representation() {
		if (isBottom())
			return Lattice.bottomRepresentation();
		if (isTop())
			return Lattice.topRepresentation();
		if (isString())
			return stringValue.representation();
		if (isNumber())
			return intValue.representation();
		if (isBool())
			return boolValue.representation();
		return new StringRepresentation(
				"(" + intValue.representation().toString() + ", " 
                + stringValue.representation().toString() + ", "
                + boolValue.representation().toString() + ")");
	}

    @Override
    public String toString() {
        return representation().toString();
    }

	private boolean sameKind(
			WholeValueAnalysis<N, S, B> other) {
		return (intValue.isBottom() == other.intValue.isBottom())
				&& (stringValue.isBottom() == other.stringValue.isBottom())
				&& (boolValue.isBottom() == other.boolValue.isBottom());
	}

	private boolean isNumber() {
		return isTop() || (!isBottom() && stringValue.isBottom() && boolValue.isBottom());
	}

	private boolean isString() {
		return isTop() || (!isBottom() && intValue.isBottom() && boolValue.isBottom());
	}

	private boolean isBool() {
		return isTop() || (!isBottom() && stringValue.isBottom() && intValue.isBottom());
	}

	private WholeValueAnalysis<N, S, B> mkStringValue(
			S stringValue) {
		return new WholeValueAnalysis<>(intValue.bottom(), stringValue, boolValue.bottom());
	}

	private WholeValueAnalysis<N, S, B> mkIntValue(
			N intValue) {
		return new WholeValueAnalysis<>(intValue, stringValue.bottom(), boolValue.bottom());
	}

	private WholeValueAnalysis<N, S, B> mkBoolValue(
			B boolValue) {
		return new WholeValueAnalysis<>(intValue.bottom(), stringValue.bottom(), boolValue);
	}

	@Override
	public WholeValueAnalysis<N, S, B> evalNonNullConstant(
			Constant constant,
			ProgramPoint pp,
			SemanticOracle oracle)
			throws SemanticException {
		if (constant.getValue() instanceof Integer)
			return mkIntValue(intValue.evalNonNullConstant(constant, pp, oracle));
		else if (constant.getValue() instanceof String)
			return mkStringValue(stringValue.evalNonNullConstant(constant, pp, oracle));
		else if (constant.getValue() instanceof Boolean)
			return mkBoolValue(boolValue.evalNonNullConstant(constant, pp, oracle));
		return top();
	}

    @Override
	public WholeValueAnalysis<N, S, B> evalUnaryExpression(
			UnaryExpression expression,
			WholeValueAnalysis<N, S, B> arg,
			ProgramPoint pp,
			SemanticOracle oracle)
			throws SemanticException {
        UnaryOperator operator = expression.getOperator();
		if (operator == StringLength.INSTANCE && arg.isString())
			return mkIntValue(intValue.generate(arg.stringValue.constraints((ValueExpression) expression.getExpression(), pp), pp));
		if (operator == NumericNegation.INSTANCE && arg.isNumber())
			return mkIntValue(intValue.evalUnaryExpression(expression, arg.intValue, pp, oracle));
		if (operator == LogicalNegation.INSTANCE && arg.isBool())
			return mkBoolValue(boolValue.evalUnaryExpression(expression, arg.boolValue, pp, oracle));
		return top();
	}

	@Override
	public WholeValueAnalysis<N, S, B> evalBinaryExpression(
			BinaryExpression expression,
			WholeValueAnalysis<N, S, B> left,
			WholeValueAnalysis<N, S, B> right,
			ProgramPoint pp,
			SemanticOracle oracle)
			throws SemanticException {
        BinaryOperator operator = expression.getOperator();
		if (operator instanceof NumericOperation && left.isNumber() && right.isNumber())
			return mkIntValue(
					intValue.evalBinaryExpression(expression, left.intValue, right.intValue, pp, oracle));
		if (operator instanceof LogicalOperation && left.isBool() && right.isBool())
			return mkBoolValue(
					boolValue.evalBinaryExpression(expression, left.boolValue, right.boolValue, pp, oracle));
		if (operator instanceof NumericComparison && left.isNumber() && right.isNumber())
			return mkBoolValue(boolValue.generate(intValue.satisfiesBinaryExpression(expression, left.intValue, right.intValue, pp, oracle).constraints(expression, pp), pp));
		if (operator == ComparisonEq.INSTANCE || operator == ComparisonNe.INSTANCE)
			return mkBoolValue(boolValue.generate(satisfiesBinaryExpression(expression, left, right, pp, oracle).constraints(expression, pp), pp));
		if (operator == StringIndexOf.INSTANCE && left.isString() && right.isString())
			return mkIntValue(intValue.generate(left.stringValue.indexOf_constr(expression, right.stringValue, pp), pp));
		if (operator == StringConcat.INSTANCE && left.isString() && right.isString())
			return mkStringValue(
					stringValue.evalBinaryExpression(expression, left.stringValue, right.stringValue, pp, oracle));
		if (operator instanceof StringOperation && left.isString() && right.isString())
			return mkBoolValue(boolValue.generate(stringValue.satisfiesBinaryExpression(expression, left.stringValue, right.stringValue, pp, oracle).constraints(expression, pp), pp));
		return top();
	}

	@Override
	public WholeValueAnalysis<N, S, B> evalTernaryExpression(
			TernaryExpression expression,
			WholeValueAnalysis<N, S, B> left,
			WholeValueAnalysis<N, S, B> middle,
			WholeValueAnalysis<N, S, B> right,
			ProgramPoint pp,
			SemanticOracle oracle)
			throws SemanticException {
        TernaryOperator operator = expression.getOperator();
		if (operator == StringSubstring.INSTANCE && left.isString() && middle.isNumber() && right.isNumber()) {
			Set<BinaryExpression> begin = middle.intValue.constraints(null, pp);
			Set<BinaryExpression> end = right.intValue.constraints(null, pp);
			return mkStringValue(left.stringValue.substring(begin, end, pp));
		} else if (operator == StringReplace.INSTANCE && left.isString() && middle.isString() && right.isString())
			return mkStringValue(stringValue.evalTernaryExpression(expression, left.stringValue, middle.stringValue,
					right.stringValue, pp, oracle));

		return top();
	}

	@Override
	public Satisfiability satisfiesBinaryExpression(
			BinaryExpression expression,
			WholeValueAnalysis<N, S, B> left,
			WholeValueAnalysis<N, S, B> right,
			ProgramPoint pp,
			SemanticOracle oracle)
			throws SemanticException {
        BinaryOperator operator = expression.getOperator();
		if (operator instanceof StringOperation && left.isString() && right.isString())
			return stringValue.satisfiesBinaryExpression(expression, left.stringValue, right.stringValue, pp, oracle);
		if (operator instanceof NumericComparison && left.isNumber() && right.isNumber())
			return intValue.satisfiesBinaryExpression(expression, left.intValue, right.intValue, pp, oracle);
		if (operator instanceof LogicalOperation && left.isBool() && right.isBool())
			return boolValue.satisfiesBinaryExpression(expression, left.boolValue, right.boolValue, pp, oracle);
		if (operator == ComparisonEq.INSTANCE) {
			if (!left.sameKind(right))
				return Satisfiability.NOT_SATISFIED;
			if (left.isString())
				return stringValue.satisfiesBinaryExpression(expression, left.stringValue, right.stringValue, pp, oracle);
			if (left.isNumber())
				return intValue.satisfiesBinaryExpression(expression, left.intValue, right.intValue, pp, oracle);
			if (left.isBool())
				return boolValue.satisfiesBinaryExpression(expression, left.boolValue, right.boolValue, pp, oracle);
		}
		if (operator == ComparisonNe.INSTANCE) {
			if (!left.sameKind(right))
				return Satisfiability.SATISFIED;
			if (left.isString())
				return stringValue.satisfiesBinaryExpression(expression, left.stringValue, right.stringValue, pp, oracle);
			if (left.isNumber())
				return intValue.satisfiesBinaryExpression(expression, left.intValue, right.intValue, pp, oracle);
			if (left.isBool())
				return boolValue.satisfiesBinaryExpression(expression, left.boolValue, right.boolValue, pp, oracle);
		}
		return Satisfiability.UNKNOWN;
	}

	@Override
	public ValueEnvironment<WholeValueAnalysis<N, S, B>> assume(
			ValueEnvironment<WholeValueAnalysis<N, S, B>> environment,
			ValueExpression expression,
			ProgramPoint src,
			ProgramPoint dest,
			SemanticOracle oracle)
			throws SemanticException {
		Satisfiability sat = satisfies(expression, environment, src, oracle);
		if (sat == Satisfiability.NOT_SATISFIED)
			return environment.bottom();
		if (sat == Satisfiability.SATISFIED)
			return environment;
		return BaseNonRelationalValueDomain.super.assume(environment, expression, src, dest, oracle);
	}
}

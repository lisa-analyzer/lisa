package it.unive.lisa.analysis.impl.numeric;

import it.unive.lisa.analysis.BaseLattice;
import it.unive.lisa.analysis.SemanticDomain.Satisfiability;
import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.analysis.nonrelational.BaseNonRelationalValueDomain;
import it.unive.lisa.cfg.type.Type;
import it.unive.lisa.symbolic.value.BinaryOperator;
import it.unive.lisa.symbolic.value.Constant;
import it.unive.lisa.symbolic.value.Identifier;
import it.unive.lisa.symbolic.value.TernaryOperator;
import it.unive.lisa.symbolic.value.UnaryOperator;

/**
 * The Parity abstract domain, tracking if a numeric value is even or odd,
 * implemented as a {@link BaseNonRelationalValueDomain}, handling top and
 * bottom values for the expression evaluation and bottom values for the
 * expression satisfiability. Top and bottom cases for least upper bound,
 * widening and less or equals operations are handled by {@link BaseLattice} in
 * {@link BaseLattice#lub}, {@link BaseLattice#widening} and
 * {@link BaseLattice#lessOrEqual} methods, respectively.
 * 
 * @author <a href="mailto:vincenzo.arceri@unive.it">Vincenzo Arceri</a>
 */
public class Parity extends BaseNonRelationalValueDomain<Parity> {

	private static final Parity EVEN = new Parity();
	private static final Parity ODD = new Parity();
	private static final Parity TOP = new Parity();
	private static final Parity BOTTOM = new Parity();

	@Override
	public Parity top() {
		return TOP;
	}

	@Override
	public Parity bottom() {
		return BOTTOM;
	}

	@Override
	public String representation() {
		if (equals(BOTTOM))
			return "BOTTOM";
		else if (equals(TOP))
			return "TOP";
		else if (equals(ODD))
			return "Odd";
		else
			return "Even";
	}

	@Override
	protected Parity evalNullConstant() {
		return top();
	}

	@Override
	protected Parity evalNonNullConstant(Constant constant) {
		if (constant.getValue() instanceof Long) {
			Long i = (Long) constant.getValue();
			return i % 2 == 0 ? EVEN : ODD;
		}

		return top();
	}

	private boolean isEven() {
		return this == EVEN;
	}

	private boolean isOdd() {
		return this == ODD;
	}

	@Override
	protected Parity evalTypeConversion(Type type, Parity arg) {
		return top();
	}

	@Override
	protected Parity evalUnaryExpression(UnaryOperator operator, Parity arg) {
		switch (operator) {
		case NUMERIC_NEG:
			return arg;
		default:
			return top();
		}
	}

	@Override
	protected Parity evalBinaryExpression(BinaryOperator operator, Parity left, Parity right) {

		switch (operator) {
		case NUMERIC_ADD:
		case NUMERIC_SUB:
			if (right.equals(left))
				return EVEN;
			else
				return ODD;
		case NUMERIC_MUL:
			if (left.isEven() || right.isEven())
				return EVEN;
			else
				return ODD;
		case NUMERIC_DIV:
			if (left.isOdd())
				return right.isOdd() ? ODD : EVEN;
			else
				return right.isOdd() ? EVEN : TOP;
		case NUMERIC_MOD:
			return top();
		default:
			return top();
		}
	}

	@Override
	protected Parity evalTernaryExpression(TernaryOperator operator, Parity left, Parity middle, Parity right) {
		return top();
	}

	@Override
	protected Parity lubAux(Parity other) throws SemanticException {
		return BOTTOM;
	}

	@Override
	protected Parity wideningAux(Parity other) throws SemanticException {
		return lubAux(other);
	}

	@Override
	protected boolean lessOrEqualAux(Parity other) throws SemanticException {
		return false;
	}

	@Override
	protected Satisfiability satisfiesIdentifier(Identifier identifier) {
		return Satisfiability.UNKNOWN;
	}

	@Override
	protected Satisfiability satisfiesNullConstant() {
		return Satisfiability.UNKNOWN;
	}

	@Override
	protected Satisfiability satisfiesNonNullConstant(Constant constant) {
		return Satisfiability.UNKNOWN;
	}

	@Override
	protected Satisfiability satisfiesTypeConversion(Type type, Parity right) {
		return Satisfiability.UNKNOWN;
	}

	@Override
	protected Satisfiability satisfiesUnaryExpression(UnaryOperator operator, Parity arg) {
		return Satisfiability.UNKNOWN;
	}

	@Override
	protected Satisfiability satisfiesBinaryExpression(BinaryOperator operator, Parity left, Parity right) {
		return Satisfiability.UNKNOWN;
	}

	@Override
	protected Satisfiability satisfiesTernaryExpression(TernaryOperator operator, Parity left, Parity middle,
			Parity right) {
		return Satisfiability.UNKNOWN;
	}

	@Override
	public int hashCode() {
		if (this == TOP)
			return 1;
		else if (this == BOTTOM)
			return 2;
		else if (this == ODD)
			return 3;
		else
			return 4;
	}

	@Override
	public boolean equals(Object obj) {
		return this == obj;
	}
}
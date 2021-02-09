package it.unive.lisa.analysis.impl.numeric;

import it.unive.lisa.analysis.BaseLattice;
import it.unive.lisa.analysis.Lattice;
import it.unive.lisa.analysis.SemanticDomain.Satisfiability;
import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.analysis.nonrelational.value.BaseNonRelationalValueDomain;
import it.unive.lisa.program.cfg.ProgramPoint;
import it.unive.lisa.symbolic.value.BinaryOperator;
import it.unive.lisa.symbolic.value.Constant;
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

	private static final Parity EVEN = new Parity(false, false);
	private static final Parity ODD = new Parity(false, false);
	private static final Parity TOP = new Parity();
	private static final Parity BOTTOM = new Parity(false, true);

	private final boolean isTop, isBottom;

	/**
	 * Builds the parity abstract domain, representing the top of the parity
	 * abstract domain.
	 */
	public Parity() {
		this(true, false);
	}

	private Parity(boolean isTop, boolean isBottom) {
		this.isTop = isTop;
		this.isBottom = isBottom;
	}

	@Override
	public Parity top() {
		return TOP;
	}

	@Override
	public boolean isTop() {
		return isTop;
	}

	@Override
	public Parity bottom() {
		return BOTTOM;
	}

	@Override
	public String representation() {
		if (equals(BOTTOM))
			return Lattice.BOTTOM_STRING;
		else if (equals(EVEN))
			return "Even";
		else if (equals(ODD))
			return "Odd";
		else
			return Lattice.TOP_STRING;
	}

	@Override
	protected Parity evalNullConstant(ProgramPoint pp) {
		return top();
	}

	@Override
	protected Parity evalNonNullConstant(Constant constant, ProgramPoint pp) {
		if (constant.getValue() instanceof Integer) {
			Integer i = (Integer) constant.getValue();
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
	protected Parity evalUnaryExpression(UnaryOperator operator, Parity arg, ProgramPoint pp) {
		switch (operator) {
		case NUMERIC_NEG:
			return arg;
		default:
			return top();
		}
	}

	@Override
	protected Parity evalBinaryExpression(BinaryOperator operator, Parity left, Parity right, ProgramPoint pp) {
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
			return TOP;
		default:
			return TOP;
		}
	}

	@Override
	protected Parity evalTernaryExpression(TernaryOperator operator, Parity left, Parity middle, Parity right,
			ProgramPoint pp) {
		return TOP;
	}

	@Override
	protected Parity lubAux(Parity other) throws SemanticException {
		return TOP;
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
	protected Satisfiability satisfiesAbstractValue(Parity value, ProgramPoint pp) {
		return Satisfiability.UNKNOWN;
	}

	@Override
	protected Satisfiability satisfiesNullConstant(ProgramPoint pp) {
		return Satisfiability.UNKNOWN;
	}

	@Override
	protected Satisfiability satisfiesNonNullConstant(Constant constant, ProgramPoint pp) {
		return Satisfiability.UNKNOWN;
	}

	@Override
	protected Satisfiability satisfiesUnaryExpression(UnaryOperator operator, Parity arg, ProgramPoint pp) {
		return Satisfiability.UNKNOWN;
	}

	@Override
	protected Satisfiability satisfiesBinaryExpression(BinaryOperator operator, Parity left, Parity right,
			ProgramPoint pp) {
		return Satisfiability.UNKNOWN;
	}

	@Override
	protected Satisfiability satisfiesTernaryExpression(TernaryOperator operator, Parity left, Parity middle,
			Parity right, ProgramPoint pp) {
		return Satisfiability.UNKNOWN;
	}

	@Override
	public int hashCode() {
		if (isBottom())
			return 1;
		else if (this == EVEN)
			return 2;
		else if (this == ODD)
			return 3;
		else
			return 4;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		Parity other = (Parity) obj;
		if (isBottom != other.isBottom)
			return false;
		if (isTop != other.isTop)
			return false;
		return isTop && other.isTop;
	}
}
package it.unive.lisa.analysis.stability;

import it.unive.lisa.analysis.Lattice;
import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.analysis.SemanticOracle;
import it.unive.lisa.analysis.lattices.Satisfiability;
import it.unive.lisa.analysis.nonrelational.value.BaseNonRelationalValueDomain;
import it.unive.lisa.program.cfg.ProgramPoint;
import it.unive.lisa.symbolic.SymbolicExpression;
import it.unive.lisa.symbolic.value.PushInv;
import it.unive.lisa.type.Type;
import it.unive.lisa.util.representation.StringRepresentation;
import it.unive.lisa.util.representation.StructuredRepresentation;
import java.util.Objects;
import java.util.Set;

/**
 * A single-variable numerical trend. Instances of this class (corresponding to
 * its public static fields) abstract the trend of the value of a variable after
 * the execution of an instruction w.r.t. its value before the execution.
 * 
 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
 */
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

	/**
	 * Builds the top trend.
	 */
	public Trend() {
		this((byte) 0);
	}

	private Trend(
			byte trend) {
		this.trend = trend;
	}

	/**
	 * Yields {@code true} if this Trend instance represent a stable variation,
	 * that is, if the current value is the same as the previous one.
	 * 
	 * @return {@code true} if this is the stable trend
	 */
	public boolean isStable() {
		return this.trend == STABLE.trend;
	}

	/**
	 * Yields {@code true} if this Trend instance represent an increasing
	 * variation, that is, if the current value is greater than the previous
	 * one.
	 * 
	 * @return {@code true} if this is the increasing trend
	 */
	public boolean isInc() {
		return this.trend == INC.trend;
	}

	/**
	 * Yields {@code true} if this Trend instance represent a decreasing
	 * variation, that is, if the current value is less than the previous one.
	 * 
	 * @return {@code true} if this is the decreasing trend
	 */
	public boolean isDec() {
		return this.trend == DEC.trend;
	}

	/**
	 * Yields {@code true} if this Trend instance represent a non-decreasing
	 * variation, that is, if the current value is greater or equal than the
	 * previous one.
	 * 
	 * @return {@code true} if this is the non-decreasing trend
	 */
	public boolean isNonDec() {
		return this.trend == NON_DEC.trend;
	}

	/**
	 * Yields {@code true} if this Trend instance represent a non-increasing
	 * variation, that is, if the current value is less or equal than the
	 * previous one.
	 * 
	 * @return {@code true} if this is the non-increasing trend
	 */
	public boolean isNonInc() {
		return this.trend == NON_INC.trend;
	}

	/**
	 * Yields {@code true} if this Trend instance represent an unstable
	 * variation, that is, if the current value is different from the previous
	 * one.
	 * 
	 * @return {@code true} if this is the unstable trend
	 */
	public boolean isNonStable() {
		return this.trend == NON_STABLE.trend;
	}

	/**
	 * Returns the direction of the trend as a {@link Satisfiability} instance.
	 * Specifically:
	 * <ul>
	 * <li>{@link Satisfiability#SATISFIED} means that this trend is
	 * non-decreasing or increasing;</li>
	 * <li>{@link Satisfiability#NOT_SATISFIED} means that this trend is
	 * non-increasing or decreasing;</li>
	 * <li>{@link Satisfiability#UNKNOWN} means that this trend is stable or
	 * unstable.</li>
	 * </ul>
	 * 
	 * @return whether this trend is possibly growing.
	 */
	public Satisfiability isPossiblyGrowing() {
		return isInc() || isNonDec() ? Satisfiability.SATISFIED
				: (isDec() || isNonInc() ? Satisfiability.NOT_SATISFIED : Satisfiability.UNKNOWN);
	}

	/**
	 * Yields the combination of this trend with the given one. This operation
	 * is to be interpreted as the sequential concatenation of the two: if two
	 * (blocks of) instructions are executed sequentially, a variable having
	 * {@code t1} trend in the former and {@code t2} trend in the latter would
	 * have {@code t1.combine(t2)} as an overall trend.
	 * 
	 * @param other the other trend
	 * 
	 * @return the combination of the two trends
	 * 
	 * @throws SemanticException if something goes wrong during the computation
	 */
	public Trend combine(
			Trend other)
			throws SemanticException {
		// bottom and top are preserved
		if (isBottom() || other.isBottom())
			return BOTTOM;
		if (isTop() || other.isTop())
			return TOP;

		// stable is neutral
		if (isStable())
			return other;
		if (other.isStable())
			return this;

		// unstable or disagreeing on direction
		if (isNonStable() || other.isNonStable() || isPossiblyGrowing() != other.isPossiblyGrowing())
			return TOP;

		// strongest wins
		if (this.lessOrEqual(other))
			return this;
		if (other.lessOrEqual(this))
			return other;

		// this should be unreachable
		return TOP;
	}

	/**
	 * Inverts the current trend. This operation is the identity on top, bottom,
	 * stable and unstable trends. Otherwise, it inverts the direction of the
	 * trend.
	 * 
	 * @return the inverted trend
	 */
	public Trend invert() {
		if (this.isTop() || this.isBottom() || this.isStable() || this.isNonStable())
			return this;
		else if (this.isInc())
			return DEC;
		else if (this.isDec())
			return INC;
		else if (this.isNonInc())
			return NON_DEC;
		else if (this.isNonDec())
			return NON_INC;
		else
			return BOTTOM;
	}

	@Override
	public int hashCode() {
		return Objects.hash(trend);
	}

	@Override
	public boolean equals(
			Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		Trend other = (Trend) obj;
		return trend == other.trend;
	}

	@Override
	public boolean lessOrEqualAux(
			Trend other)
			throws SemanticException {
		if (isStable())
			return other.isNonInc() || other.isNonDec();

		if (isInc())
			return other.isNonDec() || other.isNonStable();

		if (isDec())
			return other.isNonInc() || other.isNonStable();

		return false;
	}

	@Override
	public Trend lubAux(
			Trend other)
			throws SemanticException {
		if (this.lessOrEqual(other))
			return other;
		else if (other.lessOrEqual(this))
			return this;
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
	public Trend glbAux(
			Trend other)
			throws SemanticException {
		if (this.lessOrEqual(other))
			return this;
		else if (other.lessOrEqual(this))
			return other;
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
	public Trend top() {
		return TOP;
	}

	@Override
	public Trend bottom() {
		return BOTTOM;
	}

	@Override
	public StructuredRepresentation representation() {
		if (isBottom())
			return Lattice.bottomRepresentation();
		if (isTop())
			return Lattice.topRepresentation();

		String repr;
		if (this.isStable())
			repr = "=";
		else if (this.isInc())
			repr = "↑";
		else if (this.isDec())
			repr = "↓";
		else if (this.isNonDec())
			repr = "↑=";
		else if (this.isNonInc())
			repr = "↓=";
		else
			repr = "≠";

		return new StringRepresentation(repr);
	}

	@Override
	public boolean canProcess(
			SymbolicExpression expression,
			ProgramPoint pp,
			SemanticOracle oracle) {
		if (expression instanceof PushInv)
			// the type approximation of a pushinv is bottom, so the below check
			// will always fail regardless of the kind of value we are tracking
			return expression.getStaticType().isNumericType();

		Set<Type> rts = null;
		try {
			rts = oracle.getRuntimeTypesOf(expression, pp, oracle);
		} catch (SemanticException e) {
			return false;
		}

		if (rts == null || rts.isEmpty())
			// if we have no runtime types, either the type domain has no type
			// information for the given expression (thus it can be anything,
			// also something that we can track) or the computation returned
			// bottom (and the whole state is likely going to go to bottom
			// anyway).
			return true;

		return rts.stream().anyMatch(Type::isNumericType);
	}

	@Override
	public String toString() {
		return representation().toString();
	}
}

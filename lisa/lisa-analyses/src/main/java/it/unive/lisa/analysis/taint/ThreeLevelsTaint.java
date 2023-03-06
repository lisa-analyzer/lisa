package it.unive.lisa.analysis.taint;

import it.unive.lisa.analysis.Lattice;
import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.analysis.representation.DomainRepresentation;
import it.unive.lisa.analysis.representation.StringRepresentation;
import it.unive.lisa.program.cfg.ProgramPoint;
import it.unive.lisa.symbolic.value.operator.binary.BinaryOperator;
import it.unive.lisa.symbolic.value.operator.ternary.TernaryOperator;

/**
 * A {@link BaseTaint} implementation with three level of taintedness: clean,
 * tainted and top. As such, this class distinguishes values that are always
 * clean, always tainted, or tainted in at least one execution path.
 * 
 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
 */
public class ThreeLevelsTaint extends BaseTaint<ThreeLevelsTaint> {

	private static final ThreeLevelsTaint TOP = new ThreeLevelsTaint((byte) 3);
	private static final ThreeLevelsTaint TAINTED = new ThreeLevelsTaint((byte) 2);
	private static final ThreeLevelsTaint CLEAN = new ThreeLevelsTaint((byte) 1);
	private static final ThreeLevelsTaint BOTTOM = new ThreeLevelsTaint((byte) 0);

	private final byte taint;

	/**
	 * Builds a new instance of taint.
	 */
	public ThreeLevelsTaint() {
		this((byte) 3);
	}

	private ThreeLevelsTaint(byte v) {
		this.taint = v;
	}

	@Override
	protected ThreeLevelsTaint tainted() {
		return TAINTED;
	}

	@Override
	protected ThreeLevelsTaint clean() {
		return CLEAN;
	}

	@Override
	public boolean isAlwaysTainted() {
		return this == TAINTED;
	}

	@Override
	public boolean isPossiblyTainted() {
		return this == TOP;
	}

	@Override
	public DomainRepresentation representation() {
		return this == BOTTOM ? Lattice.bottomRepresentation()
				: this == CLEAN ? new StringRepresentation("_")
						: this == TAINTED ? new StringRepresentation("#") : Lattice.topRepresentation();
	}

	@Override
	public ThreeLevelsTaint top() {
		return TOP;
	}

	@Override
	public ThreeLevelsTaint bottom() {
		return BOTTOM;
	}

	@Override
	public ThreeLevelsTaint evalBinaryExpression(BinaryOperator operator, ThreeLevelsTaint left, ThreeLevelsTaint right,
			ProgramPoint pp) throws SemanticException {
		if (left == TAINTED || right == TAINTED)
			return TAINTED;

		if (left == TOP || right == TOP)
			return TOP;

		return CLEAN;
	}

	@Override
	public ThreeLevelsTaint evalTernaryExpression(TernaryOperator operator, ThreeLevelsTaint left,
			ThreeLevelsTaint middle, ThreeLevelsTaint right, ProgramPoint pp) throws SemanticException {
		if (left == TAINTED || right == TAINTED || middle == TAINTED)
			return TAINTED;

		if (left == TOP || right == TOP || middle == TOP)
			return TOP;

		return CLEAN;
	}

	@Override
	public ThreeLevelsTaint lubAux(ThreeLevelsTaint other) throws SemanticException {
		// only happens with clean and tainted, that are not comparable
		return TOP;
	}

	@Override
	public ThreeLevelsTaint wideningAux(ThreeLevelsTaint other) throws SemanticException {
		// only happens with clean and tainted, that are not comparable
		return TOP;
	}

	@Override
	public boolean lessOrEqualAux(ThreeLevelsTaint other) throws SemanticException {
		// only happens with clean and tainted, that are not comparable
		return false;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + taint;
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
		ThreeLevelsTaint other = (ThreeLevelsTaint) obj;
		if (taint != other.taint)
			return false;
		return true;
	}

	@Override
	public String toString() {
		return representation().toString();
	}
}
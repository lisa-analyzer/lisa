package it.unive.lisa.lattices.informationFlow;

import it.unive.lisa.analysis.Lattice;
import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.util.representation.StringRepresentation;
import it.unive.lisa.util.representation.StructuredRepresentation;

/**
 * A three-level taint lattice with three levels: tainted, clean, and top (i.e.,
 * possibly tainted).
 * 
 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
 */
public class ThreeTaint
		implements
		TaintLattice<ThreeTaint> {

	/**
	 * The top instance of this taint lattice, representing values that are
	 * possibly tainted.
	 */
	public static final ThreeTaint TOP = new ThreeTaint((byte) 3);

	/**
	 * The tainted instance of this taint lattice, representing values that are
	 * always tainted.
	 */
	public static final ThreeTaint TAINTED = new ThreeTaint((byte) 2);

	/**
	 * The clean instance of this taint lattice, representing values that are
	 * always clean.
	 */
	public static final ThreeTaint CLEAN = new ThreeTaint((byte) 1);

	/**
	 * The bottom instance of this taint lattice.
	 */
	public static final ThreeTaint BOTTOM = new ThreeTaint((byte) 0);

	private final byte taint;

	/**
	 * Builds a new instance of taint.
	 */
	public ThreeTaint() {
		this((byte) 3);
	}

	private ThreeTaint(
			byte v) {
		this.taint = v;
	}

	@Override
	public ThreeTaint tainted() {
		return TAINTED;
	}

	@Override
	public ThreeTaint clean() {
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
	public StructuredRepresentation representation() {
		return this == BOTTOM ? Lattice.bottomRepresentation()
				: this == CLEAN ? new StringRepresentation("_")
						: this == TAINTED ? new StringRepresentation("#") : Lattice.topRepresentation();
	}

	@Override
	public ThreeTaint top() {
		return TOP;
	}

	@Override
	public ThreeTaint bottom() {
		return BOTTOM;
	}

	@Override
	public ThreeTaint lubAux(
			ThreeTaint other)
			throws SemanticException {
		// only happens with clean and tainted, that are not comparable
		return TOP;
	}

	@Override
	public ThreeTaint wideningAux(
			ThreeTaint other)
			throws SemanticException {
		// only happens with clean and tainted, that are not comparable
		return TOP;
	}

	@Override
	public boolean lessOrEqualAux(
			ThreeTaint other)
			throws SemanticException {
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
	public boolean equals(
			Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		ThreeTaint other = (ThreeTaint) obj;
		if (taint != other.taint)
			return false;
		return true;
	}

	@Override
	public String toString() {
		return representation().toString();
	}

	@Override
	public ThreeTaint or(
			ThreeTaint other)
			throws SemanticException {
		if (this == TAINTED || other == TAINTED)
			return TAINTED;

		if (this == TOP || other == TOP)
			return TOP;

		return CLEAN;
	}

}

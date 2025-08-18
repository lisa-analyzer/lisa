package it.unive.lisa.lattices.informationFlow;

import it.unive.lisa.analysis.Lattice;
import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.util.representation.StringRepresentation;
import it.unive.lisa.util.representation.StructuredRepresentation;

/**
 * A simple taint lattice with two levels: tainted and clean.
 * 
 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
 */
public class SimpleTaint implements TaintLattice<SimpleTaint> {

	/**
	 * The tainted instance of this taint lattice, representing values that are
	 * possibly tainted. This also serves as the top element of this lattice.
	 */
	public static final SimpleTaint TAINTED = new SimpleTaint(true);

	/**
	 * The clean instance of this taint lattice, representing values that are
	 * always clean.
	 */
	public static final SimpleTaint CLEAN = new SimpleTaint(false);

	/**
	 * The bottom instance of this taint lattice.
	 */
	public static final SimpleTaint BOTTOM = new SimpleTaint(null);

	private final Boolean taint;

	/**
	 * Builds a new instance of taint.
	 */
	private SimpleTaint() {
		this(true);
	}

	private SimpleTaint(
			Boolean taint) {
		this.taint = taint;
	}

	@Override
	public SimpleTaint tainted() {
		return TAINTED;
	}

	@Override
	public SimpleTaint clean() {
		return CLEAN;
	}

	@Override
	public boolean isPossiblyTainted() {
		return this == TAINTED;
	}

	@Override
	public boolean isAlwaysTainted() {
		return false;
	}

	@Override
	public StructuredRepresentation representation() {
		return this == BOTTOM ? Lattice.bottomRepresentation()
				: this == CLEAN ? new StringRepresentation("_") : new StringRepresentation("#");
	}

	@Override
	public SimpleTaint top() {
		return TAINTED;
	}

	@Override
	public SimpleTaint bottom() {
		return BOTTOM;
	}

	@Override
	public SimpleTaint lubAux(
			SimpleTaint other)
			throws SemanticException {
		return TAINTED; // should never happen
	}

	@Override
	public boolean lessOrEqualAux(
			SimpleTaint other)
			throws SemanticException {
		return false; // should never happen
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((taint == null) ? 0 : taint.hashCode());
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
		SimpleTaint other = (SimpleTaint) obj;
		if (taint == null) {
			if (other.taint != null)
				return false;
		} else if (!taint.equals(other.taint))
			return false;
		return true;
	}

	@Override
	public String toString() {
		return representation().toString();
	}

	@Override
	public SimpleTaint or(
			SimpleTaint other)
			throws SemanticException {
		return lub(other);
	}

}

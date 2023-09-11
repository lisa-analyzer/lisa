package it.unive.lisa.analysis.taint;

import it.unive.lisa.analysis.Lattice;
import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.util.representation.StringRepresentation;
import it.unive.lisa.util.representation.StructuredRepresentation;

/**
 * A {@link BaseTaint} implementation with only two level of taintedness: clean
 * and tainted. As such, this class distinguishes values that are always clean
 * from values that are tainted in at least one execution path.
 * 
 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
 */
public class Taint extends BaseTaint<Taint> {

	private static final Taint TAINTED = new Taint(true);

	private static final Taint CLEAN = new Taint(false);

	private static final Taint BOTTOM = new Taint(null);

	private final Boolean taint;

	/**
	 * Builds a new instance of taint.
	 */
	public Taint() {
		this(true);
	}

	private Taint(Boolean taint) {
		this.taint = taint;
	}

	@Override
	protected Taint tainted() {
		return TAINTED;
	}

	@Override
	protected Taint clean() {
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
	public Taint top() {
		return TAINTED;
	}

	@Override
	public Taint bottom() {
		return BOTTOM;
	}

	@Override
	public Taint lubAux(Taint other) throws SemanticException {
		return TAINTED; // should never happen
	}

	@Override
	public Taint wideningAux(Taint other) throws SemanticException {
		return TAINTED; // should never happen
	}

	@Override
	public boolean lessOrEqualAux(Taint other) throws SemanticException {
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
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		Taint other = (Taint) obj;
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
}

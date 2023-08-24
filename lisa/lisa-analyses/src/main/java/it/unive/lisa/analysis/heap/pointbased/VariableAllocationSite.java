package it.unive.lisa.analysis.heap.pointbased;

import it.unive.lisa.program.cfg.CodeLocation;
import it.unive.lisa.symbolic.value.Identifier;
import it.unive.lisa.type.Type;
import java.util.Objects;

/**
 * A variabile allocation site, namely an allocation site used by point-based
 * analyses abstracting concrete pointers to variables containing primitive type
 * values.
 * 
 * @author <a href="mailto:vincenzo.arceri@unipr.it">Vincenzo Arceri</a>
 * @author <a href="mailto:simone.gazza@studenti.unipr.it">Simone Gazza</a>
 */
public class VariableAllocationSite extends AllocationSite {

	/**
	 * The identifier pointed by this allocation site.
	 */
	private final Identifier identifier;

	/**
	 * Builds the variable allocation site
	 * 
	 * @param staticType the static type
	 * @param identifier the identifier pointed by this allocation site
	 * @param location   the location of this allocation site
	 */
	public VariableAllocationSite(Type staticType, Identifier identifier, CodeLocation location) {
		super(staticType, "&" + identifier.getName(), false, location);
		this.identifier = identifier;
	}

	@Override
	public String toString() {
		return getName();
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = super.hashCode();
		result = prime * result + Objects.hash(identifier);
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (!super.equals(obj))
			return false;
		if (getClass() != obj.getClass())
			return false;
		VariableAllocationSite other = (VariableAllocationSite) obj;
		return Objects.equals(identifier, other.identifier);
	}

	/**
	 * Yields the identifier.
	 * 
	 * @return the identifier
	 */
	public Identifier getIdentifier() {
		return identifier;
	}

}

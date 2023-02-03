package it.unive.lisa.analysis.heap.pointbased;

import java.util.Objects;

import it.unive.lisa.program.cfg.CodeLocation;
import it.unive.lisa.symbolic.value.Identifier;
import it.unive.lisa.type.Type;

public class VariableAllocationSite extends AllocationSite {
	private Identifier identifier;
	
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

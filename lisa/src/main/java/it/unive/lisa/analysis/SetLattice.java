package it.unive.lisa.analysis;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public abstract class SetLattice<S extends SetLattice<S, E>, E> extends BaseLattice<S> {

	protected Set<E> elements;
	
	protected SetLattice(Set<E> elements) {
		this.elements = elements;
	}
	
	protected abstract S mk(Set<E> lub);
	
	@Override
	protected final S lubAux(S other) throws SemanticException {
		Set<E> lub = new HashSet<>(elements);
		lub.addAll(other.elements);
		return mk(lub);
	}

	@Override
	protected final S wideningAux(S other) throws SemanticException {
		return lubAux(other);
	}

	@Override
	protected final boolean lessOrEqualAux(S other) throws SemanticException {
		return other.elements.containsAll(elements);
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((elements == null) ? 0 : elements.hashCode());
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
		SetLattice<?, ?> other = (SetLattice<?, ?>) obj;
		if (elements == null) {
			if (other.elements != null)
				return false;
		} else if (!elements.equals(other.elements))
			return false;
		return true;
	}
	
	@Override
	public final String toString() {
		return isTop() ? "TOP" : isBottom() ? "BOTTOM" : elements.toString();
	}
}

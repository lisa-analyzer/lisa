package it.unive.lisa.analysis.dataflow;

import it.unive.lisa.analysis.ScopedObject;
import it.unive.lisa.symbolic.value.Identifier;
import it.unive.lisa.util.representation.StructuredObject;
import java.util.Collection;

/**
 * An element of a dataflow lattice, that contains a collection of
 * {@link Identifier}s in its definition.
 * 
 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
 * 
 * @param <E> the concrete type of {@link DataflowElement}
 */
public interface DataflowElement<E extends DataflowElement<E>> extends StructuredObject, ScopedObject<E> {

	/**
	 * Yields all the {@link Identifier}s that are involved in the definition of
	 * this element.
	 * 
	 * @return the identifiers
	 */
	Collection<Identifier> getInvolvedIdentifiers();

	/**
	 * Replaces all occurrences of the {@code source} identifier with the
	 * {@code target} identifier in this element.
	 * 
	 * @param source the identifier to replace
	 * @param target the identifier to replace with
	 * 
	 * @return a new element with the replaced identifiers
	 */
	E replaceIdentifier(
			Identifier source,
			Identifier target);

}

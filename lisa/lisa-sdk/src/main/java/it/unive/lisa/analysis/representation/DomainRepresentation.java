package it.unive.lisa.analysis.representation;

import it.unive.lisa.analysis.SemanticDomain;
import it.unive.lisa.analysis.dataflow.DataflowElement;
import it.unive.lisa.analysis.nonrelational.NonRelationalDomain;
import it.unive.lisa.outputs.serializableGraph.SerializableValue;
import it.unive.lisa.util.collections.CollectionUtilities;

/**
 * A structured representation of the abstract information present in a single
 * instance of {@link SemanticDomain}, {@link NonRelationalDomain},
 * {@link DataflowElement} or other types of domains. Instances of this class
 * can be used to depict the content of an abstract element in a domain-agnostic
 * way, such as dumping the information to a file.
 * 
 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
 */
public abstract class DomainRepresentation implements Comparable<DomainRepresentation> {

	@Override
	public final int compareTo(DomainRepresentation o) {
		if (o == null)
			return 1;

		if (getClass() != o.getClass())
			return getClass().getName().compareTo(o.getClass().getName());

		return CollectionUtilities.nullSafeCompare(true, toString(), o.toString(), String::compareTo);
	}

	@Override
	public abstract int hashCode();

	@Override
	public abstract boolean equals(Object obj);

	@Override
	public abstract String toString();

	public abstract SerializableValue toSerializableValue();
}

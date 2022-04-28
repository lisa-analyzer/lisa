package it.unive.lisa.analysis.representation;

import java.util.SortedMap;
import java.util.TreeMap;

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

	public static final String REPRESENTATION_KIND = "REPRESENTATION_KIND";

	private final SortedMap<String, String> props = new TreeMap<>();

	public SortedMap<String, String> getProps() {
		return props;
	}

	public void setProp(String key, String value) {
		this.props.put(key, value);
	}

	@Override
	public final int compareTo(DomainRepresentation o) {
		if (o == null)
			return 1;

		if (getClass() != o.getClass())
			return getClass().getName().compareTo(o.getClass().getName());

		return CollectionUtilities.nullSafeCompare(true, toString(), o.toString(), String::compareTo);
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((props == null) ? 0 : props.hashCode());
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
		DomainRepresentation other = (DomainRepresentation) obj;
		if (props == null) {
			if (other.props != null)
				return false;
		} else if (!props.equals(other.props))
			return false;
		return true;
	}

	@Override
	public abstract String toString();

	public abstract SerializableValue toSerializableValue();
}

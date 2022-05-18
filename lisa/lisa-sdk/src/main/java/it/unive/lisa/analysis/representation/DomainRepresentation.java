package it.unive.lisa.analysis.representation;

import it.unive.lisa.analysis.SemanticDomain;
import it.unive.lisa.analysis.dataflow.DataflowElement;
import it.unive.lisa.analysis.nonrelational.NonRelationalDomain;
import it.unive.lisa.outputs.serializableGraph.SerializableValue;
import it.unive.lisa.util.collections.CollectionUtilities;
import java.util.SortedMap;
import java.util.TreeMap;

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

	private final SortedMap<String, String> properties = new TreeMap<>();

	/**
	 * Yields the set collection of textual properties defined for this
	 * representation.
	 * 
	 * @return the properties
	 */
	public SortedMap<String, String> getProperties() {
		return properties;
	}

	/**
	 * Sets a textual property to enrich the information represented by this
	 * instance.
	 * 
	 * @param key   the key of the property
	 * @param value the value of the property
	 */
	public void setProperty(String key, String value) {
		this.properties.put(key, value);
	}

	/**
	 * Produces a serializable version of this representation.
	 * 
	 * @return an instance of {@link SerializableValue} containing representing
	 *             the same information as this representation
	 */
	public abstract SerializableValue toSerializableValue();

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
		result = prime * result + ((properties == null) ? 0 : properties.hashCode());
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
		if (properties == null) {
			if (other.properties != null)
				return false;
		} else if (!properties.equals(other.properties))
			return false;
		return true;
	}

	@Override
	public abstract String toString();
}

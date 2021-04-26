package it.unive.lisa.analysis.representation;

public class StringRepresentation extends DomainRepresentation {

	private final String representation;

	public StringRepresentation(String representation) {
		this.representation = representation;
	}

	public StringRepresentation(Object obj) {
		this(String.valueOf(obj));
	}

	@Override
	public String toString() {
		return String.valueOf(representation);
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((representation == null) ? 0 : representation.hashCode());
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
		StringRepresentation other = (StringRepresentation) obj;
		if (representation == null) {
			if (other.representation != null)
				return false;
		} else if (!representation.equals(other.representation))
			return false;
		return true;
	}
}

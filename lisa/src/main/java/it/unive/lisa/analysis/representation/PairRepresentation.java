package it.unive.lisa.analysis.representation;

import java.util.function.Function;

public class PairRepresentation extends DomainRepresentation {

	private final DomainRepresentation left;
	private final DomainRepresentation right;

	public <L, R> PairRepresentation(L left, R right, Function<L, DomainRepresentation> leftMapper,
			Function<R, DomainRepresentation> rightMapper) {
		this(leftMapper.apply(left), rightMapper.apply(right));
	}

	public PairRepresentation(DomainRepresentation left, DomainRepresentation right) {
		this.left = left;
		this.right = right;
	}

	@Override
	public String toString() {
		return "(" + left + ", " + right + ")";
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((left == null) ? 0 : left.hashCode());
		result = prime * result + ((right == null) ? 0 : right.hashCode());
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
		PairRepresentation other = (PairRepresentation) obj;
		if (left == null) {
			if (other.left != null)
				return false;
		} else if (!left.equals(other.left))
			return false;
		if (right == null) {
			if (other.right != null)
				return false;
		} else if (!right.equals(other.right))
			return false;
		return true;
	}
}

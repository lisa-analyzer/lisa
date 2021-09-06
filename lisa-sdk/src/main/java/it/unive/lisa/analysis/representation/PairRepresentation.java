package it.unive.lisa.analysis.representation;

import java.util.function.Function;

/**
 * A {@link DomainRepresentation} in the form of a pair of elements.
 * 
 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
 */
public class PairRepresentation extends DomainRepresentation {

	/**
	 * The left-most element.
	 */
	protected final DomainRepresentation left;

	/**
	 * The right-most element.
	 */
	protected final DomainRepresentation right;

	/**
	 * Builds a new representation starting from the given pair of elements.
	 * {@code leftMapper} and {@code rightMapper} are used for transforming both
	 * elements to their individual representation.
	 * 
	 * @param <L>         the type of the left element
	 * @param <R>         the type of the right element
	 * @param left        the left element to represent
	 * @param right       the right element to represent
	 * @param leftMapper  the function that knows how to convert the left
	 *                        element to its representation
	 * @param rightMapper the function that knows how to convert the right
	 *                        element to its representation
	 */
	public <L, R> PairRepresentation(L left, R right, Function<L, DomainRepresentation> leftMapper,
			Function<R, DomainRepresentation> rightMapper) {
		this(leftMapper.apply(left), rightMapper.apply(right));
	}

	/**
	 * Builds a new representation containing the given pair of elements.
	 * 
	 * @param left  the left-most element
	 * @param right the right-most element
	 */
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

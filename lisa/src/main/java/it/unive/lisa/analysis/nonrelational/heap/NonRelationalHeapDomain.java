package it.unive.lisa.analysis.nonrelational.heap;

import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.analysis.heap.HeapSemanticOperation;
import it.unive.lisa.analysis.nonrelational.NonRelationalDomain;
import it.unive.lisa.program.cfg.ProgramPoint;
import it.unive.lisa.symbolic.SymbolicExpression;
import it.unive.lisa.symbolic.value.Identifier;

/**
 * A non-relational heap domain, that is able to compute the value of a
 * {@link SymbolicExpression} by knowing the values of all program variables.
 * Instances of this class can be wrapped inside a {@link HeapEnvironment} to
 * represent abstract values of individual {@link Identifier}s.
 * 
 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
 * 
 * @param <T> the concrete type of the domain
 */
public interface NonRelationalHeapDomain<T extends NonRelationalHeapDomain<T>>
		extends NonRelationalDomain<T, SymbolicExpression, HeapEnvironment<T>>, HeapSemanticOperation {

	@Override
	public default HeapEnvironment<T> assume(HeapEnvironment<T> environment, SymbolicExpression expression,
			ProgramPoint pp) throws SemanticException {
		return environment;
	}

	@Override
	@SuppressWarnings("unchecked")
	public default T glb(T other) throws SemanticException {
		if (other == null || this.isBottom() || other.isTop() || this == other || this.equals(other)
				|| this.lessOrEqual(other))
			return (T) this;

		if (other.isBottom() || this.isTop() || other.lessOrEqual((T) this))
			return (T) other;

		return glbAux(other);
	}

	/**
	 * Performs the greatest lower bound operation between this domain element
	 * and {@code other}, assuming that base cases have already been handled. In
	 * particular, it is guaranteed that:
	 * <ul>
	 * <li>{@code other} is not {@code null}</li>
	 * <li>{@code other} is neither <i>top</i> nor <i>bottom</i></li>
	 * <li>{@code this} is neither <i>top</i> nor <i>bottom</i></li>
	 * <li>{@code this} and {@code other} are not the same object (according
	 * both to {@code ==} and to {@link Object#equals(Object)})</li>
	 * <li>{@code this} and {@code other} are not comparable (according to
	 * {@link NonRelationalHeapDomain#lessOrEqual(NonRelationalHeapDomain)})</li>
	 * </ul>
	 * The default implementation returns
	 * {@link NonRelationalHeapDomain#bottom()}
	 * 
	 * @param other the other domain element
	 * 
	 * @return the greatest lower bound between this domain element and other
	 */
	public default T glbAux(T other) {
		return bottom();
	}
}

package it.unive.lisa.analysis.combination;

import it.unive.lisa.analysis.SemanticDomain.Satisfiability;
import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.analysis.nonrelational.value.BaseNonRelationalValueDomain;
import it.unive.lisa.analysis.nonrelational.value.NonRelationalValueDomain;
import it.unive.lisa.analysis.nonrelational.value.ValueEnvironment;
import it.unive.lisa.analysis.representation.DomainRepresentation;
import it.unive.lisa.analysis.representation.ListRepresentation;
import it.unive.lisa.program.cfg.ProgramPoint;
import it.unive.lisa.symbolic.SymbolicExpression;
import it.unive.lisa.symbolic.value.Identifier;
import it.unive.lisa.symbolic.value.ValueExpression;
import java.util.Map.Entry;

/**
 * A generic Cartesian product abstract domain between two non-communicating
 * {@link NonRelationalValueDomain}s (i.e., no exchange of information between
 * the abstract domains), assigning the same {@link Identifier}s and handling
 * instances of the same {@link SymbolicExpression}s.
 * 
 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
 *
 * @param <C>  the concrete type of the Cartesian product
 * @param <T1> the concrete instance of the left-hand side abstract domain of
 *                 the Cartesian product
 * @param <T2> the concrete instance of the right-hand side abstract domain of
 *                 the Cartesian product
 */
public abstract class NonRelationalValueCartesianProduct<C extends NonRelationalValueCartesianProduct<C, T1, T2>,
		T1 extends NonRelationalValueDomain<T1>,
		T2 extends NonRelationalValueDomain<T2>>
		extends BaseNonRelationalValueDomain<C> {

	/**
	 * The left-hand side abstract domain.
	 */
	public final T1 left;

	/**
	 * The right-hand side abstract domain.
	 */
	public final T2 right;

	/**
	 * Builds the Cartesian product abstract domain.
	 * 
	 * @param left  the left-hand side of the Cartesian product
	 * @param right the right-hand side of the Cartesian product
	 */
	public NonRelationalValueCartesianProduct(T1 left, T2 right) {
		this.left = left;
		this.right = right;
	}

	/**
	 * Builds a new instance of Cartesian product.
	 * 
	 * @param left  the first domain
	 * @param right the second domain
	 * 
	 * @return the new instance of product
	 */
	public abstract C mk(T1 left, T2 right);

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
		NonRelationalValueCartesianProduct<?, ?, ?> other = (NonRelationalValueCartesianProduct<?, ?, ?>) obj;
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

	@Override
	public DomainRepresentation representation() {
		return new ListRepresentation(left.representation(), right.representation());
	}

	@Override
	public C top() {
		return mk(left.top(), right.top());
	}

	@Override
	public boolean isTop() {
		return left.isTop() && right.isTop();
	}

	@Override
	public C bottom() {
		return mk(left.bottom(), right.bottom());
	}

	@Override
	public boolean isBottom() {
		return left.isBottom() && right.isBottom();
	}

	@Override
	public C lubAux(C other) throws SemanticException {
		return mk(left.lub(other.left), right.lub(other.right));
	}

	@Override
	public C wideningAux(C other) throws SemanticException {
		return mk(left.widening(other.left), right.widening(other.right));
	}

	@Override
	public boolean lessOrEqualAux(C other) throws SemanticException {
		return left.lessOrEqual(other.left) && right.lessOrEqual(other.right);
	}

	@Override
	public C eval(ValueExpression expression, ValueEnvironment<C> environment, ProgramPoint pp)
			throws SemanticException {
		ValueEnvironment<T1> lenv = new ValueEnvironment<>(left);
		ValueEnvironment<T2> renv = new ValueEnvironment<>(right);
		for (Entry<Identifier, C> entry : environment) {
			lenv = lenv.putState(entry.getKey(), entry.getValue().left);
			renv = renv.putState(entry.getKey(), entry.getValue().right);
		}

		return mk(left.eval(expression, lenv, pp), right.eval(expression, renv, pp));
	}

	@Override
	public Satisfiability satisfies(ValueExpression expression, ValueEnvironment<C> environment, ProgramPoint pp)
			throws SemanticException {
		ValueEnvironment<T1> lenv = new ValueEnvironment<>(left);
		ValueEnvironment<T2> renv = new ValueEnvironment<>(right);
		for (Entry<Identifier, C> entry : environment) {
			lenv = lenv.putState(entry.getKey(), entry.getValue().left);
			renv = renv.putState(entry.getKey(), entry.getValue().right);
		}

		return left.satisfies(expression, lenv, pp).glb(right.satisfies(expression, renv, pp));
	}

	@Override
	public ValueEnvironment<C> assume(ValueEnvironment<C> environment, ValueExpression expression, ProgramPoint pp)
			throws SemanticException {
		ValueEnvironment<T1> lenv = new ValueEnvironment<>(left);
		ValueEnvironment<T2> renv = new ValueEnvironment<>(right);
		for (Entry<Identifier, C> entry : environment) {
			lenv = lenv.putState(entry.getKey(), entry.getValue().left);
			renv = renv.putState(entry.getKey(), entry.getValue().right);
		}

		ValueEnvironment<T1> lassume = left.assume(lenv, expression, pp);
		ValueEnvironment<T2> rassume = right.assume(renv, expression, pp);

		@SuppressWarnings("unchecked")
		ValueEnvironment<C> res = new ValueEnvironment<>((C) this);
		for (Entry<Identifier, T1> entry : lassume)
			res = res.putState(entry.getKey(), mk(entry.getValue(), rassume.getState(entry.getKey())));
		for (Entry<Identifier, T2> entry : rassume)
			if (!res.getKeys().contains(entry.getKey()))
				res = res.putState(entry.getKey(), mk(left.bottom(), entry.getValue()));

		return res;
	}

	@Override
	public C glb(C other) throws SemanticException {
		return mk(left.glb(other.left), right.glb(other.right));
	}

	@Override
	public C variable(Identifier id, ProgramPoint pp) throws SemanticException {
		return mk(left.variable(id, pp), right.variable(id, pp));
	}

	@Override
	public boolean canProcess(SymbolicExpression expression) {
		return left.canProcess(expression) || right.canProcess(expression);
	}

	@Override
	public boolean tracksIdentifiers(Identifier id) {
		return left.tracksIdentifiers(id) || right.tracksIdentifiers(id);
	}
}

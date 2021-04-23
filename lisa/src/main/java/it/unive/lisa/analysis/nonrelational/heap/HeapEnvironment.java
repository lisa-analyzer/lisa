package it.unive.lisa.analysis.nonrelational.heap;

import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.analysis.heap.HeapDomain;
import it.unive.lisa.analysis.lattices.ExpressionSet;
import it.unive.lisa.analysis.lattices.FunctionalLattice;
import it.unive.lisa.analysis.nonrelational.Environment;
import it.unive.lisa.program.cfg.ProgramPoint;
import it.unive.lisa.symbolic.SymbolicExpression;
import it.unive.lisa.symbolic.value.Identifier;
import it.unive.lisa.symbolic.value.ValueExpression;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * An environment for a {@link NonRelationalHeapDomain}, that maps
 * {@link Identifier}s to instances of such domain. This is a
 * {@link FunctionalLattice}, that is, it implements a function mapping keys
 * (identifiers) to values (instances of the domain), and lattice operations are
 * automatically lifted for individual elements of the environment if they are
 * mapped to the same key.
 * 
 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
 * 
 * @param <T> the concrete instance of the {@link NonRelationalHeapDomain} whose
 *                instances are mapped in this environment
 */
public final class HeapEnvironment<T extends NonRelationalHeapDomain<T>>
		extends Environment<HeapEnvironment<T>, SymbolicExpression, T> implements HeapDomain<HeapEnvironment<T>> {

	/**
	 * The rewritten expressions
	 */
	private final ExpressionSet<ValueExpression> rewritten;

	/**
	 * The substitution
	 */
	private final List<HeapReplacement> substitution;

	/**
	 * Builds an empty environment.
	 * 
	 * @param domain a singleton instance to be used during semantic operations
	 *                   to retrieve top and bottom values
	 */
	public HeapEnvironment(T domain) {
		super(domain);
		rewritten = new ExpressionSet<ValueExpression>();
		substitution = Collections.emptyList();
	}

	/**
	 * Builds an empty environment from a given mapping.
	 * 
	 * @param domain   singleton instance to be used during semantic operations
	 *                     to retrieve top and bottom values
	 * @param function the initial mapping of this heap environment
	 */
	public HeapEnvironment(T domain, Map<Identifier, T> function) {
		this(domain, function, new ExpressionSet<ValueExpression>(), Collections.emptyList());
	}

	private HeapEnvironment(T domain, Map<Identifier, T> function, ExpressionSet<ValueExpression> rewritten,
			List<HeapReplacement> substitution) {
		super(domain, function);
		this.rewritten = rewritten;
		this.substitution = substitution;
	}

	@Override
	protected HeapEnvironment<T> mk(T lattice, Map<Identifier, T> function) {
		return new HeapEnvironment<>(lattice, function);
	}

	@Override
	public ExpressionSet<ValueExpression> getRewrittenExpressions() {
		return rewritten;
	}

	@Override
	public List<HeapReplacement> getSubstitution() {
		return substitution;
	}

	@Override
	protected HeapEnvironment<T> copy() {
		return new HeapEnvironment<T>(lattice, mkNewFunction(function),
				new ExpressionSet<ValueExpression>(rewritten.elements()),
				new ArrayList<>(substitution));
	}

	@Override
	public HeapEnvironment<T> assignAux(Identifier id, SymbolicExpression value, Map<Identifier, T> function, T eval,
			ProgramPoint pp) {
		return new HeapEnvironment<>(lattice, function, eval.getRewrittenExpressions(), eval.getSubstitution());
	}

	@Override
	public HeapEnvironment<T> assume(SymbolicExpression expression, ProgramPoint pp) throws SemanticException {
		T eval = lattice.eval(expression, this, pp);
		if (lattice.satisfies(expression, this, pp) == Satisfiability.NOT_SATISFIED)
			return bottom();
		else if (lattice.satisfies(expression, this, pp) == Satisfiability.SATISFIED)
			return new HeapEnvironment<>(lattice, function, eval.getRewrittenExpressions(), eval.getSubstitution());
		else
			// TODO this could be improved
			return new HeapEnvironment<>(lattice, function, eval.getRewrittenExpressions(), eval.getSubstitution());
	}

	@Override
	public HeapEnvironment<T> smallStepSemantics(SymbolicExpression expression, ProgramPoint pp)
			throws SemanticException {
		// environment does not change without an assignment
		T eval = lattice.eval(expression, this, pp);
		return new HeapEnvironment<>(lattice, function, eval.getRewrittenExpressions(), eval.getSubstitution());
	}

	@Override
	public HeapEnvironment<T> top() {
		return isTop() ? this
				: new HeapEnvironment<>(lattice.top(), null, new ExpressionSet<ValueExpression>(),
						Collections.emptyList());
	}

	@Override
	public HeapEnvironment<T> bottom() {
		return isBottom() ? this
				: new HeapEnvironment<>(lattice.bottom(), null, new ExpressionSet<ValueExpression>(),
						Collections.emptyList());
	}
}
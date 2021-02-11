package it.unive.lisa.analysis.nonrelational.heap;

import it.unive.lisa.analysis.FunctionalLattice;
import it.unive.lisa.analysis.HeapDomain;
import it.unive.lisa.analysis.nonrelational.Environment;
import it.unive.lisa.program.cfg.ProgramPoint;
import it.unive.lisa.symbolic.SymbolicExpression;
import it.unive.lisa.symbolic.value.Identifier;
import it.unive.lisa.symbolic.value.ValueExpression;
import java.util.ArrayList;
import java.util.Collection;
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
	private final Collection<ValueExpression> rewritten;

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
		rewritten = Collections.emptyList();
		substitution = Collections.emptyList();
	}

	private HeapEnvironment(T domain, Map<Identifier, T> function) {
		this(domain, function, Collections.emptyList(), Collections.emptyList());
	}

	private HeapEnvironment(T domain, Map<Identifier, T> function, Collection<ValueExpression> rewritten,
			List<HeapReplacement> substitution) {
		super(domain, function);
		this.rewritten = rewritten;
		this.substitution = substitution;
	}

	@Override
	public Collection<ValueExpression> getRewrittenExpressions() {
		return rewritten;
	}

	@Override
	public List<HeapReplacement> getSubstitution() {
		return substitution;
	}

	@Override
	protected HeapEnvironment<T> copy() {
		return new HeapEnvironment<T>(lattice, mkNewFunction(function), new ArrayList<>(rewritten),
				new ArrayList<>(substitution));
	}

	@Override
	public HeapEnvironment<T> assignAux(Identifier id, SymbolicExpression value, Map<Identifier, T> function, T eval,
			ProgramPoint pp) {
		return new HeapEnvironment<>(lattice, function, eval.getRewrittenExpressions(), eval.getSubstitution());
	}

	@Override
	public HeapEnvironment<T> smallStepSemantics(SymbolicExpression expression, ProgramPoint pp) {
		// environment does not change without an assignment
		return new HeapEnvironment<>(lattice, function);
	}

	@Override
	public HeapEnvironment<T> top() {
		return isTop() ? this
				: new HeapEnvironment<T>(lattice.top(), null, Collections.emptyList(), Collections.emptyList());
	}

	@Override
	public HeapEnvironment<T> bottom() {
		return isBottom() ? this
				: new HeapEnvironment<T>(lattice.bottom(), null, Collections.emptyList(), Collections.emptyList());
	}
}
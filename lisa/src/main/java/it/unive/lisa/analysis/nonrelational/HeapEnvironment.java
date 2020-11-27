package it.unive.lisa.analysis.nonrelational;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import it.unive.lisa.analysis.FunctionalLattice;
import it.unive.lisa.analysis.HeapDomain;
import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.symbolic.SymbolicExpression;
import it.unive.lisa.symbolic.value.Identifier;
import it.unive.lisa.symbolic.value.ValueExpression;

/**
 * An environment for a {@link NonRelationalHeapDomain}, that maps
 * {@link Identifier}s to instances of such domain. This is a
 * {@link FunctionalLattice}, that is, it implements a function mapping keys
 * (identifiers) to values (instances of the domain), and lattice operations are
 * automatically lifted for individual elements of the environment if they are
 * mapped to the same key.
 * 
 * @param <T> the concrete instance of the {@link NonRelationalHeapDomain} whose
 *            instances are mapped in this environment
 * 
 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
 */
public final class HeapEnvironment<T extends NonRelationalHeapDomain<T>>
		extends FunctionalLattice<HeapEnvironment<T>, Identifier, T> implements HeapDomain<HeapEnvironment<T>> {

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
	 * @param domain a singleton instance to be used during semantic operations to
	 *               retrieve top and bottom values
	 */
	public HeapEnvironment(T domain) {
		super(domain);
		rewritten = Collections.emptyList();
		substitution = Collections.emptyList();
	}

	private HeapEnvironment(T domain, Map<Identifier, T> function, Collection<ValueExpression> rewritten,
			List<HeapReplacement> substitution) {
		super(domain, function);
		this.rewritten = rewritten;
		this.substitution = substitution;
	}

	@Override
	public HeapEnvironment<T> assign(Identifier id, SymbolicExpression value) {
		Map<Identifier, T> func;
		if (function == null)
			func = new HashMap<>();
		else
			func = new HashMap<>(function);
		T eval = lattice.eval(value, this);
		function.put(id, eval);
		return new HeapEnvironment<>(lattice, func, eval.getRewrittenExpressions(), eval.getSubstitution());
	}

	@Override
	public HeapEnvironment<T> smallStepSemantics(SymbolicExpression expression) {
		// environment should not change without an assignment
		return new HeapEnvironment<>(lattice, function, Collections.emptyList(), Collections.emptyList());
	}

	@Override
	public HeapEnvironment<T> assume(SymbolicExpression expression) throws SemanticException {
		// TODO: to be refined
		return new HeapEnvironment<>(lattice, function, Collections.emptyList(), Collections.emptyList());
	}

	@Override
	public Satisfiability satisfies(SymbolicExpression currentExpression) {
		// TODO: to be refined
		return Satisfiability.UNKNOWN;
	}

	@Override
	public HeapEnvironment<T> top() {
		return new HeapEnvironment<T>(lattice.top(), null, Collections.emptyList(), Collections.emptyList());
	}

	@Override
	public HeapEnvironment<T> bottom() {
		return new HeapEnvironment<T>(lattice.bottom(), null, Collections.emptyList(), Collections.emptyList());
	}

	@Override
	public boolean isTop() {
		return lattice.isTop() && function == null;
	}

	@Override
	public boolean isBottom() {
		return lattice.isBottom() && function == null;
	}

	@Override
	public HeapEnvironment<T> forgetIdentifier(Identifier id) throws SemanticException {
		if (isTop() || isBottom())
			return this;

		HeapEnvironment<T> result = new HeapEnvironment<>(lattice, new HashMap<>(function), rewritten, substitution);
		if (result.function.containsKey(id))
			result.function.remove(id);

		return result;
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
	public String toString() {
		return representation();
	}

	@Override
	public String representation() {
		if (isTop())
			return "TOP";

		if (isBottom())
			return "BOTTOM";

		StringBuilder builder = new StringBuilder();
		for (Entry<Identifier, T> entry : function.entrySet())
			builder.append(entry.getKey()).append(": ").append(entry.getValue().representation()).append("\n");

		return builder.toString().trim();
	}
}
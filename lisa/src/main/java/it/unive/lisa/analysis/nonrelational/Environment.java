package it.unive.lisa.analysis.nonrelational;

import java.util.HashMap;
import java.util.Map;

import it.unive.lisa.analysis.FunctionalLattice;
import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.analysis.ValueDomain;
import it.unive.lisa.symbolic.value.Identifier;
import it.unive.lisa.symbolic.value.ValueExpression;

/**
 * An environment for a {@link NonRelationalDomain}, that maps
 * {@link Identifier}s to instances of such domain. This is a
 * {@link FunctionalLattice}, that is, it implements a function mapping keys
 * (identifiers) to values (instances of the domain), and lattice operations are
 * automatically lifted for individual elements of the environment if they are
 * mapped to the same key.
 * 
 * @param <T> the concrete instance of the {@link NonRelationalDomain} whose
 *            instances are mapped in this environment
 * 
 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
 */
public final class Environment<T extends NonRelationalDomain<T>>
		extends FunctionalLattice<Environment<T>, Identifier, T> implements ValueDomain<Environment<T>> {

	/**
	 * Builds an empty environment.
	 * 
	 * @param domain a singleton instance to be used during semantic operations to
	 *               retrieve top and bottom values
	 */
	public Environment(T domain) {
		super(domain);
	}

	private Environment(T domain, Map<Identifier, T> function) {
		super(domain, function);
	}

	@Override
	public Environment<T> assign(Identifier id, ValueExpression value) {
		Map<Identifier, T> func;
		if (function == null)
			func = new HashMap<>();
		else
			func = new HashMap<>(function);
		function.put(id, lattice.eval(value, this));
		return new Environment<>(lattice, func);
	}

	@Override
	public Environment<T> smallStepSemantics(ValueExpression expression) {
		// environment should not change without an assignment
		return new Environment<>(lattice, function);
	}

	@Override
	public Environment<T> assume(ValueExpression expression) throws SemanticException {
		// TODO: to be refined
		return new Environment<>(lattice, function);
	}

	@Override
	public Satisfiability satisfies(ValueExpression currentExpression) {
		// TODO: to be refined
		return Satisfiability.UNKNOWN;
	}

	@Override
	public Environment<T> top() {
		return new Environment<T>(lattice.top(), null);
	}

	@Override
	public Environment<T> bottom() {
		return new Environment<T>(lattice.bottom(), null);
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
	public Environment<T> forgetIdentifier(Identifier id) throws SemanticException {
		if (function == null)
			return new Environment<>(lattice, null);

		Environment<T> result = new Environment<>(lattice, new HashMap<>(function));
		if (result.function.containsKey(id))
			result.function.remove(id);

		return result;
	}
}
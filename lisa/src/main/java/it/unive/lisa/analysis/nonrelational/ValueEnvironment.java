package it.unive.lisa.analysis.nonrelational;

import it.unive.lisa.analysis.FunctionalLattice;
import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.analysis.ValueDomain;
import it.unive.lisa.symbolic.value.Identifier;
import it.unive.lisa.symbolic.value.ValueExpression;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

/**
 * An environment for a {@link NonRelationalValueDomain}, that maps
 * {@link Identifier}s to instances of such domain. This is a
 * {@link FunctionalLattice}, that is, it implements a function mapping keys
 * (identifiers) to values (instances of the domain), and lattice operations are
 * automatically lifted for individual elements of the environment if they are
 * mapped to the same key.
 * 
 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
 * 
 * @param <T> the concrete instance of the {@link NonRelationalValueDomain}
 *                whose instances are mapped in this environment
 */
public final class ValueEnvironment<T extends NonRelationalValueDomain<T>>
		extends FunctionalLattice<ValueEnvironment<T>, Identifier, T> implements ValueDomain<ValueEnvironment<T>> {

	/**
	 * Builds an empty environment.
	 * 
	 * @param domain a singleton instance to be used during semantic operations
	 *                   to retrieve top and bottom values
	 */
	public ValueEnvironment(T domain) {
		super(domain);
	}

	private ValueEnvironment(T domain, Map<Identifier, T> function) {
		super(domain, function);
	}

	@Override
	public ValueEnvironment<T> assign(Identifier id, ValueExpression value) {
		Map<Identifier, T> func;
		if (function == null)
			func = new HashMap<>();
		else
			func = new HashMap<>(function);
		func.put(id, lattice.eval(value, this));
		return new ValueEnvironment<>(lattice, func);
	}

	@Override
	public ValueEnvironment<T> smallStepSemantics(ValueExpression expression) {
		// environment should not change without an assignment
		return new ValueEnvironment<>(lattice, function);
	}

	@Override
	public ValueEnvironment<T> assume(ValueExpression expression) throws SemanticException {
		if (lattice.satisfies(expression, this) == Satisfiability.NOT_SATISFIED)
			return bottom();
		else if (lattice.satisfies(expression, this) == Satisfiability.SATISFIED)
			return this;
		else
			//TODO: a more precise filtering is needed when satisfiability of expression is unknown
			return this;
	}

	@Override
	public Satisfiability satisfies(ValueExpression currentExpression) {
		return lattice.satisfies(currentExpression, this);
	}

	@Override
	public ValueEnvironment<T> top() {
		return new ValueEnvironment<T>(lattice.top(), null);
	}

	@Override
	public ValueEnvironment<T> bottom() {
		return new ValueEnvironment<T>(lattice.bottom(), null);
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
	public ValueEnvironment<T> forgetIdentifier(Identifier id) throws SemanticException {
		if (function == null)
			return new ValueEnvironment<>(lattice, null);

		ValueEnvironment<T> result = new ValueEnvironment<>(lattice, new HashMap<>(function));
		if (result.function.containsKey(id))
			result.function.remove(id);

		return result;
	}

	@Override
	public int hashCode() {
		return super.hashCode();
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (!super.equals(obj))
			return false;
		if (getClass() != obj.getClass())
			return false;
		return true;
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
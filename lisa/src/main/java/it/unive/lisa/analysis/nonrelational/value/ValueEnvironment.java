package it.unive.lisa.analysis.nonrelational.value;

import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.analysis.heap.HeapSemanticOperation.HeapReplacement;
import it.unive.lisa.analysis.lattices.FunctionalLattice;
import it.unive.lisa.analysis.nonrelational.Environment;
import it.unive.lisa.analysis.value.ValueDomain;
import it.unive.lisa.program.cfg.ProgramPoint;
import it.unive.lisa.symbolic.value.Identifier;
import it.unive.lisa.symbolic.value.ValueExpression;
import java.util.List;
import java.util.Map;

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
		extends Environment<ValueEnvironment<T>, ValueExpression, T> implements ValueDomain<ValueEnvironment<T>> {

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
	protected ValueEnvironment<T> copy() {
		return new ValueEnvironment<>(lattice, mkNewFunction(function));
	}

	@Override
	protected ValueEnvironment<T> assignAux(Identifier id, ValueExpression value, Map<Identifier, T> function, T eval,
			ProgramPoint pp) {
		return new ValueEnvironment<>(lattice, function);
	}

	@Override
	public ValueEnvironment<T> smallStepSemantics(ValueExpression expression, ProgramPoint pp)
			throws SemanticException {
		// the environment does not change without an assignment
		return this;
	}

	@Override
	public ValueEnvironment<T> applySubstitution(List<HeapReplacement> substitution, ProgramPoint pp)
			throws SemanticException {
		if (isTop() || isBottom())
			return this;

		ValueEnvironment<T> result = this;
		for (HeapReplacement r : substitution) {
			ValueEnvironment<T> lub = bottom();
			for (Identifier source : r.getSources()) {
				ValueEnvironment<T> partial = result;
				for (Identifier target : r.getTargets()) {
					if (partial.getKeys().contains(source)) {
						Map<Identifier, T> func = mkNewFunction(partial.function);
						func.put(target, func.get(source));
						partial = new ValueEnvironment<T>(lattice, func);
					}
				}
				lub = lub.lub(partial);
			}
			result = lub.forgetIdentifiers(r.getIdsToForget());
		}

		return result;
	}

	@Override
	public ValueEnvironment<T> top() {
		return isTop() ? this : new ValueEnvironment<T>(lattice.top(), null);
	}

	@Override
	public ValueEnvironment<T> bottom() {
		return isBottom() ? this : new ValueEnvironment<T>(lattice.bottom(), null);
	}
}
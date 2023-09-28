package it.unive.lisa.analysis.nonrelational.value;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.analysis.SemanticOracle;
import it.unive.lisa.analysis.lattices.ExpressionSet;
import it.unive.lisa.analysis.lattices.FunctionalLattice;
import it.unive.lisa.analysis.nonrelational.Environment;
import it.unive.lisa.analysis.type.TypeDomain;
import it.unive.lisa.program.cfg.ProgramPoint;
import it.unive.lisa.symbolic.SymbolicExpression;
import it.unive.lisa.symbolic.value.Identifier;
import it.unive.lisa.symbolic.value.ValueExpression;
import it.unive.lisa.type.Type;
import it.unive.lisa.type.Untyped;

/**
 * An environment for a {@link NonRelationalTypeDomain}, that maps
 * {@link Identifier}s to instances of such domain. This is a
 * {@link FunctionalLattice}, that is, it implements a function mapping keys
 * (identifiers) to values (instances of the domain), and lattice operations are
 * automatically lifted for individual elements of the environment if they are
 * mapped to the same key. An expression can be typed through
 * {@link #getRuntimeTypesOf(SymbolicExpression, ProgramPoint, SemanticOracle)}
 * (and
 * {@link #getDynamicTypeOf(SymbolicExpression, ProgramPoint, SemanticOracle)}
 * yields the lub of such types).
 * 
 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
 * 
 * @param <T> the concrete instance of the {@link NonRelationalTypeDomain} whose
 *                instances are mapped in this environment
 */
public class TypeEnvironment<T extends NonRelationalTypeDomain<T>>
		extends
		Environment<TypeEnvironment<T>, ValueExpression, T>
		implements
		TypeDomain<TypeEnvironment<T>> {

	/**
	 * Builds an empty environment.
	 * 
	 * @param domain a singleton instance to be used during semantic operations
	 *                   to retrieve top and bottom values
	 */
	public TypeEnvironment(
			T domain) {
		super(domain);
	}

	/**
	 * Builds an environment containing the given mapping. If function is
	 * {@code null}, the new environment is the top environment if
	 * {@code lattice.isTop()} holds, and it is the bottom environment if
	 * {@code lattice.isBottom()} holds.
	 * 
	 * @param domain   a singleton instance to be used during semantic
	 *                     operations to retrieve top and bottom values
	 * @param function the function representing the mapping contained in the
	 *                     new environment; can be {@code null}
	 */
	public TypeEnvironment(
			T domain,
			Map<Identifier, T> function) {
		super(domain, function);
	}

	@Override
	public TypeEnvironment<T> mk(
			T lattice,
			Map<Identifier, T> function) {
		return new TypeEnvironment<>(lattice, function);
	}

	@Override
	public TypeEnvironment<T> top() {
		return isTop() ? this : new TypeEnvironment<>(lattice.top(), null);
	}

	@Override
	public TypeEnvironment<T> bottom() {
		return isBottom() ? this : new TypeEnvironment<>(lattice.bottom(), null);
	}

	@Override
	public Set<Type> getRuntimeTypesOf(
			SymbolicExpression e,
			ProgramPoint pp,
			SemanticOracle oracle)
			throws SemanticException {
		if (!e.mightNeedRewriting() || e instanceof Identifier)
			// we shortcut the evaluation of identifiers as those will be in the
			// mapping if we know something about them
			return eval((ValueExpression) e, pp, oracle).getRuntimeTypes();

		ExpressionSet vexps = oracle.rewrite(e, pp, oracle);
		Set<Type> result = new HashSet<>();
		for (SymbolicExpression vexp : vexps)
			result.addAll(eval((ValueExpression) vexp, pp, oracle).getRuntimeTypes());
		return result;
	}

	@Override
	public Type getDynamicTypeOf(
			SymbolicExpression e,
			ProgramPoint pp,
			SemanticOracle oracle)
			throws SemanticException {
		Set<Type> types = getRuntimeTypesOf(e, pp, oracle);
		if (types.isEmpty())
			return Untyped.INSTANCE;
		return Type.commonSupertype(types, Untyped.INSTANCE);
	}
}

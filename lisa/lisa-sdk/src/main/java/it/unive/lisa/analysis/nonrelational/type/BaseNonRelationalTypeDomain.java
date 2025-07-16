package it.unive.lisa.analysis.nonrelational.type;

import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.analysis.SemanticOracle;
import it.unive.lisa.analysis.lattices.ExpressionSet;
import it.unive.lisa.analysis.nonrelational.BaseNonRelationalDomain;
import it.unive.lisa.program.cfg.ProgramPoint;
import it.unive.lisa.symbolic.SymbolicExpression;
import it.unive.lisa.symbolic.value.Identifier;
import it.unive.lisa.symbolic.value.ValueExpression;
import it.unive.lisa.type.Type;
import it.unive.lisa.type.Untyped;
import java.util.HashSet;
import java.util.Set;

/**
 * Base implementation for {@link NonRelationalTypeDomain}s, offering all
 * capabilities of {@link BaseNonRelationalDomain}s.
 * 
 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
 * 
 * @param <L> the type of lattice used as values in environments produced by
 *                this domain
 */
public interface BaseNonRelationalTypeDomain<L extends TypeValue<L>>
		extends
		BaseNonRelationalDomain<L, TypeEnvironment<L>>,
		NonRelationalTypeDomain<L> {

	@Override
	default TypeEnvironment<L> makeLattice() {
		return new TypeEnvironment<>(top());
	}

	@Override
	default boolean canProcess(
			SymbolicExpression expression,
			ProgramPoint pp,
			SemanticOracle oracle) {
		// a type domain can process everything
		return true;
	}

	@Override
	default Set<Type> getRuntimeTypesOf(
			TypeEnvironment<L> state,
			SymbolicExpression e,
			ProgramPoint pp,
			SemanticOracle oracle)
			throws SemanticException {
		if (!e.mightNeedRewriting() || e instanceof Identifier) {
			// we shortcut the evaluation of identifiers as those will be in the
			// mapping if we know something about them
			L eval = eval(state, (ValueExpression) e, pp, oracle);
			if (eval.isBottom())
				return Set.of();
			if (eval.isTop())
				return pp.getProgram().getTypes().getTypes();
			return eval.getRuntimeTypes();
		}

		ExpressionSet vexps = oracle.rewrite(e, pp);
		Set<Type> result = new HashSet<>();
		for (SymbolicExpression vexp : vexps) {
			L eval = eval(state, (ValueExpression) vexp, pp, oracle);
			if (eval.isBottom())
				continue;
			else if (eval.isTop())
				return pp.getProgram().getTypes().getTypes();
			else
				result.addAll(eval.getRuntimeTypes());
		}
		return result;
	}

	@Override
	default Type getDynamicTypeOf(
			TypeEnvironment<L> state,
			SymbolicExpression e,
			ProgramPoint pp,
			SemanticOracle oracle)
			throws SemanticException {
		Set<Type> types = getRuntimeTypesOf(state, e, pp, oracle);
		if (types.isEmpty())
			return Untyped.INSTANCE;
		return Type.commonSupertype(types, Untyped.INSTANCE);
	}

}

package it.unive.lisa.analysis.nonrelational.value;

import it.unive.lisa.analysis.Lattice;
import it.unive.lisa.analysis.NonRelationalValue;
import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.analysis.SemanticOracle;
import it.unive.lisa.analysis.nonrelational.BaseNonRelationalDomain;
import it.unive.lisa.program.cfg.ProgramPoint;
import it.unive.lisa.symbolic.value.ValueExpression;

/**
 * Base implementation for {@link NonRelationalValueDomain}s, offering all
 * capabilities of {@link BaseNonRelationalDomain}s.
 * 
 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
 * 
 * @param <L> the type of lattice used as values in environments produced by
 *                this domain
 */
public interface BaseNonRelationalValueDomain<
		L extends Lattice<L> & NonRelationalValue<L>>
		extends
		BaseNonRelationalDomain<L, ValueEnvironment<L>>,
		NonRelationalValueDomain<L> {

	@Override
	default ValueEnvironment<L> makeLattice() {
		return new ValueEnvironment<>(top());
	}

	@Override
	default L nonrel(
			ValueEnvironment<L> environment,
			ValueExpression expression,
			ProgramPoint pp,
			SemanticOracle oracle)
			throws SemanticException {
		return eval(environment, expression, pp, oracle);
	}

	@Override
	default L nonrelTop() {
		return top();
	}

	@Override
	default L nonrelBottom() {
		return bottom();
	}

}

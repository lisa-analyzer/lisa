package it.unive.lisa.analysis.value;

import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.analysis.SemanticOracle;
import it.unive.lisa.program.cfg.ProgramPoint;
import it.unive.lisa.symbolic.value.PushInv;
import it.unive.lisa.symbolic.value.ValueExpression;
import it.unive.lisa.type.Type;
import java.util.Set;

/**
 * A {@link ValueDomain} that focuses on boolean values. This interface provides
 * a default implementation of
 * {@link #canProcess(ValueExpression, ProgramPoint, SemanticOracle)} that
 * checks whether the given expression is boolean or not.
 * 
 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
 * 
 * @param <L> the type of lattice used by this domain
 */
public interface BooleanAbstraction<
		L extends ValueLattice<L>>
		extends
		ValueDomain<L> {

	@Override
	default boolean canProcess(
			ValueExpression expression,
			ProgramPoint pp,
			SemanticOracle oracle) {
		boolean whole = oracle.hasWholeValueAnlysis();
		if (expression instanceof PushInv)
			// the type approximation of a pushinv is bottom, so the below check
			// will always fail regardless of the kind of value we are tracking
			return whole ? expression.getStaticType().isBooleanType() : expression.getStaticType().isValueType();

		Set<Type> rts = null;
		try {
			rts = oracle.getRuntimeTypesOf(expression, pp);
		} catch (SemanticException e) {
			return false;
		}

		if (rts == null || rts.isEmpty())
			// if we have no runtime types, either the type domain has no type
			// information for the given expression (thus it can be anything,
			// also something that we can track) or the computation returned
			// bottom (and the whole state is likely going to go to bottom
			// anyway).
			return true;

		if (whole)
			return rts.stream().anyMatch(Type::isBooleanType);
		else
			return rts.stream().anyMatch(Type::isValueType);
	}
}

package it.unive.lisa.analysis.value;

import org.apache.commons.lang3.tuple.Pair;

import it.unive.lisa.analysis.Lattice;
import it.unive.lisa.analysis.SemanticDomain;
import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.analysis.SemanticOracle;
import it.unive.lisa.analysis.heap.HeapDomain;
import it.unive.lisa.analysis.heap.HeapSemanticOperation.HeapReplacement;
import it.unive.lisa.program.cfg.ProgramPoint;
import it.unive.lisa.symbolic.value.Identifier;
import it.unive.lisa.symbolic.value.UnaryExpression;
import it.unive.lisa.symbolic.value.ValueExpression;
import it.unive.lisa.symbolic.value.Variable;
import it.unive.lisa.symbolic.value.operator.unary.LogicalNegation;

/**
 * A semantic domain that can evaluate the semantic of statements that operate
 * on values, and not on memory locations. A value domain can handle instances
 * of {@link ValueExpression}s, and manage identifiers that are
 * {@link Variable}s.
 * 
 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
 * 
 * @param <D> the concrete type of the {@link ValueDomain}
 */
public interface ValueDomain<D extends ValueDomain<D>>
		extends
		ValueOracle,
		SemanticDomain<D, ValueExpression, Identifier>,
		Lattice<D> {

	/**
	 * Applies a substitution of identifiers that is caused by a modification of
	 * the abstraction provided in the {@link HeapDomain} of the analysis. A
	 * substitution is composed by a list of {@link HeapReplacement} instances,
	 * that <b>must be applied in order</b>.
	 * 
	 * @param r      the replacement to apply
	 * @param pp     the program point that where this operation is being
	 *                   evaluated
	 * @param oracle the oracle for inter-domain communication
	 * 
	 * @return the value domain instance modified by the substitution
	 * 
	 * @throws SemanticException if an error occurs during the computation
	 */
	@SuppressWarnings("unchecked")
	default D applyReplacement(
			HeapReplacement r,
			ProgramPoint pp,
			SemanticOracle oracle)
			throws SemanticException {
		if (isTop() || isBottom() || r.getSources().isEmpty())
			return (D) this;

		D result = (D) this;
		D lub = bottom();
		for (Identifier source : r.getSources()) {
			D partial = result;
			for (Identifier target : r.getTargets())
				partial = partial.assign(target, source, pp, oracle);
			lub = lub.lub(partial);
		}
		return lub.forgetIdentifiers(r.getIdsToForget());

	}
	
	default Pair<D, D> split(ValueExpression expr, 
			ProgramPoint src, ProgramPoint dest, SemanticOracle oracle) throws SemanticException {
		// Split operator logic implementation
		return Pair.of(this.assume(expr, src, dest, oracle), 
				this.assume(new UnaryExpression(expr.getStaticType(), 
						expr, LogicalNegation.INSTANCE, expr.getCodeLocation()), src, dest, oracle));
	}
}

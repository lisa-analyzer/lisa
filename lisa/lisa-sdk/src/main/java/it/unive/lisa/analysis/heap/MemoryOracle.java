package it.unive.lisa.analysis.heap;

import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.analysis.SemanticOracle;
import it.unive.lisa.analysis.lattices.ExpressionSet;
import it.unive.lisa.analysis.lattices.Satisfiability;
import it.unive.lisa.program.cfg.ProgramPoint;
import it.unive.lisa.symbolic.SymbolicExpression;
import it.unive.lisa.symbolic.heap.HeapExpression;
import it.unive.lisa.symbolic.value.HeapLocation;
import it.unive.lisa.symbolic.value.Identifier;
import it.unive.lisa.symbolic.value.MemoryPointer;
import it.unive.lisa.symbolic.value.ValueExpression;
import java.util.HashSet;
import java.util.Set;

/**
 * An oracle that can be queried for information about the static and dynamic
 * memory of the program.
 * 
 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
 */
public interface MemoryOracle {

	/**
	 * Rewrites the given expression to a simpler form containing no sub
	 * expressions regarding the heap (that is, {@link HeapExpression}s). Every
	 * expression contained in the result can be safely cast to
	 * {@link ValueExpression}.
	 * 
	 * @param expression the expression to rewrite
	 * @param pp         the program point where the rewrite happens
	 * @param oracle     the oracle for inter-domain communication
	 * 
	 * @return the rewritten expressions
	 * 
	 * @throws SemanticException if something goes wrong while rewriting
	 */
	ExpressionSet rewrite(
			SymbolicExpression expression,
			ProgramPoint pp,
			SemanticOracle oracle)
			throws SemanticException;

	/**
	 * Rewrites the given expressions to a simpler form containing no sub
	 * expressions regarding the heap (that is, {@link HeapExpression}s). Every
	 * expression contained in the result can be safely cast to
	 * {@link ValueExpression}.
	 * 
	 * @param expressions the expressions to rewrite
	 * @param pp          the program point where the rewrite happens
	 * @param oracle      the oracle for inter-domain communication
	 * 
	 * @return the rewritten expressions
	 * 
	 * @throws SemanticException if something goes wrong while rewriting
	 */
	default ExpressionSet rewrite(
			ExpressionSet expressions,
			ProgramPoint pp,
			SemanticOracle oracle)
			throws SemanticException {
		Set<SymbolicExpression> result = new HashSet<>();
		for (SymbolicExpression expr : expressions)
			if (!expr.mightNeedRewriting())
				result.add(expr);
			else
				result.addAll(rewrite(expr, pp, oracle).elements());
		return new ExpressionSet(result);
	}

	/**
	 * Yields whether or not the two given expressions are aliases, that is, if
	 * they point to the same region of memory. Note that, for this method to
	 * return {@link Satisfiability#SATISFIED}, both expressions should be
	 * pointers to other expressions.
	 * 
	 * @param x      the first expression
	 * @param y      the second expression
	 * @param pp     the {@link ProgramPoint} where the computation happens
	 * @param oracle the oracle for inter-domain communication
	 * 
	 * @return whether or not the two expressions are aliases
	 * 
	 * @throws SemanticException if something goes wrong during the computation
	 */
	Satisfiability alias(
			SymbolicExpression x,
			SymbolicExpression y,
			ProgramPoint pp,
			SemanticOracle oracle)
			throws SemanticException;

	/**
	 * Yields all the {@link Identifier}s that are reachable starting from the
	 * {@link Identifier} represented (directly or after rewriting) by the given
	 * expression. This corresponds to recursively explore the memory region
	 * reachable by {@code e}, traversing all possible pointers until no more
	 * are available.
	 * 
	 * @param e      the expression corresponding to the starting point
	 * @param pp     the {@link ProgramPoint} where the computation happens
	 * @param oracle the oracle for inter-domain communication
	 * 
	 * @return the expressions representing memory regions reachable from
	 *             {@code e}
	 * 
	 * @throws SemanticException if something goes wrong during the computation
	 */
	default ExpressionSet reachableFrom(
			SymbolicExpression e,
			ProgramPoint pp,
			SemanticOracle oracle)
			throws SemanticException {
		Set<SymbolicExpression> ws = new HashSet<>();
		ws.add(e);

		Set<SymbolicExpression> result = new HashSet<>();
		Set<SymbolicExpression> prev = new HashSet<>();
		Set<SymbolicExpression> locs = new HashSet<>();
		rewrite(e, pp, oracle).elements().stream().forEach(result::add);

		do {
			ws.addAll(locs);
			prev = new HashSet<>(result);
			for (SymbolicExpression id : ws) {
				ExpressionSet rewritten = rewrite(id, pp, oracle);
				locs = new HashSet<>();
				for (SymbolicExpression r : rewritten) {
					if (r instanceof MemoryPointer) {
						HeapLocation l = ((MemoryPointer) r).getReferencedLocation();
						locs.add(l);
						result.add(l);
					} else
						locs.add(r);
				}
			}
		} while (!prev.equals(result));

		return new ExpressionSet(result);
	}

	/**
	 * Yields whether or not the {@link Identifier} represented (directly or
	 * after rewriting) by the second expression is reachable starting from the
	 * {@link Identifier} represented (directly or after rewriting) by the first
	 * expression. Note that, for this method to return
	 * {@link Satisfiability#SATISFIED}, not only {@code x} needs to be a
	 * pointer to another expression, but the latter should be a pointer as
	 * well, and so on until {@code y} is reached.
	 * 
	 * @param x      the first expression
	 * @param y      the second expression
	 * @param pp     the {@link ProgramPoint} where the computation happens
	 * @param oracle the oracle for inter-domain communication
	 * 
	 * @return whether or not the second expression can be reached from the
	 *             first one
	 * 
	 * @throws SemanticException if something goes wrong during the computation
	 */
	Satisfiability isReachableFrom(
			SymbolicExpression x,
			SymbolicExpression y,
			ProgramPoint pp,
			SemanticOracle oracle)
			throws SemanticException;

	/**
	 * Yields whether or not the {@link Identifier} represented (directly or
	 * after rewriting) by the second expression is reachable starting from the
	 * {@link Identifier} represented (directly or after rewriting) by the first
	 * expression, and vice versa. This is equivalent to invoking
	 * {@code isReachableFrom(x, y, pp, oracle).and(isReachableFrom(y, x, pp, oracle))},
	 * that corresponds to the default implementation of this method.
	 * 
	 * @param x      the first expression
	 * @param y      the second expression
	 * @param pp     the {@link ProgramPoint} where the computation happens
	 * @param oracle the oracle for inter-domain communication
	 * 
	 * @return whether or not the two expressions are mutually reachable
	 * 
	 * @throws SemanticException if something goes wrong during the computation
	 */
	default Satisfiability areMutuallyReachable(
			SymbolicExpression x,
			SymbolicExpression y,
			ProgramPoint pp,
			SemanticOracle oracle)
			throws SemanticException {
		return isReachableFrom(x, y, pp, oracle).and(isReachableFrom(y, x, pp, oracle));
	}
}

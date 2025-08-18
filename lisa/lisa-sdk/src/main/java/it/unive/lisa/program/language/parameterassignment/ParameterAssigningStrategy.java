package it.unive.lisa.program.language.parameterassignment;

import it.unive.lisa.analysis.AbstractDomain;
import it.unive.lisa.analysis.AbstractLattice;
import it.unive.lisa.analysis.AnalysisState;
import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.analysis.StatementStore;
import it.unive.lisa.analysis.lattices.ExpressionSet;
import it.unive.lisa.interprocedural.InterproceduralAnalysis;
import it.unive.lisa.program.cfg.Parameter;
import it.unive.lisa.program.cfg.statement.call.Call;
import org.apache.commons.lang3.tuple.Pair;

/**
 * A strategy for assigning parameters at call sites. Depending on the language,
 * assignments from actual to formal parameters might happen in the same order
 * as they are evaluated, or they might happen with a by-name semantics, where
 * the name of a parameter is prefixed to the expression representing the value
 * to be assigned to it. The latter semantics enables parameter shuffling. Each
 * strategy comes with a different
 * {@link #prepare(Call, AnalysisState, InterproceduralAnalysis, StatementStore, Parameter[], ExpressionSet[])}
 * implementation that can automatically perform the assignments.
 * 
 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
 */
public interface ParameterAssigningStrategy {

	/**
	 * Prepares the entryState for the targets of the given {@link Call},
	 * assuming that the state when the call is executed (after the evaluation
	 * of the parameters) is {@code callState}, and each parameter to the call
	 * is represented by an element of {@code parameters}. Here, no restrictions
	 * on the order of the parameters is made: they can be passed as-is,
	 * preserving their evaluation order (Java-like), or they may be passed
	 * by-name (Python-like).
	 * 
	 * @param <A>             the kind of {@link AbstractLattice} produced by
	 *                            the domain {@code D}
	 * @param <D>             the kind of {@link AbstractDomain} to run during
	 *                            the analysis
	 * @param call            the call to be prepared
	 * @param callState       the analysis state where the call is to be
	 *                            executed
	 * @param interprocedural the interprocedural analysis of the program to
	 *                            analyze
	 * @param expressions     the cache where analysis states of intermediate
	 *                            expressions must be stored
	 * @param formals         the expressions representing the formal parameters
	 *                            of the call
	 * @param actuals         the expressions representing the actual parameters
	 *                            of the call
	 * 
	 * @return the prepared state, ready to be used as entry-state for the
	 *             targets, and the expressions to use as parameters of the call
	 * 
	 * @throws SemanticException if something goes wrong while preparing the
	 *                               entry-state
	 */
	<A extends AbstractLattice<A>, D extends AbstractDomain<A>> Pair<AnalysisState<A>, ExpressionSet[]> prepare(
			Call call,
			AnalysisState<A> callState,
			InterproceduralAnalysis<A, D> interprocedural,
			StatementStore<A> expressions,
			Parameter[] formals,
			ExpressionSet[] actuals)
			throws SemanticException;

}

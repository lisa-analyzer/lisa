package it.unive.lisa.analysis;

import java.util.Set;

import it.unive.lisa.analysis.heap.HeapDomain;
import it.unive.lisa.analysis.lattices.ExpressionSet;
import it.unive.lisa.analysis.value.TypeDomain;
import it.unive.lisa.analysis.value.ValueDomain;
import it.unive.lisa.program.cfg.ProgramPoint;
import it.unive.lisa.symbolic.SymbolicExpression;
import it.unive.lisa.symbolic.heap.HeapExpression;
import it.unive.lisa.symbolic.value.Identifier;
import it.unive.lisa.symbolic.value.ValueExpression;
import it.unive.lisa.type.Type;

/**
 * An abstract state of the analysis, composed by a heap state modeling the
 * memory layout and a value state modeling values of program variables and
 * memory locations. An abstract state also wraps a domain to reason about
 * runtime types of such variables.
 * 
 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
 * 
 * @param <A> the concrete type of the {@link AbstractState}
 * @param <H> the type of {@link HeapDomain} embedded in this state
 * @param <V> the type of {@link ValueDomain} embedded in this state
 * @param <T> the type of {@link ValueDomain} and {@link TypeDomain} embedded in
 *                this state
 */
public interface AbstractState<A extends AbstractState<A, H, V, T>,
		H extends HeapDomain<H>,
		V extends ValueDomain<V>,
		T extends TypeDomain<T>>
		extends Lattice<A>, SemanticDomain<A, SymbolicExpression, Identifier> {

	/**
	 * Rewrites the given expression to a simpler form containing no sub
	 * expressions regarding the heap (that is, {@link HeapExpression}s). Every
	 * expression contained in the result can be safely cast to
	 * {@link ValueExpression}.
	 * 
	 * @param expression the expression to rewrite
	 * @param pp         the program point where the rewrite happens
	 * 
	 * @return the rewritten expressions
	 * 
	 * @throws SemanticException if something goes wrong while rewriting
	 */
	public ExpressionSet<SymbolicExpression> rewrite(
			SymbolicExpression expression,
			ProgramPoint pp)
			throws SemanticException;

	/**
	 * Rewrites the given expressions to a simpler form containing no sub
	 * expressions regarding the heap (that is, {@link HeapExpression}s). Every
	 * expression contained in the result can be safely cast to
	 * {@link ValueExpression}.
	 * 
	 * @param expressions the expressions to rewrite
	 * @param pp          the program point where the rewrite happens
	 * 
	 * @return the rewritten expressions
	 * 
	 * @throws SemanticException if something goes wrong while rewriting
	 */
	public ExpressionSet<SymbolicExpression> rewrite(
			ExpressionSet<SymbolicExpression> expressions,
			ProgramPoint pp)
			throws SemanticException;

	/**
	 * Yields the runtime types that this analysis infers for the given
	 * expression.
	 * 
	 * @param e  the expression to type
	 * @param pp the program point where the types are required
	 * 
	 * @return the runtime types
	 * 
	 * @throws SemanticException
	 */
	Set<Type> getRuntimeTypesOf(SymbolicExpression e, ProgramPoint pp) throws SemanticException;

	/**
	 * Yields the dynamic type that this analysis infers for the given
	 * expression. The dynamic type is the least common supertype of all its
	 * runtime types.
	 * 
	 * @param e  the expression to type
	 * @param pp the program point where the types are required
	 * 
	 * @return the dynamic type
	 */
	Type getDynamicTypeOf(SymbolicExpression e, ProgramPoint pp) throws SemanticException;
}

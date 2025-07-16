package it.unive.lisa.analysis;

import it.unive.lisa.analysis.lattices.ExpressionSet;
import it.unive.lisa.analysis.lattices.Satisfiability;
import it.unive.lisa.program.cfg.ProgramPoint;
import it.unive.lisa.symbolic.SymbolicExpression;
import it.unive.lisa.symbolic.heap.HeapExpression;
import it.unive.lisa.symbolic.value.Identifier;
import it.unive.lisa.symbolic.value.ValueExpression;
import it.unive.lisa.type.Type;
import java.util.Set;

/**
 * An oracle that can be queried for semantic information on the program under
 * analysis. Put simply, a semantic oracle is a pair of a lattice instance,
 * denoting the current state of the analysis, and a domain that is able to
 * process that lattice. By having such a pair, the oracle can provide semantic
 * information about the program, such as the runtime types of expressions, the
 * rewriting of expressions to simpler forms, and so on. Instances of this class
 * can be used for inter-domain communication between different
 * {@link SemanticComponent}s.
 * 
 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
 */
public interface SemanticOracle {

	/**
	 * Yields the runtime types that this analysis infers for the given
	 * expression.
	 * 
	 * @param e  the expression to type
	 * @param pp the program point where the types are required
	 * 
	 * @return the runtime types
	 * 
	 * @throws SemanticException if something goes wrong during the computation
	 */
	Set<Type> getRuntimeTypesOf(
			SymbolicExpression e,
			ProgramPoint pp)
			throws SemanticException;

	/**
	 * Yields the dynamic type that this analysis infers for the given
	 * expression. The dynamic type is the least common supertype of all its
	 * runtime types.
	 * 
	 * @param e  the expression to type
	 * @param pp the program point where the types are required
	 * 
	 * @return the dynamic type
	 * 
	 * @throws SemanticException if something goes wrong during the computation
	 */
	Type getDynamicTypeOf(
			SymbolicExpression e,
			ProgramPoint pp)
			throws SemanticException;

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
	ExpressionSet rewrite(
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
	ExpressionSet rewrite(
			ExpressionSet expressions,
			ProgramPoint pp)
			throws SemanticException;

	/**
	 * Yields whether or not the two given expressions are aliases, that is, if
	 * they point to the same region of memory. Note that, for this method to
	 * return {@link Satisfiability#SATISFIED}, both expressions should be
	 * pointers to other expressions.
	 * 
	 * @param x  the first expression
	 * @param y  the second expression
	 * @param pp the {@link ProgramPoint} where the computation happens
	 * 
	 * @return whether or not the two expressions are aliases
	 * 
	 * @throws SemanticException if something goes wrong during the computation
	 */
	Satisfiability alias(
			SymbolicExpression x,
			SymbolicExpression y,
			ProgramPoint pp)
			throws SemanticException;

	/**
	 * Yields all the {@link Identifier}s that are reachable starting from the
	 * {@link Identifier} represented (directly or after rewriting) by the given
	 * expression. This corresponds to recursively explore the memory region
	 * reachable by {@code e}, traversing all possible pointers until no more
	 * are available.
	 * 
	 * @param e  the expression corresponding to the starting point
	 * @param pp the {@link ProgramPoint} where the computation happens
	 * 
	 * @return the expressions representing memory regions reachable from
	 *             {@code e}
	 * 
	 * @throws SemanticException if something goes wrong during the computation
	 */
	ExpressionSet reachableFrom(
			SymbolicExpression e,
			ProgramPoint pp)
			throws SemanticException;

	/**
	 * Yields whether or not the {@link Identifier} represented (directly or
	 * after rewriting) by the second expression is reachable starting from the
	 * {@link Identifier} represented (directly or after rewriting) by the first
	 * expression. Note that, for this method to return
	 * {@link Satisfiability#SATISFIED}, not only {@code x} needs to be a
	 * pointer to another expression, but the latter should be a pointer as
	 * well, and so on until {@code y} is reached.
	 * 
	 * @param x  the first expression
	 * @param y  the second expression
	 * @param pp the {@link ProgramPoint} where the computation happens
	 * 
	 * @return whether or not the second expression can be reached from the
	 *             first one
	 * 
	 * @throws SemanticException if something goes wrong during the computation
	 */
	Satisfiability isReachableFrom(
			SymbolicExpression x,
			SymbolicExpression y,
			ProgramPoint pp)
			throws SemanticException;

	/**
	 * Yields whether or not the {@link Identifier} represented (directly or
	 * after rewriting) by the second expression is reachable starting from the
	 * {@link Identifier} represented (directly or after rewriting) by the first
	 * expression, and vice versa. This is equivalent to invoking
	 * {@code isReachableFrom(x, y, pp, oracle).and(isReachableFrom(y, x, pp, oracle))},
	 * that corresponds to the default implementation of this method.
	 * 
	 * @param x  the first expression
	 * @param y  the second expression
	 * @param pp the {@link ProgramPoint} where the computation happens
	 * 
	 * @return whether or not the two expressions are mutually reachable
	 * 
	 * @throws SemanticException if something goes wrong during the computation
	 */
	Satisfiability areMutuallyReachable(
			SymbolicExpression x,
			SymbolicExpression y,
			ProgramPoint pp)
			throws SemanticException;

}

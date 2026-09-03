package it.unive.lisa.analysis;

import it.unive.lisa.analysis.value.ValueDomain;
import it.unive.lisa.events.EventQueue;
import it.unive.lisa.lattices.ExpressionSet;
import it.unive.lisa.lattices.Satisfiability;
import it.unive.lisa.program.cfg.ProgramPoint;
import it.unive.lisa.symbolic.SymbolicExpression;
import it.unive.lisa.symbolic.memory.MemoryExpression;
import it.unive.lisa.symbolic.value.BinaryExpression;
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
	 * Gets the {@link EventQueue} that can be used to post analysis events.
	 * Note that in case no listeners are registered for the analysis, the
	 * return value of this method is {@code null}.
	 * 
	 * @return the event queue
	 */
	EventQueue getEventQueue();

	/**
	 * Yields whether or not this oracle is able to provide whole value analysis
	 * constraints through the method
	 * {@link #constraints(ValueDomain, ValueExpression, ProgramPoint)}. If this
	 * method returns {@code false}, then the latter always returns an empty set
	 * of constraints, meaning that no information about the concrete values of
	 * expressions is available. For this method to return {@code true}, the
	 * analysis has to be set up to use a
	 * {@link it.unive.lisa.analysis.combination.constraints.WholeValueAnalysis}
	 * as value domain.
	 * 
	 * @return whether or not this oracle can provide whole value analysis
	 *             constraints
	 */
	boolean hasWholeValueAnlysis();

	/**
	 * Generates a set of constraints that model the concrete values of
	 * {@code e} in the state that this oracle models. The constraints must be
	 * definite, as in with each constraint the set of concrete values shrinks.
	 * An empty set of constraints thus represents any possible concrete value.
	 * A {@code null} set of constraints represents a bottom value. <br/>
	 * <br/>
	 * Note that this method always returns an empty set if
	 * {@link #hasWholeValueAnlysis} returns {@code false}. Instead, if the
	 * whole value analysis is available, all domains involved in it are queried
	 * for constraints if they can handle the target expression.<br/>
	 * <br/>
	 * The requesting domain is the one that is asking for the constraints, and
	 * it is used to avoid recursive calls to the same domain. Each constraint
	 * is given as a {@link BinaryExpression}, where the left operand is a
	 * constant and the right operand is the expression whose value is being
	 * constrained, corresponding to the parameter {@code e}.
	 * 
	 * @param requesting the domain that is requesting the constraints
	 * @param e          the expression whose value is being constrained
	 * @param pp         the program point at which the constraints are being
	 *                       generated
	 * 
	 * @return a set of constraints modeling the possible values of {@code e} in
	 *             the state modeled by this oracle
	 * 
	 * @throws SemanticException if an error occurs during the computation
	 */
	Set<BinaryExpression> constraints(
			ValueDomain<?> requesting,
			ValueExpression e,
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
	 * expressions regarding the memory (that is, {@link MemoryExpression}s).
	 * Every expression contained in the result can be safely cast to
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
	 * expressions regarding the memory (that is, {@link MemoryExpression}s).
	 * Every expression contained in the result can be safely cast to
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

	/**
	 * Yields the non-relational abstract value that this oracle's value domain
	 * associates to {@code expression}.
	 *
	 * @param expression the expression to evaluate
	 * @param pp         the program point where the evaluation happens
	 *
	 * @return the non-relational abstract value of {@code expression}
	 *
	 * @throws SemanticException if something goes wrong during the computation
	 */
	NonRelationalValue<?> nonrel(
			SymbolicExpression expression,
			ProgramPoint pp)
			throws SemanticException;

	/**
	 * Yields the top non-relational abstract value of this oracle's value
	 * domain.
	 *
	 * @return the top value
	 */
	NonRelationalValue<?> nonrelTop();

	/**
	 * Yields the bottom non-relational abstract value of this oracle's value
	 * domain.
	 *
	 * @return the bottom value
	 */
	NonRelationalValue<?> nonrelBottom();
}

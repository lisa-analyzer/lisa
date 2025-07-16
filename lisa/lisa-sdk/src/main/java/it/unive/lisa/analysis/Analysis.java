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
 * An analysis that wraps a {@link SemanticDomain} of choice and provides a set
 * of semantic operations over it. This is effectively a {@link SemanticDomain}
 * that operates on {@link AnalysisState}s, that adds direct callbacks to
 * arbitrary assignements, expression rewriting, etc.
 * 
 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
 * 
 * @param <A> the kind of {@link AbstractLattice} produced by the domain
 *                {@code D}
 * @param <D> the kind of {@link AbstractDomain} to run during the analysis
 */
public class Analysis<A extends AbstractLattice<A>,
		D extends AbstractDomain<A>>
		implements
		SemanticDomain<AnalysisState<A>, AnalysisState<A>, SymbolicExpression, Identifier> {

	/**
	 * The domain to be executed.
	 */
	public final D domain;

	/**
	 * Builds the analysis, wrapping the given domain.
	 * 
	 * @param domain the domain to wrap
	 */
	public Analysis(
			D domain) {
		this.domain = domain;
	}

	@Override
	public AnalysisState<A> assign(
			AnalysisState<A> state,
			Identifier id,
			SymbolicExpression value,
			ProgramPoint pp)
			throws SemanticException {
		A s = domain.assign(state.getState(), id, value, pp);
		return new AnalysisState<>(s, new ExpressionSet(id), state.getFixpointInformation());
	}

	/**
	 * Yields a copy of this analysis state, where the symbolic expression
	 * {@code id} has been assigned to {@code value}: if {@code id} is not an
	 * {@code Identifier}, then it is rewritten before performing the
	 * assignment.
	 * 
	 * @param state      the current analysis state
	 * @param id         the symbolic expression to be assigned
	 * @param expression the expression to assign
	 * @param pp         the program point that where this operation is being
	 *                       evaluated
	 * 
	 * @return a copy of this analysis state, modified by the assignment
	 * 
	 * @throws SemanticException if an error occurs during the computation
	 */
	public AnalysisState<A> assign(
			AnalysisState<A> state,
			SymbolicExpression id,
			SymbolicExpression expression,
			ProgramPoint pp)
			throws SemanticException {
		if (id instanceof Identifier)
			return assign(state, (Identifier) id, expression, pp);

		A s = state.getState().bottom();
		AnalysisState<A> sem = smallStepSemantics(state, id, pp);
		SemanticOracle oracle = domain.makeOracle(state.getState());
		ExpressionSet rewritten = oracle.rewrite(id, pp);
		for (SymbolicExpression i : rewritten)
			if (!(i instanceof Identifier))
				throw new SemanticException(
						"Rewriting '" + id + "' did not produce an identifier: " + i);
			else
				s = s.lub(domain.assign(sem.getState(), (Identifier) i, expression, pp));
		return new AnalysisState<>(s, rewritten, state.getFixpointInformation());
	}

	@Override
	public AnalysisState<A> smallStepSemantics(
			AnalysisState<A> state,
			SymbolicExpression expression,
			ProgramPoint pp)
			throws SemanticException {
		A s = domain.smallStepSemantics(state.getState(), expression, pp);
		return new AnalysisState<>(s, new ExpressionSet(expression), state.getFixpointInformation());
	}

	@Override
	public AnalysisState<A> assume(
			AnalysisState<A> state,
			SymbolicExpression expression,
			ProgramPoint src,
			ProgramPoint dest)
			throws SemanticException {
		A assume = domain.assume(state.getState(), expression, src, dest);
		if (assume.isBottom())
			return state.bottom();
		return new AnalysisState<>(assume, state.getComputedExpressions(), state.getFixpointInformation());
	}

	@Override
	public Satisfiability satisfies(
			AnalysisState<A> state,
			SymbolicExpression expression,
			ProgramPoint pp)
			throws SemanticException {
		return domain.satisfies(state.getState(), expression, pp);
	}

	/**
	 * Yields the runtime types that this analysis infers for the given
	 * expression.
	 * 
	 * @param state the current analysis state
	 * @param e     the expression to type
	 * @param pp    the program point where the types are required
	 * 
	 * @return the runtime types
	 * 
	 * @throws SemanticException if something goes wrong during the computation
	 */
	public Set<Type> getRuntimeTypesOf(
			AnalysisState<A> state,
			SymbolicExpression e,
			ProgramPoint pp)
			throws SemanticException {
		SemanticOracle oracle = domain.makeOracle(state.getState());
		return oracle.getRuntimeTypesOf(e, pp);
	}

	/**
	 * Yields the dynamic type that this analysis infers for the given
	 * expression. The dynamic type is the least common supertype of all its
	 * runtime types.
	 * 
	 * @param state the current analysis state
	 * @param e     the expression to type
	 * @param pp    the program point where the types are required
	 * 
	 * @return the dynamic type
	 * 
	 * @throws SemanticException if something goes wrong during the computation
	 */
	public Type getDynamicTypeOf(
			AnalysisState<A> state,
			SymbolicExpression e,
			ProgramPoint pp)
			throws SemanticException {
		SemanticOracle oracle = domain.makeOracle(state.getState());
		return oracle.getDynamicTypeOf(e, pp);
	}

	/**
	 * Rewrites the given expression to a simpler form containing no sub
	 * expressions regarding the heap (that is, {@link HeapExpression}s). Every
	 * expression contained in the result can be safely cast to
	 * {@link ValueExpression}.
	 * 
	 * @param state      the current analysis state
	 * @param expression the expression to rewrite
	 * @param pp         the program point where the rewrite happens
	 * 
	 * @return the rewritten expressions
	 * 
	 * @throws SemanticException if something goes wrong while rewriting
	 */
	public ExpressionSet rewrite(
			AnalysisState<A> state,
			SymbolicExpression expression,
			ProgramPoint pp)
			throws SemanticException {
		SemanticOracle oracle = domain.makeOracle(state.getState());
		return oracle.rewrite(expression, pp);
	}

	/**
	 * Rewrites the given expressions to a simpler form containing no sub
	 * expressions regarding the heap (that is, {@link HeapExpression}s). Every
	 * expression contained in the result can be safely cast to
	 * {@link ValueExpression}.
	 * 
	 * @param state       the current analysis state
	 * @param expressions the expressions to rewrite
	 * @param pp          the program point where the rewrite happens
	 * 
	 * @return the rewritten expressions
	 * 
	 * @throws SemanticException if something goes wrong while rewriting
	 */
	public ExpressionSet rewrite(
			AnalysisState<A> state,
			ExpressionSet expressions,
			ProgramPoint pp)
			throws SemanticException {
		SemanticOracle oracle = domain.makeOracle(state.getState());
		return oracle.rewrite(expressions, pp);
	}

	/**
	 * Yields whether or not the two given expressions are aliases, that is, if
	 * they point to the same region of memory. Note that, for this method to
	 * return {@link Satisfiability#SATISFIED}, both expressions should be
	 * pointers to other expressions.
	 * 
	 * @param state the current analysis state
	 * @param x     the first expression
	 * @param y     the second expression
	 * @param pp    the {@link ProgramPoint} where the computation happens
	 * 
	 * @return whether or not the two expressions are aliases
	 * 
	 * @throws SemanticException if something goes wrong during the computation
	 */
	public Satisfiability alias(
			AnalysisState<A> state,
			SymbolicExpression x,
			SymbolicExpression y,
			ProgramPoint pp)
			throws SemanticException {
		SemanticOracle oracle = domain.makeOracle(state.getState());
		return oracle.alias(x, y, pp);
	}

	/**
	 * Yields all the {@link Identifier}s that are reachable starting from the
	 * {@link Identifier} represented (directly or after rewriting) by the given
	 * expression. This corresponds to recursively explore the memory region
	 * reachable by {@code e}, traversing all possible pointers until no more
	 * are available.
	 * 
	 * @param state the current analysis state
	 * @param e     the expression corresponding to the starting point
	 * @param pp    the {@link ProgramPoint} where the computation happens
	 * 
	 * @return the expressions representing memory regions reachable from
	 *             {@code e}
	 * 
	 * @throws SemanticException if something goes wrong during the computation
	 */
	public ExpressionSet reachableFrom(
			AnalysisState<A> state,
			SymbolicExpression e,
			ProgramPoint pp)
			throws SemanticException {
		SemanticOracle oracle = domain.makeOracle(state.getState());
		return oracle.reachableFrom(e, pp);
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
	 * @param state the current analysis state
	 * @param x     the first expression
	 * @param y     the second expression
	 * @param pp    the {@link ProgramPoint} where the computation happens
	 * 
	 * @return whether or not the second expression can be reached from the
	 *             first one
	 * 
	 * @throws SemanticException if something goes wrong during the computation
	 */
	public Satisfiability isReachableFrom(
			AnalysisState<A> state,
			SymbolicExpression x,
			SymbolicExpression y,
			ProgramPoint pp)
			throws SemanticException {
		SemanticOracle oracle = domain.makeOracle(state.getState());
		return oracle.isReachableFrom(x, y, pp);
	}

	/**
	 * Yields whether or not the {@link Identifier} represented (directly or
	 * after rewriting) by the second expression is reachable starting from the
	 * {@link Identifier} represented (directly or after rewriting) by the first
	 * expression, and vice versa. This is equivalent to invoking
	 * {@code isReachableFrom(x, y, pp, oracle).and(isReachableFrom(y, x, pp, oracle))},
	 * that corresponds to the default implementation of this method.
	 * 
	 * @param state the current analysis state
	 * @param x     the first expression
	 * @param y     the second expression
	 * @param pp    the {@link ProgramPoint} where the computation happens
	 * 
	 * @return whether or not the two expressions are mutually reachable
	 * 
	 * @throws SemanticException if something goes wrong during the computation
	 */
	public Satisfiability areMutuallyReachable(
			AnalysisState<A> state,
			SymbolicExpression x,
			SymbolicExpression y,
			ProgramPoint pp)
			throws SemanticException {
		SemanticOracle oracle = domain.makeOracle(state.getState());
		return oracle.areMutuallyReachable(x, y, pp);
	}

	@Override
	public AnalysisState<A> makeLattice() {
		return new AnalysisState<>(domain.makeLattice(), new ExpressionSet(), new FixpointInfo());
	}

}

package it.unive.lisa.analysis;

import it.unive.lisa.analysis.continuations.Continuation;
import it.unive.lisa.analysis.continuations.Exception;
import it.unive.lisa.analysis.continuations.Exceptions;
import it.unive.lisa.analysis.continuations.Execution;
import it.unive.lisa.analysis.lattices.ExpressionSet;
import it.unive.lisa.analysis.lattices.Satisfiability;
import it.unive.lisa.conf.LiSAConfiguration;
import it.unive.lisa.program.SyntheticLocation;
import it.unive.lisa.program.cfg.CFG;
import it.unive.lisa.program.cfg.ProgramPoint;
import it.unive.lisa.program.cfg.edge.Edge;
import it.unive.lisa.program.cfg.edge.ErrorEdge;
import it.unive.lisa.program.cfg.protection.ProtectedBlock;
import it.unive.lisa.program.cfg.statement.Expression;
import it.unive.lisa.program.cfg.statement.Statement;
import it.unive.lisa.program.cfg.statement.VariableRef;
import it.unive.lisa.program.cfg.statement.call.Call;
import it.unive.lisa.symbolic.SymbolicExpression;
import it.unive.lisa.symbolic.heap.HeapExpression;
import it.unive.lisa.symbolic.value.Identifier;
import it.unive.lisa.symbolic.value.PushAny;
import it.unive.lisa.symbolic.value.Skip;
import it.unive.lisa.symbolic.value.ValueExpression;
import it.unive.lisa.symbolic.value.Variable;
import it.unive.lisa.type.Type;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;
import java.util.function.BiPredicate;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.apache.commons.lang3.tuple.Pair;

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
public class Analysis<A extends AbstractLattice<A>, D extends AbstractDomain<A>>
		implements
		SemanticDomain<AnalysisState<A>, AnalysisState<A>, SymbolicExpression, Identifier> {

	/**
	 * The domain to be executed.
	 */
	public final D domain;

	private final Predicate<Type> shouldSmashException;

	/**
	 * Builds the analysis, wrapping the given domain.
	 * 
	 * @param domain the domain to wrap
	 */
	public Analysis(
			D domain) {
		this(domain, null);
	}

	/**
	 * Builds the analysis, wrapping the given domain.
	 * 
	 * @param domain               the domain to wrap
	 * @param shouldSmashException a predicate that, if non-null, is used to
	 *                                 determine whether to smash exceptional
	 *                                 continuations or keep them separate in
	 *                                 the state
	 */
	public Analysis(
			D domain,
			Predicate<Type> shouldSmashException) {
		this.domain = domain;
		this.shouldSmashException = shouldSmashException;
	}

	@Override
	public AnalysisState<A> assign(
			AnalysisState<A> state,
			Identifier id,
			SymbolicExpression value,
			ProgramPoint pp)
			throws SemanticException {
		A s = domain.assign(state.getExecutionState(), id, value, pp);
		return state.withExecutionState(new ProgramState<>(s, new ExpressionSet(id), state.getExecutionInformation()));
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

		A s = state.getExecutionState().bottom();
		AnalysisState<A> sem = smallStepSemantics(state, id, pp);
		SemanticOracle oracle = domain.makeOracle(state.getExecutionState());
		ExpressionSet rewritten = oracle.rewrite(id, pp);
		for (SymbolicExpression i : rewritten)
			if (!(i instanceof Identifier))
				throw new SemanticException("Rewriting '" + id + "' did not produce an identifier: " + i);
			else
				s = s.lub(domain.assign(sem.getExecutionState(), (Identifier) i, expression, pp));
		return state.withExecutionState(new ProgramState<>(s, rewritten, state.getExecutionInformation()));
	}

	@Override
	public AnalysisState<A> smallStepSemantics(
			AnalysisState<A> state,
			SymbolicExpression expression,
			ProgramPoint pp)
			throws SemanticException {
		A s = domain.smallStepSemantics(state.getExecutionState(), expression, pp);
		return state
				.withExecutionState(
						new ProgramState<>(s, new ExpressionSet(expression), state.getExecutionInformation()));
	}

	@Override
	public AnalysisState<A> assume(
			AnalysisState<A> state,
			SymbolicExpression expression,
			ProgramPoint src,
			ProgramPoint dest)
			throws SemanticException {
		A assume = domain.assume(state.getExecutionState(), expression, src, dest);
		if (assume.isBottom())
			return state.bottom();
		return state.withExecutionState(
				new ProgramState<>(assume, state.getExecutionExpressions(), state.getExecutionInformation()));
	}

	@Override
	public Satisfiability satisfies(
			AnalysisState<A> state,
			SymbolicExpression expression,
			ProgramPoint pp)
			throws SemanticException {
		return domain.satisfies(state.getExecutionState(), expression, pp);
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
		SemanticOracle oracle = domain.makeOracle(state.getExecutionState());
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
		SemanticOracle oracle = domain.makeOracle(state.getExecutionState());
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
		SemanticOracle oracle = domain.makeOracle(state.getExecutionState());
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
		SemanticOracle oracle = domain.makeOracle(state.getExecutionState());
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
		SemanticOracle oracle = domain.makeOracle(state.getExecutionState());
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
		SemanticOracle oracle = domain.makeOracle(state.getExecutionState());
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
		SemanticOracle oracle = domain.makeOracle(state.getExecutionState());
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
		SemanticOracle oracle = domain.makeOracle(state.getExecutionState());
		return oracle.areMutuallyReachable(x, y, pp);
	}

	@Override
	public AnalysisState<A> makeLattice() {
		return new AnalysisState<>(new ProgramState<>(domain.makeLattice(), new ExpressionSet(), new FixpointInfo()));
	}

	/**
	 * Sets the execution state to {@link ProgramState#bottom()} after moving
	 * the current execution state to the given exception continuation. If a
	 * state already exists for the given exception continuation, it is merged
	 * with the current state. Moreover, if the given exception should be
	 * smashed (see {@link LiSAConfiguration#shouldSmashException}), the state
	 * is set/merged with the {@link Exceptions} continuation instead.
	 * 
	 * @param state     the current analysis state
	 * @param exception the exception to move the execution state to
	 * 
	 * @return the updated analysis state
	 * 
	 * @throws SemanticException if something goes wrong during the computation
	 */
	public AnalysisState<A> moveExecutionToError(
			AnalysisState<A> state,
			Exception exception)
			throws SemanticException {
		if (state.isBottom())
			return state;
		ProgramState<A> exec = state.getState(Execution.INSTANCE);
		if (exec.isBottom())
			return state;
		AnalysisState<A> result = state.putState(Execution.INSTANCE, exec.bottom());

		if (shouldSmashException == null || !shouldSmashException.test(exception.getType())) {
			ProgramState<A> exc = state.getState(exception);
			return result.putState(exception, exec.lub(exc));
		}

		Optional<Exceptions> excs = state.getKeys().stream().filter(Exceptions.class::isInstance)
				.map(Exceptions.class::cast).findFirst();
		if (excs.isEmpty()) {
			Exceptions exceptions = new Exceptions(exception.getType(), exception.getThrower());
			return result.putState(exceptions, exec);
		} else {
			Exceptions exceptions = excs.get();
			ProgramState<A> exc = state.getState(exceptions);
			Exceptions newExceptions = exceptions.add(exception.getType(), exception.getThrower());
			return result.putState(newExceptions, exec.lub(exc));
		}
	}

	/**
	 * Moves the states corresponding to the given errors to the execution
	 * state. This corresponds to collecting all states for {@link Exception}s
	 * that (i) happened inside {@code protectedBlock}, and (ii) are subtypes of
	 * a type in {@code targets} but not of a type in {@code excluded}, removing
	 * them from the current state. The lub of all corresponding states is
	 * placed as the state for the {@link Execution} continuation, discarding
	 * the state currently associated with it. The {@link Exceptions}
	 * continuation is also updated by removing the caught exceptions, and its
	 * state is taken into the lub if at least one exception is removed. All
	 * exception(s) not matching the given targets are removed from the
	 * resulting state.
	 * 
	 * @param state          the current analysis state
	 * @param protectedBlock the block that is being protected from the errors
	 *                           to move
	 * @param targets        the types to move
	 * @param excluded       the types to exclude
	 * @param variable       the variable to assign the exception to (can be
	 *                           {@code null})
	 * 
	 * @return the updated analysis state
	 * 
	 * @throws SemanticException if something goes wrong during the computation
	 */
	public AnalysisState<A> moveErrorsToExecution(
			AnalysisState<A> state,
			ProtectedBlock protectedBlock,
			Collection<Type> targets,
			Collection<Type> excluded,
			VariableRef variable)
			throws SemanticException {
		if (state.isBottom() || state.isTop() || state.function == null || state.function.isEmpty())
			return state;

		Type varType = variable == null ? null : variable.getStaticType();
		ProgramState<A> result = state.lattice.bottom();
		Map<Continuation, ProgramState<A>> function = state.mkNewFunction(null, false);
		Collection<SymbolicExpression> excs = new HashSet<>();

		// either the type is excluded (precisely or through one of
		// its super types) or is is not caught (precisely or
		// through one of its super types)
		Predicate<Type> isCaught = t -> targets.stream().anyMatch(target -> t.canBeAssignedTo(target))
				&& excluded.stream().noneMatch(ex -> t.canBeAssignedTo(ex));
		Predicate<Statement> isProtected = st -> st instanceof Expression
				? protectedBlock.getBody().contains(((Expression) st).getRootStatement())
				: protectedBlock.getBody().contains(st);

		for (Entry<Continuation, ProgramState<A>> entry : state)
			if (entry.getKey() instanceof Exception) {
				Exception cont = (Exception) entry.getKey();
				if (!isCaught.test(cont.getType()) || !isProtected.test(cont.getThrower()))
					continue;
				result = result.lub(entry.getValue());
				if (variable != null)
					for (SymbolicExpression e : entry.getValue().getComputedExpressions())
						if (e.getStaticType().canBeAssignedTo(varType))
							excs.add(e);
			} else if (entry.getKey() instanceof Exceptions) {
				Exceptions cont = (Exceptions) entry.getKey();
				Map<Type, Set<Statement>> caught = new HashMap<>();
				for (Entry<Type, Set<Statement>> ex : cont.getTypes().entrySet())
					if (isCaught.test(ex.getKey())) {
						Set<Statement> caughtThrowers = new HashSet<>();
						for (Statement thrower : ex.getValue())
							if (isProtected.test(thrower))
								caughtThrowers.add(thrower);
						if (!caughtThrowers.isEmpty())
							caught.put(ex.getKey(), caughtThrowers);
					}

				if (!caught.isEmpty()) {
					result = result.lub(entry.getValue());
					if (variable != null)
						for (SymbolicExpression e : entry.getValue().getComputedExpressions())
							if (e.getStaticType().canBeAssignedTo(varType))
								excs.add(e);
				}
			}

		if (result.isBottom())
			// nothing to catch, result should be bottom
			return state.bottom();

		A start = result.getState();
		A moved = start;
		if (variable != null) {
			A assigned = start.bottom();
			Variable target = variable.getVariable();
			for (SymbolicExpression e : excs)
				assigned = assigned.lub(domain.assign(start, target, e, variable));
			moved = assigned;
			if (moved.isBottom()) {
				// no exceptions have been assigned to the variable:
				// this happens when the catch is in a different cfg
				// TODO for now, we just set the exception to top
				moved = domain.assign(start, target, new PushAny(varType, variable.getLocation()), variable);
			}
		}

		function.put(Execution.INSTANCE, new ProgramState<>(
				moved,
				variable == null ? new Skip(SyntheticLocation.INSTANCE) : variable.getVariable()));
		return new AnalysisState<>(state.lattice, function);
	}

	/**
	 * For all exceptional continuations in the given state, move all throwers
	 * that are within {@code origin} to the given target. This is useful when
	 * returning control to the caller of {@code origin}, as exceptions that are
	 * not caught within origin are transferred from their original thrower to
	 * the call, so that they can be caught by the appropriate protection
	 * blocks.
	 * 
	 * @param state  the state to operate on
	 * @param target the target to transfer the exceptional continuations to
	 * @param origin the cfg containing the original throwers
	 * 
	 * @return a new state with the updated exceptional continuations
	 */
	public AnalysisState<A> transferThrowers(
			AnalysisState<A> state,
			Statement target,
			CFG origin) {
		if (state.isBottom() || state.isTop() || state.function == null || state.function.isEmpty())
			return state;

		if (target instanceof Call)
			target = ((Call) target).getSource();

		Map<Continuation, ProgramState<A>> newFunction = new HashMap<>();
		for (Entry<Continuation, ProgramState<A>> entry : state) {
			Continuation cont = entry.getKey();
			ProgramState<A> contState = entry.getValue();

			if (cont instanceof Exception) {
				Exception ex = (Exception) cont;
				Statement thrower = ex.getThrower();
				if (thrower instanceof Expression)
					thrower = ((Expression) thrower).getRootStatement();

				if (origin.containsNode(thrower)) {
					Exception newEx = ex.withThrower(target);
					newFunction.put(newEx, contState);
				} else
					newFunction.put(cont, contState);
			} else if (cont instanceof Exceptions) {
				Exceptions exs = (Exceptions) cont;
				Map<Type, Set<Statement>> toRemove = new HashMap<>();
				for (Entry<Type, Set<Statement>> ex : exs.getTypes().entrySet())
					for (Statement st : ex.getValue()) {
						Statement thrower = st;
						if (thrower instanceof Expression)
							thrower = ((Expression) thrower).getRootStatement();
						if (origin.containsNode(thrower))
							toRemove.computeIfAbsent(ex.getKey(), k -> new HashSet<>()).add(st);
					}

				if (toRemove.isEmpty())
					newFunction.put(cont, contState);
				else {
					Exceptions newExs = exs.removeAll(toRemove);
					for (Type t : toRemove.keySet())
						newExs = newExs.add(t, target);
					newFunction.put(newExs, contState);
				}
			} else
				newFunction.put(cont, contState);
		}

		return new AnalysisState<>(state.lattice, newFunction);
	}

	/**
	 * Yields a new state assuming that all errors present in the given one are
	 * caught by outgoing edges from {@code source}.
	 * 
	 * @param state  the state to clean
	 * @param source the statement from which to catch errors
	 * 
	 * @return the cleaned state
	 */
	public AnalysisState<A> removeCaughtErrors(
			AnalysisState<A> state,
			Statement source) {
		if (state.isBottom() || state.isTop() || state.function == null || state.function.isEmpty())
			return state;

		Set<Pair<Type, ProtectedBlock>> caught = source.getCFG().getOutgoingEdges(source)
				.stream()
				.filter(Edge::isErrorHandling)
				.map(ErrorEdge.class::cast)
				.flatMap(e -> Stream.of(e.getTypes()).map(t -> Pair.of(t, e.getProtectedBlock())))
				.collect(Collectors.toSet());

		if (caught.isEmpty())
			return state;

		Predicate<Type> isCaught = t -> caught.stream().anyMatch(target -> t.canBeAssignedTo(target.getLeft()));
		BiPredicate<Statement, ProtectedBlock> aux = (
				st,
				pb) -> st instanceof Expression ? pb.getBody().contains(((Expression) st).getRootStatement())
						: pb.getBody().contains(st);
		Predicate<Statement> isProtected = st -> caught.stream().anyMatch(target -> aux.test(st, target.getRight()));

		Map<Continuation, ProgramState<A>> newFunction = new HashMap<>();
		for (Entry<Continuation, ProgramState<A>> entry : state) {
			if (entry.getKey() instanceof Exception) {
				Exception cont = (Exception) entry.getKey();
				if (isCaught.test(cont.getType()) && isProtected.test(cont.getThrower()))
					continue;
				newFunction.put(cont, entry.getValue());
			} else if (entry.getKey() instanceof Exceptions) {
				Exceptions cont = (Exceptions) entry.getKey();
				Map<Type, Set<Statement>> caughtSet = new HashMap<>();
				for (Entry<Type, Set<Statement>> ex : cont.getTypes().entrySet())
					if (isCaught.test(ex.getKey())) {
						Set<Statement> caughtThrowers = new HashSet<>();
						for (Statement thrower : ex.getValue())
							if (isProtected.test(thrower))
								caughtThrowers.add(thrower);
						if (!caughtThrowers.isEmpty())
							caughtSet.put(ex.getKey(), caughtThrowers);
					}

				if (!caughtSet.isEmpty()) {
					Exceptions newCont = cont.removeAll(caughtSet);
					if (!newCont.getTypes().isEmpty())
						// put back the remaining exceptions
						newFunction.put(newCont, entry.getValue());
				}
			} else
				newFunction.put(entry.getKey(), entry.getValue());
		}

		return new AnalysisState<>(state.lattice, newFunction);
	}

	/**
	 * Yields a new state with all errors removed, containing only the
	 * {@link Execution} continuation.
	 * 
	 * @param state the state to clean
	 * 
	 * @return the cleaned state
	 */
	public AnalysisState<A> removeAllErrors(
			AnalysisState<A> state) {
		if (state.isBottom() || state.isTop() || state.function == null || state.function.isEmpty())
			return state;

		return state.bottom().putState(Execution.INSTANCE, state.getState(Execution.INSTANCE));
	}

	/**
	 * Yields a new state corresponding to {@code target} augmented with all the
	 * non-normal continuations (i.e., different from {@link Execution}) that
	 * are present in {@code source}.
	 * 
	 * @param target the target state where the continuations should be merged
	 * @param source the source state where to take the continuations from
	 * 
	 * @return the new merged state
	 * 
	 * @throws SemanticException if something goes wrong during the computation
	 */
	public AnalysisState<A> mergeErrors(
			AnalysisState<A> target,
			AnalysisState<A> source)
			throws SemanticException {
		if (source.isBottom() || source.isTop() || source.function == null || source.function.isEmpty())
			return target;

		Map<Continuation, ProgramState<A>> newFunction = target.mkNewFunction(target.function, false);
		for (Entry<Continuation, ProgramState<A>> entry : source)
			if (entry.getKey() instanceof Exceptions) {
				Exceptions cont = (Exceptions) entry.getKey();
				Optional<Continuation> excs = newFunction.keySet()
						.stream()
						.filter(Exceptions.class::isInstance)
						.findAny();
				if (excs.isEmpty())
					newFunction.put(cont, entry.getValue());
				else {
					Exceptions existing = (Exceptions) excs.get();
					Exceptions newCont = existing.addAll(cont.getTypes());
					ProgramState<A> prev = newFunction.remove(existing);
					newFunction.put(newCont, prev.lub(entry.getValue()));
				}
			} else if (!(entry.getKey() instanceof Execution))
				if (newFunction.containsKey(entry.getKey()))
					newFunction.put(entry.getKey(), newFunction.get(entry.getKey()).lub(entry.getValue()));
				else
					newFunction.put(entry.getKey(), entry.getValue());

		return new AnalysisState<>(target.lattice, newFunction);
	}

}

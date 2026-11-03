package it.unive.lisa.triage.model;

import it.unive.lisa.analysis.AbstractDomain;
import it.unive.lisa.analysis.AbstractLattice;
import it.unive.lisa.analysis.AnalysisState;
import it.unive.lisa.analysis.AnalysisState.Error;
import it.unive.lisa.analysis.ProgramState;
import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.lattices.GenericMapLattice;
import it.unive.lisa.lattices.GenericSetLattice;
import it.unive.lisa.program.cfg.statement.Statement;
import it.unive.lisa.type.Type;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Utilities for building the states from which replays start.
 *
 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
 */
public final class ReplayStates {

	private ReplayStates() {
	}

	/**
	 * Lifts the given state, computed by the analysis, to a state of the given
	 * {@link ReplayDomain}: every continuation (normal execution, halting,
	 * errors) keeps its abstract state and its computed expressions, paired
	 * with a store representing the identity transition.
	 *
	 * @param <A>    the type of the abstract states
	 * @param <D>    the type of the domain of the analysis
	 * @param state  the state to lift
	 * @param domain the replay domain
	 *
	 * @return the lifted state
	 *
	 * @throws SemanticException if something goes wrong while building the
	 *                               state
	 */
	public static <A extends AbstractLattice<A>, D extends AbstractDomain<A>> AnalysisState<ReplayState<A>> lift(
			AnalysisState<A> state,
			ReplayDomain<A, D> domain)
			throws SemanticException {
		ProgramState<ReplayState<A>> execution = lift(state.getExecution(), domain);
		AnalysisState<ReplayState<A>> result = new AnalysisState<>(execution)
				.bottom()
				.withExecution(execution)
				.withHalt(lift(state.getHalt(), domain));

		GenericMapLattice<Error, ProgramState<A>> errors = state.getErrors();
		if (!errors.isBottom() && !errors.isTop())
			for (Error error : errors.getKeys())
				result = result.addError(error, lift(errors.getState(error), domain));

		GenericMapLattice<Type, GenericSetLattice<Statement>> smashed = state.getSmashedErrors();
		if (!smashed.isBottom() && !smashed.isTop() && !state.getSmashedErrorsState().isBottom()) {
			Map<Type, Set<Statement>> throwers = new HashMap<>();
			for (Type type : smashed.getKeys()) {
				Set<Statement> sts = new HashSet<>();
				for (Statement st : smashed.getState(type))
					sts.add(st);
				throwers.put(type, sts);
			}
			result = result.addSmashedErrors(throwers, lift(state.getSmashedErrorsState(), domain));
		}

		return result;
	}

	/**
	 * Lifts the normal execution of the given state, computed by the analysis,
	 * to a state of the given {@link ReplayDomain} whose store represents the
	 * identity transition. The halting and error continuations of the given
	 * state are dropped: they come from executions that have already halted or
	 * raised an error, and they are not part of the executions that continue
	 * from the state.
	 *
	 * @param <A>    the type of the abstract states
	 * @param <D>    the type of the domain of the analysis
	 * @param state  the state to lift
	 * @param domain the replay domain
	 *
	 * @return the lifted state
	 */
	public static <A extends AbstractLattice<A>, D extends AbstractDomain<A>> AnalysisState<ReplayState<A>> liftExecution(
			AnalysisState<A> state,
			ReplayDomain<A, D> domain) {
		ProgramState<ReplayState<A>> execution = lift(state.getExecution(), domain);
		return new AnalysisState<>(execution).bottom().withExecution(execution);
	}

	/**
	 * Lifts the given program state, computed by the analysis, to a program
	 * state of the given {@link ReplayDomain}, whose store represents the
	 * identity transition.
	 *
	 * @param <A>    the type of the abstract states
	 * @param <D>    the type of the domain of the analysis
	 * @param state  the state to lift
	 * @param domain the replay domain
	 *
	 * @return the lifted state
	 */
	public static <A extends AbstractLattice<A>, D extends AbstractDomain<A>> ProgramState<ReplayState<A>> lift(
			ProgramState<A> state,
			ReplayDomain<A, D> domain) {
		return new ProgramState<>(
				domain.lift(state.getState()),
				state.getComputedExpressions(),
				state.getFixpointInformation());
	}
}

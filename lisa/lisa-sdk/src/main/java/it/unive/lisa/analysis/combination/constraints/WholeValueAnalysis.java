package it.unive.lisa.analysis.combination.constraints;

import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.analysis.SemanticOracle;
import it.unive.lisa.analysis.combination.constraints.events.WholeValueConstraintsEnd;
import it.unive.lisa.analysis.combination.constraints.events.WholeValueConstraintsStart;
import it.unive.lisa.analysis.value.ValueDomain;
import it.unive.lisa.analysis.value.ValueLattice;
import it.unive.lisa.events.EventQueue;
import it.unive.lisa.lattices.Satisfiability;
import it.unive.lisa.program.cfg.ProgramPoint;
import it.unive.lisa.symbolic.value.BinaryExpression;
import it.unive.lisa.symbolic.value.Identifier;
import it.unive.lisa.symbolic.value.ValueExpression;
import java.util.HashSet;
import java.util.Set;

/**
 * The constraint-based whole-value analysis among an arbitrary number of client
 * abstractions as defined in <a href=
 * "https://www.frontiersin.org/journals/computer-science/articles/10.3389/fcomp.2025.1655377/full">"Whole-value
 * analysis by abstract interpretation" by Luca Negrini</a>. This analysis
 * forwards each expression to be evaluated to all the domains that can handle
 * it, according to
 * {@link ValueDomain#canProcess(ValueExpression, ProgramPoint, SemanticOracle)}.
 * Also, the class will insert itself into the {@link SemanticOracle} so that
 * client analyses can ask it to generate constraints for any expression and to
 * evaluate them.<br/>
 * <br/>
 * The main difference between the LiSA implementation of this analysis and the
 * one defined in the paper is that the generator function {@code G} is absent
 * from the implementation: instead, the constraints are interpreted directly in
 * the abstract transformers that ask for them.
 * 
 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
 */
public class WholeValueAnalysis
		implements
		ValueDomain<WholeValue> {

	/**
	 * The domains that take part in this whole-value analysis. The order of the
	 * participants is important, as it is used to index the {@link WholeValue}
	 * instances that are produced by this analysis.
	 */
	public final ValueDomain<?>[] participants;

	/**
	 * Builds a value with the given participants.
	 * 
	 * @param participants the participants of this value
	 */
	public WholeValueAnalysis(
			ValueDomain<?>... participants) {
		this.participants = participants;
	}

	/**
	 * Returns the participant of this value.
	 * 
	 * @return the participant of this value
	 */
	public ValueDomain<?>[] getParticipants() {
		return participants;
	}

	/**
	 * Returns the participant at the given index.
	 *
	 * @param i the index of the participant to return
	 * 
	 * @return the participant at the given index
	 */
	public ValueDomain<?> get(
			int i) {
		return participants[i];
	}

	/**
	 * Returns the participant at the given index, cast to the given type. If
	 * the participant at the given index is not of the given type, an exception
	 * is thrown.
	 *
	 * @param <T>   the type of the component to return
	 * @param <L>   the type of the lattice that the component works with
	 * @param i     the index of the participant to return
	 * @param clazz the class of the participant to return
	 * 
	 * @return the participant at the given index, cast to the given type
	 * 
	 * @throws SemanticException if the participant at the given index is not of
	 *                               the given type
	 */
	@SuppressWarnings("unchecked")
	public <L extends ValueLattice<L>, T extends ValueDomain<L>> T get(
			int i,
			Class<T> clazz)
			throws SemanticException {
		try {
			return (T) participants[i];
		} catch (ClassCastException e) {
			throw new SemanticException("Participant at index " + i + " is not of type " + clazz.getName());
		}
	}

	/**
	 * Returns the first participant of the given type. If multiple participant
	 * of the same type are present, only the first one is returned. If no
	 * participant of the given type is present, an exception is thrown.
	 *
	 * @param <T>   the type of the component to return
	 * @param <L>   the type of the lattice that the component works with
	 * @param clazz the class of the participant to return
	 * 
	 * @return the first participant of the given type
	 * 
	 * @throws SemanticException if no participant of the given type is present
	 */
	public <L extends ValueLattice<L>, T extends ValueDomain<L>> T get(
			Class<T> clazz)
			throws SemanticException {
		for (ValueDomain<?> p : participants)
			if (clazz.isInstance(p))
				return clazz.cast(p);
		throw new SemanticException("No participant of type " + clazz.getName() + " found");
	}

	@Override
	@SuppressWarnings({ "rawtypes", "unchecked" })
	public WholeValue assign(
			WholeValue state,
			Identifier id,
			ValueExpression expression,
			ProgramPoint pp,
			SemanticOracle oracle)
			throws SemanticException {
		ValueLattice<?>[] lattices = new ValueLattice<?>[participants.length];
		for (int i = 0; i < participants.length; i++)
			if (participants[i].canProcess(expression, pp, oracle))
				lattices[i] = (ValueLattice<?>) ((ValueDomain) participants[i]).assign(
						state.get(i),
						id,
						expression,
						pp,
						oracle);
			else
				lattices[i] = state.get(i);
		return new WholeValue(lattices);
	}

	@Override
	@SuppressWarnings({ "rawtypes", "unchecked" })
	public WholeValue smallStepSemantics(
			WholeValue state,
			ValueExpression expression,
			ProgramPoint pp,
			SemanticOracle oracle)
			throws SemanticException {
		ValueLattice<?>[] lattices = new ValueLattice<?>[participants.length];
		for (int i = 0; i < participants.length; i++)
			if (participants[i].canProcess(expression, pp, oracle))
				lattices[i] = (ValueLattice<?>) ((ValueDomain) participants[i]).smallStepSemantics(
						state.get(i),
						expression,
						pp,
						oracle);
			else
				lattices[i] = state.get(i);
		return new WholeValue(lattices);
	}

	@Override
	@SuppressWarnings({ "rawtypes", "unchecked" })
	public Satisfiability satisfies(
			WholeValue state,
			ValueExpression expression,
			ProgramPoint pp,
			SemanticOracle oracle)
			throws SemanticException {
		Satisfiability result = null;
		for (int i = 0; i < participants.length; i++) {
			Satisfiability res = ((ValueDomain) participants[i]).satisfies(
					state.get(i),
					expression,
					pp,
					oracle);
			if (res == Satisfiability.BOTTOM)
				return Satisfiability.BOTTOM;
			else
				result = result == null ? res : result.glb(res);
		}
		return result == null ? Satisfiability.UNKNOWN : result;
	}

	@Override
	@SuppressWarnings({ "rawtypes", "unchecked" })
	public WholeValue assume(
			WholeValue state,
			ValueExpression expression,
			ProgramPoint src,
			ProgramPoint dest,
			SemanticOracle oracle)
			throws SemanticException {
		ValueLattice<?>[] lattices = new ValueLattice<?>[participants.length];
		for (int i = 0; i < participants.length; i++) {
			lattices[i] = (ValueLattice<?>) ((ValueDomain) participants[i]).assume(
					state.get(i),
					expression,
					src,
					dest,
					oracle);
			if (lattices[i].isBottom())
				return state.bottom();
		}
		return new WholeValue(lattices);
	}

	@Override
	@SuppressWarnings({ "rawtypes", "unchecked" })
	public WholeValue onCallReturn(
			WholeValue entryState,
			WholeValue callres,
			ProgramPoint call)
			throws SemanticException {
		ValueLattice<?>[] lattices = new ValueLattice<?>[participants.length];
		for (int i = 0; i < participants.length; i++)
			lattices[i] = (ValueLattice<?>) ((ValueDomain) participants[i]).onCallReturn(entryState.get(i),
					callres.get(i), call);
		return new WholeValue(lattices);
	}

	@Override
	public WholeValue makeLattice() {
		ValueLattice<?>[] lattices = new ValueLattice<?>[participants.length];
		for (int i = 0; i < participants.length; i++)
			lattices[i] = participants[i].makeLattice();
		return new WholeValue(lattices);
	}

	@Override
	public boolean canProcess(
			ValueExpression e,
			ProgramPoint pp,
			SemanticOracle oracle) {
		for (ValueDomain<?> p : participants)
			if (p.canProcess(e, pp, oracle))
				return true;
		return false;
	}

	@Override
	@SuppressWarnings({ "rawtypes", "unchecked" })
	public Set<BinaryExpression> constraints(
			ValueDomain<?> requesting,
			WholeValue state,
			ValueExpression e,
			ProgramPoint pp,
			SemanticOracle oracle)
			throws SemanticException {
		EventQueue events = oracle.getEventQueue();
		if (events != null)
			events.post(new WholeValueConstraintsStart(state, e));
		Set<BinaryExpression> result = new HashSet<>();
		for (int i = 0; i < participants.length; i++)
			if (participants[i] != requesting) {
				Set<BinaryExpression> c = ((ValueDomain) participants[i]).constraints(
						requesting,
						state.get(i),
						e,
						pp,
						oracle);
				if (c != null)
					result.addAll(c);
			}
		if (events != null)
			events.post(new WholeValueConstraintsEnd(state, e, result));
		return result;
	}
}

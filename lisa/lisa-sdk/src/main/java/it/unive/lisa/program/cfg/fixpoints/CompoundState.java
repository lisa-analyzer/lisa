package it.unive.lisa.program.cfg.fixpoints;

import it.unive.lisa.analysis.AbstractDomain;
import it.unive.lisa.analysis.AbstractLattice;
import it.unive.lisa.analysis.AnalysisState;
import it.unive.lisa.analysis.Lattice;
import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.analysis.StatementStore;
import it.unive.lisa.program.cfg.statement.Statement;
import it.unive.lisa.util.representation.ListRepresentation;
import it.unive.lisa.util.representation.StructuredRepresentation;
import org.apache.commons.lang3.StringUtils;

/**
 * A compound state for a {@link Statement}, holding the post-state of the whole
 * statement as well as the ones of the inner expressions.
 * 
 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
 * 
 * @param <A> the type of {@link AbstractLattice} contained into the analysis
 *                state
 */
public final class CompoundState<A extends AbstractLattice<A>>
		implements
		Lattice<CompoundState<A>> {

	/**
	 * Builds a compound state from the given post-states.
	 * 
	 * @param <A>                the type of {@link AbstractDomain} contained
	 *                               into the analysis state
	 * @param postState          the overall post-state of a statement
	 * @param intermediateStates the post-state of intermediate expressions
	 * 
	 * @return the generated compound state
	 */
	public static <A extends AbstractLattice<A>> CompoundState<A> of(
			AnalysisState<A> postState,
			StatementStore<A> intermediateStates) {
		return new CompoundState<>(postState, intermediateStates);
	}

	/**
	 * The overall post-state of a statement.
	 */
	public final AnalysisState<A> postState;

	/**
	 * The post-state of intermediate expressions.
	 */
	public final StatementStore<A> intermediateStates;

	private CompoundState(
			AnalysisState<A> postState,
			StatementStore<A> intermediateStates) {
		this.postState = postState;
		this.intermediateStates = intermediateStates;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((intermediateStates == null) ? 0 : intermediateStates.hashCode());
		result = prime * result + ((postState == null) ? 0 : postState.hashCode());
		return result;
	}

	@Override
	public boolean equals(
			Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		CompoundState<?> other = (CompoundState<?>) obj;
		if (intermediateStates == null) {
			if (other.intermediateStates != null)
				return false;
		} else if (!intermediateStates.equals(other.intermediateStates))
			return false;
		if (postState == null) {
			if (other.postState != null)
				return false;
		} else if (!postState.equals(other.postState))
			return false;
		return true;
	}

	@Override
	public String toString() {
		return postState + "\n[" + StringUtils.join(intermediateStates, "\n") + "]";
	}

	@Override
	public boolean lessOrEqual(
			CompoundState<A> other)
			throws SemanticException {
		return postState.lessOrEqual(other.postState) && intermediateStates.lessOrEqual(other.intermediateStates);
	}

	@Override
	public CompoundState<A> lub(
			CompoundState<A> other)
			throws SemanticException {
		return CompoundState.of(postState.lub(other.postState), intermediateStates.lub(other.intermediateStates));
	}

	@Override
	public CompoundState<A> upchain(
			CompoundState<A> other)
			throws SemanticException {
		return CompoundState.of(postState.upchain(other.postState),
				intermediateStates.upchain(other.intermediateStates));
	}

	@Override
	public CompoundState<A> downchain(
			CompoundState<A> other)
			throws SemanticException {
		return CompoundState.of(postState.downchain(other.postState),
				intermediateStates.downchain(other.intermediateStates));
	}

	@Override
	public CompoundState<A> top() {
		return CompoundState.of(postState.top(), intermediateStates.top());
	}

	@Override
	public boolean isTop() {
		return postState.isTop() && intermediateStates.isTop();
	}

	@Override
	public CompoundState<A> bottom() {
		return CompoundState.of(postState.bottom(), intermediateStates.bottom());
	}

	@Override
	public boolean isBottom() {
		return postState.isBottom() && intermediateStates.isBottom();
	}

	@Override
	public CompoundState<A> glb(
			CompoundState<A> other)
			throws SemanticException {
		return CompoundState.of(postState.glb(other.postState), intermediateStates.glb(other.intermediateStates));
	}

	@Override
	public CompoundState<A> narrowing(
			CompoundState<A> other)
			throws SemanticException {
		return CompoundState
				.of(postState.narrowing(other.postState), intermediateStates.narrowing(other.intermediateStates));
	}

	@Override
	public CompoundState<A> widening(
			CompoundState<A> other)
			throws SemanticException {
		return CompoundState
				.of(postState.widening(other.postState), intermediateStates.widening(other.intermediateStates));
	}

	@Override
	public StructuredRepresentation representation() {
		return new ListRepresentation(postState.representation(), intermediateStates.representation());
	}

}

package it.unive.lisa.program.cfg.fixpoints;

import it.unive.lisa.analysis.AbstractState;
import it.unive.lisa.analysis.AnalysisState;
import it.unive.lisa.analysis.Lattice;
import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.analysis.StatementStore;
import it.unive.lisa.analysis.heap.HeapDomain;
import it.unive.lisa.analysis.value.TypeDomain;
import it.unive.lisa.analysis.value.ValueDomain;
import it.unive.lisa.interprocedural.InterproceduralAnalysis;
import it.unive.lisa.program.cfg.CFG;
import it.unive.lisa.program.cfg.VariableTableEntry;
import it.unive.lisa.program.cfg.edge.Edge;
import it.unive.lisa.program.cfg.statement.Expression;
import it.unive.lisa.program.cfg.statement.Statement;
import it.unive.lisa.symbolic.SymbolicExpression;
import it.unive.lisa.symbolic.value.Identifier;
import it.unive.lisa.util.datastructures.graph.algorithms.Fixpoint.FixpointImplementation;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import org.apache.commons.lang3.StringUtils;

/**
 * A {@link FixpointImplementation} for {@link CFG}s.
 * 
 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
 * 
 * @param <A> the type of {@link AbstractState} contained into the analysis
 *                state computed by the fixpoint
 * @param <H> the type of {@link HeapDomain} contained into the computed
 *                abstract state
 * @param <V> the type of {@link ValueDomain} contained into the computed
 *                abstract state
 * @param <T> the type of {@link TypeDomain} contained into the computed
 *                abstract state
 */
public abstract class CFGFixpoint<A extends AbstractState<A, H, V, T>,
		H extends HeapDomain<H>,
		V extends ValueDomain<V>,
		T extends TypeDomain<T>>
		implements FixpointImplementation<Statement, Edge, CFGFixpoint.CompoundState<A, H, V, T>> {

	/**
	 * The graph targeted by this implementation.
	 */
	protected final CFG graph;

	/**
	 * The {@link InterproceduralAnalysis} to use for semantics invocations.
	 */
	protected final InterproceduralAnalysis<A, H, V, T> interprocedural;

	/**
	 * Builds the fixpoint implementation.
	 * 
	 * @param graph           the graph targeted by this implementation
	 * @param interprocedural the {@link InterproceduralAnalysis} to use for
	 *                            semantics invocation
	 */
	public CFGFixpoint(CFG graph, InterproceduralAnalysis<A, H, V, T> interprocedural) {
		this.graph = graph;
		this.interprocedural = interprocedural;
	}

	@Override
	public CompoundState<A, H, V, T> semantics(Statement node,
			CompoundState<A, H, V, T> entrystate) throws SemanticException {
		StatementStore<A, H, V, T> expressions = new StatementStore<>(entrystate.postState.bottom());
		AnalysisState<A, H, V, T> approx = node.semantics(entrystate.postState, interprocedural, expressions);
		if (node instanceof Expression)
			approx = approx.forgetIdentifiers(((Expression) node).getMetaVariables());
		return CompoundState.of(approx, expressions);
	}

	@Override
	public CompoundState<A, H, V, T> traverse(Edge edge,
			CompoundState<A, H, V, T> entrystate) throws SemanticException {
		AnalysisState<A, H, V, T> approx = edge.traverse(entrystate.postState);

		// we remove out of scope variables here
		List<VariableTableEntry> toRemove = new LinkedList<>();
		for (VariableTableEntry entry : graph.getDescriptor().getVariables())
			if (entry.getScopeEnd() == edge.getSource())
				toRemove.add(entry);

		Collection<Identifier> ids = new LinkedList<>();
		for (VariableTableEntry entry : toRemove) {
			SymbolicExpression v = entry.createReference(graph).getVariable();
			for (SymbolicExpression expr : approx.smallStepSemantics(v, edge.getSource()).getComputedExpressions())
				ids.add((Identifier) expr);
		}

		if (!ids.isEmpty())
			approx = approx.forgetIdentifiers(ids);

		return CompoundState.of(approx, new StatementStore<>(approx.bottom()));
	}

	@Override
	public CompoundState<A, H, V, T> union(Statement node,
			CompoundState<A, H, V, T> left,
			CompoundState<A, H, V, T> right) throws SemanticException {
		return left.lub(right);
	}

	/**
	 * A compound state for a {@link Statement}, holding the post-state of the
	 * whole statement as well as the ones of the inner expressions.
	 * 
	 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
	 * 
	 * @param <A> the type of {@link AbstractState} contained into the analysis
	 *                state
	 * @param <H> the type of {@link HeapDomain} contained into the computed
	 *                abstract state
	 * @param <V> the type of {@link ValueDomain} contained into the computed
	 *                abstract state
	 * @param <T> the type of {@link TypeDomain} contained into the computed
	 *                abstract state
	 */
	public static final class CompoundState<A extends AbstractState<A, H, V, T>,
			H extends HeapDomain<H>,
			V extends ValueDomain<V>,
			T extends TypeDomain<T>> implements Lattice<CompoundState<A, H, V, T>> {

		/**
		 * Builds a compound state from the given post-states.
		 * 
		 * @param <A>                the type of {@link AbstractState} contained
		 *                               into the analysis state
		 * @param <H>                the type of {@link HeapDomain} contained
		 *                               into the computed abstract state
		 * @param <V>                the type of {@link ValueDomain} contained
		 *                               into the computed abstract state
		 * @param <T>                the type of {@link TypeDomain} contained
		 *                               into the computed abstract state
		 * @param postState          the overall post-state of a statement
		 * @param intermediateStates the post-state of intermediate expressions
		 * 
		 * @return the generated compound state
		 */
		public static <A extends AbstractState<A, H, V, T>,
				H extends HeapDomain<H>,
				V extends ValueDomain<V>,
				T extends TypeDomain<T>> CompoundState<A, H, V, T> of(
						AnalysisState<A, H, V, T> postState,
						StatementStore<A, H, V, T> intermediateStates) {
			return new CompoundState<>(postState, intermediateStates);
		}

		/**
		 * The overall post-state of a statement.
		 */
		public final AnalysisState<A, H, V, T> postState;

		/**
		 * The post-state of intermediate expressions.
		 */
		public final StatementStore<A, H, V, T> intermediateStates;

		private CompoundState(AnalysisState<A, H, V, T> postState, StatementStore<A, H, V, T> intermediateStates) {
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
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (getClass() != obj.getClass())
				return false;
			CompoundState<?, ?, ?, ?> other = (CompoundState<?, ?, ?, ?>) obj;
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
			return postState + " [" + StringUtils.join(intermediateStates, "\n") + "]";
		}

		@Override
		public boolean lessOrEqual(CompoundState<A, H, V, T> other) throws SemanticException {
			return postState.lessOrEqual(other.postState) && intermediateStates.lessOrEqual(other.intermediateStates);
		}

		@Override
		public CompoundState<A, H, V, T> lub(CompoundState<A, H, V, T> other) throws SemanticException {
			return CompoundState.of(postState.lub(other.postState), intermediateStates.lub(other.intermediateStates));
		}

		@Override
		public CompoundState<A, H, V, T> top() {
			return CompoundState.of(postState.top(), intermediateStates.top());
		}

		@Override
		public boolean isTop() {
			return postState.isTop() && intermediateStates.isTop();
		}

		@Override
		public CompoundState<A, H, V, T> bottom() {
			return CompoundState.of(postState.bottom(), intermediateStates.bottom());
		}

		@Override
		public boolean isBottom() {
			return postState.isBottom() && intermediateStates.isBottom();
		}

		@Override
		public CompoundState<A, H, V, T> glb(CompoundState<A, H, V, T> other) throws SemanticException {
			return CompoundState.of(postState.glb(other.postState), intermediateStates.glb(other.intermediateStates));
		}

		@Override
		public CompoundState<A, H, V, T> narrowing(CompoundState<A, H, V, T> other) throws SemanticException {
			return CompoundState.of(postState.narrowing(other.postState),
					intermediateStates.narrowing(other.intermediateStates));
		}

		@Override
		public CompoundState<A, H, V, T> widening(CompoundState<A, H, V, T> other) throws SemanticException {
			return CompoundState.of(postState.widening(other.postState),
					intermediateStates.widening(other.intermediateStates));
		}
	}
}

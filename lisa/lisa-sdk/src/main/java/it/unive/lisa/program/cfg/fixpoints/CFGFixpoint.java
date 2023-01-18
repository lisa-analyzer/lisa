package it.unive.lisa.program.cfg.fixpoints;

import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

import org.apache.commons.lang3.StringUtils;

import it.unive.lisa.analysis.AbstractState;
import it.unive.lisa.analysis.AnalysisState;
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

public abstract class CFGFixpoint<A extends AbstractState<A, H, V, T>,
		H extends HeapDomain<H>,
		V extends ValueDomain<V>,
		T extends TypeDomain<T>>
		implements
		FixpointImplementation<Statement, Edge,
				it.unive.lisa.program.cfg.fixpoints.CFGFixpoint.CompoundState<A, H, V, T>> {

	protected final CFG graph;
	protected final InterproceduralAnalysis<A, H, V, T> interprocedural;

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
		return CompoundState.of(left.postState.lub(right.postState),
				left.intermediateStates.lub(right.intermediateStates));
	}

	public static final class CompoundState<A extends AbstractState<A, H, V, T>,
			H extends HeapDomain<H>,
			V extends ValueDomain<V>,
			T extends TypeDomain<T>> {

		public static <A extends AbstractState<A, H, V, T>,
				H extends HeapDomain<H>,
				V extends ValueDomain<V>,
				T extends TypeDomain<T>> CompoundState<A, H, V, T> of(AnalysisState<A, H, V, T> postState,
						StatementStore<A, H, V, T> intermediateStates) {
			return new CompoundState<>(postState, intermediateStates);
		}

		public final AnalysisState<A, H, V, T> postState;
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
	}
}

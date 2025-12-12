package it.unive.lisa.program.cfg.fixpoints;

import it.unive.lisa.analysis.AbstractDomain;
import it.unive.lisa.analysis.AbstractLattice;
import it.unive.lisa.analysis.AnalysisState;
import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.analysis.StatementStore;
import it.unive.lisa.interprocedural.InterproceduralAnalysis;
import it.unive.lisa.program.cfg.CFG;
import it.unive.lisa.program.cfg.VariableTableEntry;
import it.unive.lisa.program.cfg.edge.Edge;
import it.unive.lisa.program.cfg.fixpoints.CFGFixpoint.CompoundState;
import it.unive.lisa.program.cfg.statement.Expression;
import it.unive.lisa.program.cfg.statement.Statement;
import it.unive.lisa.symbolic.SymbolicExpression;
import it.unive.lisa.symbolic.value.Identifier;
import it.unive.lisa.util.datastructures.graph.algorithms.Fixpoint.FixpointImplementation;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

/**
 * A {@link FixpointImplementation} for {@link CFG}s.
 * 
 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
 * 
 * @param <A> the kind of {@link AbstractLattice} produced by the domain
 *                {@code D}
 * @param <D> the kind of {@link AbstractDomain} to run during the analysis
 */
public abstract class BackwardCFGFixpoint<A extends AbstractLattice<A>,
		D extends AbstractDomain<A>>
		implements
		FixpointImplementation<Statement, Edge, CompoundState<A>> {

	/**
	 * The graph targeted by this implementation.
	 */
	protected final CFG graph;

	/**
	 * The {@link InterproceduralAnalysis} to use for semantics invocations.
	 */
	protected final InterproceduralAnalysis<A, D> interprocedural;

	/**
	 * Builds the fixpoint implementation.
	 * 
	 * @param graph           the graph targeted by this implementation
	 * @param interprocedural the {@link InterproceduralAnalysis} to use for
	 *                            semantics invocation
	 */
	public BackwardCFGFixpoint(
			CFG graph,
			InterproceduralAnalysis<A, D> interprocedural) {
		this.graph = graph;
		this.interprocedural = interprocedural;
	}

	@Override
	public CompoundState<A> semantics(
			Statement node,
			CompoundState<A> entrystate)
			throws SemanticException {
		StatementStore<A> expressions = new StatementStore<>(entrystate.postState).bottom();
		AnalysisState<A> approx = node.backwardSemantics(entrystate.postState, interprocedural, expressions);
		if (node instanceof Expression)
			// we forget the meta variables now as the values are popped from
			// the stack here
			approx = approx.forgetIdentifiers(((Expression) node).getMetaVariables(), node);
		return CompoundState.of(approx, expressions);
	}

	@Override
	public CompoundState<A> traverse(
			Edge edge,
			CompoundState<A> entrystate)
			throws SemanticException {
		AnalysisState<A> approx = edge.traverseBackwards(entrystate.postState, interprocedural.getAnalysis());

		// we remove out of scope variables here
		List<VariableTableEntry> toRemove = new LinkedList<>();
		for (VariableTableEntry entry : graph.getDescriptor().getVariables())
			if (entry.getScopeStart() == edge.getDestination())
				toRemove.add(entry);

		Collection<Identifier> ids = new LinkedList<>();
		for (VariableTableEntry entry : toRemove) {
			SymbolicExpression v = entry.createReference(graph).getVariable();
			for (SymbolicExpression expr : interprocedural.getAnalysis()
					.smallStepSemantics(approx, v, edge.getSource())
					.getExecutionExpressions())
				ids.add((Identifier) expr);
		}

		if (!ids.isEmpty())
			approx = approx.forgetIdentifiers(ids, edge.getSource());

		return CompoundState.of(approx, new StatementStore<>(approx).bottom());
	}

	@Override
	public CompoundState<A> union(
			Statement node,
			CompoundState<A> left,
			CompoundState<A> right)
			throws SemanticException {
		return left.lub(right);
	}

}

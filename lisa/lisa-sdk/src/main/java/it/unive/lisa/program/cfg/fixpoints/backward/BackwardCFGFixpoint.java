package it.unive.lisa.program.cfg.fixpoints.backward;

import it.unive.lisa.analysis.AbstractDomain;
import it.unive.lisa.analysis.AbstractLattice;
import it.unive.lisa.analysis.AnalysisState;
import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.analysis.StatementStore;
import it.unive.lisa.events.EventQueue;
import it.unive.lisa.interprocedural.InterproceduralAnalysis;
import it.unive.lisa.program.cfg.CFG;
import it.unive.lisa.program.cfg.VariableTableEntry;
import it.unive.lisa.program.cfg.edge.Edge;
import it.unive.lisa.program.cfg.fixpoints.AnalysisFixpoint;
import it.unive.lisa.program.cfg.fixpoints.CompoundState;
import it.unive.lisa.program.cfg.fixpoints.events.EdgeTraverseEnd;
import it.unive.lisa.program.cfg.fixpoints.events.EdgeTraverseStart;
import it.unive.lisa.program.cfg.fixpoints.events.PreStateComputed;
import it.unive.lisa.program.cfg.fixpoints.events.StatementSemanticsEnd;
import it.unive.lisa.program.cfg.fixpoints.events.StatementSemanticsStart;
import it.unive.lisa.program.cfg.statement.Expression;
import it.unive.lisa.program.cfg.statement.Statement;
import it.unive.lisa.symbolic.SymbolicExpression;
import it.unive.lisa.symbolic.value.Identifier;
import it.unive.lisa.util.datastructures.graph.algorithms.BackwardFixpoint;
import it.unive.lisa.util.datastructures.graph.algorithms.FixpointException;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;
import org.apache.commons.lang3.tuple.Pair;

/**
 * A {@link BackwardFixpoint} for {@link CFG}s.
 * 
 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
 * 
 * @param <A> the kind of {@link AbstractLattice} produced by the domain
 *                {@code D}
 * @param <D> the kind of {@link AbstractDomain} to run during the analysis
 */
public abstract class BackwardCFGFixpoint<A extends AbstractLattice<A>, D extends AbstractDomain<A>>
		extends
		BackwardFixpoint<CFG, Statement, Edge, CompoundState<A>>
		implements
		AnalysisFixpoint<BackwardCFGFixpoint<A, D>, A, D> {

	/**
	 * The {@link InterproceduralAnalysis} to use for semantics invocations.
	 */
	protected final InterproceduralAnalysis<A, D> interprocedural;

	/**
	 * The event queue to notify analysis events.
	 */
	protected EventQueue events;

	/**
	 * Builds the fixpoint implementation.
	 * 
	 * @param graph               the graph targeted by this implementation
	 * @param forceFullEvaluation whether or not the fixpoint should evaluate
	 *                                all nodes independently of the fixpoint
	 *                                implementation
	 * @param interprocedural     the {@link InterproceduralAnalysis} to use for
	 *                                semantics invocation
	 */
	public BackwardCFGFixpoint(
			CFG graph,
			boolean forceFullEvaluation,
			InterproceduralAnalysis<A, D> interprocedural) {
		super(graph, forceFullEvaluation);
		this.interprocedural = interprocedural;
	}

	/**
	 * Sets the {@link EventQueue} that this fixpoint can use to post events.
	 * Note that, to avoid unnecessary overhead, this method will only be
	 * invoked if at least one event listener has been registered in the event
	 * queue. Dereferences of the queue should thus be null-checked.
	 * 
	 * @param queue the event queue to use
	 */
	public void setEventQueue(
			EventQueue queue) {
		this.events = queue;
	}

	@Override
	public Pair<CompoundState<A>, Statement> semantics(
			Statement node,
			CompoundState<A> entrystate,
			Map<Statement, CompoundState<A>> result)
			throws SemanticException {
		StatementStore<A> expressions = new StatementStore<>(entrystate.postState).bottom();
		if (events != null)
			events.post(new StatementSemanticsStart<>(node, entrystate.postState));

		AnalysisState<A> approx = node.backwardSemantics(entrystate.postState, interprocedural, expressions);
		if (node instanceof Expression)
			// we forget the meta variables now as the values are popped from
			// the stack here
			approx = approx.forgetIdentifiers(((Expression) node).getMetaVariables(), node);

		if (events != null)
			events.post(new StatementSemanticsEnd<>(node, entrystate.postState, approx));

		return Pair.of(CompoundState.of(approx, expressions), node);
	}

	@Override
	public CompoundState<A> traverse(
			Edge edge,
			CompoundState<A> entrystate)
			throws SemanticException {
		AnalysisState<A> state = entrystate.postState;

		if (events != null)
			events.post(new EdgeTraverseStart<>(edge, state));

		AnalysisState<A> approx = edge.traverseBackwards(state, interprocedural.getAnalysis());

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

		if (events != null)
			events.post(new EdgeTraverseEnd<>(edge, state, approx));

		return CompoundState.of(approx, new StatementStore<>(approx).bottom());
	}

	@Override
	protected CompoundState<A> getExitState(
			CFG graph,
			Statement node,
			CompoundState<A> startstate,
			Map<Statement, CompoundState<A>> result)
			throws FixpointException {
		CompoundState<A> exitState = super.getExitState(graph, node, startstate, result);
		if (events != null)
			events.post(new PreStateComputed<>(node, exitState.postState));
		return exitState;
	}

	@Override
	public CompoundState<A> union(
			Statement node,
			CompoundState<A> left,
			CompoundState<A> right)
			throws SemanticException {
		return left.lub(right);
	}

	@Override
	public boolean isOptimized() {
		return false;
	}

	@Override
	public BackwardCFGFixpoint<A, D> asBackward() {
		return this;
	}

	@Override
	public BackwardCFGFixpoint<A, D> asUnoptimized() {
		return this;
	}

	// we redefine the following to narrow the return type
	@Override
	public abstract BackwardCFGFixpoint<A, D> asOptimized();

	@Override
	public BackwardCFGFixpoint<A, D> withHotspots(
			Predicate<Statement> hotspots) {
		return this;
	}
}

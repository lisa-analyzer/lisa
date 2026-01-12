package it.unive.lisa.program.cfg.fixpoints.forward;

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
import it.unive.lisa.util.datastructures.graph.algorithms.FixpointException;
import it.unive.lisa.util.datastructures.graph.algorithms.ForwardFixpoint;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;
import org.apache.commons.lang3.tuple.Pair;

/**
 * A {@link ForwardFixpoint} for {@link CFG}s.
 * 
 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
 * 
 * @param <A> the kind of {@link AbstractLattice} produced by the domain
 *                {@code D}
 * @param <D> the kind of {@link AbstractDomain} to run during the analysis
 */
public abstract class ForwardCFGFixpoint<A extends AbstractLattice<A>, D extends AbstractDomain<A>>
		extends
		ForwardFixpoint<CFG, Statement, Edge, CompoundState<A>>
		implements
		AnalysisFixpoint<ForwardCFGFixpoint<A, D>, A, D> {

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
	public ForwardCFGFixpoint(
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

		AnalysisState<A> approx = node.forwardSemantics(entrystate.postState, interprocedural, expressions);

		if (events != null)
			events.post(new StatementSemanticsEnd<>(node, entrystate.postState, approx));
		// we do not remove the meta variables here, since they might be
		// used for deciding whether or not to traverse an edge
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

		// we remove the exceptions that are caught by error edges
		// going out from the same source
		if (!edge.isErrorHandling())
			state = interprocedural.getAnalysis().removeCaughtErrors(state, edge.getSource());

		AnalysisState<A> approx = edge.traverseForward(state, interprocedural.getAnalysis());

		// we remove out of scope variables here
		List<VariableTableEntry> toRemove = new LinkedList<>();
		for (VariableTableEntry entry : graph.getDescriptor().getVariables())
			if (entry.getScopeEnd() == edge.getSource())
				toRemove.add(entry);

		Collection<Identifier> ids = new LinkedList<>();
		for (VariableTableEntry entry : toRemove) {
			SymbolicExpression v = entry.createReference(graph).getVariable();
			for (SymbolicExpression expr : interprocedural.getAnalysis()
					.smallStepSemantics(approx, v, edge.getSource())
					.getExecutionExpressions())
				ids.add((Identifier) expr);
		}

		if (edge.getSource() instanceof Expression)
			// we forget the meta variables now as the values are popped from
			// the stack after the execution of the source expression,
			// and at this point we used them for evaluation of guards
			ids.addAll(((Expression) edge.getSource()).getMetaVariables());

		if (!ids.isEmpty())
			approx = approx.forgetIdentifiers(ids, edge.getSource());

		if (events != null)
			events.post(new EdgeTraverseEnd<>(edge, state, approx));

		return CompoundState.of(approx, new StatementStore<>(approx).bottom());
	}

	@Override
	protected CompoundState<A> getEntryState(
			CFG graph,
			Statement node,
			CompoundState<A> startstate,
			Map<Statement, CompoundState<A>> result)
			throws FixpointException {
		CompoundState<A> entryState = super.getEntryState(graph, node, startstate, result);
		if (events != null)
			events.post(new PreStateComputed<>(node, entryState.postState));
		return entryState;
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
	public ForwardCFGFixpoint<A, D> asForward() {
		return this;
	}

	@Override
	public ForwardCFGFixpoint<A, D> asUnoptimized() {
		return this;
	}

	// we redefine the following to narrow the return type
	@Override
	public abstract ForwardCFGFixpoint<A, D> asOptimized();

	@Override
	public ForwardCFGFixpoint<A, D> withHotspots(
			Predicate<Statement> hotspots) {
		return this;
	}
}

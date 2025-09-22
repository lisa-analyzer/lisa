package it.unive.lisa.program.cfg;

import it.unive.lisa.analysis.AbstractDomain;
import it.unive.lisa.analysis.AbstractLattice;
import it.unive.lisa.analysis.AnalysisState;
import it.unive.lisa.analysis.AnalyzedCFG;
import it.unive.lisa.analysis.BackwardAnalyzedCFG;
import it.unive.lisa.analysis.OptimizedAnalyzedCFG;
import it.unive.lisa.analysis.StatementStore;
import it.unive.lisa.conf.FixpointConfiguration;
import it.unive.lisa.conf.LiSAConfiguration.DescendingPhaseType;
import it.unive.lisa.interprocedural.InterproceduralAnalysis;
import it.unive.lisa.interprocedural.ScopeId;
import it.unive.lisa.outputs.serializableGraph.SerializableCFG;
import it.unive.lisa.outputs.serializableGraph.SerializableGraph;
import it.unive.lisa.outputs.serializableGraph.SerializableValue;
import it.unive.lisa.program.Program;
import it.unive.lisa.program.ProgramValidationException;
import it.unive.lisa.program.SyntheticLocation;
import it.unive.lisa.program.Unit;
import it.unive.lisa.program.cfg.controlFlow.ControlFlowExtractor;
import it.unive.lisa.program.cfg.controlFlow.ControlFlowStructure;
import it.unive.lisa.program.cfg.controlFlow.IfThenElse;
import it.unive.lisa.program.cfg.controlFlow.Loop;
import it.unive.lisa.program.cfg.edge.Edge;
import it.unive.lisa.program.cfg.edge.ErrorEdge;
import it.unive.lisa.program.cfg.edge.SequentialEdge;
import it.unive.lisa.program.cfg.fixpoints.AscendingFixpoint;
import it.unive.lisa.program.cfg.fixpoints.BackwardAscendingFixpoint;
import it.unive.lisa.program.cfg.fixpoints.CFGFixpoint.CompoundState;
import it.unive.lisa.program.cfg.fixpoints.DescendingGLBFixpoint;
import it.unive.lisa.program.cfg.fixpoints.DescendingNarrowingFixpoint;
import it.unive.lisa.program.cfg.fixpoints.OptimizedBackwardFixpoint;
import it.unive.lisa.program.cfg.fixpoints.OptimizedFixpoint;
import it.unive.lisa.program.cfg.protection.CatchBlock;
import it.unive.lisa.program.cfg.protection.ProtectedBlock;
import it.unive.lisa.program.cfg.protection.ProtectionBlock;
import it.unive.lisa.program.cfg.statement.Expression;
import it.unive.lisa.program.cfg.statement.NoOp;
import it.unive.lisa.program.cfg.statement.Statement;
import it.unive.lisa.program.cfg.statement.call.Call;
import it.unive.lisa.util.collections.workset.VisitOnceFIFOWorkingSet;
import it.unive.lisa.util.collections.workset.VisitOnceWorkingSet;
import it.unive.lisa.util.collections.workset.WorkingSet;
import it.unive.lisa.util.datastructures.graph.algorithms.BackwardFixpoint;
import it.unive.lisa.util.datastructures.graph.algorithms.Fixpoint;
import it.unive.lisa.util.datastructures.graph.algorithms.FixpointException;
import it.unive.lisa.util.datastructures.graph.code.CodeGraph;
import it.unive.lisa.util.datastructures.graph.code.NodeList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.function.BiPredicate;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * A control flow graph with an implementation, that has {@link Statement}s as
 * nodes and {@link Edge}s as edges.<br>
 * <br>
 * Note that this class does not implement {@link #equals(Object)} nor
 * {@link #hashCode()} since all cfgs are unique.
 * 
 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
 */
public class CFG
		extends
		CodeGraph<CFG, Statement, Edge>
		implements
		CodeMember {

	private static final Logger LOG = LogManager.getLogger(CFG.class);

	/**
	 * The descriptor of this control flow graph.
	 */
	private final CodeMemberDescriptor descriptor;

	/**
	 * The lazily computed basic blocks of this cfg, available only after
	 * {@link #computeBasicBlocks()} has been invoked.
	 */
	private Map<Statement, Statement[]> basicBlocks;

	/**
	 * Builds the control flow graph.
	 * 
	 * @param descriptor the descriptor of this cfg
	 */
	public CFG(
			CodeMemberDescriptor descriptor) {
		super(new SequentialEdge());
		this.descriptor = descriptor;
	}

	/**
	 * Builds the control flow graph.
	 * 
	 * @param descriptor  the descriptor of this cfg
	 * @param entrypoints the statements of this cfg that will be reachable from
	 *                        other cfgs
	 * @param list        the node list containing all the statements and the
	 *                        edges that will be part of this cfg
	 */
	public CFG(
			CodeMemberDescriptor descriptor,
			Collection<Statement> entrypoints,
			NodeList<CFG, Statement, Edge> list) {
		super(entrypoints, list);
		this.descriptor = descriptor;
	}

	/**
	 * Clones the given control flow graph.
	 * 
	 * @param other the original cfg
	 */
	public CFG(
			CFG other) {
		super(other.entrypoints, other.list);
		this.descriptor = other.descriptor;
		this.basicBlocks = other.basicBlocks;
	}

	/**
	 * Yields the descriptor of this control flow graph.
	 * 
	 * @return the name
	 */
	public final CodeMemberDescriptor getDescriptor() {
		return descriptor;
	}

	/**
	 * Yields the unit where this CFG is defined.
	 * 
	 * @return the unit
	 */
	public final Unit getUnit() {
		return descriptor.getUnit();
	}

	/**
	 * Yields the program where this CFG is defined.
	 * 
	 * @return the program
	 */
	public final Program getProgram() {
		return descriptor.getUnit().getProgram();
	}

	/**
	 * Yields the statements of this control flow graph that are normal
	 * exitpoints, that is, that normally ends the execution of this cfg,
	 * returning the control to the caller without throwing an error (i.e., all
	 * such statements on which {@link Statement#stopsExecution()} holds but
	 * {@link Statement#throwsError()} does not).
	 * 
	 * @return the normal exitpoints of this cfg.
	 */
	public Collection<Statement> getNormalExitpoints() {
		return list.getNodes()
				.stream()
				.filter(st -> st.stopsExecution() && !st.throwsError())
				.collect(Collectors.toList());
	}

	/**
	 * Yields the statements of this control flow graph that are normal
	 * exitpoints, that is, that normally ends the execution of this cfg,
	 * returning the control to the caller, or throwing an error (i.e., all such
	 * statements on which either {@link Statement#stopsExecution()} or
	 * {@link Statement#throwsError()} hold).
	 * 
	 * @return the exitpoints of this cfg.
	 */
	public Collection<Statement> getAllExitpoints() {
		return list.getNodes()
				.stream()
				.filter(st -> st.stopsExecution() || st.throwsError())
				.collect(Collectors.toList());
	}

	/**
	 * Runs the given {@link ControlFlowExtractor} over this cfg to build
	 * {@link ControlFlowStructure}s, in case those cannot be generated by
	 * frontends.
	 * 
	 * @param extractor the extractor to run
	 */
	public void extractControlFlowStructures(
			ControlFlowExtractor extractor) {
		LOG.debug("Extracting control flow structures from " + this);
		extractor.extract(this).forEach(descriptor::addControlFlowStructure);
	}

	@Override
	public String toString() {
		return descriptor.toString();
	}

	/**
	 * Simplifies this cfg, removing all {@link NoOp}s and rewriting the edge
	 * set accordingly. This method will throw an
	 * {@link UnsupportedOperationException} if one of the {@link NoOp}s has an
	 * outgoing edge that is not a {@link SequentialEdge}, since such statement
	 * is expected to always be sequential.
	 * 
	 * @throws UnsupportedOperationException if there exists at least one
	 *                                           {@link NoOp} with an outgoing
	 *                                           non-sequential edge.
	 */
	public void simplify() {
		Set<Statement> targets = getNodes().stream()
				.filter(k -> k instanceof NoOp)
				.filter(k -> !isNotSimplifiable(k))
				.collect(Collectors.toSet());
		targets.forEach(this::preSimplify);
		list.simplify(targets, entrypoints, new LinkedList<>(), new HashMap<>());
		descriptor.getControlFlowStructures().forEach(ControlFlowStructure::simplify);
		descriptor.getProtectionBlocks().forEach(ProtectionBlock::simplify);
	}

	private boolean isNotSimplifiable(
			Statement st) {
		for (ControlFlowStructure cfs : descriptor.getControlFlowStructures())
			if (cfs.getCondition() == st)
				return true;

		// removing the only node in a protected block
		// would mean removing the whole block from the
		// cfg, but that would make it different w.r.t.
		// the source code, so we keep it. It is also
		// problematic to remove if it is a noop-return
		List<BiPredicate<ProtectedBlock, Statement>> exclusions = new LinkedList<>();
		// single node in protected block
		exclusions.add(
				(
						pb,
						s) -> pb.getBody().size() == 1 && pb.getStart() == s);
		// pre-exit in protected block
		exclusions.add(
				(
						pb,
						s) -> pb.getBody().size() == 2 && pb.getStart() == s && pb.getEnd().stopsExecution());
		// branch target followed by finally execution
		exclusions.add(
				(
						pb,
						s) -> getIngoingEdges(s).size() == 1
								&& getIngoingEdges(s).stream().anyMatch(Predicate.not(Edge::isUnconditional))
								&& getOutgoingEdges(s).stream()
										.allMatch(Edge::isErrorHandling));

		for (ProtectionBlock pb : descriptor.getProtectionBlocks())
			if (exclusions.stream().anyMatch(e -> e.test(pb.getTryBlock(), st)))
				return true;
			else if (pb.getElseBlock() != null && (exclusions.stream().anyMatch(e -> e.test(pb.getElseBlock(), st))))
				return true;
			else if (pb.getFinallyBlock() != null
					&& (exclusions.stream().anyMatch(e -> e.test(pb.getFinallyBlock(), st))))
				return true;
			else
				for (CatchBlock cb : pb.getCatchBlocks())
					if (exclusions.stream().anyMatch(e -> e.test(cb.getBody(), st)))
						return true;
		return false;
	}

	private void preSimplify(
			Statement node) {
		shiftVariableScopes(node);
		shiftControlFlowStructuresEnd(node);
		shiftProtectionBlocks(node);
	}

	private void shiftControlFlowStructuresEnd(
			Statement node) {
		Collection<Statement> followers = followersOf(node);

		Statement candidate;
		for (ControlFlowStructure cfs : descriptor.getControlFlowStructures())
			if (node == cfs.getFirstFollower())
				if (followers.isEmpty())
					cfs.setFirstFollower(null);
				else if (followers.size() == 1)
					if (!((candidate = followers.iterator().next()) instanceof NoOp))
						cfs.setFirstFollower(candidate);
					else
						cfs.setFirstFollower(firstNonNoOpDeterministicFollower(candidate));
				else {
					LOG.warn(
							"{} is the first follower of a control flow structure, it is being simplified but has multiple followers: the first follower of the conditional structure will be lost",
							node);
					cfs.setFirstFollower(null);
				}
	}

	private void shiftProtectionBlocks(
			Statement node) {
		Collection<Statement> followers = followersOf(node);

		Statement candidate;
		for (ProtectionBlock pb : descriptor.getProtectionBlocks())
			if (node == pb.getClosing())
				if (followers.isEmpty())
					pb.setClosing(null);
				else if (followers.size() == 1)
					if (!((candidate = followers.iterator().next()) instanceof NoOp))
						pb.setClosing(candidate);
					else
						pb.setClosing(firstNonNoOpDeterministicFollower(candidate));
				else {
					LOG.warn(
							"{} is the closing of a protection block, it is being simplified but has multiple followers: the closing of the protection block will be lost",
							node);
					pb.setClosing(null);
				}
	}

	private Statement firstNonNoOpDeterministicFollower(
			Statement st) {
		Statement current = st;
		while (current instanceof NoOp) {
			Collection<Statement> followers = followersOf(current);
			if (followers.isEmpty() || followers.size() > 1)
				// we reached the end or we have more than one follower
				return null;
			current = followers.iterator().next();
		}

		return current;
	}

	private void shiftVariableScopes(
			Statement node) {
		Collection<VariableTableEntry> starting = descriptor.getVariables()
				.stream()
				.filter(v -> v.getScopeStart() == node)
				.collect(Collectors.toList());
		Collection<VariableTableEntry> ending = descriptor.getVariables()
				.stream()
				.filter(v -> v.getScopeEnd() == node)
				.collect(Collectors.toList());
		if (ending.isEmpty() && starting.isEmpty())
			return;

		Collection<Statement> predecessors = predecessorsOf(node);
		Collection<Statement> followers = followersOf(node);

		if (predecessors.isEmpty() && followers.isEmpty()) {
			LOG.warn(
					"Simplifying the only statement of '{}': all variables will be made visible for the entire cfg",
					this);
			starting.forEach(v -> v.setScopeStart(null));
			ending.forEach(v -> v.setScopeEnd(null));
			return;
		}

		String format = "Simplifying the scope-{} statement of a variable with {} "
				+ "is not supported: {} will be made visible {} of '"
				+ this
				+ "'";
		if (!starting.isEmpty())
			if (predecessors.isEmpty()) {
				// no predecessors: move the starting scope forward
				Statement follow;
				if (followers.size() > 1) {
					LOG.warn(format, "starting", "no predecessors and multiple followers", starting, "from the start");
					follow = null;
				} else
					follow = followers.iterator().next();
				starting.forEach(v -> v.setScopeStart(follow));
			} else {
				// move the starting scope backward
				Statement pred;
				if (predecessors.size() > 1) {
					LOG.warn(format, "starting", "multiple predecessors", starting, "from the start");
					pred = null;
				} else
					pred = predecessors.iterator().next();
				starting.forEach(v -> v.setScopeStart(pred));
			}

		if (!ending.isEmpty())
			if (followers.isEmpty()) {
				// no followers: move the ending scope backward
				Statement pred;
				if (predecessors.size() > 1) {
					LOG.warn(format, "ending", "no followers and multiple predecessors", ending, "until the end");
					pred = null;
				} else
					pred = predecessors.iterator().next();
				ending.forEach(v -> v.setScopeEnd(pred));
			} else {
				// move the ending scope forward
				Statement follow;
				if (followers.size() > 1) {
					LOG.warn(format, "ending", "multiple followers", ending, "until the end");
					follow = null;
				} else
					follow = followers.iterator().next();
				ending.forEach(v -> v.setScopeEnd(follow));
			}
	}

	/**
	 * Computes a fixpoint over this control flow graph. This method returns a
	 * {@link AnalyzedCFG} instance mapping each {@link Statement} to the
	 * {@link AnalysisState} computed by this method. The computation starts at
	 * the statements returned by {@link #getEntrypoints()}, using
	 * {@code entryState} as entry state for all of them.
	 * {@code interprocedural} will be invoked to get the approximation of all
	 * invoked cfgs, while {@code ws} is used as working set for the statements
	 * to process.
	 * 
	 * @param <A>             the kind of {@link AbstractLattice} produced by
	 *                            the domain {@code D}
	 * @param <D>             the kind of {@link AbstractDomain} to run during
	 *                            the analysis
	 * @param entryState      the entry states to apply to each
	 *                            {@link Statement} returned by
	 *                            {@link #getEntrypoints()}
	 * @param interprocedural the interprocedural analysis that can be queried
	 *                            when a call towards an other cfg is
	 *                            encountered
	 * @param ws              the {@link WorkingSet} instance to use for this
	 *                            computation
	 * @param conf            the {@link FixpointConfiguration} containing the
	 *                            parameters tuning fixpoint behavior
	 * @param id              a {@link ScopeId} meant to identify this specific
	 *                            result based on how it has been produced, that
	 *                            will be embedded in the returned cfg
	 * 
	 * @return a {@link AnalyzedCFG} instance that is equivalent to this control
	 *             flow graph, and that stores for each {@link Statement} the
	 *             result of the fixpoint computation
	 * 
	 * @throws FixpointException if an error occurs during the semantic
	 *                               computation of a statement, or if some
	 *                               unknown/invalid statement ends up in the
	 *                               working set
	 */
	public <A extends AbstractLattice<A>, D extends AbstractDomain<A>> AnalyzedCFG<A> fixpoint(
			AnalysisState<A> entryState,
			InterproceduralAnalysis<A, D> interprocedural,
			WorkingSet<Statement> ws,
			FixpointConfiguration conf,
			ScopeId id)
			throws FixpointException {
		Map<Statement, AnalysisState<A>> start = new HashMap<>();
		entrypoints.forEach(e -> start.put(e, entryState));
		return fixpoint(entryState, start, interprocedural, ws, conf, id);
	}

	/**
	 * Computes a fixpoint over this control flow graph. This method returns a
	 * {@link AnalyzedCFG} instance mapping each {@link Statement} to the
	 * {@link AnalysisState} computed by this method. The computation starts at
	 * the statements in {@code entrypoints}, using {@code entryState} as entry
	 * state for all of them. {@code interprocedural} will be invoked to get the
	 * approximation of all invoked cfgs, while {@code ws} is used as working
	 * set for the statements to process.
	 * 
	 * @param <A>             the kind of {@link AbstractLattice} produced by
	 *                            the domain {@code D}
	 * @param <D>             the kind of {@link AbstractDomain} to run during
	 *                            the analysis
	 * @param entrypoints     the collection of {@link Statement}s that to use
	 *                            as a starting point of the computation (that
	 *                            must be nodes of this cfg)
	 * @param entryState      the entry states to apply to each
	 *                            {@link Statement} in {@code entrypoints}
	 * @param interprocedural the interprocedural analysis that can be queried
	 *                            when a call towards an other cfg is
	 *                            encountered
	 * @param ws              the {@link WorkingSet} instance to use for this
	 *                            computation
	 * @param conf            the {@link FixpointConfiguration} containing the
	 *                            parameters tuning fixpoint behavior
	 * @param id              a {@link ScopeId} meant to identify this specific
	 *                            result based on how it has been produced, that
	 *                            will be embedded in the returned cfg
	 * 
	 * @return a {@link AnalyzedCFG} instance that is equivalent to this control
	 *             flow graph, and that stores for each {@link Statement} the
	 *             result of the fixpoint computation
	 * 
	 * @throws FixpointException if an error occurs during the semantic
	 *                               computation of a statement, or if some
	 *                               unknown/invalid statement ends up in the
	 *                               working set
	 */
	public <A extends AbstractLattice<A>, D extends AbstractDomain<A>> AnalyzedCFG<A> fixpoint(
			Collection<Statement> entrypoints,
			AnalysisState<A> entryState,
			InterproceduralAnalysis<A, D> interprocedural,
			WorkingSet<Statement> ws,
			FixpointConfiguration conf,
			ScopeId id)
			throws FixpointException {
		Map<Statement, AnalysisState<A>> start = new HashMap<>();
		entrypoints.forEach(e -> start.put(e, entryState));
		return fixpoint(entryState, start, interprocedural, ws, conf, id);
	}

	/**
	 * Computes a fixpoint over this control flow graph. This method returns a
	 * {@link AnalyzedCFG} instance mapping each {@link Statement} to the
	 * {@link AnalysisState} computed by this method. The computation starts at
	 * the statements in {@code startingPoints}, using as its entry state their
	 * respective value. {@code interprocedural} will be invoked to get the
	 * approximation of all invoked cfgs, while {@code ws} is used as working
	 * set for the statements to process.
	 * 
	 * @param <A>             the kind of {@link AbstractLattice} produced by
	 *                            the domain {@code D}
	 * @param <D>             the kind of {@link AbstractDomain} to run during
	 *                            the analysis
	 * @param singleton       an instance of the {@link AnalysisState}
	 *                            containing the abstract state of the analysis
	 *                            to run, used to retrieve top and bottom values
	 * @param startingPoints  a map between {@link Statement}s to use as a
	 *                            starting point of the computation (that must
	 *                            be nodes of this cfg) and the entry states to
	 *                            apply on them
	 * @param interprocedural the interprocedural analysis that can be queried
	 *                            when a call towards an other cfg is
	 *                            encountered
	 * @param ws              the {@link WorkingSet} instance to use for this
	 *                            computation
	 * @param conf            the {@link FixpointConfiguration} containing the
	 *                            parameters tuning fixpoint behavior
	 * @param id              a {@link ScopeId} meant to identify this specific
	 *                            result based on how it has been produced, that
	 *                            will be embedded in the returned cfg
	 * 
	 * @return a {@link AnalyzedCFG} instance that is equivalent to this control
	 *             flow graph, and that stores for each {@link Statement} the
	 *             result of the fixpoint computation
	 * 
	 * @throws FixpointException if an error occurs during the semantic
	 *                               computation of a statement, or if some
	 *                               unknown/invalid statement ends up in the
	 *                               working set
	 */
	public <A extends AbstractLattice<A>, D extends AbstractDomain<A>> AnalyzedCFG<A> fixpoint(
			AnalysisState<A> singleton,
			Map<Statement, AnalysisState<A>> startingPoints,
			InterproceduralAnalysis<A, D> interprocedural,
			WorkingSet<Statement> ws,
			FixpointConfiguration conf,
			ScopeId id)
			throws FixpointException {
		// we disable optimizations for ascending phases if there is a
		// descending one: the latter will need full results to start applying
		// glbs/narrowings from a post-fixpoint
		boolean isOptimized = conf.optimize && conf.descendingPhaseType == DescendingPhaseType.NONE;
		Fixpoint<CFG,
				Statement,
				Edge,
				CompoundState<A>> fix = isOptimized
						? new OptimizedFixpoint<>(this, false, conf.hotspots)
						: new Fixpoint<>(this, false);
		AscendingFixpoint<A, D> asc = new AscendingFixpoint<>(this, interprocedural, conf);

		Map<Statement, CompoundState<A>> starting = new HashMap<>();
		StatementStore<A> bot = new StatementStore<>(singleton.bottom());
		startingPoints.forEach(
				(
						st,
						state) -> starting.put(st, CompoundState.of(state, bot)));
		Map<Statement, CompoundState<A>> ascending = fix.fixpoint(starting, ws, asc);

		if (conf.descendingPhaseType == DescendingPhaseType.NONE)
			return flatten(isOptimized, singleton, startingPoints, interprocedural, id, ascending);

		fix = conf.optimize ? new OptimizedFixpoint<>(this, true, conf.hotspots) : new Fixpoint<>(this, true);
		Map<Statement, CompoundState<A>> descending;
		switch (conf.descendingPhaseType) {
		case GLB:
			DescendingGLBFixpoint<A, D> dg = new DescendingGLBFixpoint<>(this, interprocedural, conf);
			descending = fix.fixpoint(starting, ws, dg, ascending);
			break;
		case NARROWING:
			DescendingNarrowingFixpoint<A, D> dn = new DescendingNarrowingFixpoint<>(this, interprocedural, conf);
			descending = fix.fixpoint(starting, ws, dn, ascending);
			break;
		case NONE:
		default:
			// should never happen
			descending = ascending;
			break;
		}

		return flatten(conf.optimize, singleton, startingPoints, interprocedural, id, descending);
	}

	private <A extends AbstractLattice<A>, D extends AbstractDomain<A>> AnalyzedCFG<A> flatten(
			boolean isOptimized,
			AnalysisState<A> singleton,
			Map<Statement, AnalysisState<A>> startingPoints,
			InterproceduralAnalysis<A, D> interprocedural,
			ScopeId id,
			Map<Statement, CompoundState<A>> fixpointResults) {
		Map<Statement, AnalysisState<A>> finalResults = new HashMap<>(fixpointResults.size());
		for (Entry<Statement, CompoundState<A>> e : fixpointResults.entrySet()) {
			finalResults.put(e.getKey(), e.getValue().postState);
			for (Entry<Statement, AnalysisState<A>> ee : e.getValue().intermediateStates)
				finalResults.put(ee.getKey(), ee.getValue());
		}

		return isOptimized
				? new OptimizedAnalyzedCFG<>(this, id, singleton, startingPoints, finalResults, interprocedural)
				: new AnalyzedCFG<>(this, id, singleton, startingPoints, finalResults);
	}

	/**
	 * Computes a backward fixpoint over this control flow graph. This method
	 * returns a {@link BackwardAnalyzedCFG} instance mapping each
	 * {@link Statement} to the {@link AnalysisState} computed by this method.
	 * The computation starts at the statements returned by
	 * {@link #getAllExitpoints()}, using {@code exitState} as exit state for
	 * all of them. {@code interprocedural} will be invoked to get the
	 * approximation of all invoked cfgs, while {@code ws} is used as working
	 * set for the statements to process.
	 * 
	 * @param <A>             the kind of {@link AbstractLattice} produced by
	 *                            the domain {@code D}
	 * @param <D>             the kind of {@link AbstractDomain} to run during
	 *                            the analysis
	 * @param exitState       the exit states to apply to each {@link Statement}
	 *                            returned by {@link #getAllExitpoints()}
	 * @param interprocedural the interprocedural analysis that can be queried
	 *                            when a call towards an other cfg is
	 *                            encountered
	 * @param ws              the {@link WorkingSet} instance to use for this
	 *                            computation
	 * @param conf            the {@link FixpointConfiguration} containing the
	 *                            parameters tuning fixpoint behavior
	 * @param id              a {@link ScopeId} meant to identify this specific
	 *                            result based on how it has been produced, that
	 *                            will be embedded in the returned cfg
	 * 
	 * @return a {@link BackwardAnalyzedCFG} instance that is equivalent to this
	 *             control flow graph, and that stores for each
	 *             {@link Statement} the result of the fixpoint computation
	 * 
	 * @throws FixpointException if an error occurs during the semantic
	 *                               computation of a statement, or if some
	 *                               unknown/invalid statement ends up in the
	 *                               working set
	 */
	public <A extends AbstractLattice<A>, D extends AbstractDomain<A>> AnalyzedCFG<A> backwardFixpoint(
			AnalysisState<A> exitState,
			InterproceduralAnalysis<A, D> interprocedural,
			WorkingSet<Statement> ws,
			FixpointConfiguration conf,
			ScopeId id)
			throws FixpointException {
		Map<Statement, AnalysisState<A>> start = new HashMap<>();
		getAllExitpoints().forEach(e -> start.put(e, exitState));
		return backwardFixpoint(exitState, start, interprocedural, ws, conf, id);
	}

	/**
	 * Computes a backward fixpoint over this control flow graph. This method
	 * returns a {@link BackwardAnalyzedCFG} instance mapping each
	 * {@link Statement} to the {@link AnalysisState} computed by this method.
	 * The computation starts at the statements in {@code exitpoints}, using
	 * {@code exitState} as exit state for all of them. {@code interprocedural}
	 * will be invoked to get the approximation of all invoked cfgs, while
	 * {@code ws} is used as working set for the statements to process.
	 * 
	 * @param <A>             the kind of {@link AbstractLattice} produced by
	 *                            the domain {@code D}
	 * @param <D>             the kind of {@link AbstractDomain} to run during
	 *                            the analysis
	 * @param exitpoints      the collection of {@link Statement}s that to use
	 *                            as a starting point of the computation (that
	 *                            must be nodes of this cfg)
	 * @param exitState       the exit states to apply to each {@link Statement}
	 *                            in {@code exitpoints}
	 * @param interprocedural the interprocedural analysis that can be queried
	 *                            when a call towards an other cfg is
	 *                            encountered
	 * @param ws              the {@link WorkingSet} instance to use for this
	 *                            computation
	 * @param conf            the {@link FixpointConfiguration} containing the
	 *                            parameters tuning fixpoint behavior
	 * @param id              a {@link ScopeId} meant to identify this specific
	 *                            result based on how it has been produced, that
	 *                            will be embedded in the returned cfg
	 * 
	 * @return a {@link BackwardAnalyzedCFG} instance that is equivalent to this
	 *             control flow graph, and that stores for each
	 *             {@link Statement} the result of the fixpoint computation
	 * 
	 * @throws FixpointException if an error occurs during the semantic
	 *                               computation of a statement, or if some
	 *                               unknown/invalid statement ends up in the
	 *                               working set
	 */
	public <A extends AbstractLattice<A>, D extends AbstractDomain<A>> AnalyzedCFG<A> backwardFixpoint(
			Collection<Statement> exitpoints,
			AnalysisState<A> exitState,
			InterproceduralAnalysis<A, D> interprocedural,
			WorkingSet<Statement> ws,
			FixpointConfiguration conf,
			ScopeId id)
			throws FixpointException {
		Map<Statement, AnalysisState<A>> start = new HashMap<>();
		exitpoints.forEach(e -> start.put(e, exitState));
		return backwardFixpoint(exitState, start, interprocedural, ws, conf, id);
	}

	/**
	 * Computes a backward fixpoint over this control flow graph. This method
	 * returns a {@link BackwardAnalyzedCFG} instance mapping each
	 * {@link Statement} to the {@link AnalysisState} computed by this method.
	 * The computation starts at the statements in {@code startingPoints}, using
	 * as its exit state their respective value. {@code interprocedural} will be
	 * invoked to get the approximation of all invoked cfgs, while {@code ws} is
	 * used as working set for the statements to process.
	 * 
	 * @param <A>             the kind of {@link AbstractLattice} produced by
	 *                            the domain {@code D}
	 * @param <D>             the kind of {@link AbstractDomain} to run during
	 *                            the analysis
	 * @param singleton       an instance of the {@link AnalysisState}
	 *                            containing the abstract state of the analysis
	 *                            to run, used to retrieve top and bottom values
	 * @param startingPoints  a map between {@link Statement}s to use as a
	 *                            starting point of the computation (that must
	 *                            be nodes of this cfg) and the exit states to
	 *                            apply on them
	 * @param interprocedural the interprocedural analysis that can be queried
	 *                            when a call towards an other cfg is
	 *                            encountered
	 * @param ws              the {@link WorkingSet} instance to use for this
	 *                            computation
	 * @param conf            the {@link FixpointConfiguration} containing the
	 *                            parameters tuning fixpoint behavior
	 * @param id              a {@link ScopeId} meant to identify this specific
	 *                            result based on how it has been produced, that
	 *                            will be embedded in the returned cfg
	 * 
	 * @return a {@link BackwardAnalyzedCFG} instance that is equivalent to this
	 *             control flow graph, and that stores for each
	 *             {@link Statement} the result of the fixpoint computation
	 * 
	 * @throws FixpointException if an error occurs during the semantic
	 *                               computation of a statement, or if some
	 *                               unknown/invalid statement ends up in the
	 *                               working set
	 */
	public <A extends AbstractLattice<A>, D extends AbstractDomain<A>> AnalyzedCFG<A> backwardFixpoint(
			AnalysisState<A> singleton,
			Map<Statement, AnalysisState<A>> startingPoints,
			InterproceduralAnalysis<A, D> interprocedural,
			WorkingSet<Statement> ws,
			FixpointConfiguration conf,
			ScopeId id)
			throws FixpointException {
		// we disable optimizations for ascending phases if there is a
		// descending one: the latter will need full results to start applying
		// glbs/narrowings from a post-fixpoint
		boolean isOptimized = conf.optimize && conf.descendingPhaseType == DescendingPhaseType.NONE;
		BackwardFixpoint<CFG,
				Statement,
				Edge,
				CompoundState<A>> fix = isOptimized ? new OptimizedBackwardFixpoint<>(this, false, conf.hotspots)
						: new BackwardFixpoint<>(this, false);
		BackwardAscendingFixpoint<A, D> asc = new BackwardAscendingFixpoint<>(this, interprocedural, conf);

		Map<Statement, CompoundState<A>> starting = new HashMap<>();
		StatementStore<A> bot = new StatementStore<>(singleton.bottom());
		startingPoints.forEach(
				(
						st,
						state) -> starting.put(st, CompoundState.of(state, bot)));
		Map<Statement, CompoundState<A>> ascending = fix.fixpoint(starting, ws, asc);

		if (conf.descendingPhaseType == DescendingPhaseType.NONE)
			return flatten(isOptimized, singleton, startingPoints, interprocedural, id, ascending);

		fix = conf.optimize ? new OptimizedBackwardFixpoint<>(this, true, conf.hotspots)
				: new BackwardFixpoint<>(this, true);
		Map<Statement, CompoundState<A>> descending;
		switch (conf.descendingPhaseType) {
		case GLB:
			DescendingGLBFixpoint<A, D> dg = new DescendingGLBFixpoint<>(this, interprocedural, conf);
			descending = fix.fixpoint(starting, ws, dg, ascending);
			break;
		case NARROWING:
			DescendingNarrowingFixpoint<A, D> dn = new DescendingNarrowingFixpoint<>(this, interprocedural, conf);
			descending = fix.fixpoint(starting, ws, dn, ascending);
			break;
		case NONE:
		default:
			// should never happen
			descending = ascending;
			break;
		}

		return flatten(conf.optimize, singleton, startingPoints, interprocedural, id, descending);
	}

	@Override
	public SerializableGraph toSerializableGraph(
			BiFunction<CFG, Statement, SerializableValue> descriptionGenerator) {
		return SerializableCFG.fromCFG(this, descriptionGenerator);
	}

	/**
	 * Yields a generic {@link ProgramPoint} happening inside this cfg. A
	 * generic program point can be used for semantic evaluations of
	 * instrumented {@link Statement}s, that are not tied to any concrete
	 * statement.
	 * 
	 * @return a generic program point happening in this cfg
	 */
	public ProgramPoint getGenericProgramPoint() {
		return new ProgramPoint() {

			@Override
			public CFG getCFG() {
				return CFG.this;
			}

			@Override
			public String toString() {
				return "unknown program point in " + CFG.this.getDescriptor().getSignature();
			}

			@Override
			public CodeLocation getLocation() {
				return SyntheticLocation.INSTANCE;
			}

		};
	}

	/**
	 * {@inheritDoc} This method checks that:
	 * <ul>
	 * <li>the underlying adjacency matrix is valid, through
	 * {@link NodeList#validate(Collection)}</li>
	 * <li>all {@link ControlFlowStructure}s of this cfg contains node
	 * effectively in the cfg</li>
	 * <li>all {@link Statement}s that stop the execution (according to
	 * {@link Statement#stopsExecution()}) do not have outgoing edges</li>
	 * <li>all {@link Statement}s that do not have outgoing edges stop the
	 * execution (according to {@link Statement#stopsExecution()})</li>
	 * <li>all entrypoints are effectively part of this cfg</li>
	 * </ul>
	 */
	@Override
	public void validate()
			throws ProgramValidationException {
		try {
			list.validate(entrypoints);
		} catch (ProgramValidationException e) {
			throw new ProgramValidationException("The matrix behind " + this + " is invalid", e);
		}

		for (ControlFlowStructure struct : descriptor.getControlFlowStructures()) {
			for (Statement st : struct.allStatements())
				// we tolerate null values only if its the follower
				if ((st == null && struct.getFirstFollower() != null) || (st != null && !list.containsNode(st)))
					throw new ProgramValidationException(
							this
									+ " has a conditional structure ("
									+ struct
									+ ") that contains a node not in the graph: "
									+ st);
		}

		for (Statement st : list) {
			// no outgoing edges in execution-terminating statements
			// unless they are error-handling ones
			Collection<Edge> outs = list.getOutgoingEdges(st);
			if (st.stopsExecution() && !outs.isEmpty())
				if (!st.throwsError() || outs.stream().anyMatch(Predicate.not(ErrorEdge.class::isInstance)))
					throw new ProgramValidationException(
							this + " contains an execution-stopping node that has followers: " + st);
			if (outs.isEmpty() && !st.stopsExecution() && !st.throwsError())
				throw new ProgramValidationException(
						this + " contains a node with no followers that is not execution-stopping: " + st);
		}

		// all entrypoints should be within the cfg
		if (!list.getNodes().containsAll(entrypoints))
			throw new ProgramValidationException(
					this
							+ " has entrypoints that are not part of the graph: "
							+ new HashSet<>(entrypoints).retainAll(list.getNodes()));
	}

	private Collection<ControlFlowStructure> getControlFlowsContaining(
			ProgramPoint pp) {
		if (!(pp instanceof Statement))
			// synthetic pp
			return Collections.emptyList();

		Statement st = (Statement) pp;
		if (st instanceof Call) {
			Call original = (Call) st;
			while (original.getSource() != null)
				original = original.getSource();
			if (original != st)
				st = original;
		}
		if (st instanceof Expression)
			st = ((Expression) st).getRootStatement();

		Collection<ControlFlowStructure> res = new LinkedList<>();
		for (ControlFlowStructure cf : descriptor.getControlFlowStructures())
			if (cf.contains(st))
				res.add(cf);

		return res;
	}

	/**
	 * Yields {@code true} if and only if the given program point is inside the
	 * body of a {@link ControlFlowStructure}, regardless of its type.<br>
	 * <br>
	 * Note that if no control flow structures have been provided by frontends,
	 * and no attempt at extracting them has been made yet, invoking this method
	 * will cause a {@link ControlFlowExtractor} to try to extract them.
	 * 
	 * @param pp the program point
	 * 
	 * @return {@code true} if {@code pp} is inside a control flow structure
	 */
	public boolean isGuarded(
			ProgramPoint pp) {
		return !getControlFlowsContaining(pp).isEmpty();
	}

	/**
	 * Yields {@code true} if and only if the given program point is inside the
	 * body of a {@link Loop}.<br>
	 * <br>
	 * Note that if no control flow structures have been provided by frontends,
	 * and no attempt at extracting them has been made yet, invoking this method
	 * will cause a {@link ControlFlowExtractor} to try to extract them.
	 * 
	 * @param pp the program point
	 * 
	 * @return {@code true} if {@code pp} is inside a loop
	 */
	public boolean isInsideLoop(
			ProgramPoint pp) {
		return getControlFlowsContaining(pp).stream().anyMatch(Loop.class::isInstance);
	}

	/**
	 * Yields {@code true} if and only if the given program point is inside one
	 * of the branches of an {@link IfThenElse}.<br>
	 * <br>
	 * Note that if no control flow structures have been provided by frontends,
	 * and no attempt at extracting them has been made yet, invoking this method
	 * will cause a {@link ControlFlowExtractor} to try to extract them.
	 * 
	 * @param pp the program point
	 * 
	 * @return {@code true} if {@code pp} is inside an if-then-else
	 */
	public boolean isInsideIfThenElse(
			ProgramPoint pp) {
		return getControlFlowsContaining(pp).stream().anyMatch(IfThenElse.class::isInstance);
	}

	/**
	 * Yields the guard of all the {@link ControlFlowStructure}s, regardless of
	 * their type, containing the given program point. If the program point is
	 * not part of the body of a control structure, this method returns an empty
	 * collection.<br>
	 * <br>
	 * Note that if no control flow structures have been provided by frontends,
	 * and no attempt at extracting them has been made yet, invoking this method
	 * will cause a {@link ControlFlowExtractor} to try to extract them.
	 * 
	 * @param pp the program point
	 * 
	 * @return the collection of the guards of all structures containing
	 *             {@code pp}
	 */
	public Collection<Statement> getGuards(
			ProgramPoint pp) {
		return getControlFlowsContaining(pp).stream()
				.map(ControlFlowStructure::getCondition)
				.collect(Collectors.toList());
	}

	/**
	 * Yields the guard of all {@link Loop}s containing the given program point.
	 * If the program point is not part of the body of a loop, this method
	 * returns an empty collection.<br>
	 * <br>
	 * Note that if no control flow structures have been provided by frontends,
	 * and no attempt at extracting them has been made yet, invoking this method
	 * will cause a {@link ControlFlowExtractor} to try to extract them.
	 * 
	 * @param pp the program point
	 * 
	 * @return the collection of the guards of all loops containing {@code pp}
	 */
	public Collection<Statement> getLoopGuards(
			ProgramPoint pp) {
		return getControlFlowsContaining(pp).stream()
				.filter(Loop.class::isInstance)
				.map(ControlFlowStructure::getCondition)
				.collect(Collectors.toList());
	}

	/**
	 * Yields the guard of all the {@link IfThenElse} containing the given
	 * program point. If the program point is not part of a branch of an
	 * if-then-else, this method returns an empty collection.<br>
	 * <br>
	 * Note that if no control flow structures have been provided by frontends,
	 * and no attempt at extracting them has been made yet, invoking this method
	 * will cause a {@link ControlFlowExtractor} to try to extract them.
	 * 
	 * @param pp the program point
	 * 
	 * @return the collection of the guards of all if-then-elses containing
	 *             {@code pp}
	 */
	public Collection<Statement> getIfThenElseGuards(
			ProgramPoint pp) {
		return getControlFlowsContaining(pp).stream()
				.filter(IfThenElse.class::isInstance)
				.map(ControlFlowStructure::getCondition)
				.collect(Collectors.toList());
	}

	private Statement getRecent(
			ProgramPoint pp,
			Predicate<ControlFlowStructure> filter) {
		if (!(pp instanceof Statement))
			// synthetic pp
			return null;

		Statement st = (Statement) pp;
		Collection<ControlFlowStructure> cfs = getControlFlowsContaining(pp);
		Statement recent = null;
		int min = Integer.MAX_VALUE, m;
		for (ControlFlowStructure cf : cfs)
			if (filter.test(cf))
				if (recent == null) {
					recent = cf.getCondition();
					min = cf.distance(st);
				} else if ((m = cf.distance(st)) < min || min == -1) {
					recent = cf.getCondition();
					min = m;
				}

		if (min == -1)
			throw new IllegalStateException(
					"Conditional flow structures containing "
							+ pp
							+ " could not evaluate the distance from the root of the structure to the statement itself");

		return recent;
	}

	/**
	 * Yields the guard of the most recent {@link ControlFlowStructure},
	 * regardless of its type, containing the given program point. If the
	 * program point is not part of the body of a control structure, this method
	 * returns {@code null}.<br>
	 * <br>
	 * Note that if no control flow structures have been provided by frontends,
	 * and no attempt at extracting them has been made yet, invoking this method
	 * will cause a {@link ControlFlowExtractor} to try to extract them.
	 * 
	 * @param pp the program point
	 * 
	 * @return the most recent if-then-else guard, or {@code null}
	 */
	public Statement getMostRecentGuard(
			ProgramPoint pp) {
		return getRecent(pp, cf -> true);
	}

	/**
	 * Yields the guard of the most recent {@link Loop} containing the given
	 * program point. If the program point is not part of the body of a loop,
	 * this method returns {@code null}.<br>
	 * <br>
	 * Note that if no control flow structures have been provided by frontends,
	 * and no attempt at extracting them has been made yet, invoking this method
	 * will cause a {@link ControlFlowExtractor} to try to extract them.
	 * 
	 * @param pp the program point
	 * 
	 * @return the most recent loop guard, or {@code null}
	 */
	public Statement getMostRecentLoopGuard(
			ProgramPoint pp) {
		return getRecent(pp, Loop.class::isInstance);
	}

	/**
	 * Yields the guard of the most recent {@link IfThenElse} containing the
	 * given program point. If the program point is not part of a branch of an
	 * if-then-else, this method returns {@code null}.<br>
	 * <br>
	 * Note that if no control flow structures have been provided by frontends,
	 * and no attempt at extracting them has been made yet, invoking this method
	 * will cause a {@link ControlFlowExtractor} to try to extract them.
	 * 
	 * @param pp the program point
	 * 
	 * @return the most recent if-then-else guard, or {@code null}
	 */
	public Statement getMostRecentIfThenElseGuard(
			ProgramPoint pp) {
		return getRecent(pp, IfThenElse.class::isInstance);
	}

	/**
	 * Yields the {@link ControlFlowStructure} that uses {@code guard} as
	 * condition, if any.
	 * 
	 * @param guard the condition
	 * 
	 * @return the control flow structure, or {@code null}
	 */
	public ControlFlowStructure getControlFlowStructureOf(
			ProgramPoint guard) {
		for (ControlFlowStructure struct : descriptor.getControlFlowStructures())
			if (struct.getCondition().equals(guard))
				return struct;
		return null;
	}

	/**
	 * {@inheritDoc} <br>
	 * <br>
	 * In a CFG, the normal reasoning is replaced by taking all the {@link Loop}
	 * conditions appearing in the cfg's control flow structures.
	 */
	@Override
	public Collection<Statement> getCycleEntries() {
		Collection<Statement> result = new HashSet<>();

		for (ControlFlowStructure cfs : descriptor.getControlFlowStructures())
			if (cfs instanceof Loop)
				result.add(cfs.getCondition());

		return result;
	}

	/**
	 * Computes the basic blocks of this cfg, that is, the sequences of
	 * statements with no incoming branches (except to the first statement) and
	 * no outgoing branches (except from the last statement). The result of the
	 * computation can be retrieved through {@link #getBasicBlocks()}. <br>
	 * <br>
	 * Note that control flow structures must be available for this task: if
	 * those have not been provided at construction, ensure that
	 * {@link #extractControlFlowStructures(ControlFlowExtractor)} has been
	 * invoked after constructing the cfg.
	 */
	public void computeBasicBlocks() {
		Collection<Statement> leaders = new HashSet<>();
		leaders.addAll(entrypoints);
		for (ControlFlowStructure struct : descriptor.getControlFlowStructures())
			leaders.addAll(struct.getTargetedStatements());
		for (ProtectionBlock block : descriptor.getProtectionBlocks()) {
			leaders.add(block.getTryBlock().getStart());
			if (block.getElseBlock() != null)
				leaders.add(block.getElseBlock().getStart());
			if (block.getFinallyBlock() != null) {
				leaders.add(block.getFinallyBlock().getStart());
				Collection<Statement> body = block.getFinallyBlock().getBody();
				for (Edge e : getEdges())
					if (body.contains(e.getSource()) && !body.contains(e.getDestination()))
						leaders.add(e.getDestination());
			}
			for (CatchBlock cb : block.getCatchBlocks())
				leaders.add(cb.getBody().getStart());
			if (block.getClosing() != null)
				// TODO not having the appropriate statements here might be a
				// problem
				leaders.add(block.getClosing());
		}

		basicBlocks = new IdentityHashMap<>(leaders.size());
		for (Statement leader : leaders) {
			VisitOnceWorkingSet<Statement> ws = VisitOnceFIFOWorkingSet.mk();
			ws.push(leader);
			List<Statement> bb = new LinkedList<>();
			while (!ws.isEmpty()) {
				Statement current = ws.pop();
				bb.add(current);
				Collection<Statement> followers = list.followersOf(current);
				boolean pushed = false;
				for (Statement follower : followers)
					if (leaders.contains(follower))
						continue;
					else if (pushed)
						throw new IllegalStateException(
								"Cannot have statements with more than one follower inside a basic block");
					else {
						ws.push(follower);
						pushed = true;
					}
			}

			basicBlocks.put(leader, bb.toArray(Statement[]::new));
		}
	}

	/**
	 * Yields the basic blocks of this cfg, available only after
	 * {@link #computeBasicBlocks()} has been invoked.
	 * 
	 * @return the basic blocks, returned as pairs in the form of &lt;leader,
	 *             block&gt;
	 * 
	 * @throws IllegalStateException if {@link #computeBasicBlocks()} has not
	 *                                   been invoked first
	 */
	public Map<Statement, Statement[]> getBasicBlocks() {
		if (basicBlocks == null)
			throw new IllegalStateException("Cannot retrieve basic blocks before computing them");
		return basicBlocks;
	}

	/**
	 * Yields the protection blocks that enclose the given program point. The
	 * blocks are returned in order, from the inner-most to the outer-most.
	 * 
	 * @param pp the program point
	 * 
	 * @return the protection blocks
	 */
	public List<ProtectionBlock> getProtectionsOf(
			ProgramPoint pp) {
		if (!(pp instanceof Statement))
			// synthetic pp
			return List.of();

		Statement st = (Statement) pp;
		List<ProtectionBlock> blocks = new LinkedList<>();

		for (ProtectionBlock pb : getDescriptor().getProtectionBlocks())
			if (pb.getTryBlock().getBody().contains(st))
				blocks.add(pb);

		if (blocks.isEmpty())
			return List.of();
		if (blocks.size() == 1)
			return blocks;

		blocks.sort((
				b1,
				b2) -> {
			if (b1 == b2)
				return 0;
			if (b2.getTryBlock().getBody().containsAll(b1.getFullBody(true)))
				return -1;
			return 1;
		});

		return blocks;
	}

}

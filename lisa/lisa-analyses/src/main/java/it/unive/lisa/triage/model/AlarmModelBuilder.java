package it.unive.lisa.triage.model;

import it.unive.lisa.analysis.AbstractDomain;
import it.unive.lisa.analysis.AbstractLattice;
import it.unive.lisa.analysis.AnalysisState;
import it.unive.lisa.analysis.AnalysisState.Error;
import it.unive.lisa.analysis.AnalyzedCFG;
import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.checks.semantic.SemanticTool;
import it.unive.lisa.checks.verification.ConditionViolation;
import it.unive.lisa.checks.verification.ErrorViolation;
import it.unive.lisa.checks.verification.ReachViolation;
import it.unive.lisa.checks.verification.Violation;
import it.unive.lisa.lattices.Satisfiability;
import it.unive.lisa.program.cfg.Parameter;
import it.unive.lisa.program.cfg.statement.Expression;
import it.unive.lisa.program.cfg.statement.Statement;
import it.unive.lisa.symbolic.SymbolicExpression;
import it.unive.lisa.symbolic.value.BinaryExpression;
import it.unive.lisa.symbolic.value.Identifier;
import it.unive.lisa.symbolic.value.PushAny;
import it.unive.lisa.symbolic.value.TernaryExpression;
import it.unive.lisa.symbolic.value.UnaryExpression;
import it.unive.lisa.symbolic.value.operator.binary.LogicalAnd;
import it.unive.lisa.symbolic.value.operator.unary.LogicalNegation;
import it.unive.lisa.triage.encoding.SortPolicy;
import it.unive.lisa.triage.inputs.InputSource;
import it.unive.lisa.triage.inputs.InputSourceResolver;
import it.unive.lisa.type.Type;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Builds the {@link AlarmModel} of a candidate violation, given the results of
 * an analysis. The locations of the model are the nodes of the cfg that can
 * reach the bad location (its <i>cone</i>): the node of the statement on which
 * the warning would be issued, or, for {@link ConditionViolation}s, the node
 * containing the statement after which the condition is evaluated.
 *
 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
 */
public final class AlarmModelBuilder {

	private AlarmModelBuilder() {
	}

	/**
	 * Builds the model of the given violation.
	 *
	 * @param <A>       the type of the abstract states of the analysis
	 * @param <D>       the type of the domain of the analysis
	 * @param tool      the tool containing the results of the analysis
	 * @param result    the result containing the statement
	 * @param st        the statement on which the warning would be issued
	 * @param violation the violation
	 * @param maxCases  the maximum number of cases of each transition
	 *
	 * @return the model
	 *
	 * @throws SemanticException if something goes wrong while building the
	 *                               model
	 */
	public static <A extends AbstractLattice<A>, D extends AbstractDomain<A>> AlarmModel build(
			SemanticTool<A, D> tool,
			AnalyzedCFG<A> result,
			Statement st,
			Violation violation,
			int maxCases)
			throws SemanticException {
		Statement bad = violation instanceof ConditionViolation
				? rootOf(((ConditionViolation) violation).getWhere())
				: rootOf(st);

		Set<Statement> cone = cone(result, bad);
		Set<Statement> initial = new LinkedHashSet<>(result.getEntrypoints());
		initial.retainAll(cone);

		InputSourceResolver resolver = new InputSourceResolver(tool);
		FreshValueFactory factory = new FreshValueFactory(resolver, new HeapModel(result.getProgram()));
		TransitionExtractor<A, D> extractor = new TransitionExtractor<>(tool, result, factory, maxCases);
		Map<Statement, NodeTransitions> transitions = new HashMap<>();
		Map<Statement, AnalysisState<A>> preStates = new HashMap<>();
		for (Statement node : cone) {
			transitions.put(node, extractor.extract(node));
			preStates.put(node, extractor.preStateOf(node));
		}

		List<BadCase> badCases = badCases(transitions.get(bad), violation, factory);

		Map<Identifier, InputSource> parameters = new HashMap<>();
		for (Parameter formal : result.getDescriptor().getFormals()) {
			InputSource source = resolver.resolve(new PushAny(formal.getStaticType(), formal.getLocation()), null);
			parameters.put(formal.toSymbolicVariable(), source);
		}

		Set<Identifier> vocabulary = new LinkedHashSet<>(parameters.keySet());
		for (NodeTransitions tr : transitions.values())
			collect(tr, vocabulary);
		for (BadCase c : badCases) {
			for (SymbolicExpression cond : c.getPathCondition())
				collect(cond, vocabulary);
			if (c.getViolation() != null)
				collect(c.getViolation(), vocabulary);
		}

		ConstraintExporter<A, D> exporter = new ConstraintExporter<>(tool.getAnalysis().domain, result.getId());
		Map<Statement, Set<SymbolicExpression>> seeds = new HashMap<>();
		Set<SymbolicExpression> conditions = conditions(transitions.values(), badCases);
		for (Statement node : cone) {
			Set<SymbolicExpression> cs = exporter.constraints(preStates.get(node), vocabulary, node);
			if (cs == null)
				continue;
			// conditions of the model whose truth is decided by the analysis
			// are invariants, even if the domain cannot express them as
			// constraints
			for (SymbolicExpression cond : conditions)
				try {
					Satisfiability sat = tool.getAnalysis().satisfies(preStates.get(node), cond, result.getId(),
							node);
					if (sat == Satisfiability.SATISFIED)
						cs.add(cond);
					else if (sat == Satisfiability.NOT_SATISFIED)
						cs.add(new UnaryExpression(cond.getStaticType(), cond, LogicalNegation.INSTANCE,
								cond.getCodeLocation()));
				} catch (SemanticException e) {
					// the condition cannot be evaluated in this state
				}
			seeds.put(node, cs);
		}
		Set<SymbolicExpression> init = new HashSet<>();
		if (!initial.isEmpty()) {
			Set<SymbolicExpression> cs = exporter.constraints(result.getEntryState(), vocabulary,
					initial.iterator().next());
			if (cs != null)
				init.addAll(cs);
		}

		Map<Identifier, Set<Type>> types = new HashMap<>();
		for (Statement node : cone) {
			AnalysisState<A> pre = preStates.get(node);
			if (pre.getExecution().isBottom())
				continue;
			for (Identifier id : vocabulary)
				if (pre.getExecutionState().knowsIdentifier(id))
					types.computeIfAbsent(id, i -> new HashSet<>())
							.addAll(extractor.getAnalysis().getRuntimeTypesOf(pre, id, result.getId(), node));
		}
		for (FreshValue fresh : factory.getValues().values())
			if (!fresh.getStaticType().isUntyped())
				types.put(fresh, Set.of(fresh.getStaticType()));

		boolean confirmable = result.getId().isStartingId()
				&& result.getProgram().getEntryPoints().stream()
						.anyMatch(cfg -> cfg.getDescriptor().equals(result.getDescriptor()));

		return new AlarmModel(
				result,
				bad,
				cone,
				initial,
				transitions,
				init,
				badCases,
				seeds,
				vocabulary,
				new SortPolicy(types, bad.getProgram().getTypes().getTypes()),
				factory.getValues(),
				parameters,
				confirmable);
	}

	private static Statement rootOf(
			Statement st) {
		return st instanceof Expression ? ((Expression) st).getRootStatement() : st;
	}

	/**
	 * The nodes that can reach the given one, including the node itself.
	 */
	private static Set<Statement> cone(
			AnalyzedCFG<?> result,
			Statement node) {
		Set<Statement> cone = new LinkedHashSet<>();
		LinkedList<Statement> ws = new LinkedList<>();
		ws.add(node);
		while (!ws.isEmpty()) {
			Statement current = ws.removeFirst();
			if (cone.add(current))
				ws.addAll(result.predecessorsOf(current));
		}
		return cone;
	}

	private static List<BadCase> badCases(
			NodeTransitions bad,
			Violation violation,
			FreshValueFactory factory) {
		List<BadCase> cases = new ArrayList<>();
		if (violation instanceof ReachViolation)
			// reaching the bad location is the violation
			cases.add(new BadCase(List.of(), null));
		else if (violation instanceof ConditionViolation) {
			ConditionViolation cond = (ConditionViolation) violation;
			SymbolicStore store = bad.getPartial(cond.getWhere());
			if (store != null)
				for (SymbolicCase c : store.getCases())
					cases.add(new BadCase(
							c.getPathCondition(),
							c.substitute(cond.getCondition(), factory, cond.getWhere())));
		} else if (violation instanceof ErrorViolation) {
			ErrorViolation err = (ErrorViolation) violation;
			for (Map.Entry<Error, SymbolicStore> e : bad.getErrors().entrySet())
				if (err.matches(e.getKey().getType()))
					for (SymbolicCase c : e.getValue().getCases())
						cases.add(new BadCase(c.getPathCondition(), null));
		}
		return cases;
	}

	/**
	 * Yields the conjuncts of the path conditions of the transitions and of the
	 * bad cases that do not contain fresh values nor the heap model, that is,
	 * the ones that can be evaluated in the states computed by the analysis.
	 */
	private static Set<SymbolicExpression> conditions(
			Collection<NodeTransitions> transitions,
			List<BadCase> badCases) {
		List<SymbolicExpression> all = new ArrayList<>();
		for (NodeTransitions tr : transitions)
			for (EdgeTransition e : tr.getOutgoing())
				for (SymbolicCase c : e.getStore().getCases())
					all.addAll(c.getPathCondition());
		for (BadCase c : badCases) {
			all.addAll(c.getPathCondition());
			if (c.getViolation() != null)
				all.add(c.getViolation());
		}
		Set<SymbolicExpression> result = new LinkedHashSet<>();
		for (SymbolicExpression e : all)
			conjuncts(e, result);
		return result;
	}

	private static void conjuncts(
			SymbolicExpression e,
			Set<SymbolicExpression> result) {
		if (e instanceof BinaryExpression && ((BinaryExpression) e).getOperator() == LogicalAnd.INSTANCE) {
			conjuncts(((BinaryExpression) e).getLeft(), result);
			conjuncts(((BinaryExpression) e).getRight(), result);
		} else if (!hasFreshValues(e) && !HeapModel.mentionsHeapModel(e))
			// the analysis cannot evaluate the heap model
			result.add(e);
	}

	private static boolean hasFreshValues(
			SymbolicExpression e) {
		if (e instanceof FreshValue)
			return true;
		if (e instanceof UnaryExpression)
			return hasFreshValues(((UnaryExpression) e).getExpression());
		if (e instanceof BinaryExpression)
			return hasFreshValues(((BinaryExpression) e).getLeft())
					|| hasFreshValues(((BinaryExpression) e).getRight());
		if (e instanceof TernaryExpression)
			return hasFreshValues(((TernaryExpression) e).getLeft())
					|| hasFreshValues(((TernaryExpression) e).getMiddle())
					|| hasFreshValues(((TernaryExpression) e).getRight());
		return false;
	}

	private static void collect(
			NodeTransitions tr,
			Set<Identifier> ids) {
		List<SymbolicStore> stores = new ArrayList<>();
		for (EdgeTransition e : tr.getOutgoing())
			stores.add(e.getStore());
		if (tr.getExit() != null)
			stores.add(tr.getExit());
		stores.addAll(tr.getErrors().values());
		for (SymbolicStore store : stores)
			for (SymbolicCase c : store.getCases()) {
				for (SymbolicExpression cond : c.getPathCondition())
					collect(cond, ids);
				for (Map.Entry<Identifier, SymbolicExpression> u : c.getUpdates().entrySet()) {
					collect(u.getKey(), ids);
					collect(u.getValue(), ids);
				}
			}
	}

	private static void collect(
			SymbolicExpression e,
			Set<Identifier> ids) {
		if (e instanceof FreshValue)
			return;
		if (e instanceof Identifier)
			ids.add((Identifier) e);
		else if (e instanceof UnaryExpression)
			collect(((UnaryExpression) e).getExpression(), ids);
		else if (e instanceof BinaryExpression) {
			collect(((BinaryExpression) e).getLeft(), ids);
			collect(((BinaryExpression) e).getRight(), ids);
		} else if (e instanceof TernaryExpression) {
			collect(((TernaryExpression) e).getLeft(), ids);
			collect(((TernaryExpression) e).getMiddle(), ids);
			collect(((TernaryExpression) e).getRight(), ids);
		}
	}
}

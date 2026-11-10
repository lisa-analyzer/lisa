package it.unive.lisa.triage.pdr;

import it.unive.lisa.analysis.AbstractDomain;
import it.unive.lisa.analysis.AbstractLattice;
import it.unive.lisa.analysis.AnalysisState;
import it.unive.lisa.analysis.AnalyzedCFG;
import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.checks.semantic.SemanticTool;
import it.unive.lisa.checks.verification.Violation;
import it.unive.lisa.program.SyntheticLocation;
import it.unive.lisa.program.cfg.CFG;
import it.unive.lisa.program.cfg.CodeMember;
import it.unive.lisa.program.cfg.Parameter;
import it.unive.lisa.program.cfg.statement.Return;
import it.unive.lisa.program.cfg.statement.Statement;
import it.unive.lisa.symbolic.SymbolicExpression;
import it.unive.lisa.symbolic.value.BinaryExpression;
import it.unive.lisa.symbolic.value.Constant;
import it.unive.lisa.symbolic.value.Identifier;
import it.unive.lisa.symbolic.value.UnaryExpression;
import it.unive.lisa.symbolic.value.Variable;
import it.unive.lisa.symbolic.value.operator.binary.BinaryOperator;
import it.unive.lisa.symbolic.value.operator.binary.ComparisonEq;
import it.unive.lisa.symbolic.value.operator.binary.ComparisonGe;
import it.unive.lisa.symbolic.value.operator.binary.ComparisonLe;
import it.unive.lisa.symbolic.value.operator.binary.LogicalAnd;
import it.unive.lisa.symbolic.value.operator.binary.Numeric32BitAdd;
import it.unive.lisa.symbolic.value.operator.binary.Numeric64BitAdd;
import it.unive.lisa.symbolic.value.operator.binary.NumericNonOverflowingAdd;
import it.unive.lisa.symbolic.value.operator.unary.LogicalNegation;
import it.unive.lisa.triage.encoding.ExpressionEncoderFactory;
import it.unive.lisa.triage.model.AlarmModel;
import it.unive.lisa.triage.model.AlarmModelBuilder;
import it.unive.lisa.triage.model.EdgeTransition;
import it.unive.lisa.triage.model.ModelLocation;
import it.unive.lisa.triage.model.ReplayInterprocedural;
import it.unive.lisa.triage.model.ResultCache;
import it.unive.lisa.triage.model.SymbolicCase;
import it.unive.lisa.triage.smt.SmtException;
import it.unive.lisa.triage.smt.SmtSolver;
import it.unive.lisa.type.Type;
import it.unive.lisa.type.TypeSystem;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;

/**
 * The computation of the summaries of recursive members (see
 * {@link ResultCache#summaryOf(CodeMember)}), in the style of Houdini: for each
 * member that calls itself (directly or through other members), the candidate
 * facts are simple relations between its returned value and its integer formal
 * parameters (equalities, inequalities, sums, signs). A candidate is kept if,
 * at every return of the member, in every result of the analysis, the returned
 * value satisfies it, assuming that the remaining candidates hold for the
 * values returned by the recursive calls (that are not expanded): by induction
 * on the depth of the recursion, the kept facts hold whenever the member
 * returns. Candidates over formal parameters that the member assigns are not
 * kept, since summaries relate the returned value to the values of the
 * parameters when the member is called.
 *
 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
 */
public final class RecursionSummaries {

	/**
	 * The maximum number of queries of each check of the candidates at a
	 * return.
	 */
	public static final int MAX_QUERIES = 500;

	/**
	 * The maximum level of each check of the candidates at a return.
	 */
	public static final int MAX_LEVEL = 30;

	private RecursionSummaries() {
	}

	/**
	 * Computes the summaries of the recursive members of the analyzed program,
	 * storing them in the given cache, and records that the summaries have been
	 * computed. If the given time passes before the candidates are decided, no
	 * summary is kept.
	 *
	 * @param <A>      the type of the abstract states of the analysis
	 * @param <D>      the type of the domain of the analysis
	 * @param tool     the tool containing the results of the analysis
	 * @param cache    the cache where summaries are stored
	 * @param maxCases the maximum number of cases of each transition
	 * @param solvers  the supplier of the solvers to use
	 * @param encoders the factory of the encoders of expressions
	 * @param deadline the time by which the computation must end
	 *
	 * @throws SemanticException if something goes wrong while building the
	 *                               models of the members
	 */
	public static <A extends AbstractLattice<A>, D extends AbstractDomain<A>> void compute(
			SemanticTool<A, D> tool,
			ResultCache<A> cache,
			int maxCases,
			Supplier<SmtSolver> solvers,
			ExpressionEncoderFactory encoders,
			long deadline)
			throws SemanticException {
		cache.setSummarized();
		Map<CFG, List<SymbolicExpression>> candidates = new LinkedHashMap<>();
		for (CFG cfg : tool.getResults().keySet())
			if (isRecursive(tool, cfg)) {
				List<SymbolicExpression> cands = candidates(cfg);
				if (!cands.isEmpty())
					candidates.put(cfg, cands);
			}
		if (candidates.isEmpty())
			return;

		boolean decided = false;
		// the conditions of the models of the members relate their returned
		// values to their parameters, and might be expensive for the analysis
		// to decide: they are not used as seeds
		cache.setDecidesConditions(false);
		try (SmtSolver solver = solvers.get()) {
			decided = houdini(tool, cache, candidates, maxCases, solver, encoders, deadline);
		} catch (SmtException e) {
			decided = false;
		} finally {
			cache.setDecidesConditions(true);
		}
		for (Map.Entry<CFG, List<SymbolicExpression>> entry : candidates.entrySet())
			cache.setSummary(entry.getKey(), decided ? entry.getValue() : Collections.emptyList());
	}

	/**
	 * Drops the candidates that are not preserved until the remaining ones are,
	 * yielding whether this has been decided within the deadline.
	 */
	private static <A extends AbstractLattice<A>, D extends AbstractDomain<A>> boolean houdini(
			SemanticTool<A, D> tool,
			ResultCache<A> cache,
			Map<CFG, List<SymbolicExpression>> candidates,
			int maxCases,
			SmtSolver solver,
			ExpressionEncoderFactory encoders,
			long deadline)
			throws SemanticException {
		boolean changed = true;
		while (changed) {
			changed = false;
			// the recursive calls assume the remaining candidates
			for (Map.Entry<CFG, List<SymbolicExpression>> entry : candidates.entrySet())
				cache.setSummary(entry.getKey(), new ArrayList<>(entry.getValue()));
			for (Map.Entry<CFG, List<SymbolicExpression>> entry : candidates.entrySet()) {
				List<SymbolicExpression> current = entry.getValue();
				AnalyzedCFG<A> result = allExecutions(tool, cache, entry.getKey());
				if (result == null) {
					// the executions of the member cannot be described
					current.clear();
					continue;
				}
				for (Statement node : entry.getKey().getNodes())
						if (node instanceof Return && !current.isEmpty()) {
							if (System.currentTimeMillis() >= deadline)
								return false;
							int before = current.size();
							Boolean ok = check(tool, cache, result, (Return) node, current, maxCases, solver,
									encoders, deadline);
							if (ok == null)
								return false;
							if (current.size() != before)
								changed = true;
						}
			}
		}
		return true;
	}

	/**
	 * Yields a result describing all the executions of the given cfg: its only
	 * result, or the one merging all its results (e.g., one for each depth of
	 * the recursion), or {@code null} if they cannot be merged.
	 */
	private static <A extends AbstractLattice<A>, D extends AbstractDomain<A>> AnalyzedCFG<A> allExecutions(
			SemanticTool<A, D> tool,
			ResultCache<A> cache,
			CFG cfg) {
		Collection<AnalyzedCFG<A>> results = tool.getResultOf(cfg);
		if (results.isEmpty())
			return null;
		AnalyzedCFG<A> first = results.iterator().next();
		if (results.size() == 1)
			return first;
		return cache.merged(results, first.getId(), () -> ReplayInterprocedural.merge(cfg, results, first.getId()));
	}

	/**
	 * Drops from the given candidates the ones that do not hold for the values
	 * returned by the given return of the given result. Yields {@code null} if
	 * the check could not be completed within the deadline.
	 */
	private static <A extends AbstractLattice<A>, D extends AbstractDomain<A>> Boolean check(
			SemanticTool<A, D> tool,
			ResultCache<A> cache,
			AnalyzedCFG<A> result,
			Return ret,
			List<SymbolicExpression> candidates,
			int maxCases,
			SmtSolver solver,
			ExpressionEncoderFactory encoders,
			long deadline)
			throws SemanticException {
		Statement returned = ret.getSubExpression();
		AnalysisState<A> post = result.getAnalysisStateAfter(returned);
		if (post.getExecution().isBottom())
			// the return is never reached
			return true;
		Collection<SymbolicExpression> values = post.getExecutionExpressions().elements();
		if (values.isEmpty())
			return true;
		TypeSystem types = ret.getProgram().getTypes();
		// candidates are checked one by one: conditions made of many of them
		// might be expensive for the analysis to evaluate (when building the
		// models)
		List<SymbolicExpression> kept = new ArrayList<>();
		for (SymbolicExpression candidate : candidates) {
			Boolean one = holds(tool, cache, result, ret, returned, values, List.of(candidate), types, maxCases,
					solver, encoders, deadline);
			if (one == null)
				return null;
			if (one)
				kept.add(candidate);
		}
		candidates.retainAll(kept);
		return true;
	}

	/**
	 * Yields whether the conjunction of the given candidates holds for each of
	 * the given values returned by the given return (all the candidates
	 * failing if the model assigns a formal parameter they mention), or
	 * {@code null} if this could not be decided within the deadline.
	 */
	private static <A extends AbstractLattice<A>, D extends AbstractDomain<A>> Boolean holds(
			SemanticTool<A, D> tool,
			ResultCache<A> cache,
			AnalyzedCFG<A> result,
			Return ret,
			Statement returned,
			Collection<SymbolicExpression> values,
			List<SymbolicExpression> candidates,
			TypeSystem types,
			int maxCases,
			SmtSolver solver,
			ExpressionEncoderFactory encoders,
			long deadline)
			throws SemanticException {
		for (SymbolicExpression value : values) {
			SymbolicExpression conjunction = null;
			for (SymbolicExpression c : candidates) {
				SymbolicExpression instance = c.replace(ResultCache.RETURNED, value);
				conjunction = conjunction == null ? instance
						: new BinaryExpression(types.getBooleanType(), conjunction, instance, LogicalAnd.INSTANCE,
								SyntheticLocation.INSTANCE);
			}
			SymbolicExpression violated = new UnaryExpression(types.getBooleanType(), conjunction,
					LogicalNegation.INSTANCE, SyntheticLocation.INSTANCE);
			// the values of the formal parameters are in the ranges of their
			// types
			for (SymbolicExpression range : ranges(ret.getCFG(), types))
				violated = new BinaryExpression(types.getBooleanType(), range, violated, LogicalAnd.INSTANCE,
						SyntheticLocation.INSTANCE);
			AlarmModel model = AlarmModelBuilder.build(tool, result, ret, Violation.condition(returned, violated),
					maxCases, Collections.emptySet(), AlarmModelBuilder.DEFAULT_MAX_LOCATIONS,
					Collections.emptyList(), deadline, cache);
			if (model == null)
				return null;
			if (model.getSeeds(model.getBadLocation()) == null || model.getBadCases().isEmpty())
				continue;
			if (assigns(model, mentioned(candidates, ret)))
				return false;
			solver.reset();
			PdrEngine engine = new PdrEngine(model, solver, false, encoders);
			// the models of the bodies of the members are small
			engine.setInvariants(false);
			PdrEngine.Status status = engine.run(MAX_LEVEL, MAX_QUERIES,
					Math.max(1, deadline - System.currentTimeMillis()));
			if (status == PdrEngine.Status.BUDGET_EXHAUSTED && System.currentTimeMillis() >= deadline)
				return null;
			if (status != PdrEngine.Status.SAFE)
				return false;
		}
		return true;
	}

	/**
	 * Yields the conditions stating that the integer formal parameters of the
	 * given cfg are in the ranges of their types, if their widths are known.
	 */
	private static List<SymbolicExpression> ranges(
			CFG cfg,
			TypeSystem types) {
		List<SymbolicExpression> result = new ArrayList<>();
		for (Parameter formal : cfg.getDescriptor().getFormals()) {
			Type type = formal.getStaticType();
			if (!isIntegral(type))
				continue;
			int bits = type.asNumericType().getNBits();
			if (bits <= 0 || bits > 64)
				continue;
			long min = type.asNumericType().isUnsigned() ? 0 : -(1L << (bits - 1));
			long max = bits == 64 ? Long.MAX_VALUE
					: type.asNumericType().isUnsigned() ? (1L << bits) - 1 : (1L << (bits - 1)) - 1;
			Variable var = ResultCache.formal(formal);
			result.add(relation(types, var, new Constant(type, min, SyntheticLocation.INSTANCE),
					ComparisonGe.INSTANCE));
			result.add(relation(types, var, new Constant(type, max, SyntheticLocation.INSTANCE),
					ComparisonLe.INSTANCE));
		}
		return result;
	}

	/**
	 * Yields the formal parameters (as variables) mentioned by the given
	 * candidates of the member containing the given statement.
	 */
	private static Set<Identifier> mentioned(
			List<SymbolicExpression> candidates,
			Statement st) {
		Set<Identifier> result = new HashSet<>();
		for (Parameter formal : st.getCFG().getDescriptor().getFormals()) {
			Variable var = ResultCache.formal(formal);
			for (SymbolicExpression c : candidates)
				if (!c.replace(var, ResultCache.RETURNED).equals(c))
					result.add(var);
		}
		return result;
	}

	/**
	 * Yields whether some transition of the given model assigns one of the
	 * given identifiers.
	 */
	private static boolean assigns(
			AlarmModel model,
			Set<Identifier> ids) {
		for (ModelLocation loc : model.getLocations())
			for (EdgeTransition e : model.getTransitions(loc))
				for (SymbolicCase c : e.getStore().getCases())
					for (Identifier id : c.getUpdates().keySet())
						if (ids.contains(id))
							return true;
		return false;
	}

	/**
	 * Yields whether the given cfg might call itself, directly or through other
	 * members.
	 */
	private static boolean isRecursive(
			SemanticTool<?, ?> tool,
			CFG cfg) {
		Set<CodeMember> seen = new HashSet<>();
		LinkedList<CodeMember> ws = new LinkedList<>(callees(tool, cfg));
		while (!ws.isEmpty()) {
			CodeMember current = ws.removeFirst();
			if (current.equals(cfg))
				return true;
			if (seen.add(current))
				ws.addAll(callees(tool, current));
		}
		return false;
	}

	/**
	 * Yields the callees of the given member, or none if it is not part of the
	 * call graph (e.g., it is never called).
	 */
	private static Collection<CodeMember> callees(
			SemanticTool<?, ?> tool,
			CodeMember member) {
		try {
			return tool.getCallees(member);
		} catch (IllegalArgumentException e) {
			return Collections.emptyList();
		}
	}

	/**
	 * Yields the candidate facts of the summary of the given cfg, if it returns
	 * an integer.
	 */
	private static List<SymbolicExpression> candidates(
			CFG cfg) {
		Type returned = cfg.getDescriptor().getReturnType();
		if (!isIntegral(returned))
			return Collections.emptyList();
		TypeSystem types = cfg.getProgram().getTypes();
		List<Variable> formals = new ArrayList<>();
		for (Parameter formal : cfg.getDescriptor().getFormals())
			if (isIntegral(formal.getStaticType()))
				formals.add(ResultCache.formal(formal));
		Set<SymbolicExpression> result = new LinkedHashSet<>();
		Variable ret = ResultCache.RETURNED;
		result.add(relation(types, ret, new Constant(returned, 0, SyntheticLocation.INSTANCE),
				ComparisonGe.INSTANCE));
		for (int i = 0; i < formals.size(); i++) {
			Variable f = formals.get(i);
			result.add(relation(types, ret, f, ComparisonEq.INSTANCE));
			result.add(relation(types, ret, f, ComparisonGe.INSTANCE));
			result.add(relation(types, ret, f, ComparisonLe.INSTANCE));
			for (int j = i + 1; j < formals.size(); j++)
				for (BinaryOperator add : additions(returned)) {
					SymbolicExpression sum = new BinaryExpression(returned, f, formals.get(j), add,
							SyntheticLocation.INSTANCE);
					result.add(relation(types, ret, sum, ComparisonEq.INSTANCE));
				}
		}
		return new ArrayList<>(result);
	}

	/**
	 * The additions over values of the given type: the mathematical one, and
	 * the one wrapping around the bit width of the type, if it has one.
	 */
	private static List<BinaryOperator> additions(
			Type type) {
		List<BinaryOperator> result = new ArrayList<>();
		result.add(NumericNonOverflowingAdd.INSTANCE);
		if (type.isNumericType() && type.asNumericType().is32Bits())
			result.add(Numeric32BitAdd.INSTANCE);
		else if (type.isNumericType() && type.asNumericType().is64Bits())
			result.add(Numeric64BitAdd.INSTANCE);
		return result;
	}

	private static SymbolicExpression relation(
			TypeSystem types,
			SymbolicExpression left,
			SymbolicExpression right,
			BinaryOperator op) {
		return new BinaryExpression(types.getBooleanType(), left, right, op, SyntheticLocation.INSTANCE);
	}

	private static boolean isIntegral(
			Type type) {
		return type.isNumericType() && type.asNumericType().isIntegral();
	}
}

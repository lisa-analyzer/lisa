package it.unive.lisa.cron;

import it.unive.lisa.AnalysisExecutionException;
import it.unive.lisa.AnalysisSetupException;
import it.unive.lisa.analysis.AbstractDomain;
import it.unive.lisa.analysis.AbstractLattice;
import it.unive.lisa.analysis.AnalysisState;
import it.unive.lisa.analysis.AnalyzedCFG;
import it.unive.lisa.analysis.Lattice;
import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.analysis.SimpleAbstractDomain;
import it.unive.lisa.analysis.combination.constraints.WholeValue;
import it.unive.lisa.analysis.combination.constraints.WholeValueAnalysis;
import it.unive.lisa.analysis.combination.smash.SmashedSum;
import it.unive.lisa.analysis.combination.smash.SmashedSumStringDomain;
import it.unive.lisa.analysis.combination.smash.SmashedValue;
import it.unive.lisa.analysis.memory.MonolithicMemory;
import it.unive.lisa.analysis.nonrelational.value.BooleanPowerset;
import it.unive.lisa.analysis.nonrelational.value.ValueEnvironment;
import it.unive.lisa.analysis.numeric.IntegerConstantPropagation;
import it.unive.lisa.analysis.numeric.Interval;
import it.unive.lisa.analysis.string.BoundedStringSet;
import it.unive.lisa.analysis.string.CharInclusion;
import it.unive.lisa.analysis.string.Prefix;
import it.unive.lisa.analysis.string.Suffix;
import it.unive.lisa.analysis.string.tarsis.Tarsis;
import it.unive.lisa.analysis.traces.TracePartitioning;
import it.unive.lisa.analysis.types.InferredTypes;
import it.unive.lisa.analysis.value.StringAbstraction;
import it.unive.lisa.analysis.value.ValueDomain;
import it.unive.lisa.checks.semantic.SemanticCheck;
import it.unive.lisa.checks.semantic.SemanticTool;
import it.unive.lisa.imp.constructs.StringContains.IMPStringContains;
import it.unive.lisa.imp.expressions.IMPAssert;
import it.unive.lisa.interprocedural.ReturnTopPolicy;
import it.unive.lisa.interprocedural.callgraph.RTACallGraph;
import it.unive.lisa.interprocedural.context.ContextBasedAnalysis;
import it.unive.lisa.lattices.Satisfiability;
import it.unive.lisa.lattices.SimpleAbstractState;
import it.unive.lisa.outputs.JSONResults;
import it.unive.lisa.program.SourceCodeLocation;
import it.unive.lisa.program.cfg.CFG;
import it.unive.lisa.program.cfg.CodeLocation;
import it.unive.lisa.program.cfg.fixpoints.optforward.OptimizedForwardAscendingFixpoint;
import it.unive.lisa.program.cfg.statement.BinaryExpression;
import it.unive.lisa.program.cfg.statement.Expression;
import it.unive.lisa.program.cfg.statement.Statement;
import it.unive.lisa.program.cfg.statement.literal.StringLiteral;
import it.unive.lisa.symbolic.SymbolicExpression;
import it.unive.lisa.symbolic.value.Identifier;
import it.unive.lisa.util.testing.TestConfiguration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;

public class WholeValueAnalysesTest
		extends
		IMPCronExecutor {

	private static class AssertionCheck<A extends AbstractLattice<A>,
			D extends AbstractDomain<A>>
			implements
			SemanticCheck<A, D> {

		private boolean first = true;

		private Map<CodeLocation, String> assertions = new HashMap<>();

		private Lattice<?> valuesAtFirstAssertion;

		@SuppressWarnings({ "unchecked", "rawtypes" })
		@Override
		public boolean visit(
				SemanticTool<A, D> tool,
				CFG graph,
				Statement node) {

			if (node instanceof IMPAssert) {
				Expression assertion = ((IMPAssert) node).getSubExpression();
				Expression target = ((BinaryExpression) assertion).getLeft();
				Expression conditional = ((BinaryExpression) assertion).getRight();

				for (AnalyzedCFG<A> res : tool.getResultOf(graph)) {
					AnalysisState<A> post = res.getAnalysisStateAfter(assertion);
					AnalysisState<A> targetPost = res.getAnalysisStateAfter(target);

					try {
						A state = (A) post.getLatticeInstance(SimpleAbstractState.class);
						D domain;
						if (tool.getAnalysis().domain instanceof TracePartitioning)
							domain = ((TracePartitioning<A, D>) tool.getAnalysis().domain).domain;
						else
							domain = tool.getAnalysis().domain;

						if (first) {
							first = false;
							ValueEnvironment<?> env = null;
							if (domain instanceof WholeValueAnalysis) {
								ValueDomain<?>[] participants = ((WholeValueAnalysis) domain).participants;
								for (int i = 0; i < participants.length; i++)
									if (participants[i] instanceof StringAbstraction)
										env = (ValueEnvironment<?>) targetPost.getExecutionState()
												.getLatticeInstance(WholeValue.class)
												.get(i);
							} else
								env = targetPost.getExecutionState().getLatticeInstance(ValueEnvironment.class);
							Lattice vals = null;
							for (SymbolicExpression expr : targetPost.getExecutionExpressions()) {
								Lattice val = env.getState((Identifier) expr);
								if (vals == null)
									vals = val;
								else
									vals.lub(val);
							}
							valuesAtFirstAssertion = vals;
						}

						if (assertion instanceof IMPStringContains) {
							StringLiteral ch = (StringLiteral) conditional;
							if (ch.getValue().length() == 1)
								containsCharAssertion(tool, node, res, target, ch);
							else
								assertion(tool, node, post, domain, state);
						} else
							assertion(tool, node, post, domain, state);
					} catch (SemanticException e) {
						throw new AnalysisExecutionException("Error while checking assertions", e);
					}
				}
			}
			return true;
		}

		@SuppressWarnings({ "unchecked", "rawtypes" })
		private void containsCharAssertion(
				SemanticTool<A, D> tool,
				Statement node,
				AnalyzedCFG<A> res,
				Expression variable,
				StringLiteral ch)
				throws SemanticException {
			AnalysisState<A> target = res.getAnalysisStateAfter(variable);
			for (SymbolicExpression expr : target.getExecutionExpressions()) {
				ValueDomain<?> vdom = ((SimpleAbstractDomain<?, ?, ?>) tool.getAnalysis().domain).valueDomain;
				Satisfiability sat = Satisfiability.UNKNOWN;
				if (vdom instanceof SmashedSum<?, ?>) {
					SmashedSumStringDomain dom = ((SmashedSum<?, ?>) vdom).strDom;
					ValueEnvironment<?> values = target.getExecutionState().getLatticeInstance(ValueEnvironment.class);
					Lattice<?> state = values.getState((Identifier) expr);
					Lattice<?> abstractString = ((SmashedValue<?, ?>) state).getStringValue();
					sat = dom.containsChar(abstractString, ch.getValue().charAt(0));
				} else {
					ValueEnvironment<?> env = null;
					SmashedSumStringDomain dom = null;
					ValueDomain<?>[] participants = ((WholeValueAnalysis) vdom).participants;
					for (int i = 0; i < participants.length; i++)
						if (participants[i] instanceof StringAbstraction) {
							env = (ValueEnvironment<?>) target.getExecutionState()
									.getLatticeInstance(WholeValue.class)
									.get(i);
							// fine since all domains involved in the tests are
							// smashed sum domains
							dom = (SmashedSumStringDomain) participants[i];
						}
					Lattice<?> state = env.getState((Identifier) expr);
					sat = dom.containsChar(state, ch.getValue().charAt(0));
				}
				if (sat == Satisfiability.UNKNOWN)
					warnOn(tool, node, "This assertion might fail");
				else if (sat == Satisfiability.NOT_SATISFIED)
					warnOn(tool, node, "This assertion always fails");
				else
					warnOn(tool, node, null);
			}
		}

		private void assertion(
				SemanticTool<A, D> tool,
				Statement node,
				AnalysisState<A> post,
				D domain,
				A state)
				throws SemanticException {
			for (SymbolicExpression expr : post.getExecutionExpressions()) {
				Satisfiability sat = domain.satisfies(state, expr, node);
				if (sat == Satisfiability.UNKNOWN)
					warnOn(tool, node, "This assertion might fail");
				else if (sat == Satisfiability.NOT_SATISFIED)
					warnOn(tool, node, "This assertion always fails");
				else
					warnOn(tool, node, null);
			}
		}

		private void warnOn(
				SemanticTool<A, D> tool,
				Statement node,
				String message) {
			if (message != null) {
				tool.warnOn(node, message);
				assertions.put(node.getLocation(), message.contains("might fail") ? "possible" : "definite");
			} else
				assertions.put(node.getLocation(), "-");
		}

	}

	private static TestConfiguration mkConf()
			throws AnalysisSetupException {
		CronConfiguration conf = new CronConfiguration();
		conf.semanticChecks.add(new AssertionCheck<>());
		conf.openCallPolicy = ReturnTopPolicy.INSTANCE;
		conf.callGraph = new RTACallGraph();
		conf.interproceduralAnalysis = new ContextBasedAnalysis<>(-1);
		conf.compareWithOptimization = false;
		conf.outputs.add(new JSONResults<>());
		conf.forwardFixpoint = new OptimizedForwardAscendingFixpoint<>();
		conf.hotspots = st -> st instanceof IMPAssert
				|| (st instanceof Expression && ((Expression) st).getRootStatement() instanceof IMPAssert);
		return conf;
	}

	private void perform(
			String dir,
			String subDir,
			String program,
			TestConfiguration conf) {
		conf.testDir = dir;
		conf.testSubDir = subDir;
		conf.programFile = program;
		perform(conf);
	}

	private static Map<String, Map<String, Lattice<?>>> STATES = new HashMap<>();

	private static Map<String, Map<String, Map<CodeLocation, String>>> MESSAGES = new HashMap<>();

	@Test
	public void testSmashedCpPrefixToString() {
		TestConfiguration conf = mkConf();
		conf.analysis = new SimpleAbstractDomain<>(
				new MonolithicMemory(),
				new SmashedSum<>(new IntegerConstantPropagation(), new Prefix()),
				new InferredTypes());
		perform("whole-value", "smashed/cp-prefix-toString", "toString.imp", conf);
		AssertionCheck<?, ?> check = (AssertionCheck<?, ?>) conf.semanticChecks.iterator().next();
		STATES.computeIfAbsent("smashed-cp-prefix", k -> new HashMap<>())
				.put("toString.imp", check.valuesAtFirstAssertion);
		MESSAGES.computeIfAbsent("smashed-cp-prefix", k -> new HashMap<>())
				.put("toString.imp", check.assertions);
	}

	@Test
	public void testSmashedCpSuffixToString() {
		TestConfiguration conf = mkConf();
		conf.analysis = new SimpleAbstractDomain<>(
				new MonolithicMemory(),
				new SmashedSum<>(new IntegerConstantPropagation(), new Suffix()),
				new InferredTypes());
		perform("whole-value", "smashed/cp-suffix-toString", "toString.imp", conf);
		AssertionCheck<?, ?> check = (AssertionCheck<?, ?>) conf.semanticChecks.iterator().next();
		STATES.computeIfAbsent("smashed-cp-suffix", k -> new HashMap<>())
				.put("toString.imp", check.valuesAtFirstAssertion);
		MESSAGES.computeIfAbsent("smashed-cp-suffix", k -> new HashMap<>())
				.put("toString.imp", check.assertions);
	}

	@Test
	public void testSmashedCpBSSToString() {
		TestConfiguration conf = mkConf();
		conf.analysis = new SimpleAbstractDomain<>(
				new MonolithicMemory(),
				new SmashedSum<>(new IntegerConstantPropagation(), new BoundedStringSet(5)),
				new InferredTypes());
		perform("whole-value", "smashed/cp-bss-toString", "toString.imp", conf);
		AssertionCheck<?, ?> check = (AssertionCheck<?, ?>) conf.semanticChecks.iterator().next();
		STATES.computeIfAbsent("smashed-cp-bss", k -> new HashMap<>())
				.put("toString.imp", check.valuesAtFirstAssertion);
		MESSAGES.computeIfAbsent("smashed-cp-bss", k -> new HashMap<>())
				.put("toString.imp", check.assertions);
	}

	@Test
	public void testSmashedCpCIToString() {
		TestConfiguration conf = mkConf();
		conf.analysis = new SimpleAbstractDomain<>(
				new MonolithicMemory(),
				new SmashedSum<>(new IntegerConstantPropagation(), new CharInclusion()),
				new InferredTypes());
		perform("whole-value", "smashed/cp-ci-toString", "toString.imp", conf);
		AssertionCheck<?, ?> check = (AssertionCheck<?, ?>) conf.semanticChecks.iterator().next();
		STATES.computeIfAbsent("smashed-cp-ci", k -> new HashMap<>())
				.put("toString.imp", check.valuesAtFirstAssertion);
		MESSAGES.computeIfAbsent("smashed-cp-ci", k -> new HashMap<>())
				.put("toString.imp", check.assertions);
	}

	@Test
	public void testSmashedCpTarsisToString() {
		TestConfiguration conf = mkConf();
		conf.analysis = new SimpleAbstractDomain<>(
				new MonolithicMemory(),
				new SmashedSum<>(new IntegerConstantPropagation(), new Tarsis()),
				new InferredTypes());
		perform("whole-value", "smashed/cp-tarsis-toString", "toString.imp", conf);
		AssertionCheck<?, ?> check = (AssertionCheck<?, ?>) conf.semanticChecks.iterator().next();
		STATES.computeIfAbsent("smashed-cp-tarsis", k -> new HashMap<>())
				.put("toString.imp", check.valuesAtFirstAssertion);
		MESSAGES.computeIfAbsent("smashed-cp-tarsis", k -> new HashMap<>())
				.put("toString.imp", check.assertions);
	}

	@Test
	public void testSmashedIntvPrefixToString() {
		TestConfiguration conf = mkConf();
		conf.analysis = new SimpleAbstractDomain<>(
				new MonolithicMemory(),
				new SmashedSum<>(new Interval(), new Prefix()),
				new InferredTypes());
		perform("whole-value", "smashed/intv-prefix-toString", "toString.imp", conf);
		AssertionCheck<?, ?> check = (AssertionCheck<?, ?>) conf.semanticChecks.iterator().next();
		STATES.computeIfAbsent("smashed-intv-prefix", k -> new HashMap<>())
				.put("toString.imp", check.valuesAtFirstAssertion);
		MESSAGES.computeIfAbsent("smashed-intv-prefix", k -> new HashMap<>())
				.put("toString.imp", check.assertions);
	}

	@Test
	public void testSmashedIntvSuffixToString() {
		TestConfiguration conf = mkConf();
		conf.analysis = new SimpleAbstractDomain<>(
				new MonolithicMemory(),
				new SmashedSum<>(new Interval(), new Suffix()),
				new InferredTypes());
		perform("whole-value", "smashed/intv-suffix-toString", "toString.imp", conf);
		AssertionCheck<?, ?> check = (AssertionCheck<?, ?>) conf.semanticChecks.iterator().next();
		STATES.computeIfAbsent("smashed-intv-suffix", k -> new HashMap<>())
				.put("toString.imp", check.valuesAtFirstAssertion);
		MESSAGES.computeIfAbsent("smashed-intv-suffix", k -> new HashMap<>())
				.put("toString.imp", check.assertions);
	}

	@Test
	public void testSmashedIntvBSSToString() {
		TestConfiguration conf = mkConf();
		conf.analysis = new SimpleAbstractDomain<>(
				new MonolithicMemory(),
				new SmashedSum<>(new Interval(), new BoundedStringSet(5)),
				new InferredTypes());
		perform("whole-value", "smashed/intv-bss-toString", "toString.imp", conf);
		AssertionCheck<?, ?> check = (AssertionCheck<?, ?>) conf.semanticChecks.iterator().next();
		STATES.computeIfAbsent("smashed-intv-bss", k -> new HashMap<>())
				.put("toString.imp", check.valuesAtFirstAssertion);
		MESSAGES.computeIfAbsent("smashed-intv-bss", k -> new HashMap<>())
				.put("toString.imp", check.assertions);
	}

	@Test
	public void testSmashedIntvCIToString() {
		TestConfiguration conf = mkConf();
		conf.analysis = new SimpleAbstractDomain<>(
				new MonolithicMemory(),
				new SmashedSum<>(new Interval(), new CharInclusion()),
				new InferredTypes());
		perform("whole-value", "smashed/intv-ci-toString", "toString.imp", conf);
		AssertionCheck<?, ?> check = (AssertionCheck<?, ?>) conf.semanticChecks.iterator().next();
		STATES.computeIfAbsent("smashed-intv-ci", k -> new HashMap<>())
				.put("toString.imp", check.valuesAtFirstAssertion);
		MESSAGES.computeIfAbsent("smashed-intv-ci", k -> new HashMap<>())
				.put("toString.imp", check.assertions);
	}

	@Test
	public void testSmashedIntvTarsisToString() {
		TestConfiguration conf = mkConf();
		conf.analysis = new SimpleAbstractDomain<>(
				new MonolithicMemory(),
				new SmashedSum<>(new Interval(), new Tarsis()),
				new InferredTypes());
		perform("whole-value", "smashed/intv-tarsis-toString", "toString.imp", conf);
		AssertionCheck<?, ?> check = (AssertionCheck<?, ?>) conf.semanticChecks.iterator().next();
		STATES.computeIfAbsent("smashed-intv-tarsis", k -> new HashMap<>())
				.put("toString.imp", check.valuesAtFirstAssertion);
		MESSAGES.computeIfAbsent("smashed-intv-tarsis", k -> new HashMap<>())
				.put("toString.imp", check.assertions);
	}

	@Test
	public void testSmashedCpPrefixLoop() {
		TestConfiguration conf = mkConf();
		conf.analysis = new SimpleAbstractDomain<>(
				new MonolithicMemory(),
				new SmashedSum<>(new IntegerConstantPropagation(), new Prefix()),
				new InferredTypes());
		perform("whole-value", "smashed/cp-prefix-loop", "loop.imp", conf);
		AssertionCheck<?, ?> check = (AssertionCheck<?, ?>) conf.semanticChecks.iterator().next();
		STATES.computeIfAbsent("smashed-cp-prefix", k -> new HashMap<>())
				.put("loop.imp", check.valuesAtFirstAssertion);
		MESSAGES.computeIfAbsent("smashed-cp-prefix", k -> new HashMap<>())
				.put("loop.imp", check.assertions);
	}

	@Test
	public void testSmashedCpSuffixLoop() {
		TestConfiguration conf = mkConf();
		conf.analysis = new SimpleAbstractDomain<>(
				new MonolithicMemory(),
				new SmashedSum<>(new IntegerConstantPropagation(), new Suffix()),
				new InferredTypes());
		perform("whole-value", "smashed/cp-suffix-loop", "loop.imp", conf);
		AssertionCheck<?, ?> check = (AssertionCheck<?, ?>) conf.semanticChecks.iterator().next();
		STATES.computeIfAbsent("smashed-cp-suffix", k -> new HashMap<>())
				.put("loop.imp", check.valuesAtFirstAssertion);
		MESSAGES.computeIfAbsent("smashed-cp-suffix", k -> new HashMap<>())
				.put("loop.imp", check.assertions);
	}

	@Test
	public void testSmashedCpBSSLoop() {
		TestConfiguration conf = mkConf();
		conf.analysis = new SimpleAbstractDomain<>(
				new MonolithicMemory(),
				new SmashedSum<>(new IntegerConstantPropagation(), new BoundedStringSet(5)),
				new InferredTypes());
		perform("whole-value", "smashed/cp-bss-loop", "loop.imp", conf);
		AssertionCheck<?, ?> check = (AssertionCheck<?, ?>) conf.semanticChecks.iterator().next();
		STATES.computeIfAbsent("smashed-cp-bss", k -> new HashMap<>())
				.put("loop.imp", check.valuesAtFirstAssertion);
		MESSAGES.computeIfAbsent("smashed-cp-bss", k -> new HashMap<>())
				.put("loop.imp", check.assertions);
	}

	@Test
	public void testSmashedCpCILoop() {
		TestConfiguration conf = mkConf();
		conf.analysis = new SimpleAbstractDomain<>(
				new MonolithicMemory(),
				new SmashedSum<>(new IntegerConstantPropagation(), new CharInclusion()),
				new InferredTypes());
		perform("whole-value", "smashed/cp-ci-loop", "loop.imp", conf);
		AssertionCheck<?, ?> check = (AssertionCheck<?, ?>) conf.semanticChecks.iterator().next();
		STATES.computeIfAbsent("smashed-cp-ci", k -> new HashMap<>())
				.put("loop.imp", check.valuesAtFirstAssertion);
		MESSAGES.computeIfAbsent("smashed-cp-ci", k -> new HashMap<>())
				.put("loop.imp", check.assertions);
	}

	@Test
	public void testSmashedCpTarsisLoop() {
		TestConfiguration conf = mkConf();
		conf.analysis = new SimpleAbstractDomain<>(
				new MonolithicMemory(),
				new SmashedSum<>(new IntegerConstantPropagation(), new Tarsis()),
				new InferredTypes());
		perform("whole-value", "smashed/cp-tarsis-loop", "loop.imp", conf);
		AssertionCheck<?, ?> check = (AssertionCheck<?, ?>) conf.semanticChecks.iterator().next();
		STATES.computeIfAbsent("smashed-cp-tarsis", k -> new HashMap<>())
				.put("loop.imp", check.valuesAtFirstAssertion);
		MESSAGES.computeIfAbsent("smashed-cp-tarsis", k -> new HashMap<>())
				.put("loop.imp", check.assertions);
	}

	@Test
	public void testSmashedIntvPrefixLoop() {
		TestConfiguration conf = mkConf();
		conf.analysis = new SimpleAbstractDomain<>(
				new MonolithicMemory(),
				new SmashedSum<>(new Interval(), new Prefix()),
				new InferredTypes());
		perform("whole-value", "smashed/intv-prefix-loop", "loop.imp", conf);
		AssertionCheck<?, ?> check = (AssertionCheck<?, ?>) conf.semanticChecks.iterator().next();
		STATES.computeIfAbsent("smashed-intv-prefix", k -> new HashMap<>())
				.put("loop.imp", check.valuesAtFirstAssertion);
		MESSAGES.computeIfAbsent("smashed-intv-prefix", k -> new HashMap<>())
				.put("loop.imp", check.assertions);
	}

	@Test
	public void testSmashedIntvSuffixLoop() {
		TestConfiguration conf = mkConf();
		conf.analysis = new SimpleAbstractDomain<>(
				new MonolithicMemory(),
				new SmashedSum<>(new Interval(), new Suffix()),
				new InferredTypes());
		perform("whole-value", "smashed/intv-suffix-loop", "loop.imp", conf);
		AssertionCheck<?, ?> check = (AssertionCheck<?, ?>) conf.semanticChecks.iterator().next();
		STATES.computeIfAbsent("smashed-intv-suffix", k -> new HashMap<>())
				.put("loop.imp", check.valuesAtFirstAssertion);
		MESSAGES.computeIfAbsent("smashed-intv-suffix", k -> new HashMap<>())
				.put("loop.imp", check.assertions);
	}

	@Test
	public void testSmashedIntvBSSLoop() {
		TestConfiguration conf = mkConf();
		conf.analysis = new SimpleAbstractDomain<>(
				new MonolithicMemory(),
				new SmashedSum<>(new Interval(), new BoundedStringSet(5)),
				new InferredTypes());
		perform("whole-value", "smashed/intv-bss-loop", "loop.imp", conf);
		AssertionCheck<?, ?> check = (AssertionCheck<?, ?>) conf.semanticChecks.iterator().next();
		STATES.computeIfAbsent("smashed-intv-bss", k -> new HashMap<>())
				.put("loop.imp", check.valuesAtFirstAssertion);
		MESSAGES.computeIfAbsent("smashed-intv-bss", k -> new HashMap<>())
				.put("loop.imp", check.assertions);
	}

	@Test
	public void testSmashedIntvCILoop() {
		TestConfiguration conf = mkConf();
		conf.analysis = new SimpleAbstractDomain<>(
				new MonolithicMemory(),
				new SmashedSum<>(new Interval(), new CharInclusion()),
				new InferredTypes());
		perform("whole-value", "smashed/intv-ci-loop", "loop.imp", conf);
		AssertionCheck<?, ?> check = (AssertionCheck<?, ?>) conf.semanticChecks.iterator().next();
		STATES.computeIfAbsent("smashed-intv-ci", k -> new HashMap<>())
				.put("loop.imp", check.valuesAtFirstAssertion);
		MESSAGES.computeIfAbsent("smashed-intv-ci", k -> new HashMap<>())
				.put("loop.imp", check.assertions);
	}

	@Test
	public void testSmashedIntvTarsisLoop() {
		TestConfiguration conf = mkConf();
		conf.analysis = new SimpleAbstractDomain<>(
				new MonolithicMemory(),
				new SmashedSum<>(new Interval(), new Tarsis()),
				new InferredTypes());
		perform("whole-value", "smashed/intv-tarsis-loop", "loop.imp", conf);
		AssertionCheck<?, ?> check = (AssertionCheck<?, ?>) conf.semanticChecks.iterator().next();
		STATES.computeIfAbsent("smashed-intv-tarsis", k -> new HashMap<>())
				.put("loop.imp", check.valuesAtFirstAssertion);
		MESSAGES.computeIfAbsent("smashed-intv-tarsis", k -> new HashMap<>())
				.put("loop.imp", check.assertions);
	}

	@Test
	public void testSmashedCpPrefixSubs() {
		TestConfiguration conf = mkConf();
		conf.analysis = new SimpleAbstractDomain<>(
				new MonolithicMemory(),
				new SmashedSum<>(new IntegerConstantPropagation(), new Prefix()),
				new InferredTypes());
		perform("whole-value", "smashed/cp-prefix-subs", "subs.imp", conf);
		AssertionCheck<?, ?> check = (AssertionCheck<?, ?>) conf.semanticChecks.iterator().next();
		STATES.computeIfAbsent("smashed-cp-prefix", k -> new HashMap<>())
				.put("subs.imp", check.valuesAtFirstAssertion);
		MESSAGES.computeIfAbsent("smashed-cp-prefix", k -> new HashMap<>())
				.put("subs.imp", check.assertions);
	}

	@Test
	public void testSmashedCpSuffixSubs() {
		TestConfiguration conf = mkConf();
		conf.analysis = new SimpleAbstractDomain<>(
				new MonolithicMemory(),
				new SmashedSum<>(new IntegerConstantPropagation(), new Suffix()),
				new InferredTypes());
		perform("whole-value", "smashed/cp-suffix-subs", "subs.imp", conf);
		AssertionCheck<?, ?> check = (AssertionCheck<?, ?>) conf.semanticChecks.iterator().next();
		STATES.computeIfAbsent("smashed-cp-suffix", k -> new HashMap<>())
				.put("subs.imp", check.valuesAtFirstAssertion);
		MESSAGES.computeIfAbsent("smashed-cp-suffix", k -> new HashMap<>())
				.put("subs.imp", check.assertions);
	}

	@Test
	public void testSmashedCpBSSSubs() {
		TestConfiguration conf = mkConf();
		conf.analysis = new SimpleAbstractDomain<>(
				new MonolithicMemory(),
				new SmashedSum<>(new IntegerConstantPropagation(), new BoundedStringSet(5)),
				new InferredTypes());
		perform("whole-value", "smashed/cp-bss-subs", "subs.imp", conf);
		AssertionCheck<?, ?> check = (AssertionCheck<?, ?>) conf.semanticChecks.iterator().next();
		STATES.computeIfAbsent("smashed-cp-bss", k -> new HashMap<>())
				.put("subs.imp", check.valuesAtFirstAssertion);
		MESSAGES.computeIfAbsent("smashed-cp-bss", k -> new HashMap<>())
				.put("subs.imp", check.assertions);
	}

	@Test
	public void testSmashedCpCISubs() {
		TestConfiguration conf = mkConf();
		conf.analysis = new SimpleAbstractDomain<>(
				new MonolithicMemory(),
				new SmashedSum<>(new IntegerConstantPropagation(), new CharInclusion()),
				new InferredTypes());
		perform("whole-value", "smashed/cp-ci-subs", "subs.imp", conf);
		AssertionCheck<?, ?> check = (AssertionCheck<?, ?>) conf.semanticChecks.iterator().next();
		STATES.computeIfAbsent("smashed-cp-ci", k -> new HashMap<>())
				.put("subs.imp", check.valuesAtFirstAssertion);
		MESSAGES.computeIfAbsent("smashed-cp-ci", k -> new HashMap<>())
				.put("subs.imp", check.assertions);
	}

	@Test
	public void testSmashedCpTarsisSubs() {
		TestConfiguration conf = mkConf();
		conf.analysis = new SimpleAbstractDomain<>(
				new MonolithicMemory(),
				new SmashedSum<>(new IntegerConstantPropagation(), new Tarsis()),
				new InferredTypes());
		perform("whole-value", "smashed/cp-tarsis-subs", "subs.imp", conf);
		AssertionCheck<?, ?> check = (AssertionCheck<?, ?>) conf.semanticChecks.iterator().next();
		STATES.computeIfAbsent("smashed-cp-tarsis", k -> new HashMap<>())
				.put("subs.imp", check.valuesAtFirstAssertion);
		MESSAGES.computeIfAbsent("smashed-cp-tarsis", k -> new HashMap<>())
				.put("subs.imp", check.assertions);
	}

	@Test
	public void testSmashedIntvPrefixSubs() {
		TestConfiguration conf = mkConf();
		conf.analysis = new SimpleAbstractDomain<>(
				new MonolithicMemory(),
				new SmashedSum<>(new Interval(), new Prefix()),
				new InferredTypes());
		perform("whole-value", "smashed/intv-prefix-subs", "subs.imp", conf);
		AssertionCheck<?, ?> check = (AssertionCheck<?, ?>) conf.semanticChecks.iterator().next();
		STATES.computeIfAbsent("smashed-intv-prefix", k -> new HashMap<>())
				.put("subs.imp", check.valuesAtFirstAssertion);
		MESSAGES.computeIfAbsent("smashed-intv-prefix", k -> new HashMap<>())
				.put("subs.imp", check.assertions);
	}

	@Test
	public void testSmashedIntvSuffixSubs() {
		TestConfiguration conf = mkConf();
		conf.analysis = new SimpleAbstractDomain<>(
				new MonolithicMemory(),
				new SmashedSum<>(new Interval(), new Suffix()),
				new InferredTypes());
		perform("whole-value", "smashed/intv-suffix-subs", "subs.imp", conf);
		AssertionCheck<?, ?> check = (AssertionCheck<?, ?>) conf.semanticChecks.iterator().next();
		STATES.computeIfAbsent("smashed-intv-suffix", k -> new HashMap<>())
				.put("subs.imp", check.valuesAtFirstAssertion);
		MESSAGES.computeIfAbsent("smashed-intv-suffix", k -> new HashMap<>())
				.put("subs.imp", check.assertions);
	}

	@Test
	public void testSmashedIntvBSSSubs() {
		TestConfiguration conf = mkConf();
		conf.analysis = new SimpleAbstractDomain<>(
				new MonolithicMemory(),
				new SmashedSum<>(new Interval(), new BoundedStringSet(5)),
				new InferredTypes());
		perform("whole-value", "smashed/intv-bss-subs", "subs.imp", conf);
		AssertionCheck<?, ?> check = (AssertionCheck<?, ?>) conf.semanticChecks.iterator().next();
		STATES.computeIfAbsent("smashed-intv-bss", k -> new HashMap<>())
				.put("subs.imp", check.valuesAtFirstAssertion);
		MESSAGES.computeIfAbsent("smashed-intv-bss", k -> new HashMap<>())
				.put("subs.imp", check.assertions);
	}

	@Test
	public void testSmashedIntvCISubs() {
		TestConfiguration conf = mkConf();
		conf.analysis = new SimpleAbstractDomain<>(
				new MonolithicMemory(),
				new SmashedSum<>(new Interval(), new CharInclusion()),
				new InferredTypes());
		perform("whole-value", "smashed/intv-ci-subs", "subs.imp", conf);
		AssertionCheck<?, ?> check = (AssertionCheck<?, ?>) conf.semanticChecks.iterator().next();
		STATES.computeIfAbsent("smashed-intv-ci", k -> new HashMap<>())
				.put("subs.imp", check.valuesAtFirstAssertion);
		MESSAGES.computeIfAbsent("smashed-intv-ci", k -> new HashMap<>())
				.put("subs.imp", check.assertions);
	}

	@Test
	public void testSmashedIntvTarsisSubs() {
		TestConfiguration conf = mkConf();
		conf.analysis = new SimpleAbstractDomain<>(
				new MonolithicMemory(),
				new SmashedSum<>(new Interval(), new Tarsis()),
				new InferredTypes());
		perform("whole-value", "smashed/intv-tarsis-subs", "subs.imp", conf);
		AssertionCheck<?, ?> check = (AssertionCheck<?, ?>) conf.semanticChecks.iterator().next();
		STATES.computeIfAbsent("smashed-intv-tarsis", k -> new HashMap<>())
				.put("subs.imp", check.valuesAtFirstAssertion);
		MESSAGES.computeIfAbsent("smashed-intv-tarsis", k -> new HashMap<>())
				.put("subs.imp", check.assertions);
	}

	@Test
	public void testSmashedCpPrefixCount() {
		TestConfiguration conf = mkConf();
		conf.analysis = new SimpleAbstractDomain<>(
				new MonolithicMemory(),
				new SmashedSum<>(new IntegerConstantPropagation(), new Prefix()),
				new InferredTypes());
		conf.analysis = new TracePartitioning<>(conf.analysis);
		perform("whole-value", "smashed/cp-prefix-count", "count.imp", conf);
		AssertionCheck<?, ?> check = (AssertionCheck<?, ?>) conf.semanticChecks.iterator().next();
		STATES.computeIfAbsent("smashed-cp-prefix", k -> new HashMap<>())
				.put("count.imp", check.valuesAtFirstAssertion);
		MESSAGES.computeIfAbsent("smashed-cp-prefix", k -> new HashMap<>())
				.put("count.imp", check.assertions);
	}

	@Test
	public void testSmashedCpSuffixCount() {
		TestConfiguration conf = mkConf();
		conf.analysis = new SimpleAbstractDomain<>(
				new MonolithicMemory(),
				new SmashedSum<>(new IntegerConstantPropagation(), new Suffix()),
				new InferredTypes());
		conf.analysis = new TracePartitioning<>(conf.analysis);
		perform("whole-value", "smashed/cp-suffix-count", "count.imp", conf);
		AssertionCheck<?, ?> check = (AssertionCheck<?, ?>) conf.semanticChecks.iterator().next();
		STATES.computeIfAbsent("smashed-cp-suffix", k -> new HashMap<>())
				.put("count.imp", check.valuesAtFirstAssertion);
		MESSAGES.computeIfAbsent("smashed-cp-suffix", k -> new HashMap<>())
				.put("count.imp", check.assertions);
	}

	@Test
	public void testSmashedCpBSSCount() {
		TestConfiguration conf = mkConf();
		conf.analysis = new SimpleAbstractDomain<>(
				new MonolithicMemory(),
				new SmashedSum<>(new IntegerConstantPropagation(), new BoundedStringSet(5)),
				new InferredTypes());
		conf.analysis = new TracePartitioning<>(conf.analysis);
		perform("whole-value", "smashed/cp-bss-count", "count.imp", conf);
		AssertionCheck<?, ?> check = (AssertionCheck<?, ?>) conf.semanticChecks.iterator().next();
		STATES.computeIfAbsent("smashed-cp-bss", k -> new HashMap<>())
				.put("count.imp", check.valuesAtFirstAssertion);
		MESSAGES.computeIfAbsent("smashed-cp-bss", k -> new HashMap<>())
				.put("count.imp", check.assertions);
	}

	@Test
	public void testSmashedCpCICount() {
		TestConfiguration conf = mkConf();
		conf.analysis = new SimpleAbstractDomain<>(
				new MonolithicMemory(),
				new SmashedSum<>(new IntegerConstantPropagation(), new CharInclusion()),
				new InferredTypes());
		conf.analysis = new TracePartitioning<>(conf.analysis);
		perform("whole-value", "smashed/cp-ci-count", "count.imp", conf);
		AssertionCheck<?, ?> check = (AssertionCheck<?, ?>) conf.semanticChecks.iterator().next();
		STATES.computeIfAbsent("smashed-cp-ci", k -> new HashMap<>())
				.put("count.imp", check.valuesAtFirstAssertion);
		MESSAGES.computeIfAbsent("smashed-cp-ci", k -> new HashMap<>())
				.put("count.imp", check.assertions);
	}

	@Test
	public void testSmashedCpTarsisCount() {
		TestConfiguration conf = mkConf();
		conf.analysis = new SimpleAbstractDomain<>(
				new MonolithicMemory(),
				new SmashedSum<>(new IntegerConstantPropagation(), new Tarsis()),
				new InferredTypes());
		conf.analysis = new TracePartitioning<>(conf.analysis);
		perform("whole-value", "smashed/cp-tarsis-count", "count.imp", conf);
		AssertionCheck<?, ?> check = (AssertionCheck<?, ?>) conf.semanticChecks.iterator().next();
		STATES.computeIfAbsent("smashed-cp-tarsis", k -> new HashMap<>())
				.put("count.imp", check.valuesAtFirstAssertion);
		MESSAGES.computeIfAbsent("smashed-cp-tarsis", k -> new HashMap<>())
				.put("count.imp", check.assertions);
	}

	@Test
	public void testSmashedIntvPrefixCount() {
		TestConfiguration conf = mkConf();
		conf.analysis = new SimpleAbstractDomain<>(
				new MonolithicMemory(),
				new SmashedSum<>(new Interval(), new Prefix()),
				new InferredTypes());
		conf.analysis = new TracePartitioning<>(conf.analysis);
		perform("whole-value", "smashed/intv-prefix-count", "count.imp", conf);
		AssertionCheck<?, ?> check = (AssertionCheck<?, ?>) conf.semanticChecks.iterator().next();
		STATES.computeIfAbsent("smashed-intv-prefix", k -> new HashMap<>())
				.put("count.imp", check.valuesAtFirstAssertion);
		MESSAGES.computeIfAbsent("smashed-intv-prefix", k -> new HashMap<>())
				.put("count.imp", check.assertions);
	}

	@Test
	public void testSmashedIntvSuffixCount() {
		TestConfiguration conf = mkConf();
		conf.analysis = new SimpleAbstractDomain<>(
				new MonolithicMemory(),
				new SmashedSum<>(new Interval(), new Suffix()),
				new InferredTypes());
		conf.analysis = new TracePartitioning<>(conf.analysis);
		perform("whole-value", "smashed/intv-suffix-count", "count.imp", conf);
		AssertionCheck<?, ?> check = (AssertionCheck<?, ?>) conf.semanticChecks.iterator().next();
		STATES.computeIfAbsent("smashed-intv-suffix", k -> new HashMap<>())
				.put("count.imp", check.valuesAtFirstAssertion);
		MESSAGES.computeIfAbsent("smashed-intv-suffix", k -> new HashMap<>())
				.put("count.imp", check.assertions);
	}

	@Test
	public void testSmashedIntvBSSCount() {
		TestConfiguration conf = mkConf();
		conf.analysis = new SimpleAbstractDomain<>(
				new MonolithicMemory(),
				new SmashedSum<>(new Interval(), new BoundedStringSet(5)),
				new InferredTypes());
		conf.analysis = new TracePartitioning<>(conf.analysis);
		perform("whole-value", "smashed/intv-bss-count", "count.imp", conf);
		AssertionCheck<?, ?> check = (AssertionCheck<?, ?>) conf.semanticChecks.iterator().next();
		STATES.computeIfAbsent("smashed-intv-bss", k -> new HashMap<>())
				.put("count.imp", check.valuesAtFirstAssertion);
		MESSAGES.computeIfAbsent("smashed-intv-bss", k -> new HashMap<>())
				.put("count.imp", check.assertions);
	}

	@Test
	public void testSmashedIntvCICount() {
		TestConfiguration conf = mkConf();
		conf.analysis = new SimpleAbstractDomain<>(
				new MonolithicMemory(),
				new SmashedSum<>(new Interval(), new CharInclusion()),
				new InferredTypes());
		conf.analysis = new TracePartitioning<>(conf.analysis);
		perform("whole-value", "smashed/intv-ci-count", "count.imp", conf);
		AssertionCheck<?, ?> check = (AssertionCheck<?, ?>) conf.semanticChecks.iterator().next();
		STATES.computeIfAbsent("smashed-intv-ci", k -> new HashMap<>())
				.put("count.imp", check.valuesAtFirstAssertion);
		MESSAGES.computeIfAbsent("smashed-intv-ci", k -> new HashMap<>())
				.put("count.imp", check.assertions);
	}

	@Test
	public void testSmashedIntvTarsisCount() {
		TestConfiguration conf = mkConf();
		conf.analysis = new SimpleAbstractDomain<>(
				new MonolithicMemory(),
				new SmashedSum<>(new Interval(), new Tarsis()),
				new InferredTypes());
		conf.analysis = new TracePartitioning<>(conf.analysis);
		perform("whole-value", "smashed/intv-tarsis-count", "count.imp", conf);
		AssertionCheck<?, ?> check = (AssertionCheck<?, ?>) conf.semanticChecks.iterator().next();
		STATES.computeIfAbsent("smashed-intv-tarsis", k -> new HashMap<>())
				.put("count.imp", check.valuesAtFirstAssertion);
		MESSAGES.computeIfAbsent("smashed-intv-tarsis", k -> new HashMap<>())
				.put("count.imp", check.assertions);
	}

	@Test
	public void testConstrCpPrefixToString() {
		TestConfiguration conf = mkConf();
		conf.analysis = new SimpleAbstractDomain<>(
				new MonolithicMemory(),
				new WholeValueAnalysis(new IntegerConstantPropagation(), new Prefix(), new BooleanPowerset()),
				new InferredTypes());
		perform("whole-value", "constr/cp-prefix-toString", "toString.imp", conf);
		AssertionCheck<?, ?> check = (AssertionCheck<?, ?>) conf.semanticChecks.iterator().next();
		STATES.computeIfAbsent("constr-cp-prefix", k -> new HashMap<>())
				.put("toString.imp", check.valuesAtFirstAssertion);
		MESSAGES.computeIfAbsent("constr-cp-prefix", k -> new HashMap<>())
				.put("toString.imp", check.assertions);
	}

	@Test
	public void testConstrCpSuffixToString() {
		TestConfiguration conf = mkConf();
		conf.analysis = new SimpleAbstractDomain<>(
				new MonolithicMemory(),
				new WholeValueAnalysis(new IntegerConstantPropagation(), new Suffix(), new BooleanPowerset()),
				new InferredTypes());
		perform("whole-value", "constr/cp-suffix-toString", "toString.imp", conf);
		AssertionCheck<?, ?> check = (AssertionCheck<?, ?>) conf.semanticChecks.iterator().next();
		STATES.computeIfAbsent("constr-cp-suffix", k -> new HashMap<>())
				.put("toString.imp", check.valuesAtFirstAssertion);
		MESSAGES.computeIfAbsent("constr-cp-suffix", k -> new HashMap<>())
				.put("toString.imp", check.assertions);
	}

	@Test
	public void testConstrCpBSSToString() {
		TestConfiguration conf = mkConf();
		conf.analysis = new SimpleAbstractDomain<>(
				new MonolithicMemory(),
				new WholeValueAnalysis(new IntegerConstantPropagation(), new BoundedStringSet(5),
						new BooleanPowerset()),
				new InferredTypes());
		perform("whole-value", "constr/cp-bss-toString", "toString.imp", conf);
		AssertionCheck<?, ?> check = (AssertionCheck<?, ?>) conf.semanticChecks.iterator().next();
		STATES.computeIfAbsent("constr-cp-bss", k -> new HashMap<>())
				.put("toString.imp", check.valuesAtFirstAssertion);
		MESSAGES.computeIfAbsent("constr-cp-bss", k -> new HashMap<>())
				.put("toString.imp", check.assertions);
	}

	@Test
	public void testConstrCpCIToString() {
		TestConfiguration conf = mkConf();
		conf.analysis = new SimpleAbstractDomain<>(
				new MonolithicMemory(),
				new WholeValueAnalysis(new IntegerConstantPropagation(), new CharInclusion(), new BooleanPowerset()),
				new InferredTypes());
		perform("whole-value", "constr/cp-ci-toString", "toString.imp", conf);
		AssertionCheck<?, ?> check = (AssertionCheck<?, ?>) conf.semanticChecks.iterator().next();
		STATES.computeIfAbsent("constr-cp-ci", k -> new HashMap<>())
				.put("toString.imp", check.valuesAtFirstAssertion);
		MESSAGES.computeIfAbsent("constr-cp-ci", k -> new HashMap<>())
				.put("toString.imp", check.assertions);
	}

	@Test
	public void testConstrCpTarsisToString() {
		TestConfiguration conf = mkConf();
		conf.analysis = new SimpleAbstractDomain<>(
				new MonolithicMemory(),
				new WholeValueAnalysis(new IntegerConstantPropagation(), new Tarsis(), new BooleanPowerset()),
				new InferredTypes());
		perform("whole-value", "constr/cp-tarsis-toString", "toString.imp", conf);
		AssertionCheck<?, ?> check = (AssertionCheck<?, ?>) conf.semanticChecks.iterator().next();
		STATES.computeIfAbsent("constr-cp-tarsis", k -> new HashMap<>())
				.put("toString.imp", check.valuesAtFirstAssertion);
		MESSAGES.computeIfAbsent("constr-cp-tarsis", k -> new HashMap<>())
				.put("toString.imp", check.assertions);
	}

	@Test
	public void testConstrIntvPrefixToString() {
		TestConfiguration conf = mkConf();
		conf.analysis = new SimpleAbstractDomain<>(
				new MonolithicMemory(),
				new WholeValueAnalysis(new Interval(), new Prefix(), new BooleanPowerset()),
				new InferredTypes());
		perform("whole-value", "constr/intv-prefix-toString", "toString.imp", conf);
		AssertionCheck<?, ?> check = (AssertionCheck<?, ?>) conf.semanticChecks.iterator().next();
		STATES.computeIfAbsent("constr-intv-prefix", k -> new HashMap<>())
				.put("toString.imp", check.valuesAtFirstAssertion);
		MESSAGES.computeIfAbsent("constr-intv-prefix", k -> new HashMap<>())
				.put("toString.imp", check.assertions);
	}

	@Test
	public void testConstrIntvSuffixToString() {
		TestConfiguration conf = mkConf();
		conf.analysis = new SimpleAbstractDomain<>(
				new MonolithicMemory(),
				new WholeValueAnalysis(new Interval(), new Suffix(), new BooleanPowerset()),
				new InferredTypes());
		perform("whole-value", "constr/intv-suffix-toString", "toString.imp", conf);
		AssertionCheck<?, ?> check = (AssertionCheck<?, ?>) conf.semanticChecks.iterator().next();
		STATES.computeIfAbsent("constr-intv-suffix", k -> new HashMap<>())
				.put("toString.imp", check.valuesAtFirstAssertion);
		MESSAGES.computeIfAbsent("constr-intv-suffix", k -> new HashMap<>())
				.put("toString.imp", check.assertions);
	}

	@Test
	public void testConstrIntvBSSToString() {
		TestConfiguration conf = mkConf();
		conf.analysis = new SimpleAbstractDomain<>(
				new MonolithicMemory(),
				new WholeValueAnalysis(new Interval(), new BoundedStringSet(5), new BooleanPowerset()),
				new InferredTypes());
		perform("whole-value", "constr/intv-bss-toString", "toString.imp", conf);
		AssertionCheck<?, ?> check = (AssertionCheck<?, ?>) conf.semanticChecks.iterator().next();
		STATES.computeIfAbsent("constr-intv-bss", k -> new HashMap<>())
				.put("toString.imp", check.valuesAtFirstAssertion);
		MESSAGES.computeIfAbsent("constr-intv-bss", k -> new HashMap<>())
				.put("toString.imp", check.assertions);
	}

	@Test
	public void testConstrIntvCIToString() {
		TestConfiguration conf = mkConf();
		conf.analysis = new SimpleAbstractDomain<>(
				new MonolithicMemory(),
				new WholeValueAnalysis(new Interval(), new CharInclusion(), new BooleanPowerset()),
				new InferredTypes());
		perform("whole-value", "constr/intv-ci-toString", "toString.imp", conf);
		AssertionCheck<?, ?> check = (AssertionCheck<?, ?>) conf.semanticChecks.iterator().next();
		STATES.computeIfAbsent("constr-intv-ci", k -> new HashMap<>())
				.put("toString.imp", check.valuesAtFirstAssertion);
		MESSAGES.computeIfAbsent("constr-intv-ci", k -> new HashMap<>())
				.put("toString.imp", check.assertions);
	}

	@Test
	public void testConstrIntvTarsisToString() {
		TestConfiguration conf = mkConf();
		conf.analysis = new SimpleAbstractDomain<>(
				new MonolithicMemory(),
				new WholeValueAnalysis(new Interval(), new Tarsis(), new BooleanPowerset()),
				new InferredTypes());
		perform("whole-value", "constr/intv-tarsis-toString", "toString.imp", conf);
		AssertionCheck<?, ?> check = (AssertionCheck<?, ?>) conf.semanticChecks.iterator().next();
		STATES.computeIfAbsent("constr-intv-tarsis", k -> new HashMap<>())
				.put("toString.imp", check.valuesAtFirstAssertion);
		MESSAGES.computeIfAbsent("constr-intv-tarsis", k -> new HashMap<>())
				.put("toString.imp", check.assertions);
	}

	@Test
	public void testConstrCpPrefixLoop() {
		TestConfiguration conf = mkConf();
		conf.analysis = new SimpleAbstractDomain<>(
				new MonolithicMemory(),
				new WholeValueAnalysis(new IntegerConstantPropagation(), new Prefix(), new BooleanPowerset()),
				new InferredTypes());
		perform("whole-value", "constr/cp-prefix-loop", "loop.imp", conf);
		AssertionCheck<?, ?> check = (AssertionCheck<?, ?>) conf.semanticChecks.iterator().next();
		STATES.computeIfAbsent("constr-cp-prefix", k -> new HashMap<>())
				.put("loop.imp", check.valuesAtFirstAssertion);
		MESSAGES.computeIfAbsent("constr-cp-prefix", k -> new HashMap<>())
				.put("loop.imp", check.assertions);
	}

	@Test
	public void testConstrCpSuffixLoop() {
		TestConfiguration conf = mkConf();
		conf.analysis = new SimpleAbstractDomain<>(
				new MonolithicMemory(),
				new WholeValueAnalysis(new IntegerConstantPropagation(), new Suffix(), new BooleanPowerset()),
				new InferredTypes());
		perform("whole-value", "constr/cp-suffix-loop", "loop.imp", conf);
		AssertionCheck<?, ?> check = (AssertionCheck<?, ?>) conf.semanticChecks.iterator().next();
		STATES.computeIfAbsent("constr-cp-suffix", k -> new HashMap<>())
				.put("loop.imp", check.valuesAtFirstAssertion);
		MESSAGES.computeIfAbsent("constr-cp-suffix", k -> new HashMap<>())
				.put("loop.imp", check.assertions);
	}

	@Test
	public void testConstrCpBSSLoop() {
		TestConfiguration conf = mkConf();
		conf.analysis = new SimpleAbstractDomain<>(
				new MonolithicMemory(),
				new WholeValueAnalysis(new IntegerConstantPropagation(), new BoundedStringSet(5),
						new BooleanPowerset()),
				new InferredTypes());
		perform("whole-value", "constr/cp-bss-loop", "loop.imp", conf);
		AssertionCheck<?, ?> check = (AssertionCheck<?, ?>) conf.semanticChecks.iterator().next();
		STATES.computeIfAbsent("constr-cp-bss", k -> new HashMap<>())
				.put("loop.imp", check.valuesAtFirstAssertion);
		MESSAGES.computeIfAbsent("constr-cp-bss", k -> new HashMap<>())
				.put("loop.imp", check.assertions);
	}

	@Test
	public void testConstrCpCILoop() {
		TestConfiguration conf = mkConf();
		conf.analysis = new SimpleAbstractDomain<>(
				new MonolithicMemory(),
				new WholeValueAnalysis(new IntegerConstantPropagation(), new CharInclusion(), new BooleanPowerset()),
				new InferredTypes());
		perform("whole-value", "constr/cp-ci-loop", "loop.imp", conf);
		AssertionCheck<?, ?> check = (AssertionCheck<?, ?>) conf.semanticChecks.iterator().next();
		STATES.computeIfAbsent("constr-cp-ci", k -> new HashMap<>())
				.put("loop.imp", check.valuesAtFirstAssertion);
		MESSAGES.computeIfAbsent("constr-cp-ci", k -> new HashMap<>())
				.put("loop.imp", check.assertions);
	}

	@Test
	public void testConstrCpTarsisLoop() {
		TestConfiguration conf = mkConf();
		conf.analysis = new SimpleAbstractDomain<>(
				new MonolithicMemory(),
				new WholeValueAnalysis(new IntegerConstantPropagation(), new Tarsis(), new BooleanPowerset()),
				new InferredTypes());
		perform("whole-value", "constr/cp-tarsis-loop", "loop.imp", conf);
		AssertionCheck<?, ?> check = (AssertionCheck<?, ?>) conf.semanticChecks.iterator().next();
		STATES.computeIfAbsent("constr-cp-tarsis", k -> new HashMap<>())
				.put("loop.imp", check.valuesAtFirstAssertion);
		MESSAGES.computeIfAbsent("constr-cp-tarsis", k -> new HashMap<>())
				.put("loop.imp", check.assertions);
	}

	@Test
	public void testConstrIntvPrefixLoop() {
		TestConfiguration conf = mkConf();
		conf.analysis = new SimpleAbstractDomain<>(
				new MonolithicMemory(),
				new WholeValueAnalysis(new Interval(), new Prefix(), new BooleanPowerset()),
				new InferredTypes());
		perform("whole-value", "constr/intv-prefix-loop", "loop.imp", conf);
		AssertionCheck<?, ?> check = (AssertionCheck<?, ?>) conf.semanticChecks.iterator().next();
		STATES.computeIfAbsent("constr-intv-prefix", k -> new HashMap<>())
				.put("loop.imp", check.valuesAtFirstAssertion);
		MESSAGES.computeIfAbsent("constr-intv-prefix", k -> new HashMap<>())
				.put("loop.imp", check.assertions);
	}

	@Test
	public void testConstrIntvSuffixLoop() {
		TestConfiguration conf = mkConf();
		conf.analysis = new SimpleAbstractDomain<>(
				new MonolithicMemory(),
				new WholeValueAnalysis(new Interval(), new Suffix(), new BooleanPowerset()),
				new InferredTypes());
		perform("whole-value", "constr/intv-suffix-loop", "loop.imp", conf);
		AssertionCheck<?, ?> check = (AssertionCheck<?, ?>) conf.semanticChecks.iterator().next();
		STATES.computeIfAbsent("constr-intv-suffix", k -> new HashMap<>())
				.put("loop.imp", check.valuesAtFirstAssertion);
		MESSAGES.computeIfAbsent("constr-intv-suffix", k -> new HashMap<>())
				.put("loop.imp", check.assertions);
	}

	@Test
	public void testConstrIntvBSSLoop() {
		TestConfiguration conf = mkConf();
		conf.analysis = new SimpleAbstractDomain<>(
				new MonolithicMemory(),
				new WholeValueAnalysis(new Interval(), new BoundedStringSet(5), new BooleanPowerset()),
				new InferredTypes());
		perform("whole-value", "constr/intv-bss-loop", "loop.imp", conf);
		AssertionCheck<?, ?> check = (AssertionCheck<?, ?>) conf.semanticChecks.iterator().next();
		STATES.computeIfAbsent("constr-intv-bss", k -> new HashMap<>())
				.put("loop.imp", check.valuesAtFirstAssertion);
		MESSAGES.computeIfAbsent("constr-intv-bss", k -> new HashMap<>())
				.put("loop.imp", check.assertions);
	}

	@Test
	public void testConstrIntvCILoop() {
		TestConfiguration conf = mkConf();
		conf.analysis = new SimpleAbstractDomain<>(
				new MonolithicMemory(),
				new WholeValueAnalysis(new Interval(), new CharInclusion(), new BooleanPowerset()),
				new InferredTypes());
		perform("whole-value", "constr/intv-ci-loop", "loop.imp", conf);
		AssertionCheck<?, ?> check = (AssertionCheck<?, ?>) conf.semanticChecks.iterator().next();
		STATES.computeIfAbsent("constr-intv-ci", k -> new HashMap<>())
				.put("loop.imp", check.valuesAtFirstAssertion);
		MESSAGES.computeIfAbsent("constr-intv-ci", k -> new HashMap<>())
				.put("loop.imp", check.assertions);
	}

	@Test
	public void testConstrIntvTarsisLoop() {
		TestConfiguration conf = mkConf();
		conf.analysis = new SimpleAbstractDomain<>(
				new MonolithicMemory(),
				new WholeValueAnalysis(new Interval(), new Tarsis(), new BooleanPowerset()),
				new InferredTypes());
		perform("whole-value", "constr/intv-tarsis-loop", "loop.imp", conf);
		AssertionCheck<?, ?> check = (AssertionCheck<?, ?>) conf.semanticChecks.iterator().next();
		STATES.computeIfAbsent("constr-intv-tarsis", k -> new HashMap<>())
				.put("loop.imp", check.valuesAtFirstAssertion);
		MESSAGES.computeIfAbsent("constr-intv-tarsis", k -> new HashMap<>())
				.put("loop.imp", check.assertions);
	}

	@Test
	public void testConstrCpPrefixSubs() {
		TestConfiguration conf = mkConf();
		conf.analysis = new SimpleAbstractDomain<>(
				new MonolithicMemory(),
				new WholeValueAnalysis(new IntegerConstantPropagation(), new Prefix(), new BooleanPowerset()),
				new InferredTypes());
		perform("whole-value", "constr/cp-prefix-subs", "subs.imp", conf);
		AssertionCheck<?, ?> check = (AssertionCheck<?, ?>) conf.semanticChecks.iterator().next();
		STATES.computeIfAbsent("constr-cp-prefix", k -> new HashMap<>())
				.put("subs.imp", check.valuesAtFirstAssertion);
		MESSAGES.computeIfAbsent("constr-cp-prefix", k -> new HashMap<>())
				.put("subs.imp", check.assertions);
	}

	@Test
	public void testConstrCpSuffixSubs() {
		TestConfiguration conf = mkConf();
		conf.analysis = new SimpleAbstractDomain<>(
				new MonolithicMemory(),
				new WholeValueAnalysis(new IntegerConstantPropagation(), new Suffix(), new BooleanPowerset()),
				new InferredTypes());
		perform("whole-value", "constr/cp-suffix-subs", "subs.imp", conf);
		AssertionCheck<?, ?> check = (AssertionCheck<?, ?>) conf.semanticChecks.iterator().next();
		STATES.computeIfAbsent("constr-cp-suffix", k -> new HashMap<>())
				.put("subs.imp", check.valuesAtFirstAssertion);
		MESSAGES.computeIfAbsent("constr-cp-suffix", k -> new HashMap<>())
				.put("subs.imp", check.assertions);
	}

	@Test
	public void testConstrCpBSSSubs() {
		TestConfiguration conf = mkConf();
		conf.analysis = new SimpleAbstractDomain<>(
				new MonolithicMemory(),
				new WholeValueAnalysis(new IntegerConstantPropagation(), new BoundedStringSet(5),
						new BooleanPowerset()),
				new InferredTypes());
		perform("whole-value", "constr/cp-bss-subs", "subs.imp", conf);
		AssertionCheck<?, ?> check = (AssertionCheck<?, ?>) conf.semanticChecks.iterator().next();
		STATES.computeIfAbsent("constr-cp-bss", k -> new HashMap<>())
				.put("subs.imp", check.valuesAtFirstAssertion);
		MESSAGES.computeIfAbsent("constr-cp-bss", k -> new HashMap<>())
				.put("subs.imp", check.assertions);
	}

	@Test
	public void testConstrCpCISubs() {
		TestConfiguration conf = mkConf();
		conf.analysis = new SimpleAbstractDomain<>(
				new MonolithicMemory(),
				new WholeValueAnalysis(new IntegerConstantPropagation(), new CharInclusion(), new BooleanPowerset()),
				new InferredTypes());
		perform("whole-value", "constr/cp-ci-subs", "subs.imp", conf);
		AssertionCheck<?, ?> check = (AssertionCheck<?, ?>) conf.semanticChecks.iterator().next();
		STATES.computeIfAbsent("constr-cp-ci", k -> new HashMap<>())
				.put("subs.imp", check.valuesAtFirstAssertion);
		MESSAGES.computeIfAbsent("constr-cp-ci", k -> new HashMap<>())
				.put("subs.imp", check.assertions);
	}

	@Test
	public void testConstrCpTarsisSubs() {
		TestConfiguration conf = mkConf();
		conf.analysis = new SimpleAbstractDomain<>(
				new MonolithicMemory(),
				new WholeValueAnalysis(new IntegerConstantPropagation(), new Tarsis(), new BooleanPowerset()),
				new InferredTypes());
		perform("whole-value", "constr/cp-tarsis-subs", "subs.imp", conf);
		AssertionCheck<?, ?> check = (AssertionCheck<?, ?>) conf.semanticChecks.iterator().next();
		STATES.computeIfAbsent("constr-cp-tarsis", k -> new HashMap<>())
				.put("subs.imp", check.valuesAtFirstAssertion);
		MESSAGES.computeIfAbsent("constr-cp-tarsis", k -> new HashMap<>())
				.put("subs.imp", check.assertions);
	}

	@Test
	public void testConstrIntvPrefixSubs() {
		TestConfiguration conf = mkConf();
		conf.analysis = new SimpleAbstractDomain<>(
				new MonolithicMemory(),
				new WholeValueAnalysis(new Interval(), new Prefix(), new BooleanPowerset()),
				new InferredTypes());
		perform("whole-value", "constr/intv-prefix-subs", "subs.imp", conf);
		AssertionCheck<?, ?> check = (AssertionCheck<?, ?>) conf.semanticChecks.iterator().next();
		STATES.computeIfAbsent("constr-intv-prefix", k -> new HashMap<>())
				.put("subs.imp", check.valuesAtFirstAssertion);
		MESSAGES.computeIfAbsent("constr-intv-prefix", k -> new HashMap<>())
				.put("subs.imp", check.assertions);
	}

	@Test
	public void testConstrIntvSuffixSubs() {
		TestConfiguration conf = mkConf();
		conf.analysis = new SimpleAbstractDomain<>(
				new MonolithicMemory(),
				new WholeValueAnalysis(new Interval(), new Suffix(), new BooleanPowerset()),
				new InferredTypes());
		perform("whole-value", "constr/intv-suffix-subs", "subs.imp", conf);
		AssertionCheck<?, ?> check = (AssertionCheck<?, ?>) conf.semanticChecks.iterator().next();
		STATES.computeIfAbsent("constr-intv-suffix", k -> new HashMap<>())
				.put("subs.imp", check.valuesAtFirstAssertion);
		MESSAGES.computeIfAbsent("constr-intv-suffix", k -> new HashMap<>())
				.put("subs.imp", check.assertions);
	}

	@Test
	public void testConstrIntvBSSSubs() {
		TestConfiguration conf = mkConf();
		conf.analysis = new SimpleAbstractDomain<>(
				new MonolithicMemory(),
				new WholeValueAnalysis(new Interval(), new BoundedStringSet(5), new BooleanPowerset()),
				new InferredTypes());
		perform("whole-value", "constr/intv-bss-subs", "subs.imp", conf);
		AssertionCheck<?, ?> check = (AssertionCheck<?, ?>) conf.semanticChecks.iterator().next();
		STATES.computeIfAbsent("constr-intv-bss", k -> new HashMap<>())
				.put("subs.imp", check.valuesAtFirstAssertion);
		MESSAGES.computeIfAbsent("constr-intv-bss", k -> new HashMap<>())
				.put("subs.imp", check.assertions);
	}

	@Test
	public void testConstrIntvCISubs() {
		TestConfiguration conf = mkConf();
		conf.analysis = new SimpleAbstractDomain<>(
				new MonolithicMemory(),
				new WholeValueAnalysis(new Interval(), new CharInclusion(), new BooleanPowerset()),
				new InferredTypes());
		perform("whole-value", "constr/intv-ci-subs", "subs.imp", conf);
		AssertionCheck<?, ?> check = (AssertionCheck<?, ?>) conf.semanticChecks.iterator().next();
		STATES.computeIfAbsent("constr-intv-ci", k -> new HashMap<>())
				.put("subs.imp", check.valuesAtFirstAssertion);
		MESSAGES.computeIfAbsent("constr-intv-ci", k -> new HashMap<>())
				.put("subs.imp", check.assertions);
	}

	@Test
	public void testConstrIntvTarsisSubs() {
		TestConfiguration conf = mkConf();
		conf.analysis = new SimpleAbstractDomain<>(
				new MonolithicMemory(),
				new WholeValueAnalysis(new Interval(), new Tarsis(), new BooleanPowerset()),
				new InferredTypes());
		perform("whole-value", "constr/intv-tarsis-subs", "subs.imp", conf);
		AssertionCheck<?, ?> check = (AssertionCheck<?, ?>) conf.semanticChecks.iterator().next();
		STATES.computeIfAbsent("constr-intv-tarsis", k -> new HashMap<>())
				.put("subs.imp", check.valuesAtFirstAssertion);
		MESSAGES.computeIfAbsent("constr-intv-tarsis", k -> new HashMap<>())
				.put("subs.imp", check.assertions);
	}

	@Test
	public void testConstrCpPrefixCount() {
		TestConfiguration conf = mkConf();
		conf.analysis = new SimpleAbstractDomain<>(
				new MonolithicMemory(),
				new WholeValueAnalysis(new IntegerConstantPropagation(), new Prefix(), new BooleanPowerset()),
				new InferredTypes());
		conf.analysis = new TracePartitioning<>(conf.analysis);
		perform("whole-value", "constr/cp-prefix-count", "count.imp", conf);
		AssertionCheck<?, ?> check = (AssertionCheck<?, ?>) conf.semanticChecks.iterator().next();
		STATES.computeIfAbsent("constr-cp-prefix", k -> new HashMap<>())
				.put("count.imp", check.valuesAtFirstAssertion);
		MESSAGES.computeIfAbsent("constr-cp-prefix", k -> new HashMap<>())
				.put("count.imp", check.assertions);
	}

	@Test
	public void testConstrCpSuffixCount() {
		TestConfiguration conf = mkConf();
		conf.analysis = new SimpleAbstractDomain<>(
				new MonolithicMemory(),
				new WholeValueAnalysis(new IntegerConstantPropagation(), new Suffix(), new BooleanPowerset()),
				new InferredTypes());
		conf.analysis = new TracePartitioning<>(conf.analysis);
		perform("whole-value", "constr/cp-suffix-count", "count.imp", conf);
		AssertionCheck<?, ?> check = (AssertionCheck<?, ?>) conf.semanticChecks.iterator().next();
		STATES.computeIfAbsent("constr-cp-suffix", k -> new HashMap<>())
				.put("count.imp", check.valuesAtFirstAssertion);
		MESSAGES.computeIfAbsent("constr-cp-suffix", k -> new HashMap<>())
				.put("count.imp", check.assertions);
	}

	@Test
	public void testConstrCpBSSCount() {
		TestConfiguration conf = mkConf();
		conf.analysis = new SimpleAbstractDomain<>(
				new MonolithicMemory(),
				new WholeValueAnalysis(new IntegerConstantPropagation(), new BoundedStringSet(5),
						new BooleanPowerset()),
				new InferredTypes());
		conf.analysis = new TracePartitioning<>(conf.analysis);
		perform("whole-value", "constr/cp-bss-count", "count.imp", conf);
		AssertionCheck<?, ?> check = (AssertionCheck<?, ?>) conf.semanticChecks.iterator().next();
		STATES.computeIfAbsent("constr-cp-bss", k -> new HashMap<>())
				.put("count.imp", check.valuesAtFirstAssertion);
		MESSAGES.computeIfAbsent("constr-cp-bss", k -> new HashMap<>())
				.put("count.imp", check.assertions);
	}

	@Test
	public void testConstrCpCICount() {
		TestConfiguration conf = mkConf();
		conf.analysis = new SimpleAbstractDomain<>(
				new MonolithicMemory(),
				new WholeValueAnalysis(new IntegerConstantPropagation(), new CharInclusion(), new BooleanPowerset()),
				new InferredTypes());
		conf.analysis = new TracePartitioning<>(conf.analysis);
		perform("whole-value", "constr/cp-ci-count", "count.imp", conf);
		AssertionCheck<?, ?> check = (AssertionCheck<?, ?>) conf.semanticChecks.iterator().next();
		STATES.computeIfAbsent("constr-cp-ci", k -> new HashMap<>())
				.put("count.imp", check.valuesAtFirstAssertion);
		MESSAGES.computeIfAbsent("constr-cp-ci", k -> new HashMap<>())
				.put("count.imp", check.assertions);
	}

	@Test
	public void testConstrCpTarsisCount() {
		TestConfiguration conf = mkConf();
		conf.analysis = new SimpleAbstractDomain<>(
				new MonolithicMemory(),
				new WholeValueAnalysis(new IntegerConstantPropagation(), new Tarsis(), new BooleanPowerset()),
				new InferredTypes());
		conf.analysis = new TracePartitioning<>(conf.analysis);
		perform("whole-value", "constr/cp-tarsis-count", "count.imp", conf);
		AssertionCheck<?, ?> check = (AssertionCheck<?, ?>) conf.semanticChecks.iterator().next();
		STATES.computeIfAbsent("constr-cp-tarsis", k -> new HashMap<>())
				.put("count.imp", check.valuesAtFirstAssertion);
		MESSAGES.computeIfAbsent("constr-cp-tarsis", k -> new HashMap<>())
				.put("count.imp", check.assertions);
	}

	@Test
	public void testConstrIntvPrefixCount() {
		TestConfiguration conf = mkConf();
		conf.analysis = new SimpleAbstractDomain<>(
				new MonolithicMemory(),
				new WholeValueAnalysis(new Interval(), new Prefix(), new BooleanPowerset()),
				new InferredTypes());
		conf.analysis = new TracePartitioning<>(conf.analysis);
		perform("whole-value", "constr/intv-prefix-count", "count.imp", conf);
		AssertionCheck<?, ?> check = (AssertionCheck<?, ?>) conf.semanticChecks.iterator().next();
		STATES.computeIfAbsent("constr-intv-prefix", k -> new HashMap<>())
				.put("count.imp", check.valuesAtFirstAssertion);
		MESSAGES.computeIfAbsent("constr-intv-prefix", k -> new HashMap<>())
				.put("count.imp", check.assertions);
	}

	@Test
	public void testConstrIntvSuffixCount() {
		TestConfiguration conf = mkConf();
		conf.analysis = new SimpleAbstractDomain<>(
				new MonolithicMemory(),
				new WholeValueAnalysis(new Interval(), new Suffix(), new BooleanPowerset()),
				new InferredTypes());
		conf.analysis = new TracePartitioning<>(conf.analysis);
		perform("whole-value", "constr/intv-suffix-count", "count.imp", conf);
		AssertionCheck<?, ?> check = (AssertionCheck<?, ?>) conf.semanticChecks.iterator().next();
		STATES.computeIfAbsent("constr-intv-suffix", k -> new HashMap<>())
				.put("count.imp", check.valuesAtFirstAssertion);
		MESSAGES.computeIfAbsent("constr-intv-suffix", k -> new HashMap<>())
				.put("count.imp", check.assertions);
	}

	@Test
	public void testConstrIntvBSSCount() {
		TestConfiguration conf = mkConf();
		conf.analysis = new SimpleAbstractDomain<>(
				new MonolithicMemory(),
				new WholeValueAnalysis(new Interval(), new BoundedStringSet(5), new BooleanPowerset()),
				new InferredTypes());
		conf.analysis = new TracePartitioning<>(conf.analysis);
		perform("whole-value", "constr/intv-bss-count", "count.imp", conf);
		AssertionCheck<?, ?> check = (AssertionCheck<?, ?>) conf.semanticChecks.iterator().next();
		STATES.computeIfAbsent("constr-intv-bss", k -> new HashMap<>())
				.put("count.imp", check.valuesAtFirstAssertion);
		MESSAGES.computeIfAbsent("constr-intv-bss", k -> new HashMap<>())
				.put("count.imp", check.assertions);
	}

	@Test
	public void testConstrIntvCICount() {
		TestConfiguration conf = mkConf();
		conf.analysis = new SimpleAbstractDomain<>(
				new MonolithicMemory(),
				new WholeValueAnalysis(new Interval(), new CharInclusion(), new BooleanPowerset()),
				new InferredTypes());
		conf.analysis = new TracePartitioning<>(conf.analysis);
		perform("whole-value", "constr/intv-ci-count", "count.imp", conf);
		AssertionCheck<?, ?> check = (AssertionCheck<?, ?>) conf.semanticChecks.iterator().next();
		STATES.computeIfAbsent("constr-intv-ci", k -> new HashMap<>())
				.put("count.imp", check.valuesAtFirstAssertion);
		MESSAGES.computeIfAbsent("constr-intv-ci", k -> new HashMap<>())
				.put("count.imp", check.assertions);
	}

	@Test
	public void testConstrIntvTarsisCount() {
		TestConfiguration conf = mkConf();
		conf.analysis = new SimpleAbstractDomain<>(
				new MonolithicMemory(),
				new WholeValueAnalysis(new Interval(), new Tarsis(), new BooleanPowerset()),
				new InferredTypes());
		conf.analysis = new TracePartitioning<>(conf.analysis);
		perform("whole-value", "constr/intv-tarsis-count", "count.imp", conf);
		AssertionCheck<?, ?> check = (AssertionCheck<?, ?>) conf.semanticChecks.iterator().next();
		STATES.computeIfAbsent("constr-intv-tarsis", k -> new HashMap<>())
				.put("count.imp", check.valuesAtFirstAssertion);
		MESSAGES.computeIfAbsent("constr-intv-tarsis", k -> new HashMap<>())
				.put("count.imp", check.assertions);
	}

	@AfterAll
	public static void summary() {
		List<String> int_domains = List.of("intv", "cp");
		List<String> string_domains = List.of("prefix", "suffix", "ci", "tarsis", "bss");
		List<String> testfiles = List.of("toString.imp", "subs.imp", "loop.imp", "count.imp");

		for (String testFile : testfiles) {
			System.out.println("\n\n### Test file: " + testFile);

			Map<CodeLocation, String> map = MESSAGES.values().iterator().next().get(testFile);
			if (map == null) {
				System.out.println("No assertions found for test file " + testFile);
				continue;
			}
			Set<CodeLocation> assertionLocs = new TreeSet<>(map.keySet());
			Set<String> sortedDoms = new TreeSet<>();
			for (String strDom : string_domains)
				for (String intDom : int_domains) {
					sortedDoms.add("smashed-" + intDom + "-" + strDom);
					sortedDoms.add("constr-" + intDom + "-" + strDom);
				}

			String[][] table = new String[sortedDoms.size() + 1][2 + assertionLocs.size()];
			table[0][0] = "DOMAIN";
			int i = 1;
			for (CodeLocation loc : assertionLocs)
				table[0][i++] = "LINE " + ((SourceCodeLocation) loc).getLine();
			table[0][i] = "APPROXIMATION";

			i = 1;
			for (String domain : sortedDoms) {
				table[i][0] = domain;
				int j = 1;
				Map<CodeLocation, String> messages;
				Map<String, Map<CodeLocation, String>> dom = MESSAGES.get(domain);
				if (dom == null) {
					for (; j < assertionLocs.size() + 1; j++)
						table[i][j] = "<missing>";
				} else {
					messages = dom.get(testFile);
					if (messages == null) {
						for (; j < assertionLocs.size() + 1; j++)
							table[i][j] = "<missing>";
					} else
						for (CodeLocation loc : assertionLocs) {
							String msg = messages.get(loc);
							table[i][j++] = msg;
						}
				}

				Map<String, Lattice<?>> states = STATES.get(domain);
				if (states == null) {
					table[i][j] = "<missing>";
				} else {
					Lattice<?> state = states.get(testFile);
					if (state == null) {
						table[i][j] = "<missing>";
					} else
						table[i][j] = state.toString();
				}
				i++;
			}
			System.out.println(toString(table));
		}
	}

	public static String toString(
			String[][] table) {
		int cols = table[0].length;
		int[] colWidths = new int[cols];
		StringBuilder builder = new StringBuilder();

		// Calculate max width for each column
		for (int c = 0; c < cols; c++)
			for (String[] row : table)
				for (String line : row[c].split("\n"))
					colWidths[c] = Math.max(colWidths[c], line.length());

		// Print each row
		separatorLine(cols, colWidths, builder);
		for (int r = 0; r < table.length; r++) {
			for (int c = 0; c < cols; c++) {
				if (c > 0)
					builder.append(" ");
				builder.append("| ").append(padRight(table[r][c], colWidths[c]));
			}
			builder.append("|\n");
			if (r == 0 || r == table.length - 1)
				separatorLine(cols, colWidths, builder);
		}
		return builder.toString();
	}

	private static void separatorLine(
			int cols,
			int[] colWidths,
			StringBuilder builder) {
		for (int c = 0; c < cols; c++) {
			if (c > 0)
				builder.append("-");
			builder.append("+-").append("-".repeat(colWidths[c]));
		}
		builder.append("+\n");
	}

	private static String padRight(
			String s,
			int n) {
		return String.format("%-" + n + "s", s);
	}

}

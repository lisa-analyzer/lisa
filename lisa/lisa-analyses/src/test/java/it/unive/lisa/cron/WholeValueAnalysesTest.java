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
import it.unive.lisa.analysis.combination.constraints.WholeValueElement;
import it.unive.lisa.analysis.combination.constraints.WholeValueStringDomain;
import it.unive.lisa.analysis.combination.smash.SmashedSum;
import it.unive.lisa.analysis.combination.smash.SmashedSumIntDomain;
import it.unive.lisa.analysis.combination.smash.SmashedSumStringDomain;
import it.unive.lisa.analysis.combination.smash.SmashedValue;
import it.unive.lisa.analysis.heap.MonolithicHeap;
import it.unive.lisa.analysis.nonrelational.value.BaseNonRelationalValueDomain;
import it.unive.lisa.analysis.nonrelational.value.BooleanPowerset;
import it.unive.lisa.analysis.nonrelational.value.NonRelationalValueDomain;
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
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import org.junit.AfterClass;
import org.junit.Test;

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
							ValueEnvironment<
									?> env = targetPost.getExecutionState().getLatticeInstance(ValueEnvironment.class);
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
				ValueEnvironment<?> values = target.getExecutionState().getLatticeInstance(ValueEnvironment.class);
				Lattice<?> state = values.getState((Identifier) expr);
				Satisfiability sat = Satisfiability.UNKNOWN;
				ValueDomain<?> vdom = ((SimpleAbstractDomain<?, ?, ?>) tool.getAnalysis().domain).valueDomain;
				if (state instanceof SmashedValue<?, ?>) {
					SmashedSumStringDomain dom = ((SmashedSum<?, ?>) vdom).strDom;
					Lattice<?> abstractString = ((SmashedValue<?, ?>) state).getStringValue();
					sat = dom.containsChar(abstractString, ch.getValue().charAt(0));
				} else {
					WholeValueStringDomain dom = ((WholeValueAnalysis<?, ?, ?>) vdom).strDom;
					WholeValueElement<?> abstractString = ((WholeValue<?, ?, ?>) state).getStringValue();
					sat = dom.containsChar(abstractString, ch.getValue().charAt(0));
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

	private static Map<String, NonRelationalValueDomain<?>> INT_DOMAINS = Map
			.of("intv", new Interval(), "cp", new IntegerConstantPropagation());

	private static Map<String,
			NonRelationalValueDomain<?>> STRING_DOMAINS = Map.of(
					"prefix",
					new Prefix(),
					"suffix",
					new Suffix(),
					"ci",
					new CharInclusion(),
					"tarsis",
					new Tarsis(),
					"bss",
					new BoundedStringSet(5));

	private static Map<String,
			Boolean> TESTFILES = Map.of("toString.imp", false, "subs.imp", false, "loop.imp", false, "count.imp", true);

	private static Map<String, Map<String, Lattice<?>>> STATES = new HashMap<>();

	private static Map<String, Map<String, Map<CodeLocation, String>>> MESSAGES = new HashMap<>();

	@Test
	@SuppressWarnings({ "rawtypes", "unchecked" })
	public void testSmashedSum() {
		for (Map.Entry<String, NonRelationalValueDomain<?>> intDomain : INT_DOMAINS.entrySet())
			for (Map.Entry<String, NonRelationalValueDomain<?>> strDomain : STRING_DOMAINS.entrySet())
				for (Map.Entry<String, Boolean> test : TESTFILES.entrySet()) {
					TestConfiguration conf = mkConf();
					conf.analysis = new SimpleAbstractDomain<>(
							new MonolithicHeap(),
							new SmashedSum<>(
									(SmashedSumIntDomain) intDomain.getValue(),
									(SmashedSumStringDomain) strDomain.getValue()),
							new InferredTypes());
					if (test.getValue())
						conf.analysis = new TracePartitioning(conf.analysis);
					// conf.analysisGraphs =
					// it.unive.lisa.conf.LiSAConfiguration.GraphType.HTML_WITH_SUBNODES;
					String testKey = intDomain.getKey() + "-" + strDomain.getKey() + "-" + test.getKey();
					System.out.println("\n\n### Running test " + testKey);
					perform("whole-value", "smashed/" + testKey.replace(".imp", ""), test.getKey(), conf);
					AssertionCheck<?, ?> check = (AssertionCheck<?, ?>) conf.semanticChecks.iterator().next();
					STATES
							.computeIfAbsent(
									"smashed-" + intDomain.getKey() + "-" + strDomain.getKey(),
									k -> new HashMap<>())
							.put(test.getKey(), check.valuesAtFirstAssertion);
					MESSAGES
							.computeIfAbsent(
									"smashed-" + intDomain.getKey() + "-" + strDomain.getKey(),
									k -> new HashMap<>())
							.put(test.getKey(), check.assertions);
				}
	}

	@Test
	@SuppressWarnings({ "rawtypes", "unchecked" })
	public void testConstraints() {
		for (Map.Entry<String, NonRelationalValueDomain<?>> intDomain : INT_DOMAINS.entrySet())
			for (Map.Entry<String, NonRelationalValueDomain<?>> strDomain : STRING_DOMAINS.entrySet())
				for (Map.Entry<String, Boolean> test : TESTFILES.entrySet()) {
					TestConfiguration conf = mkConf();
					conf.analysis = new SimpleAbstractDomain<>(
							new MonolithicHeap(),
							new WholeValueAnalysis(
									(BaseNonRelationalValueDomain<?>) intDomain.getValue(),
									(WholeValueStringDomain<?>) strDomain.getValue(),
									new BooleanPowerset()),
							new InferredTypes());
					if (test.getValue())
						conf.analysis = new TracePartitioning(conf.analysis);
					// conf.analysisGraphs =
					// it.unive.lisa.conf.LiSAConfiguration.GraphType.HTML_WITH_SUBNODES;
					String testKey = intDomain.getKey() + "-" + strDomain.getKey() + "-" + test.getKey();
					System.out.println("\n\n### Running test " + testKey);
					perform("whole-value", "constr/" + testKey.replace(".imp", ""), test.getKey(), conf);
					AssertionCheck<?, ?> check = (AssertionCheck<?, ?>) conf.semanticChecks.iterator().next();
					STATES.computeIfAbsent(
							"constr-" + intDomain.getKey() + "-" + strDomain.getKey(),
							k -> new HashMap<>())
							.put(test.getKey(), check.valuesAtFirstAssertion);
					MESSAGES.computeIfAbsent(
							"constr-" + intDomain.getKey() + "-" + strDomain.getKey(),
							k -> new HashMap<>())
							.put(test.getKey(), check.assertions);
				}
	}

	@AfterClass
	public static void summary() {
		for (String testFile : TESTFILES.keySet()) {
			System.out.println("\n\n### Test file: " + testFile);

			Set<CodeLocation> assertionLocs = new TreeSet<>(MESSAGES.values().iterator().next().get(testFile).keySet());
			String[][] table = new String[STATES.size() + 1][2 + assertionLocs.size()];
			table[0][0] = "DOMAIN";
			int i = 1;
			for (CodeLocation loc : assertionLocs)
				table[0][i++] = "LINE " + ((SourceCodeLocation) loc).getLine();
			table[0][i] = "APPROXIMATION";

			Set<String> sortedDoms = new TreeSet<>();
			for (String strDom : STRING_DOMAINS.keySet())
				for (String intDom : INT_DOMAINS.keySet()) {
					sortedDoms.add("smashed-" + intDom + "-" + strDom);
					sortedDoms.add("constr-" + intDom + "-" + strDom);
				}
			i = 1;
			for (String domain : sortedDoms) {
				table[i][0] = domain;
				Map<CodeLocation, String> messages = MESSAGES.get(domain).get(testFile);
				int j = 1;
				for (CodeLocation loc : assertionLocs) {
					String msg = messages.get(loc);
					table[i][j++] = msg;
				}
				table[i][j] = STATES.get(domain).get(testFile).toString();
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

package it.unive.lisa.cron;

import java.io.IOException;

import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;

import it.unive.lisa.AnalysisExecutionException;
import it.unive.lisa.AnalysisSetupException;
import it.unive.lisa.AnalysisTestExecutor;
import it.unive.lisa.CronConfiguration;
import it.unive.lisa.analysis.AbstractState;
import it.unive.lisa.analysis.AnalysisState;
import it.unive.lisa.analysis.AnalyzedCFG;
import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.analysis.SimpleAbstractState;
import it.unive.lisa.analysis.combination.SmashedSum;
import it.unive.lisa.analysis.combination.SmashedSumStringDomain;
import it.unive.lisa.analysis.heap.MonolithicHeap;
import it.unive.lisa.analysis.lattices.Satisfiability;
import it.unive.lisa.analysis.nonrelational.value.TypeEnvironment;
import it.unive.lisa.analysis.nonrelational.value.ValueEnvironment;
import it.unive.lisa.analysis.numeric.Interval;
import it.unive.lisa.analysis.string.CharInclusion;
import it.unive.lisa.analysis.string.Prefix;
import it.unive.lisa.analysis.string.Suffix;
import it.unive.lisa.analysis.string.bricks.Bricks;
import it.unive.lisa.analysis.string.tarsis.Tarsis;
import it.unive.lisa.analysis.traces.TracePartitioning;
import it.unive.lisa.analysis.types.InferredTypes;
import it.unive.lisa.checks.semantic.CheckToolWithAnalysisResults;
import it.unive.lisa.checks.semantic.SemanticCheck;
import it.unive.lisa.imp.constructs.StringContains.IMPStringContains;
import it.unive.lisa.imp.expressions.IMPAssert;
import it.unive.lisa.interprocedural.ReturnTopPolicy;
import it.unive.lisa.interprocedural.callgraph.RTACallGraph;
import it.unive.lisa.interprocedural.context.ContextBasedAnalysis;
import it.unive.lisa.interprocedural.context.FullStackToken;
import it.unive.lisa.program.cfg.CFG;
import it.unive.lisa.program.cfg.statement.Expression;
import it.unive.lisa.program.cfg.statement.Statement;
import it.unive.lisa.program.cfg.statement.VariableRef;
import it.unive.lisa.program.cfg.statement.literal.StringLiteral;
import it.unive.lisa.symbolic.SymbolicExpression;
import it.unive.lisa.symbolic.value.Identifier;

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class WholeValueAnalysesTest extends AnalysisTestExecutor {

	private static class AssertionCheck<A extends AbstractState<A>, S extends SmashedSumStringDomain<S>>
			implements
			SemanticCheck<A> {

		private boolean first = true;

		@Override
		public boolean visit(
				CheckToolWithAnalysisResults<A> tool,
				CFG graph,
				Statement node) {

			if (node instanceof IMPAssert) {
				Expression assertion = ((IMPAssert) node).getSubExpression();

				for (AnalyzedCFG<A> res : tool.getResultOf(graph)) {
					AnalysisState<A> post = res.getAnalysisStateAfter(assertion);

					try {
						@SuppressWarnings("unchecked")
						A state = (A) post.getState().getDomainInstance(SimpleAbstractState.class);

						if (first) {
							first = false;
							@SuppressWarnings("unchecked")
							ValueEnvironment<SmashedSum<S>> values = (ValueEnvironment<SmashedSum<S>>) state.getDomainInstance(ValueEnvironment.class);
							System.err.println("Values at first assertion:\n" + values);
						}

						if (assertion instanceof IMPStringContains) {
							Expression[] args = ((IMPStringContains) assertion).getSubExpressions();
							VariableRef variable = (VariableRef) args[0];
							StringLiteral ch = (StringLiteral) args[1];
							if (ch.getValue().length() == 1)
								containsCharAssertion(tool, node, res, variable, ch);
							else
								assertion(tool, node, post, state);
						} else
							assertion(tool, node, post, state);
					} catch (SemanticException e) {
						throw new AnalysisExecutionException("Error while checking assertions", e);
					}
				}
			}
			return true;
		}

		private void containsCharAssertion(CheckToolWithAnalysisResults<A> tool, Statement node, AnalyzedCFG<A> res,
				VariableRef variable, StringLiteral ch) throws SemanticException {
			AnalysisState<A> target = res.getAnalysisStateAfter(variable);
			for (SymbolicExpression expr : target.getComputedExpressions()) {
				@SuppressWarnings("unchecked")
				ValueEnvironment<SmashedSum<S>> values = (ValueEnvironment<SmashedSum<S>>) target.getState().getDomainInstance(ValueEnvironment.class);
				S abstractString = values.getState((Identifier) expr).getStringValue();
				Satisfiability sat = abstractString.containsChar(ch.getValue().charAt(0));
				if (sat == Satisfiability.UNKNOWN)
					warnOn(tool, node, "This assertion might fail");
				else if (sat == Satisfiability.NOT_SATISFIED)
					warnOn(tool, node, "This assertion always fails");
				else
					warnOn(tool, node, null);
			}
		}

		private void assertion(CheckToolWithAnalysisResults<A> tool, Statement node, AnalysisState<A> post, A state)
				throws SemanticException {
			for (SymbolicExpression expr : post.getComputedExpressions()) {
				Satisfiability sat = state.satisfies(expr, node, state);
				if (sat == Satisfiability.UNKNOWN)
					warnOn(tool, node, "This assertion might fail");
				else if (sat == Satisfiability.NOT_SATISFIED)
					warnOn(tool, node, "This assertion always fails");
				else
					warnOn(tool, node, null);
			}
		}

		private void warnOn(
				CheckToolWithAnalysisResults<A> tool,
				Statement node,
				String message) {
			if (message != null) {
				tool.warnOn(node, message);
				System.err.println("Warning on " + node.getLocation() + ": " + message);
			} else 
				System.err.println("No warning on " + node.getLocation());
		}
	}

	private static <S extends SmashedSumStringDomain<S>> CronConfiguration baseConf(
			S stringDomain)
			throws AnalysisSetupException {
		return baseConf(stringDomain, false, false);
	}

	private static <S extends SmashedSumStringDomain<S>> CronConfiguration baseConf(
			S stringDomain, 
			boolean traces)
			throws AnalysisSetupException {
		return baseConf(stringDomain, traces, false);
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	private static <S extends SmashedSumStringDomain<S>> CronConfiguration baseConf(
			S stringDomain,
			boolean traces,
			boolean dump)
			throws AnalysisSetupException {
		CronConfiguration conf = new CronConfiguration();
		conf.jsonOutput = true;
		conf.abstractState = new SimpleAbstractState<>(
			new MonolithicHeap(),
			new ValueEnvironment<>(new SmashedSum<>(new Interval(), stringDomain, Satisfiability.UNKNOWN)),
			new TypeEnvironment<>(new InferredTypes()));
		if (traces)
			conf.abstractState = new TracePartitioning(conf.abstractState);
		conf.semanticChecks.add(new AssertionCheck<>());
		conf.openCallPolicy = ReturnTopPolicy.INSTANCE;
		conf.callGraph = new RTACallGraph();
		conf.interproceduralAnalysis = new ContextBasedAnalysis<>(FullStackToken.getSingleton());
		conf.compareWithOptimization = false;
		conf.optimize = true;
		conf.hotspots = st -> st instanceof IMPAssert
				|| (st instanceof Expression && ((Expression) st).getRootStatement() instanceof IMPAssert);
		if (dump) {
			conf.serializeResults = true;
			conf.analysisGraphs = it.unive.lisa.conf.LiSAConfiguration.GraphType.HTML_WITH_SUBNODES;
		}
		return conf;
	}

	private void perform(
			String dir,
			String subDir,
			String program,
			CronConfiguration conf) {
		conf.testDir = dir;
		conf.testSubDir = subDir;
		conf.programFile = program;
		perform(conf);
	}

	@Test
	public void toStringPrefixTest() throws IOException, AnalysisSetupException {
		perform("whole-value", "tostring/prefix", "toString.imp", baseConf(new Prefix()));
	}

	@Test
	public void toStringSuffixTest() throws IOException, AnalysisSetupException {
		perform("whole-value", "tostring/suffix", "toString.imp", baseConf(new Suffix()));
	}

	@Test
	public void toStringCiTest() throws IOException, AnalysisSetupException {
		perform("whole-value", "tostring/ci", "toString.imp", baseConf(new CharInclusion()));
	}

	@Test
	public void toStringBricksTest() throws IOException, AnalysisSetupException {
		perform("whole-value", "tostring/bricks", "toString.imp", baseConf(new Bricks()));
	}

	@Test
	public void toStringTarsisTest() throws IOException, AnalysisSetupException {
		perform("whole-value", "tostring/tarsis", "toString.imp", baseConf(new Tarsis()));
	}

	@Test
	public void substringPrefixTest() throws IOException, AnalysisSetupException {
		perform("whole-value", "substring/prefix", "subs.imp", baseConf(new Prefix()));
	}

	@Test
	public void substringSuffixTest() throws IOException, AnalysisSetupException {
		perform("whole-value", "substring/suffix", "subs.imp", baseConf(new Suffix()));
	}

	@Test
	public void substringCiTest() throws IOException, AnalysisSetupException {
		perform("whole-value", "substring/ci", "subs.imp", baseConf(new CharInclusion()));
	}

	@Test
	public void substringBricksTest() throws IOException, AnalysisSetupException {
		perform("whole-value", "substring/bricks", "subs.imp", baseConf(new Bricks()));
	}

	@Test
	public void substringTarsisTest() throws IOException, AnalysisSetupException {
		perform("whole-value", "substring/tarsis", "subs.imp", baseConf(new Tarsis()));
	}

	@Test
	public void loopPrefixTest() throws IOException, AnalysisSetupException {
		perform("whole-value", "loop/prefix", "loop.imp", baseConf(new Prefix()));
	}

	@Test
	public void loopSuffixTest() throws IOException, AnalysisSetupException {
		perform("whole-value", "loop/suffix", "loop.imp", baseConf(new Suffix()));
	}

	@Test
	public void loopCiTest() throws IOException, AnalysisSetupException {
		perform("whole-value", "loop/ci", "loop.imp", baseConf(new CharInclusion()));
	}

	@Test
	public void loopBricksTest() throws IOException, AnalysisSetupException {
		perform("whole-value", "loop/bricks", "loop.imp", baseConf(new Bricks()));
	}

	@Test
	public void loopTarsisTest() throws IOException, AnalysisSetupException {
		perform("whole-value", "loop/tarsis", "loop.imp", baseConf(new Tarsis()));
	}

	@Test
	public void cmPrefixTest() throws IOException, AnalysisSetupException {
		perform("whole-value", "count/prefix", "count.imp", baseConf(new Prefix(), true));
	}

	@Test
	public void cmSuffixTest() throws IOException, AnalysisSetupException {
		perform("whole-value", "count/suffix", "count.imp", baseConf(new Suffix(), true));
	}

	@Test
	public void cmCiTest() throws IOException, AnalysisSetupException {
		perform("whole-value", "count/ci", "count.imp", baseConf(new CharInclusion(), true));
	}

	@Test
	public void cmBricksTest() throws IOException, AnalysisSetupException {
		perform("whole-value", "count/bricks", "count.imp", baseConf(new Bricks(), true));
	}

	@Test
	public void cmTarsisTest() throws IOException, AnalysisSetupException {
		perform("whole-value", "count/tarsis", "count.imp", baseConf(new Tarsis(), true));
	}
}
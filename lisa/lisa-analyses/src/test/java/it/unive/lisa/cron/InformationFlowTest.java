package it.unive.lisa.cron;

import it.unive.lisa.DefaultConfiguration;
import it.unive.lisa.analysis.AnalysisState;
import it.unive.lisa.analysis.AnalyzedCFG;
import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.analysis.SemanticOracle;
import it.unive.lisa.analysis.SimpleAbstractDomain;
import it.unive.lisa.analysis.heap.MonolithicHeap;
import it.unive.lisa.analysis.informationFlow.BaseTaint;
import it.unive.lisa.analysis.informationFlow.NonInterference;
import it.unive.lisa.analysis.informationFlow.ThreeLevelsTaint;
import it.unive.lisa.analysis.informationFlow.TwoLevelsTaint;
import it.unive.lisa.analysis.nonrelational.type.TypeEnvironment;
import it.unive.lisa.analysis.nonrelational.value.ValueEnvironment;
import it.unive.lisa.analysis.types.InferredTypes;
import it.unive.lisa.checks.semantic.CheckToolWithAnalysisResults;
import it.unive.lisa.checks.semantic.SemanticCheck;
import it.unive.lisa.interprocedural.ReturnTopPolicy;
import it.unive.lisa.interprocedural.callgraph.RTACallGraph;
import it.unive.lisa.interprocedural.context.ContextBasedAnalysis;
import it.unive.lisa.interprocedural.context.FullStackToken;
import it.unive.lisa.lattices.SimpleAbstractState;
import it.unive.lisa.lattices.heap.Monolith;
import it.unive.lisa.lattices.informationFlow.NonInterferenceEnvironment;
import it.unive.lisa.lattices.informationFlow.NonInterferenceValue;
import it.unive.lisa.lattices.informationFlow.TaintLattice;
import it.unive.lisa.lattices.types.TypeSet;
import it.unive.lisa.program.cfg.CFG;
import it.unive.lisa.program.cfg.CodeMember;
import it.unive.lisa.program.cfg.Parameter;
import it.unive.lisa.program.cfg.statement.Assignment;
import it.unive.lisa.program.cfg.statement.Expression;
import it.unive.lisa.program.cfg.statement.Statement;
import it.unive.lisa.program.cfg.statement.call.CFGCall;
import it.unive.lisa.program.cfg.statement.call.Call;
import it.unive.lisa.program.cfg.statement.call.UnresolvedCall;
import it.unive.lisa.symbolic.SymbolicExpression;
import it.unive.lisa.symbolic.value.ValueExpression;
import org.junit.Test;

public class InformationFlowTest
		extends
		IMPCronExecutor {

	@Test
	public void testTaint() {
		CronConfiguration conf = new CronConfiguration();
		conf.analysis = DefaultConfiguration
				.simpleState(
						DefaultConfiguration.defaultHeapDomain(),
						new TwoLevelsTaint(),
						DefaultConfiguration.defaultTypeDomain());
		conf.serializeResults = true;
		conf.openCallPolicy = ReturnTopPolicy.INSTANCE;
		conf.callGraph = new RTACallGraph();
		conf.interproceduralAnalysis = new ContextBasedAnalysis<>();
		conf.semanticChecks.add(new TaintCheck<>());
		conf.testDir = "taint";
		conf.testSubDir = "2val";
		conf.programFile = "taint.imp";
		conf.hotspots = st -> st instanceof Expression && ((Expression) st).getParentStatement() instanceof Call;
		perform(conf);
	}

	@Test
	public void testThreeLevelsTaint() {
		CronConfiguration conf = new CronConfiguration();
		conf.analysis = DefaultConfiguration
				.simpleState(
						DefaultConfiguration.defaultHeapDomain(),
						new ThreeLevelsTaint(),
						DefaultConfiguration.defaultTypeDomain());
		conf.serializeResults = true;
		conf.openCallPolicy = ReturnTopPolicy.INSTANCE;
		conf.callGraph = new RTACallGraph();
		conf.interproceduralAnalysis = new ContextBasedAnalysis<>();
		conf.semanticChecks.add(new TaintCheck<>());
		conf.testDir = "taint";
		conf.testSubDir = "3val";
		conf.programFile = "taint.imp";
		conf.hotspots = st -> st instanceof Expression && ((Expression) st).getParentStatement() instanceof Call;
		perform(conf);
	}

	private static class TaintCheck<L extends TaintLattice<L>>
			implements
			SemanticCheck<SimpleAbstractState<Monolith,
					ValueEnvironment<L>,
					TypeEnvironment<TypeSet>>,
					SimpleAbstractDomain<Monolith,
							ValueEnvironment<L>,
							TypeEnvironment<TypeSet>>> {

		@Override
		public boolean visit(
				CheckToolWithAnalysisResults<SimpleAbstractState<Monolith,
						ValueEnvironment<L>,
						TypeEnvironment<TypeSet>>,
						SimpleAbstractDomain<Monolith,
								ValueEnvironment<L>,
								TypeEnvironment<TypeSet>>> tool,
				CFG graph,
				Statement node) {
			if (!(node instanceof UnresolvedCall))
				return true;

			UnresolvedCall call = (UnresolvedCall) node;
			BaseTaint<L> domain = (BaseTaint<L>) tool.getAnalysis().domain.valueDomain;

			try {
				for (AnalyzedCFG<SimpleAbstractState<Monolith,
						ValueEnvironment<L>,
						TypeEnvironment<TypeSet>>> result : tool.getResultOf(call.getCFG())) {
					Call resolved = (Call) tool.getResolvedVersion(call, result);

					if (resolved instanceof CFGCall) {
						CFGCall cfg = (CFGCall) resolved;
						for (CodeMember n : cfg.getTargets()) {
							Parameter[] parameters = n.getDescriptor().getFormals();
							for (int i = 0; i < parameters.length; i++)
								if (parameters[i].getAnnotations().contains(BaseTaint.CLEAN_MATCHER)) {
									AnalysisState<SimpleAbstractState<Monolith,
											ValueEnvironment<L>,
											TypeEnvironment<TypeSet>>> post = result
													.getAnalysisStateAfter(call.getParameters()[i]);
									SemanticOracle oracle = tool.getAnalysis().domain.makeOracle(post.getState());

									for (SymbolicExpression e : tool
											.getAnalysis()
											.rewrite(post, post.getComputedExpressions(), node)) {
										L stack = domain
												.eval(post.getState().valueState, (ValueExpression) e, node, oracle);

										if (stack.isAlwaysTainted())
											tool
													.warnOn(
															call,
															"Parameter " + i + " is always tainted");
										else if (stack.isPossiblyTainted())
											tool
													.warnOn(
															call,
															"Parameter " + i + " is possibly tainted");
									}
								}
						}
					}
				}
			} catch (SemanticException e1) {
				e1.printStackTrace();
			}

			return true;
		}

	}

	@Test
	public void testConfidentialityNI() {
		CronConfiguration conf = new CronConfiguration();
		conf.serializeResults = true;
		conf.analysis = new SimpleAbstractDomain<>(new MonolithicHeap(), new NonInterference(), new InferredTypes());
		conf.semanticChecks.add(new NICheck());
		conf.testDir = "non-interference/confidentiality";
		conf.programFile = "program.imp";
		conf.hotspots = st -> st instanceof Assignment
				|| (st instanceof Expression && ((Expression) st).getParentStatement() instanceof Assignment);
		perform(conf);
	}

	@Test
	public void testIntegrityNI() {
		CronConfiguration conf = new CronConfiguration();
		conf.serializeResults = true;
		conf.analysis = new SimpleAbstractDomain<>(new MonolithicHeap(), new NonInterference(), new InferredTypes());
		conf.semanticChecks.add(new NICheck());
		conf.testDir = "non-interference/integrity";
		conf.programFile = "program.imp";
		conf.hotspots = st -> st instanceof Assignment
				|| (st instanceof Expression && ((Expression) st).getParentStatement() instanceof Assignment);
		perform(conf);
	}

	@Test
	public void testDeclassification() {
		CronConfiguration conf = new CronConfiguration();
		conf.serializeResults = true;
		conf.analysis = new SimpleAbstractDomain<>(new MonolithicHeap(), new NonInterference(), new InferredTypes());
		conf.callGraph = new RTACallGraph();
		conf.interproceduralAnalysis = new ContextBasedAnalysis<>(FullStackToken.getSingleton());
		conf.semanticChecks.add(new NICheck());
		conf.testDir = "non-interference/interproc";
		conf.programFile = "program.imp";
		conf.hotspots = st -> st instanceof Assignment
				|| (st instanceof Expression && ((Expression) st).getParentStatement() instanceof Assignment);
		perform(conf);
	}

	private static class NICheck
			implements
			SemanticCheck<SimpleAbstractState<Monolith, NonInterferenceEnvironment, TypeEnvironment<TypeSet>>,
					SimpleAbstractDomain<Monolith, NonInterferenceEnvironment, TypeEnvironment<TypeSet>>> {

		@Override
		public boolean visit(
				CheckToolWithAnalysisResults<
						SimpleAbstractState<Monolith, NonInterferenceEnvironment, TypeEnvironment<TypeSet>>,
						SimpleAbstractDomain<Monolith,
								NonInterferenceEnvironment,
								TypeEnvironment<TypeSet>>> tool,
				CFG graph,
				Statement node) {
			if (!(node instanceof Assignment))
				return true;

			Assignment assign = (Assignment) node;
			var results = tool.getResultOf(graph);

			for (var result : results)
				try {
					var post = result.getAnalysisStateAfter(assign);
					NonInterferenceEnvironment state = post.getState()
							.getLatticeInstance(NonInterferenceEnvironment.class);
					var postL = result.getAnalysisStateAfter(assign.getLeft());
					var postR = result.getAnalysisStateAfter(assign.getRight());
					NonInterference domain = (NonInterference) tool.getAnalysis().domain.valueDomain;
					SemanticOracle oracleL = tool.getAnalysis().domain.makeOracle(postL.getState());
					SemanticOracle oracleR = tool.getAnalysis().domain.makeOracle(postR.getState());

					for (SymbolicExpression l : tool
							.getAnalysis()
							.rewrite(postL, postL.getComputedExpressions(), assign))
						for (SymbolicExpression r : tool
								.getAnalysis()
								.rewrite(postR, postR.getComputedExpressions(), assign)) {
							NonInterferenceValue ll = domain
									.eval(postL.getState().valueState, (ValueExpression) l, assign.getLeft(), oracleL);
							NonInterferenceValue rr = domain
									.eval(
											postR.getState().valueState,
											(ValueExpression) r,
											assign.getRight(),
											oracleR);

							if (ll.isLowConfidentiality() && rr.isHighConfidentiality())
								tool
										.warnOn(
												assign,
												"This assignment assigns a HIGH confidentiality value to a LOW confidentiality variable, thus violating non-interference");

							if (ll.isLowConfidentiality() && state.getExecutionState().isHighConfidentiality())
								tool
										.warnOn(
												assign,
												"This assignment, located in a HIGH confidentiality block, assigns a LOW confidentiality variable, thus violating non-interference");

							if (ll.isHighIntegrity() && rr.isLowIntegrity())
								tool
										.warnOn(
												assign,
												"This assignment assigns a LOW integrity value to a HIGH integrity variable, thus violating non-interference");

							if (ll.isHighIntegrity() && state.getExecutionState().isLowIntegrity())
								tool
										.warnOn(
												assign,
												"This assignment, located in a LOW integrity block, assigns a HIGH integrity variable, thus violating non-interference");
						}
				} catch (SemanticException e) {
					throw new RuntimeException(e);
				}

			return true;
		}

	}

}

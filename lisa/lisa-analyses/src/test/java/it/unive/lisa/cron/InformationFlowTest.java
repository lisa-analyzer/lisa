package it.unive.lisa.cron;

import it.unive.lisa.AnalysisTestExecutor;
import it.unive.lisa.CronConfiguration;
import it.unive.lisa.DefaultConfiguration;
import it.unive.lisa.analysis.AnalysisState;
import it.unive.lisa.analysis.AnalyzedCFG;
import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.analysis.SimpleAbstractState;
import it.unive.lisa.analysis.heap.MonolithicHeap;
import it.unive.lisa.analysis.nonInterference.NonInterference;
import it.unive.lisa.analysis.nonInterference.NonInterference.NI;
import it.unive.lisa.analysis.nonrelational.value.TypeEnvironment;
import it.unive.lisa.analysis.nonrelational.value.ValueEnvironment;
import it.unive.lisa.analysis.taint.BaseTaint;
import it.unive.lisa.analysis.taint.Taint;
import it.unive.lisa.analysis.taint.ThreeLevelsTaint;
import it.unive.lisa.analysis.types.InferredTypes;
import it.unive.lisa.checks.semantic.CheckToolWithAnalysisResults;
import it.unive.lisa.checks.semantic.SemanticCheck;
import it.unive.lisa.interprocedural.ReturnTopPolicy;
import it.unive.lisa.interprocedural.callgraph.RTACallGraph;
import it.unive.lisa.interprocedural.context.ContextBasedAnalysis;
import it.unive.lisa.interprocedural.context.FullStackToken;
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
import java.util.Collection;
import org.junit.Test;

public class InformationFlowTest extends AnalysisTestExecutor {

	@Test
	public void testTaint() {
		CronConfiguration conf = new CronConfiguration();
		conf.abstractState = DefaultConfiguration.simpleState(
				DefaultConfiguration.defaultHeapDomain(),
				new ValueEnvironment<>(new Taint()),
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
		conf.abstractState = DefaultConfiguration.simpleState(
				DefaultConfiguration.defaultHeapDomain(),
				new ValueEnvironment<>(new ThreeLevelsTaint()),
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

	private static class TaintCheck<T extends BaseTaint<T>>
			implements
			SemanticCheck<
					SimpleAbstractState<MonolithicHeap, ValueEnvironment<T>, TypeEnvironment<InferredTypes>>> {

		@Override
		public boolean visit(
				CheckToolWithAnalysisResults<
						SimpleAbstractState<MonolithicHeap, ValueEnvironment<T>, TypeEnvironment<InferredTypes>>> tool,
				CFG graph,
				Statement node) {
			if (!(node instanceof UnresolvedCall))
				return true;

			UnresolvedCall call = (UnresolvedCall) node;

			try {
				for (AnalyzedCFG<
						SimpleAbstractState<
								MonolithicHeap,
								ValueEnvironment<T>,
								TypeEnvironment<InferredTypes>>> result : tool.getResultOf(call.getCFG())) {

					Call resolved = (Call) tool.getResolvedVersion(call, result);
					if (resolved instanceof CFGCall) {
						CFGCall cfg = (CFGCall) resolved;
						for (CodeMember n : cfg.getTargets()) {
							Parameter[] parameters = n.getDescriptor().getFormals();
							for (int i = 0; i < parameters.length; i++)
								if (parameters[i].getAnnotations().contains(BaseTaint.CLEAN_MATCHER)) {
									AnalysisState<SimpleAbstractState<MonolithicHeap, ValueEnvironment<T>,
											TypeEnvironment<InferredTypes>>> post = result
													.getAnalysisStateAfter(call.getParameters()[i]);
									for (SymbolicExpression e : post.getState().rewrite(post.getComputedExpressions(),
											node, post.getState())) {
										T stack = post
												.getState()
												.getValueState()
												.eval((ValueExpression) e, node, post.getState());
										if (stack.isAlwaysTainted())
											tool.warnOn(call, "Parameter " + i + " is always tainted");
										else if (stack.isPossiblyTainted())
											tool.warnOn(call, "Parameter " + i + " is possibly tainted");
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
		conf.abstractState = new SimpleAbstractState<>(
				new MonolithicHeap(),
				new NonInterference(),
				new TypeEnvironment<>(new InferredTypes()));
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
		conf.abstractState = new SimpleAbstractState<>(
				new MonolithicHeap(),
				new NonInterference(),
				new TypeEnvironment<>(new InferredTypes()));
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
		conf.abstractState = new SimpleAbstractState<>(
				new MonolithicHeap(),
				new NonInterference(),
				new TypeEnvironment<>(new InferredTypes()));
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
			SemanticCheck<
					SimpleAbstractState<MonolithicHeap, NonInterference, TypeEnvironment<InferredTypes>>> {

		@Override
		public boolean visit(
				CheckToolWithAnalysisResults<
						SimpleAbstractState<MonolithicHeap, NonInterference, TypeEnvironment<InferredTypes>>> tool,
				CFG graph,
				Statement node) {
			if (!(node instanceof Assignment))
				return true;

			Assignment assign = (Assignment) node;
			Collection<?> results = tool.getResultOf(graph);

			for (Object res : results)
				try {
					AnalyzedCFG<?> result = (AnalyzedCFG<?>) res;
					AnalysisState<?> post = result.getAnalysisStateAfter(assign);
					NonInterference state = post.getState().getDomainInstance(NonInterference.class);
					AnalysisState<?> postL = result.getAnalysisStateAfter(assign.getLeft());
					NonInterference left = postL.getState().getDomainInstance(NonInterference.class);
					AnalysisState<?> postR = result.getAnalysisStateAfter(assign.getRight());
					NonInterference right = postR.getState().getDomainInstance(NonInterference.class);

					for (SymbolicExpression l : postL.getState().rewrite(postL.getComputedExpressions(), assign,
							postL.getState()))
						for (SymbolicExpression r : postR.getState().rewrite(postR.getComputedExpressions(), assign,
								postR.getState())) {
							NI ll = left.eval((ValueExpression) l, assign.getLeft(), postL.getState());
							NI rr = right.eval((ValueExpression) r, assign.getRight(), postR.getState());

							if (ll.isLowConfidentiality() && rr.isHighConfidentiality())
								tool.warnOn(assign,
										"This assignment assigns a HIGH confidentiality value to a LOW confidentiality variable, thus violating non-interference");

							if (ll.isLowConfidentiality() && state.getExecutionState().isHighConfidentiality())
								tool.warnOn(assign,
										"This assignment, located in a HIGH confidentiality block, assigns a LOW confidentiality variable, thus violating non-interference");

							if (ll.isHighIntegrity() && rr.isLowIntegrity())
								tool.warnOn(assign,
										"This assignment assigns a LOW integrity value to a HIGH integrity variable, thus violating non-interference");

							if (ll.isHighIntegrity() && state.getExecutionState().isLowIntegrity())
								tool.warnOn(assign,
										"This assignment, located in a LOW integrity block, assigns a HIGH integrity variable, thus violating non-interference");
						}
				} catch (SemanticException e) {
					throw new RuntimeException(e);
				}

			return true;
		}
	}
}

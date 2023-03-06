package it.unive.lisa.cron.taint;

import it.unive.lisa.AnalysisSetupException;
import it.unive.lisa.AnalysisTestExecutor;
import it.unive.lisa.LiSAFactory;
import it.unive.lisa.analysis.AbstractState;
import it.unive.lisa.analysis.AnalyzedCFG;
import it.unive.lisa.analysis.SimpleAbstractState;
import it.unive.lisa.analysis.heap.HeapDomain;
import it.unive.lisa.analysis.heap.MonolithicHeap;
import it.unive.lisa.analysis.nonrelational.value.TypeEnvironment;
import it.unive.lisa.analysis.nonrelational.value.ValueEnvironment;
import it.unive.lisa.analysis.taint.BaseTaint;
import it.unive.lisa.analysis.taint.Taint;
import it.unive.lisa.analysis.taint.ThreeLevelsTaint;
import it.unive.lisa.analysis.types.InferredTypes;
import it.unive.lisa.analysis.value.TypeDomain;
import it.unive.lisa.checks.semantic.CheckToolWithAnalysisResults;
import it.unive.lisa.checks.semantic.SemanticCheck;
import it.unive.lisa.conf.LiSAConfiguration;
import it.unive.lisa.interprocedural.ContextBasedAnalysis;
import it.unive.lisa.interprocedural.ReturnTopPolicy;
import it.unive.lisa.interprocedural.callgraph.RTACallGraph;
import it.unive.lisa.program.cfg.CFG;
import it.unive.lisa.program.cfg.CodeMember;
import it.unive.lisa.program.cfg.Parameter;
import it.unive.lisa.program.cfg.statement.Statement;
import it.unive.lisa.program.cfg.statement.call.CFGCall;
import it.unive.lisa.program.cfg.statement.call.Call;
import it.unive.lisa.program.cfg.statement.call.UnresolvedCall;
import org.junit.Test;

public class TaintAnalysesTest extends AnalysisTestExecutor {

	@Test
	public void testTaint() throws AnalysisSetupException {
		LiSAConfiguration conf = new LiSAConfiguration();
		conf.abstractState = LiSAFactory.getDefaultFor(AbstractState.class,
				LiSAFactory.getDefaultFor(HeapDomain.class),
				new ValueEnvironment<>(new Taint()),
				LiSAFactory.getDefaultFor(TypeDomain.class));
		conf.jsonOutput = true;
		conf.openCallPolicy = ReturnTopPolicy.INSTANCE;
		conf.callGraph = new RTACallGraph();
		conf.interproceduralAnalysis = new ContextBasedAnalysis<>();
		conf.semanticChecks.add(new TaintCheck<>());
		perform("taint", "2val", "taint.imp", conf);
	}

	@Test
	public void testThreeLevelsTaint() throws AnalysisSetupException {
		LiSAConfiguration conf = new LiSAConfiguration();
		conf.abstractState = LiSAFactory.getDefaultFor(AbstractState.class,
				LiSAFactory.getDefaultFor(HeapDomain.class),
				new ValueEnvironment<>(new ThreeLevelsTaint()),
				LiSAFactory.getDefaultFor(TypeDomain.class));
		conf.jsonOutput = true;
		conf.openCallPolicy = ReturnTopPolicy.INSTANCE;
		conf.callGraph = new RTACallGraph();
		conf.interproceduralAnalysis = new ContextBasedAnalysis<>();
		conf.semanticChecks.add(new TaintCheck<>());
		perform("taint", "3val", "taint.imp", conf);
	}

	private static class TaintCheck<T extends BaseTaint<T>> implements
			SemanticCheck<SimpleAbstractState<MonolithicHeap, ValueEnvironment<T>, TypeEnvironment<InferredTypes>>,
					MonolithicHeap, ValueEnvironment<T>, TypeEnvironment<InferredTypes>> {

		@Override
		public boolean visit(
				CheckToolWithAnalysisResults<
						SimpleAbstractState<MonolithicHeap, ValueEnvironment<T>, TypeEnvironment<InferredTypes>>,
						MonolithicHeap, ValueEnvironment<T>, TypeEnvironment<InferredTypes>> tool,
				CFG graph, Statement node) {
			if (!(node instanceof UnresolvedCall))
				return true;

			UnresolvedCall call = (UnresolvedCall) node;
			Call resolved = (Call) tool.getResolvedVersion(call);

			if (resolved instanceof CFGCall) {
				CFGCall cfg = (CFGCall) resolved;
				for (CodeMember n : cfg.getTargets()) {
					Parameter[] parameters = n.getDescriptor().getFormals();
					for (int i = 0; i < parameters.length; i++)
						if (parameters[i].getAnnotations().contains(BaseTaint.CLEAN_MATCHER))
							for (AnalyzedCFG<
									SimpleAbstractState<
											MonolithicHeap,
											ValueEnvironment<T>,
											TypeEnvironment<InferredTypes>>,
									MonolithicHeap,
									ValueEnvironment<T>,
									TypeEnvironment<InferredTypes>> result : tool.getResultOf(call.getCFG())) {
								T stack = result.getAnalysisStateAfter(call.getParameters()[i])
										.getState()
										.getValueState()
										.getValueOnStack();
								if (stack.isAlwaysTainted())
									tool.warnOn(call, "Parameter " + i + " is always tainted");
								else if (stack.isPossiblyTainted())
									tool.warnOn(call, "Parameter " + i + " is possibly tainted");
							}
				}
			}

			return true;
		}
	}
}

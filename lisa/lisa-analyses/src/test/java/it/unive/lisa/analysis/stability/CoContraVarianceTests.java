package it.unive.lisa.analysis.stability;

import it.unive.lisa.AnalysisTestExecutor;
import it.unive.lisa.DefaultConfiguration;
import it.unive.lisa.LiSA;
import it.unive.lisa.analysis.*;
import it.unive.lisa.analysis.heap.MonolithicHeap;
import it.unive.lisa.analysis.nonrelational.value.TypeEnvironment;
import it.unive.lisa.analysis.nonrelational.value.ValueEnvironment;
import it.unive.lisa.analysis.numeric.Interval;
import it.unive.lisa.analysis.types.InferredTypes;
import it.unive.lisa.analysis.value.ValueDomain;
import it.unive.lisa.checks.semantic.CheckToolWithAnalysisResults;
import it.unive.lisa.checks.semantic.SemanticCheck;
import it.unive.lisa.conf.LiSAConfiguration;
import it.unive.lisa.imp.IMPFrontend;
import it.unive.lisa.imp.ParsingException;
import it.unive.lisa.interprocedural.context.ContextBasedAnalysis;
import it.unive.lisa.program.Program;
import it.unive.lisa.program.cfg.CFG;
import it.unive.lisa.program.cfg.statement.Assignment;
import it.unive.lisa.program.cfg.statement.Statement;
import it.unive.lisa.symbolic.value.Identifier;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

public class CoContraVarianceTests extends AnalysisTestExecutor {

    @Test
    public void testThreeLevelsTaint() throws ParsingException {

        Program program = IMPFrontend.processFile("imp-testcases/stability/assign_mult.imp");
        LiSAConfiguration conf = new DefaultConfiguration();
        conf.workdir = "output/stability/assign_mult";
        conf.serializeResults = true;
        //conf.analysisGraphs = LiSAConfiguration.GraphType.HTML;
        conf.abstractState = new SimpleAbstractState<>(
                // heap domain
                new MonolithicHeap(),
                // value domain
                //new Stability<>(new DoubleCheeseburger()),
                new Stability<>(new ValueEnvironment<>(new Interval()).top()),
                //new Stability<>(new ValueEnvironment<>(new Sign()).top()),
                // type domain
                new TypeEnvironment<>(new InferredTypes()));

        //??
        conf.interproceduralAnalysis = new ContextBasedAnalysis<>();
        conf.semanticChecks.add(new CoContraVarianceCheck<Stability<ValueEnvironment<Interval>>>());

        LiSA lisa = new LiSA(conf);
        lisa.run(program);
    }

    private static class CoContraVarianceCheck<T extends BaseLattice<T> & ValueDomain<T>>
            implements SemanticCheck<SimpleAbstractState<MonolithicHeap, Stability<T>, TypeEnvironment<InferredTypes>>> {

        Map<Statement, Stability<T>> stateBeforeMap = new HashMap<Statement, Stability<T>>();

        @Override
        public boolean visit(CheckToolWithAnalysisResults<SimpleAbstractState<MonolithicHeap, Stability<T>, TypeEnvironment<InferredTypes>>> tool,
                             CFG graph,
                             Statement node) {

            //if (node instanceof Assignment ) { }
            for (AnalyzedCFG<SimpleAbstractState<MonolithicHeap, Stability<T>,TypeEnvironment<InferredTypes>>>
                    result : tool.getResultOf(graph)){

                Stability<T> postState = result.getAnalysisStateAfter(node).getState().getValueState();

                Statement next = node.getEvaluationSuccessor(); //q: in the case of branches??
                if (stateBeforeMap.containsKey(next))
                    stateBeforeMap.put(next, stateBeforeMap.get(next).combine(postState));
                else
                    stateBeforeMap.put(next, postState);
            }

            return true;
        }

        public Map<Statement, Stability<T>> getStateBeforeMap() {
            return stateBeforeMap;
        }

        @Override
        public void afterExecution(CheckToolWithAnalysisResults<SimpleAbstractState<MonolithicHeap, Stability<T>, TypeEnvironment<InferredTypes>>> tool) {
            System.out.println(stateBeforeMap.toString());
            SemanticCheck.super.afterExecution(tool);
        }
    }

}

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
import it.unive.lisa.program.cfg.statement.Statement;
import it.unive.lisa.symbolic.value.Identifier;
import org.junit.Test;

import java.util.*;
import java.util.stream.Stream;

public class CoContraVarianceTests extends AnalysisTestExecutor {

    @Test
    public void testThreeLevelsTaint() throws ParsingException {

        Program program = IMPFrontend.processFile("imp-testcases/stability/saneScale.imp");
        LiSAConfiguration conf = new DefaultConfiguration();
        conf.workdir = "output/stability/DCS";
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

        Map<Statement, Stability<T>> stateBeforeMap = new HashMap<>();
        Map<Statement, Stability<T>> resultsMap = new HashMap<>();

        List<Statement> nodes = new ArrayList<>();

        @Override
        public boolean visit(CheckToolWithAnalysisResults<SimpleAbstractState<MonolithicHeap, Stability<T>, TypeEnvironment<InferredTypes>>> tool,
                             CFG graph,
                             Statement node) {
            //System.out.println(node);

            if (graph.containsNode(node)) {
                for (AnalyzedCFG<SimpleAbstractState<MonolithicHeap, Stability<T>, TypeEnvironment<InferredTypes>>>
                        result : tool.getResultOf(graph)) {

                    Stability<T> postState = result.getAnalysisStateAfter(node).getState().getValueState();
                    Stability<T> cumulativeState = postState;

                    // computing cumulative trend for node
                    if (stateBeforeMap.containsKey(node)) {
                        Stability<T> preState = stateBeforeMap.get(node);
                        cumulativeState = preState.combine(postState);
                    }

                    //System.out.println("$ " + node + ": " + cumulativeState);
                    resultsMap.put(node, cumulativeState);


                    // computing new entry state for next node
                    for (Statement next : graph.followersOf(node)) {

                        if (stateBeforeMap.containsKey(next)) {
                            try {
                                stateBeforeMap.put(next, stateBeforeMap.get(next).lub(cumulativeState)); // join converging branches
                            } catch (SemanticException e) {
                                e.printStackTrace();
                            }
                        } else {
                            //System.out.println("$ prestate(" + next + "): " + cumulativeState); // NO NODES IN statesBeforeMap!!!
                            stateBeforeMap.put(next, cumulativeState); // the pre of next is the post of this
                        }
                    }

                    /*else {
                        Statement next = graph.followersOf() ;
                        Stability<T> preState = stateBeforeMap.get(node);
                        resultsMap.put(node, preState); // propagate forward
                        stateBeforeMap.put(next, preState);
                    }*/
                }
            }
            return true;

        }



        // Cute sout of results
        @Override
        public void afterExecution(CheckToolWithAnalysisResults<SimpleAbstractState<MonolithicHeap, Stability<T>, TypeEnvironment<InferredTypes>>> tool) {

            // sort results -> make LinkedMap and avoid??
            Stream<Map.Entry<Statement, Stability<T>>> results =
            resultsMap.entrySet().stream().sorted(new Comparator<Map.Entry<Statement, Stability<T>>>() {
                @Override
                public int compare(Map.Entry<Statement, Stability<T>> o1, Map.Entry<Statement, Stability<T>> o2) {
                    return o1.getKey().compareTo(o2.getKey());
                }
            });

            for (Object objectEntry: results.toArray() ) {  //resultsMap.entrySet()) { // in case we go back to sanity ??
                Map.Entry<Statement, Stability<T>> entry = (Map.Entry<Statement, Stability<T>>) objectEntry; // crimes !!

                // get covClasses (and also switch Trend for String representing that trend for printing)
                HashMap<String, ArrayList<Identifier>> covClasses = new HashMap<>();
                for (Map.Entry<Trend, ArrayList<Identifier>> el : entry.getValue().getCovarianceClasses().entrySet()) {
                    covClasses.put(el.getKey().representation().toString(), el.getValue());
                }

                // get line number for Statement (absolutely not necessary, just for cute prints uwu)
                String locationString = entry.getKey().getLocation().getCodeLocation();
                locationString = locationString.substring(locationString.indexOf(':') + 1, locationString.lastIndexOf(':'));

                // print
                System.out.println("[" + locationString + "] " + entry.getKey() + ": " + covClasses);
            }

            SemanticCheck.super.afterExecution(tool);
        }
    }

}

package it.unive.lisa.cron.visualization;

import static it.unive.lisa.LiSAFactory.getDefaultFor;
import static org.junit.Assert.assertTrue;

import it.unive.lisa.AnalysisSetupException;
import it.unive.lisa.AnalysisTestExecutor;
import it.unive.lisa.LiSAConfiguration;
import it.unive.lisa.LiSAConfiguration.GraphType;
import it.unive.lisa.analysis.AbstractState;
import it.unive.lisa.analysis.heap.HeapDomain;
import it.unive.lisa.analysis.value.TypeDomain;
import it.unive.lisa.analysis.value.ValueDomain;
import it.unive.lisa.interprocedural.ContextBasedAnalysis;
import it.unive.lisa.interprocedural.RecursionFreeToken;
import it.unive.lisa.interprocedural.callgraph.RTACallGraph;
import java.util.Collection;
import java.util.HashSet;
import org.junit.AfterClass;
import org.junit.Test;

public class VisualizationTest extends AnalysisTestExecutor {

	private static LiSAConfiguration config() throws AnalysisSetupException {
		LiSAConfiguration conf = new LiSAConfiguration();
		conf.abstractState = getDefaultFor(AbstractState.class,
				getDefaultFor(HeapDomain.class),
				getDefaultFor(ValueDomain.class),
				getDefaultFor(TypeDomain.class));
		conf.interproceduralAnalysis = new ContextBasedAnalysis<>(RecursionFreeToken.getSingleton());
		conf.callGraph = new RTACallGraph();
		return conf;
	}

	@AfterClass
	public static void ensureAllTested() {
		Collection<GraphType> notTested = new HashSet<>();
		for (GraphType cls : GraphType.values())
			if (cls != GraphType.NONE)
				try {
					VisualizationTest.class.getMethod("test" + cls.name());
				} catch (NoSuchMethodException | SecurityException e) {
					notTested.add(cls);
				}

		if (!notTested.isEmpty())
			System.err.println("The following visualization types have not been tested: " + notTested);

		assertTrue("Not all visualization types have been tested", notTested.isEmpty());
	}

	@Test
	public void testInputSerialization() throws AnalysisSetupException {
		LiSAConfiguration conf = config();
		conf.serializeInputs = true;
		perform("visualization", "inputs", "program.imp", conf);
	}

	@Test
	public void testDOT() throws AnalysisSetupException {
		LiSAConfiguration conf = config();
		conf.analysisGraphs = GraphType.DOT;
		perform("visualization", "dot", "program.imp", conf);
	}

	@Test
	public void testGRAPHML() throws AnalysisSetupException {
		LiSAConfiguration conf = config();
		conf.analysisGraphs = GraphType.GRAPHML;
		perform("visualization", "graphml", "program.imp", conf);
	}

	@Test
	public void testGRAPHML_WITH_SUBNODES() throws AnalysisSetupException {
		LiSAConfiguration conf = config();
		conf.analysisGraphs = GraphType.GRAPHML_WITH_SUBNODES;
		perform("visualization", "graphml-sub", "program.imp", conf);
	}

	@Test
	public void testHTML() throws AnalysisSetupException {
		LiSAConfiguration conf = config();
		conf.analysisGraphs = GraphType.HTML;
		perform("visualization", "html", "program.imp", conf);
	}

	@Test
	public void testHTML_WITH_SUBNODES() throws AnalysisSetupException {
		LiSAConfiguration conf = config();
		conf.analysisGraphs = GraphType.HTML_WITH_SUBNODES;
		perform("visualization", "html-sub", "program.imp", conf);
	}
}

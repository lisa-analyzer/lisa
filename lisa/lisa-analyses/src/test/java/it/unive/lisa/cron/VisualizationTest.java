package it.unive.lisa.cron;

import static org.junit.Assert.assertTrue;

import java.util.Collection;
import java.util.HashSet;

import org.junit.AfterClass;
import org.junit.Test;

import it.unive.lisa.DefaultConfiguration;
import it.unive.lisa.conf.LiSAConfiguration.GraphType;
import it.unive.lisa.interprocedural.callgraph.RTACallGraph;
import it.unive.lisa.interprocedural.context.ContextBasedAnalysis;
import it.unive.lisa.interprocedural.context.FullStackToken;
import it.unive.lisa.util.testing.TestConfiguration;

public class VisualizationTest
		extends
		IMPCronExecutor {

	private static TestConfiguration config() {
		CronConfiguration conf = new CronConfiguration();
		conf.analysis = DefaultConfiguration.defaultAbstractDomain();
		conf.interproceduralAnalysis = new ContextBasedAnalysis<>(FullStackToken.getSingleton());
		conf.callGraph = new RTACallGraph();
		return conf;
	}

	@AfterClass
	public static void ensureAllTested() {
		Collection<GraphType> notTested = new HashSet<>();
		for (GraphType cls : GraphType.values())
			if (cls != GraphType.NONE)
				try {
					VisualizationTest.class
							.getMethod(
									"test" + cls.name());
				} catch (NoSuchMethodException | SecurityException e) {
					notTested.add(cls);
				}

		if (!notTested.isEmpty())
			System.err
					.println(
							"The following visualization types have not been tested: " + notTested);

		assertTrue("Not all visualization types have been tested", notTested.isEmpty());
	}

	@Test
	public void testInputSerialization() {
		TestConfiguration conf = config();
		conf.serializeInputs = true;
		conf.testDir = "visualization";
		conf.testSubDir = "inputs";
		conf.programFile = "visualization.imp";
		perform(conf);
	}

	@Test
	public void testDOT() {
		TestConfiguration conf = config();
		conf.analysisGraphs = GraphType.DOT;
		conf.testDir = "visualization";
		conf.testSubDir = "dot";
		conf.programFile = "visualization.imp";
		perform(conf);
	}

	@Test
	public void testHTML() {
		TestConfiguration conf = config();
		conf.analysisGraphs = GraphType.HTML;
		conf.testDir = "visualization";
		conf.testSubDir = "html";
		conf.programFile = "visualization.imp";
		perform(conf);
	}

	@Test
	public void testHTML_WITH_SUBNODES() {
		TestConfiguration conf = config();
		conf.analysisGraphs = GraphType.HTML_WITH_SUBNODES;
		conf.testDir = "visualization";
		conf.testSubDir = "html-sub";
		conf.programFile = "visualization.imp";
		perform(conf);
	}

	@Test
	public void testHTMLInputs() {
		CronConfiguration conf = new CronConfiguration();
		conf.serializeInputs = true;
		conf.analysisGraphs = GraphType.HTML;
		conf.testDir = "visualization";
		conf.testSubDir = "html-inputs";
		conf.programFile = "visualization.imp";
		perform(conf);
	}

}

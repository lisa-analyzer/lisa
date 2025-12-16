package it.unive.lisa.cron;

import it.unive.lisa.DefaultConfiguration;
import it.unive.lisa.interprocedural.callgraph.RTACallGraph;
import it.unive.lisa.interprocedural.context.ContextBasedAnalysis;
import it.unive.lisa.outputs.DotResults;
import it.unive.lisa.outputs.HtmlInputs;
import it.unive.lisa.outputs.HtmlResults;
import it.unive.lisa.outputs.JSONInputs;
import it.unive.lisa.util.testing.TestConfiguration;
import org.junit.Test;

public class VisualizationTest
		extends
		IMPCronExecutor {

	private static TestConfiguration config() {
		CronConfiguration conf = new CronConfiguration();
		conf.analysis = DefaultConfiguration.defaultAbstractDomain();
		conf.interproceduralAnalysis = new ContextBasedAnalysis<>(-1);
		conf.callGraph = new RTACallGraph();
		return conf;
	}

	@Test
	public void testInputSerialization() {
		TestConfiguration conf = config();
		conf.outputs.add(new JSONInputs());
		conf.testDir = "visualization";
		conf.testSubDir = "inputs";
		conf.programFile = "visualization.imp";
		perform(conf);
	}

	@Test
	public void testDOT() {
		TestConfiguration conf = config();
		conf.outputs.add(new DotResults<>());
		conf.testDir = "visualization";
		conf.testSubDir = "dot";
		conf.programFile = "visualization.imp";
		perform(conf);
	}

	@Test
	public void testHTML() {
		TestConfiguration conf = config();
		conf.outputs.add(new HtmlResults<>(false));
		conf.testDir = "visualization";
		conf.testSubDir = "html";
		conf.programFile = "visualization.imp";
		perform(conf);
	}

	@Test
	public void testHTML_WITH_SUBNODES() {
		TestConfiguration conf = config();
		conf.outputs.add(new HtmlResults<>(true));
		conf.testDir = "visualization";
		conf.testSubDir = "html-sub";
		conf.programFile = "visualization.imp";
		perform(conf);
	}

	@Test
	public void testHTMLInputs() {
		CronConfiguration conf = new CronConfiguration();
		conf.outputs.add(new JSONInputs());
		conf.outputs.add(new HtmlInputs(false));
		conf.testDir = "visualization";
		conf.testSubDir = "html-inputs";
		conf.programFile = "visualization.imp";
		perform(conf);
	}

}

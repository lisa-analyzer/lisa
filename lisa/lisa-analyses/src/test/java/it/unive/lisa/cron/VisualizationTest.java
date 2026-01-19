package it.unive.lisa.cron;

import it.unive.lisa.DefaultConfiguration;
import it.unive.lisa.interprocedural.callgraph.RTACallGraph;
import it.unive.lisa.interprocedural.context.ContextBasedAnalysis;
import it.unive.lisa.listeners.TracingListener;
import it.unive.lisa.listeners.TracingListener.TraceLevel;
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
		conf.forceUpdate=true;
		perform(conf);
	}

	@Test
	public void testHTML_WITH_SUBNODES() {
		TestConfiguration conf = config();
		conf.outputs.add(new HtmlResults<>(true));
		conf.testDir = "visualization";
		conf.testSubDir = "html-sub";
		conf.programFile = "visualization.imp";
		conf.forceUpdate=true;
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
		conf.forceUpdate=true;
		perform(conf);
	}

	@Test
	public void testInterproceduralTrace() {
		TestConfiguration conf = config();
		conf.asynchronousListeners.add(new TracingListener(TraceLevel.INTERPROCEDURAL));
		conf.testDir = "visualization";
		conf.testSubDir = "interproc-trace";
		conf.programFile = "visualization.imp";
		perform(conf);
	}

	@Test
	public void testFixpointTrace() {
		TestConfiguration conf = config();
		conf.asynchronousListeners.add(new TracingListener(TraceLevel.FIXPOINT));
		conf.testDir = "visualization";
		conf.testSubDir = "fix-trace";
		conf.programFile = "visualization.imp";
		perform(conf);
	}

	@Test
	public void testAnalysisTrace() {
		TestConfiguration conf = config();
		conf.asynchronousListeners.add(new TracingListener(TraceLevel.ANALYSIS));
		conf.testDir = "visualization";
		conf.testSubDir = "analysis-trace";
		conf.programFile = "visualization.imp";
		perform(conf);
	}

	@Test
	public void testDomainTrace() {
		TestConfiguration conf = config();
		conf.asynchronousListeners.add(new TracingListener(TraceLevel.DOMAIN));
		conf.testDir = "visualization";
		conf.testSubDir = "domain-trace";
		conf.programFile = "visualization.imp";
		perform(conf);
	}

	@Test
	public void testAllTrace() {
		TestConfiguration conf = config();
		conf.asynchronousListeners.add(new TracingListener(TraceLevel.ALL));
		conf.testDir = "visualization";
		conf.testSubDir = "all-trace";
		conf.programFile = "visualization.imp";
		perform(conf);
	}
}

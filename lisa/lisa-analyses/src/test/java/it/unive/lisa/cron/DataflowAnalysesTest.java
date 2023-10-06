package it.unive.lisa.cron;

import it.unive.lisa.AnalysisTestExecutor;
import it.unive.lisa.CronConfiguration;
import it.unive.lisa.DefaultConfiguration;
import it.unive.lisa.analysis.dataflow.AvailableExpressions;
import it.unive.lisa.analysis.dataflow.ConstantPropagation;
import it.unive.lisa.analysis.dataflow.DefiniteDataflowDomain;
import it.unive.lisa.analysis.dataflow.PossibleDataflowDomain;
import it.unive.lisa.analysis.dataflow.ReachingDefinitions;
import it.unive.lisa.conf.LiSAConfiguration.GraphType;
import it.unive.lisa.interprocedural.BackwardModularWorstCaseAnalysis;

import org.junit.Ignore;
import org.junit.Test;

public class DataflowAnalysesTest extends AnalysisTestExecutor {

	@Test
	public void testAvailableExpressions() {
		CronConfiguration conf = new CronConfiguration();
		conf.serializeResults = true;
		conf.abstractState = DefaultConfiguration.simpleState(
				DefaultConfiguration.defaultHeapDomain(),
				new DefiniteDataflowDomain<>(new AvailableExpressions()),
				DefaultConfiguration.defaultTypeDomain());
		conf.testDir = "available-expressions";
		conf.programFile = "available-expressions.imp";
		perform(conf);
	}

	@Test
	public void testConstantPropagation() {
		CronConfiguration conf = new CronConfiguration();
		conf.serializeResults = true;
		conf.abstractState = DefaultConfiguration.simpleState(
				DefaultConfiguration.defaultHeapDomain(),
				new DefiniteDataflowDomain<>(new ConstantPropagation()),
				DefaultConfiguration.defaultTypeDomain());
		conf.testDir = "constant-propagation-df";
		conf.programFile = "constant-propagation.imp";
		perform(conf);
	}

	@Test
	public void testReachingDefinitions() {
		CronConfiguration conf = new CronConfiguration();
		conf.serializeResults = true;
		conf.abstractState = DefaultConfiguration.simpleState(
				DefaultConfiguration.defaultHeapDomain(),
				new PossibleDataflowDomain<>(new ReachingDefinitions()),
				DefaultConfiguration.defaultTypeDomain());
		conf.testDir = "reaching-definitions";
		conf.programFile = "reaching-definitions.imp";
		perform(conf);
	}

	@Test
	@Ignore
	public void testLiveness() {
		CronConfiguration conf = new CronConfiguration();
		conf.serializeResults = true;
		conf.interproceduralAnalysis = new BackwardModularWorstCaseAnalysis<>();
		conf.abstractState = DefaultConfiguration.simpleState(
				DefaultConfiguration.defaultHeapDomain(),
				new DefiniteDataflowDomain<>(new AvailableExpressions()),
				DefaultConfiguration.defaultTypeDomain());
		conf.testDir = "liveness";
		conf.programFile = "liveness.imp";
		conf.analysisGraphs = GraphType.HTML_WITH_SUBNODES;
		conf.forceUpdate = true;
		perform(conf);
	}
}
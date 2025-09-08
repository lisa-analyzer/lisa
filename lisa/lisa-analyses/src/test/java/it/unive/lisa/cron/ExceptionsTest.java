package it.unive.lisa.cron;

import it.unive.lisa.DefaultConfiguration;
import it.unive.lisa.interprocedural.context.ContextBasedAnalysis;
import it.unive.lisa.interprocedural.context.FullStackToken;
import org.junit.Test;

public class ExceptionsTest
		extends
		IMPCronExecutor {

	@Test
	public void testBasic() {
		CronConfiguration conf = new CronConfiguration();
		conf.serializeResults = true;
		conf.analysis = DefaultConfiguration.defaultAbstractDomain();
		conf.interproceduralAnalysis = new ContextBasedAnalysis<>(FullStackToken.getSingleton());
		conf.allMethods = true;
		conf.testDir = "exceptions";
		conf.testSubDir = "basic";
		conf.programFile = "basic.imp";
		perform(conf);
	}

	@Test
	public void testAdvanced() {
		CronConfiguration conf = new CronConfiguration();
		conf.serializeResults = true;
		// we need higher widening since the finally in
		// tryCatchElseFinallyWithComplexControlFlow
		// has lots of predecessors and end up widening
		// variable z even if it is not really increasing.
		// this causes a difference w.r.t. optimized
		// analysis since the widening does not happen
		// there
		conf.wideningThreshold = 10;
		conf.analysis = DefaultConfiguration.defaultAbstractDomain();
		conf.interproceduralAnalysis = new ContextBasedAnalysis<>(FullStackToken.getSingleton());
		conf.allMethods = true;
		conf.testDir = "exceptions";
		conf.testSubDir = "advanced";
		conf.programFile = "advanced.imp";
		perform(conf);
	}

	@Test
	public void testCalls() {
		CronConfiguration conf = new CronConfiguration();
		conf.serializeResults = true;
		conf.analysis = DefaultConfiguration.defaultAbstractDomain();
		conf.interproceduralAnalysis = new ContextBasedAnalysis<>(FullStackToken.getSingleton());
		conf.testDir = "exceptions";
		conf.testSubDir = "calls";
		conf.programFile = "calls.imp";
		perform(conf);
	}

	@Test
	public void testSmashed() {
		CronConfiguration conf = new CronConfiguration();
		conf.serializeResults = true;
		conf.analysis = DefaultConfiguration.defaultAbstractDomain();
		conf.interproceduralAnalysis = new ContextBasedAnalysis<>(FullStackToken.getSingleton());
		conf.shouldSmashException = t -> t.toString().equals("D") || t.toString().equals("E");
		conf.testDir = "exceptions";
		conf.testSubDir = "smashed";
		conf.programFile = "smashed.imp";
		// conf.forceUpdate = true;
		conf.analysisGraphs = CronConfiguration.GraphType.HTML_WITH_SUBNODES;
		perform(conf);
	}

}

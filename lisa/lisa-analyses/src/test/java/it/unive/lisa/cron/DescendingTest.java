package it.unive.lisa.cron;

import it.unive.lisa.DefaultConfiguration;
import it.unive.lisa.outputs.JSONResults;
import it.unive.lisa.program.cfg.fixpoints.forward.ForwardDescendingGLBFixpoint;
import it.unive.lisa.program.cfg.fixpoints.forward.ForwardDescendingNarrowingFixpoint;
import org.junit.Test;

public class DescendingTest
		extends
		IMPCronExecutor {

	@Test
	public void testIntervalDescendingWidening() {
		CronConfiguration conf = new CronConfiguration();
		conf.outputs.add(new JSONResults<>());
		conf.analysis = DefaultConfiguration.simpleDomain(
				DefaultConfiguration.defaultHeapDomain(),
				DefaultConfiguration.defaultValueDomain(),
				DefaultConfiguration.defaultTypeDomain());
		conf.forwardDescendingFixpoint = new ForwardDescendingNarrowingFixpoint<>();
		conf.testDir = "descending";
		conf.testSubDir = "widening";
		conf.programFile = "descending.imp";
		perform(conf);
	}

	@Test
	public void testIntervalDescendingMaxGlb() {
		CronConfiguration conf = new CronConfiguration();
		conf.outputs.add(new JSONResults<>());
		conf.analysis = DefaultConfiguration.simpleDomain(
				DefaultConfiguration.defaultHeapDomain(),
				DefaultConfiguration.defaultValueDomain(),
				DefaultConfiguration.defaultTypeDomain());
		conf.forwardDescendingFixpoint = new ForwardDescendingGLBFixpoint<>();
		conf.glbThreshold = 5;
		conf.testDir = "descending";
		conf.testSubDir = "maxglb";
		conf.programFile = "descending.imp";
		perform(conf);
	}

}

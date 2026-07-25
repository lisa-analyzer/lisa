package it.unive.lisa.cron;

import it.unive.lisa.DefaultConfiguration;
import it.unive.lisa.analysis.numeric.Interval;
import it.unive.lisa.outputs.JSONResults;
import it.unive.lisa.program.cfg.transform.LoopUnrolling;
import org.junit.jupiter.api.Test;

public class LoopUnrollingTest
		extends
		IMPCronExecutor {

	@Test
	public void testBaseline() {
		CronConfiguration conf = new CronConfiguration();
		conf.outputs.add(new JSONResults<>());
		conf.analysis = DefaultConfiguration.simpleDomain(
				DefaultConfiguration.defaultHeapDomain(),
				new Interval(),
				DefaultConfiguration.defaultTypeDomain());
		conf.testDir = "loop-unrolling";
		conf.testSubDir = "baseline";
		conf.programFile = "loop-unrolling.imp";
		conf.compareWithOptimization = false;
		perform(conf);
	}

	@Test
	public void testUnrollFactor2() {
		CronConfiguration conf = new CronConfiguration();
		conf.outputs.add(new JSONResults<>());
		conf.analysis = DefaultConfiguration.simpleDomain(
				DefaultConfiguration.defaultHeapDomain(),
				new Interval(),
				DefaultConfiguration.defaultTypeDomain());
		conf.cfgTransformations.add(new LoopUnrolling(2));
		conf.testDir = "loop-unrolling";
		conf.testSubDir = "unroll-2";
		conf.programFile = "loop-unrolling.imp";
		conf.compareWithOptimization = false;
		perform(conf);
	}

	@Test
	public void testUnrollFactor4() {
		CronConfiguration conf = new CronConfiguration();
		conf.outputs.add(new JSONResults<>());
		conf.analysis = DefaultConfiguration.simpleDomain(
				DefaultConfiguration.defaultHeapDomain(),
				new Interval(),
				DefaultConfiguration.defaultTypeDomain());
		conf.cfgTransformations.add(new LoopUnrolling(4));
		conf.testDir = "loop-unrolling";
		conf.testSubDir = "unroll-4";
		conf.programFile = "loop-unrolling.imp";
		conf.compareWithOptimization = false;
		perform(conf);
	}

	@Test
	public void testUnrollFactor100() {
		CronConfiguration conf = new CronConfiguration();
		conf.outputs.add(new JSONResults<>());
		conf.analysis = DefaultConfiguration.simpleDomain(
				DefaultConfiguration.defaultHeapDomain(),
				new Interval(),
				DefaultConfiguration.defaultTypeDomain());
		conf.cfgTransformations.add(new LoopUnrolling(100));
		conf.testDir = "loop-unrolling";
		conf.testSubDir = "unroll-100";
		conf.programFile = "loop-unrolling.imp";
		conf.compareWithOptimization = false;
		perform(conf);
	}
}

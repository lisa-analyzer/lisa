package it.unive.lisa.cron;

import it.unive.lisa.DefaultConfiguration;
import it.unive.lisa.analysis.HistoryDomain;
import it.unive.lisa.analysis.numeric.IntegerConstantPropagation;
import it.unive.lisa.analysis.numeric.Interval;
import it.unive.lisa.analysis.numeric.NonRedundantIntervals;
import it.unive.lisa.analysis.numeric.Parity;
import it.unive.lisa.analysis.numeric.Pentagon;
import it.unive.lisa.analysis.numeric.Sign;
import it.unive.lisa.outputs.JSONResults;
import it.unive.lisa.program.cfg.fixpoints.forward.ForwardDescendingGLBFixpoint;
import org.junit.Test;

public class NumericAnalysesTest
		extends
		IMPCronExecutor {

	@Test
	public void testSign() {
		CronConfiguration conf = new CronConfiguration();
		conf.outputs.add(new JSONResults<>());
		conf.analysis = DefaultConfiguration.simpleDomain(
				DefaultConfiguration.defaultHeapDomain(),
				new Sign(),
				DefaultConfiguration.defaultTypeDomain());
		conf.testDir = "numeric";
		conf.testSubDir = "sign";
		conf.programFile = "numeric.imp";
		perform(conf);
	}

	@Test
	public void testParity() {
		CronConfiguration conf = new CronConfiguration();
		conf.outputs.add(new JSONResults<>());
		conf.analysis = DefaultConfiguration.simpleDomain(
				DefaultConfiguration.defaultHeapDomain(),
				new Parity(),
				DefaultConfiguration.defaultTypeDomain());
		conf.testDir = "numeric";
		conf.testSubDir = "parity";
		conf.programFile = "numeric.imp";
		perform(conf);
	}

	@Test
	public void testInterval() {
		CronConfiguration conf = new CronConfiguration();
		conf.outputs.add(new JSONResults<>());
		conf.analysis = DefaultConfiguration.simpleDomain(
				DefaultConfiguration.defaultHeapDomain(),
				new Interval(),
				DefaultConfiguration.defaultTypeDomain());
		conf.testDir = "numeric";
		conf.testSubDir = "interval";
		conf.programFile = "numeric.imp";
		perform(conf);
	}

	@Test
	public void testIntervalWithHistory() {
		CronConfiguration conf = new CronConfiguration();
		conf.outputs.add(new JSONResults<>());
		conf.analysis = new HistoryDomain<>(
				DefaultConfiguration.simpleDomain(
						DefaultConfiguration.defaultHeapDomain(),
						new Interval(),
						DefaultConfiguration.defaultTypeDomain()));
		conf.testDir = "numeric";
		conf.testSubDir = "interval-history";
		conf.programFile = "numeric.imp";
		// the unwinding is inherently different: since the loop guard has
		// already the final widened approximation, all history from loop
		// iterations is lost
		conf.compareWithOptimization = false;
		perform(conf);
	}

	@Test
	public void testIntegerConstantPropagation() {
		CronConfiguration conf = new CronConfiguration();
		conf.outputs.add(new JSONResults<>());
		conf.analysis = DefaultConfiguration.simpleDomain(
				DefaultConfiguration.defaultHeapDomain(),
				new IntegerConstantPropagation(),
				DefaultConfiguration.defaultTypeDomain());
		conf.testDir = "numeric";
		conf.testSubDir = "int-const";
		conf.programFile = "numeric.imp";
		perform(conf);
	}

	@Test
	public void testNonRedundantSetOfInterval() {
		CronConfiguration conf = new CronConfiguration();
		conf.outputs.add(new JSONResults<>());
		conf.analysis = DefaultConfiguration.simpleDomain(
				DefaultConfiguration.defaultHeapDomain(),
				new NonRedundantIntervals(),
				DefaultConfiguration.defaultTypeDomain());
		conf.testDir = "numeric";
		conf.testSubDir = "interval-set";
		conf.programFile = "numeric.imp";
		conf.forwardDescendingFixpoint = new ForwardDescendingGLBFixpoint<>();
		conf.glbThreshold = 5;
		// there seem to be one less round of redundancy removal
		// that avoids compacting two elements into a single one when running an
		// optimized analysis. the result is still sound and more precise
		// however.
		conf.compareWithOptimization = false;
		perform(conf);
	}

	@Test
	public void testPentagons() {
		CronConfiguration conf = new CronConfiguration();
		conf.outputs.add(new JSONResults<>());
		conf.analysis = DefaultConfiguration.simpleDomain(
				DefaultConfiguration.defaultHeapDomain(),
				new Pentagon(),
				DefaultConfiguration.defaultTypeDomain());
		conf.testDir = "numeric";
		conf.testSubDir = "pentagons";
		conf.programFile = "pentagons.imp";
		perform(conf);
	}

}

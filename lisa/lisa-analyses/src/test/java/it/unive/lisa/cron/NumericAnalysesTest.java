package it.unive.lisa.cron;

import it.unive.lisa.AnalysisTestExecutor;
import it.unive.lisa.CronConfiguration;
import it.unive.lisa.DefaultConfiguration;
import it.unive.lisa.analysis.numeric.IntegerConstantPropagation;
import it.unive.lisa.analysis.numeric.Interval;
import it.unive.lisa.analysis.numeric.NonRedundantIntervals;
import it.unive.lisa.analysis.numeric.Parity;
import it.unive.lisa.analysis.numeric.Pentagon;
import it.unive.lisa.analysis.numeric.Sign;
import it.unive.lisa.conf.LiSAConfiguration.DescendingPhaseType;
import org.junit.Test;

public class NumericAnalysesTest
		extends
		AnalysisTestExecutor {

	@Test
	public void testSign() {
		CronConfiguration conf = new CronConfiguration();
		conf.serializeResults = true;
		conf.analysis = DefaultConfiguration
				.simpleState(
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
		conf.serializeResults = true;
		conf.analysis = DefaultConfiguration
				.simpleState(
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
		conf.serializeResults = true;
		conf.analysis = DefaultConfiguration
				.simpleState(
						DefaultConfiguration.defaultHeapDomain(),
						new Interval(),
						DefaultConfiguration.defaultTypeDomain());
		conf.testDir = "numeric";
		conf.testSubDir = "interval";
		conf.programFile = "numeric.imp";
		perform(conf);
	}

	@Test
	public void testIntegerConstantPropagation() {
		CronConfiguration conf = new CronConfiguration();
		conf.serializeResults = true;
		conf.analysis = DefaultConfiguration
				.simpleState(
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
		conf.serializeResults = true;
		conf.analysis = DefaultConfiguration
				.simpleState(
						DefaultConfiguration.defaultHeapDomain(),
						new NonRedundantIntervals(),
						DefaultConfiguration.defaultTypeDomain());
		conf.testDir = "numeric";
		conf.testSubDir = "interval-set";
		conf.programFile = "numeric.imp";
		conf.descendingPhaseType = DescendingPhaseType.GLB;
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
		conf.serializeResults = true;
		conf.analysis = DefaultConfiguration
				.simpleState(
						DefaultConfiguration.defaultHeapDomain(),
						new Pentagon(),
						DefaultConfiguration.defaultTypeDomain());
		conf.testDir = "numeric";
		conf.testSubDir = "pentagons";
		conf.programFile = "pentagons.imp";
		perform(conf);
	}

}

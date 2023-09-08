package it.unive.lisa.cron;

import org.junit.Test;

import it.unive.lisa.AnalysisSetupException;
import it.unive.lisa.AnalysisTestExecutor;
import it.unive.lisa.CronConfiguration;
import it.unive.lisa.DefaultConfiguration;
import it.unive.lisa.analysis.nonrelational.value.ValueEnvironment;
import it.unive.lisa.analysis.numeric.IntegerConstantPropagation;
import it.unive.lisa.analysis.numeric.Interval;
import it.unive.lisa.analysis.numeric.Parity;
import it.unive.lisa.analysis.numeric.Sign;

public class NumericAnalysesTest extends AnalysisTestExecutor {

	@Test
	public void testSign() throws AnalysisSetupException {
		CronConfiguration conf = new CronConfiguration();
		conf.serializeResults = true;
		conf.abstractState = DefaultConfiguration.simpleState(
				DefaultConfiguration.defaultHeapDomain(),
				new ValueEnvironment<>(new Sign()),
				DefaultConfiguration.defaultTypeDomain());
		conf.testDir = "sign";
		conf.programFile = "program.imp";
		perform(conf);
	}

	@Test
	public void testParity() throws AnalysisSetupException {
		CronConfiguration conf = new CronConfiguration();
		conf.serializeResults = true;
		conf.abstractState = DefaultConfiguration.simpleState(
				DefaultConfiguration.defaultHeapDomain(),
				new ValueEnvironment<>(new Parity()),
				DefaultConfiguration.defaultTypeDomain());
		conf.testDir = "parity";
		conf.programFile = "program.imp";
		perform(conf);
	}

	@Test
	public void testInterval() throws AnalysisSetupException {
		CronConfiguration conf = new CronConfiguration();
		conf.serializeResults = true;
		conf.abstractState = DefaultConfiguration.simpleState(
				DefaultConfiguration.defaultHeapDomain(),
				new ValueEnvironment<>(new Interval()),
				DefaultConfiguration.defaultTypeDomain());
		conf.testDir = "interval";
		conf.programFile = "program.imp";
		perform(conf);
	}

	@Test
	public void testIntegerConstantPropagation() throws AnalysisSetupException {
		CronConfiguration conf = new CronConfiguration();
		conf.serializeResults = true;
		conf.abstractState = DefaultConfiguration.simpleState(
				DefaultConfiguration.defaultHeapDomain(),
				new ValueEnvironment<>(new IntegerConstantPropagation()),
				DefaultConfiguration.defaultTypeDomain());
		conf.testDir = "int-const";
		conf.programFile = "program.imp";
		perform(conf);
	}
}

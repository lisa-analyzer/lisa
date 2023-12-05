package it.unive.lisa.cron;

import it.unive.lisa.AnalysisTestExecutor;
import it.unive.lisa.CronConfiguration;
import it.unive.lisa.DefaultConfiguration;
import it.unive.lisa.analysis.nonrelational.value.ValueEnvironment;
import it.unive.lisa.analysis.string.CharInclusion;
import it.unive.lisa.analysis.string.Prefix;
import it.unive.lisa.analysis.string.Suffix;
import it.unive.lisa.analysis.string.bricks.Bricks;
import it.unive.lisa.analysis.string.fsa.FSA;
import it.unive.lisa.analysis.string.tarsis.Tarsis;
import org.junit.Test;

public class StringAnalysesTest extends AnalysisTestExecutor {

	@Test
	public void testPrefix() {
		CronConfiguration conf = new CronConfiguration();
		conf.serializeResults = true;
		conf.abstractState = DefaultConfiguration.simpleState(
				DefaultConfiguration.defaultHeapDomain(),
				new ValueEnvironment<>(new Prefix()),
				DefaultConfiguration.defaultTypeDomain());
		conf.testDir = "string";
		conf.testSubDir = "prefix";
		conf.programFile = "strings.imp";
		perform(conf);
	}

	@Test
	public void testSuffix() {
		CronConfiguration conf = new CronConfiguration();
		conf.serializeResults = true;
		conf.abstractState = DefaultConfiguration.simpleState(
				DefaultConfiguration.defaultHeapDomain(),
				new ValueEnvironment<>(new Suffix()),
				DefaultConfiguration.defaultTypeDomain());
		conf.testDir = "string";
		conf.testSubDir = "suffix";
		conf.programFile = "strings.imp";
		perform(conf);
	}

	@Test
	public void testCharInclusion() {
		CronConfiguration conf = new CronConfiguration();
		conf.serializeResults = true;
		conf.abstractState = DefaultConfiguration.simpleState(
				DefaultConfiguration.defaultHeapDomain(),
				new ValueEnvironment<>(new CharInclusion()),
				DefaultConfiguration.defaultTypeDomain());
		conf.testDir = "string";
		conf.testSubDir = "char-inclusion";
		conf.programFile = "strings.imp";
		perform(conf);
	}

	@Test
	public void testBricks() {
		CronConfiguration conf = new CronConfiguration();
		conf.serializeResults = true;
		conf.abstractState = DefaultConfiguration.simpleState(
				DefaultConfiguration.defaultHeapDomain(),
				new ValueEnvironment<>(new Bricks()),
				DefaultConfiguration.defaultTypeDomain());
		conf.testDir = "string";
		conf.testSubDir = "bricks";
		conf.programFile = "strings.imp";
		// we disable optimized test because of bricks normalization: without
		// optimization, loops that get iterated more than once will have
		// poststates of instructions within them built with at least one lub
		// invocation between the different iterations, and that will invoke the
		// normalization algorithm. Optimized run instead will not iterate
		// multiple times, and poststates will be the plain ones returned by
		// abstract transformers. Even if they are semantically equivalent,
		// comparisons will fail nonetheless
		conf.compareWithOptimization = false;
		perform(conf);
	}

	@Test
	public void testFSA() {
		CronConfiguration conf = new CronConfiguration();
		conf.serializeResults = true;
		conf.abstractState = DefaultConfiguration.simpleState(
				DefaultConfiguration.defaultHeapDomain(),
				new ValueEnvironment<>(new FSA()),
				DefaultConfiguration.defaultTypeDomain());
		conf.testDir = "string";
		conf.testSubDir = "fsa";
		conf.programFile = "strings.imp";
		perform(conf);
	}

	@Test
	public void testTarsis() {
		CronConfiguration conf = new CronConfiguration();
		conf.serializeResults = true;
		conf.abstractState = DefaultConfiguration.simpleState(
				DefaultConfiguration.defaultHeapDomain(),
				new ValueEnvironment<>(new Tarsis()),
				DefaultConfiguration.defaultTypeDomain());
		conf.testDir = "string";
		conf.testSubDir = "tarsis";
		conf.programFile = "strings.imp";
		perform(conf);
	}
}

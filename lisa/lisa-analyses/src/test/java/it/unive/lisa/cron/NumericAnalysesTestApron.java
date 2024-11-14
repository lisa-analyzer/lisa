package it.unive.lisa.cron;

import it.unive.lisa.AnalysisTestExecutor;	
import it.unive.lisa.CronConfiguration;
import it.unive.lisa.DefaultConfiguration;
import it.unive.lisa.analysis.nonRedundantSet.NonRedundantPowersetOfInterval;
import it.unive.lisa.analysis.nonrelational.value.ValueEnvironment;
import it.unive.lisa.analysis.numeric.*;
import it.unive.lisa.conf.LiSAConfiguration;
import it.unive.lisa.conf.LiSAConfiguration.DescendingPhaseType;
import org.junit.Test;
import it.unive.lisa.analysis.Apron;

public class NumericAnalysesTestApron extends AnalysisTestExecutor {


	@Test
	public void testSignPPLite() {
		CronConfiguration conf = new CronConfiguration();
		conf.serializeResults = true;
		Apron.setManager(Apron.ApronDomain.PPLite);
		Apron apron=new Apron();

		conf.analysisGraphs= LiSAConfiguration.GraphType.DOT;
		conf.abstractState = DefaultConfiguration.simpleState(
				DefaultConfiguration.defaultHeapDomain(),
				apron,
				DefaultConfiguration.defaultTypeDomain());
		conf.testDir = "numeric";
		conf.testSubDir = "sign.PPLite";
		conf.programFile = "numeric.imp";
		perform(conf);
	}

	@Test
	public void testSignBox() {
		CronConfiguration conf = new CronConfiguration();
		conf.serializeResults = true;
		Apron.setManager(Apron.ApronDomain.Box);
		Apron apron=new Apron();

		conf.analysisGraphs= LiSAConfiguration.GraphType.DOT;
		conf.abstractState = DefaultConfiguration.simpleState(
				DefaultConfiguration.defaultHeapDomain(),
				apron,
				DefaultConfiguration.defaultTypeDomain());
		conf.testDir = "numericApron";
		conf.testSubDir = "sign";
		conf.programFile = "numeric.imp";
		perform(conf);
	}

	@Test
	public void testSignOctagon() {
		CronConfiguration conf = new CronConfiguration();
		conf.serializeResults = true;
		Apron.setManager(Apron.ApronDomain.Octagon);
		Apron apron=new Apron();

		conf.analysisGraphs= LiSAConfiguration.GraphType.DOT;
		conf.abstractState = DefaultConfiguration.simpleState(
				DefaultConfiguration.defaultHeapDomain(),
				apron,
				DefaultConfiguration.defaultTypeDomain());
		conf.testDir = "numeric";
		conf.testSubDir = "sign";
		conf.programFile = "numeric.imp";
		perform(conf);
	}

	@Test
	public void testSignPolka() {
		CronConfiguration conf = new CronConfiguration();
		conf.serializeResults = true;
		Apron.setManager(Apron.ApronDomain.Polka);
		Apron apron=new Apron();

		conf.analysisGraphs= LiSAConfiguration.GraphType.DOT;
		conf.abstractState = DefaultConfiguration.simpleState(
				DefaultConfiguration.defaultHeapDomain(),
				apron,
				DefaultConfiguration.defaultTypeDomain());
		conf.testDir = "numeric";
		conf.testSubDir = "sign";
		conf.programFile = "numeric.imp";
		perform(conf);
	}

	@Test
	public void testSignPolkaEq() {
		CronConfiguration conf = new CronConfiguration();
		conf.serializeResults = true;
		Apron.setManager(Apron.ApronDomain.PolkaEq);
		Apron apron=new Apron();

		conf.analysisGraphs= LiSAConfiguration.GraphType.DOT;
		conf.abstractState = DefaultConfiguration.simpleState(
				DefaultConfiguration.defaultHeapDomain(),
				apron,
				DefaultConfiguration.defaultTypeDomain());
		conf.testDir = "numeric";
		conf.testSubDir = "sign";
		conf.programFile = "numeric.imp";
		perform(conf);
	}

	/*
	@Test
	public void testSignPplGrid() {
		CronConfiguration conf = new CronConfiguration();
		conf.serializeResults = true;
		Apron.setManager(Apron.ApronDomain.PplGrid);
		Apron apron=new Apron();

		conf.analysisGraphs= LiSAConfiguration.GraphType.DOT;
		conf.abstractState = DefaultConfiguration.simpleState(
				DefaultConfiguration.defaultHeapDomain(),
				apron,
				DefaultConfiguration.defaultTypeDomain());
		conf.testDir = "numeric";
		conf.testSubDir = "sign";
		conf.programFile = "numeric.imp";
		perform(conf);
	}
	*/

	/*
	@Test
	public void testSignPplPoly() {
		CronConfiguration conf = new CronConfiguration();
		conf.serializeResults = true;
		Apron.setManager(Apron.ApronDomain.PplPoly);
		Apron apron=new Apron();

		conf.analysisGraphs= LiSAConfiguration.GraphType.DOT;
		conf.abstractState = DefaultConfiguration.simpleState(
				DefaultConfiguration.defaultHeapDomain(),
				apron,
				DefaultConfiguration.defaultTypeDomain());
		conf.testDir = "numeric";
		conf.testSubDir = "sign";
		conf.programFile = "numeric.imp";
		perform(conf);
	}
	*/
}

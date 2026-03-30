package it.unive.lisa.cron;

import it.unive.lisa.DefaultConfiguration;
import it.unive.lisa.analysis.apron.Apron;
import it.unive.lisa.outputs.HtmlResults;
import it.unive.lisa.outputs.JSONResults;
import org.junit.jupiter.api.Test;

public class ApronAnalysisTest
		extends
		IMPCronExecutor {

	@Test
	public void testApronBox() {

		Apron.loadLibrary();

		Apron.setManager(Apron.ApronDomain.Box);

		CronConfiguration conf = new CronConfiguration();
		conf.outputs.add(new JSONResults<>());
		// conf.outputs.add(new HtmlResults<>(true));
		// conf.forceUpdate = true;
		conf.analysis = DefaultConfiguration.simpleDomain(
				DefaultConfiguration.defaultHeapDomain(),
				new Apron(),
				DefaultConfiguration.defaultTypeDomain());
		conf.testDir = "numeric/apron-box";
		conf.programFile = "box.imp";
		perform(conf);
	}

	@Test
	public void testApronOctagon() {
		Apron.loadLibrary();

		Apron.setManager(Apron.ApronDomain.Octagon);

		CronConfiguration conf = new CronConfiguration();
		conf.outputs.add(new JSONResults<>());
		conf.outputs.add(new HtmlResults<>(true));
		// conf.forceUpdate = true;
		conf.analysis = DefaultConfiguration.simpleDomain(
				DefaultConfiguration.defaultHeapDomain(),
				new Apron(),
				DefaultConfiguration.defaultTypeDomain());
		conf.testDir = "numeric/apron-octagon";
		conf.programFile = "octagon.imp";
		perform(conf);
	}
}

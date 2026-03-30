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
    public void testApron() {

        Apron.loadLibrary();

        CronConfiguration conf = new CronConfiguration();
        conf.outputs.add(new JSONResults<>());
        conf.outputs.add(new HtmlResults<>(true));
        conf.analysis = DefaultConfiguration.simpleDomain(
                DefaultConfiguration.defaultHeapDomain(),
                new Apron(),
                DefaultConfiguration.defaultTypeDomain());
        conf.testDir = "numeric";
        conf.testSubDir = "apron";
        conf.programFile = "numeric.imp";
        perform(conf);
    }

}

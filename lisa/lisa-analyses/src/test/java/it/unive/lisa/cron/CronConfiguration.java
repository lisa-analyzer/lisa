package it.unive.lisa.cron;

import it.unive.lisa.DefaultConfiguration;
import it.unive.lisa.util.testing.TestConfiguration;

public class CronConfiguration extends TestConfiguration{

    public boolean allMethods = false;

    public CronConfiguration() {
        this.callGraph = DefaultConfiguration.defaultCallGraph();
		this.interproceduralAnalysis = DefaultConfiguration.defaultInterproceduralAnalysis();
    }
}

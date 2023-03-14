package it.unive.lisa;

import it.unive.lisa.conf.LiSAConfiguration;

/**
 * An extended {@link LiSAConfiguration} that also holds test configuration
 * keys.
 * 
 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
 */
public class CronConfiguration extends LiSAConfiguration {

	/**
	 * The name of the test folder; this is used for searching expected results
	 * and as a working directory for executing tests in the test execution
	 * folder.
	 */
	public String testDir;

	/**
	 * An additional folder that is appended to {@link #testDir} both when
	 * computing the working directory and when searching for the expected
	 * results, but <b>not</b> for searching the source IMP program.
	 */
	public String testSubDir;

	/**
	 * The name of the imp source file to be searched in {@link #testDir}.
	 */
	public String programFile;

	/**
	 * If {@code true}, baselines will be updated if the test fails.
	 */
	public boolean forceUpdate = false;

	/**
	 * If {@code true}, a second analysis will be ran with optimization enabled
	 * and the results will be checked to be equal to the non-optimized version.
	 */
	public boolean compareWithOptimization = true;
}

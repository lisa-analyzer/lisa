package it.unive.lisa.conf;

import it.unive.lisa.conf.LiSAConfiguration.DescendingPhaseType;

/**
 * An immutable configuration holding fixpoint-specific parameters.
 * 
 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
 */
public class FixpointConfiguration extends BaseConfiguration {

	/**
	 * Holder of {@link LiSAConfiguration#wideningThreshold}.
	 */
	public final int wideningThreshold;

	/**
	 * Holder of {@link LiSAConfiguration#glbThreshold}.
	 */
	public final int glbThreshold;

	/**
	 * Holder of {@link LiSAConfiguration#descendingPhaseType}.
	 */
	public final DescendingPhaseType descendingPhaseType;

	/**
	 * Builds the configuration.
	 * 
	 * @param parent the root configuration to draw data from.
	 */
	public FixpointConfiguration(LiSAConfiguration parent) {
		this.wideningThreshold = parent.wideningThreshold;
		this.glbThreshold = parent.glbThreshold;
		this.descendingPhaseType = parent.descendingPhaseType;
	}
}

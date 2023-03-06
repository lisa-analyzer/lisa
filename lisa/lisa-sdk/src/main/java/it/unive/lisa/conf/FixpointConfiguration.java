package it.unive.lisa.conf;

import it.unive.lisa.conf.LiSAConfiguration.DescendingPhaseType;

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

	public FixpointConfiguration(LiSAConfiguration parent) {
		this.wideningThreshold = parent.wideningThreshold;
		this.glbThreshold = parent.glbThreshold;
		this.descendingPhaseType = parent.descendingPhaseType;
	}

	public FixpointConfiguration(
			int wideningThreshold,
			int glbThreshold,
			DescendingPhaseType descendingPhaseType,
			boolean optimize) {
		this.wideningThreshold = wideningThreshold;
		this.glbThreshold = glbThreshold;
		this.descendingPhaseType = descendingPhaseType;
	}

}

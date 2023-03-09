package it.unive.lisa.conf;

import java.util.function.Predicate;

import it.unive.lisa.conf.LiSAConfiguration.DescendingPhaseType;
import it.unive.lisa.program.cfg.statement.Statement;

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

	public final boolean optimize;

	public final Predicate<Statement> hotspots;

	/**
	 * Builds the configuration.
	 * 
	 * @param parent the root configuration to draw data from.
	 */
	public FixpointConfiguration(LiSAConfiguration parent) {
		this.wideningThreshold = parent.wideningThreshold;
		this.glbThreshold = parent.glbThreshold;
		this.descendingPhaseType = parent.descendingPhaseType;
		this.optimize = parent.optimize;
		this.hotspots = parent.hotspots;
	}
}

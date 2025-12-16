package it.unive.lisa.conf;

import it.unive.lisa.analysis.AbstractDomain;
import it.unive.lisa.analysis.AbstractLattice;
import it.unive.lisa.program.cfg.fixpoints.backward.BackwardCFGFixpoint;
import it.unive.lisa.program.cfg.fixpoints.forward.ForwardCFGFixpoint;
import it.unive.lisa.program.cfg.statement.Statement;
import it.unive.lisa.util.collections.workset.WorkingSet;
import java.util.function.Predicate;

/**
 * An immutable configuration holding fixpoint-specific parameters.
 * 
 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
 * 
 * @param <A> the kind of {@link AbstractLattice} produced by the domain
 *                {@code D}
 * @param <D> the kind of {@link AbstractDomain} to run during the analysis
 */
public class FixpointConfiguration<
		A extends AbstractLattice<A>,
		D extends AbstractDomain<A>>
		extends
		BaseConfiguration {

	/**
	 * Holder of {@link LiSAConfiguration#fixpointWorkingSet}.
	 */
	public final WorkingSet<Statement> fixpointWorkingSet;

	/**
	 * Holder of {@link LiSAConfiguration#wideningThreshold}.
	 */
	public final int wideningThreshold;

	/**
	 * Holder of {@link LiSAConfiguration#recursionWideningThreshold}.
	 */
	public final int recursionWideningThreshold;

	/**
	 * Holder of {@link LiSAConfiguration#glbThreshold}.
	 */
	public final int glbThreshold;

	/**
	 * Holder of {@link LiSAConfiguration#useWideningPoints}.
	 */
	public final boolean useWideningPoints;

	/**
	 * Holder of {@link LiSAConfiguration#hotspots}.
	 */
	public final Predicate<Statement> hotspots;

	/**
	 * Holder of {@link LiSAConfiguration#forwardFixpoint}.
	 */
	public final ForwardCFGFixpoint<A, D> forwardFixpoint;

	/**
	 * Holder of {@link LiSAConfiguration#forwardDescendingFixpoint}.
	 */
	public final ForwardCFGFixpoint<A, D> forwardDescendingFixpoint;

	/**
	 * Holder of {@link LiSAConfiguration#backwardFixpoint}.
	 */
	public final BackwardCFGFixpoint<A, D> backwardFixpoint;

	/**
	 * Holder of {@link LiSAConfiguration#backwardDescendingFixpoint}.
	 */
	public final BackwardCFGFixpoint<A, D> backwardDescendingFixpoint;

	/**
	 * Builds the configuration.
	 * 
	 * @param parent the root configuration to draw data from.
	 */
	@SuppressWarnings("unchecked")
	public FixpointConfiguration(
			LiSAConfiguration parent) {
		this.fixpointWorkingSet = parent.fixpointWorkingSet;
		this.wideningThreshold = parent.wideningThreshold;
		this.recursionWideningThreshold = parent.recursionWideningThreshold;
		this.glbThreshold = parent.glbThreshold;
		this.useWideningPoints = parent.useWideningPoints;
		this.hotspots = parent.hotspots;
		this.forwardFixpoint = (ForwardCFGFixpoint<A, D>) parent.forwardFixpoint;
		this.forwardDescendingFixpoint = (ForwardCFGFixpoint<A, D>) parent.forwardDescendingFixpoint;
		this.backwardFixpoint = (BackwardCFGFixpoint<A, D>) parent.backwardFixpoint;
		this.backwardDescendingFixpoint = (BackwardCFGFixpoint<A, D>) parent.backwardDescendingFixpoint;
	}

	/**
	 * Yields {@code true} if this configuration uses an optimized forward
	 * fixpoint.
	 * 
	 * @return {@code true} if this configuration uses an optimized forward
	 *             fixpoint
	 */
	public boolean usesOptimizedForwardFixpoint() {
		return forwardFixpoint.isOptimized()
				|| (forwardDescendingFixpoint != null && forwardDescendingFixpoint.isOptimized());
	}

	/**
	 * Yields {@code true} if this configuration uses an optimized backward
	 * fixpoint.
	 * 
	 * @return {@code true} if this configuration uses an optimized backward
	 *             fixpoint
	 */
	public boolean usesOptimizedBackwardFixpoint() {
		return backwardFixpoint.isOptimized()
				|| (backwardDescendingFixpoint != null && backwardDescendingFixpoint.isOptimized());
	}
}

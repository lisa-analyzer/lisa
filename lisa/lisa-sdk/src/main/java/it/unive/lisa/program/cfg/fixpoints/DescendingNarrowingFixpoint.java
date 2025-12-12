package it.unive.lisa.program.cfg.fixpoints;

import it.unive.lisa.analysis.AbstractDomain;
import it.unive.lisa.analysis.AbstractLattice;
import it.unive.lisa.analysis.AnalysisState;
import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.analysis.StatementStore;
import it.unive.lisa.conf.FixpointConfiguration;
import it.unive.lisa.interprocedural.InterproceduralAnalysis;
import it.unive.lisa.program.cfg.CFG;
import it.unive.lisa.program.cfg.statement.Statement;
import java.util.Collection;

/**
 * A {@link CFGFixpoint} that traverses descending chains using narrowings.
 * 
 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
 * 
 * @param <A> the kind of {@link AbstractLattice} produced by the domain
 *                {@code D}
 * @param <D> the kind of {@link AbstractDomain} to run during the analysis
 */
public class DescendingNarrowingFixpoint<A extends AbstractLattice<A>,
		D extends AbstractDomain<A>>
		extends
		CFGFixpoint<A, D> {

	private final FixpointConfiguration config;

	private final Collection<Statement> wideningPoints;

	/**
	 * Builds the fixpoint implementation.
	 * 
	 * @param target          the target of the implementation
	 * @param interprocedural the {@link InterproceduralAnalysis} to use for
	 *                            semantics computations
	 * @param config          the {@link FixpointConfiguration} to use
	 */
	public DescendingNarrowingFixpoint(
			CFG target,
			InterproceduralAnalysis<A, D> interprocedural,
			FixpointConfiguration config) {
		super(target, interprocedural);
		this.config = config;
		this.wideningPoints = config.useWideningPoints ? target.getCycleEntries() : null;
	}

	@Override
	public CompoundState<A> join(
			Statement node,
			CompoundState<A> approx,
			CompoundState<A> old)
			throws SemanticException {
		if (wideningPoints == null || !wideningPoints.contains(node))
			// optimization: never apply narrowing on normal instructions,
			// save time and precision and only apply to widening points
			return old.glb(approx);

		AnalysisState<A> post = old.postState.narrowing(approx.postState);
		StatementStore<A> intermediate;
		if (config.useWideningPoints)
			// no need to narrow the intermediate expressions as
			// well: we force convergence on the final post state
			// only, to recover as much precision as possible
			intermediate = old.intermediateStates.glb(approx.intermediateStates);
		else
			intermediate = old.intermediateStates.narrowing(approx.intermediateStates);
		return CompoundState.of(post, intermediate);
	}

	@Override
	public boolean leq(
			Statement node,
			CompoundState<A> approx,
			CompoundState<A> old)
			throws SemanticException {
		return old.lessOrEqual(approx);
	}

}

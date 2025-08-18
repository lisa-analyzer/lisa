package it.unive.lisa.checks.semantic;

import it.unive.lisa.analysis.AbstractDomain;
import it.unive.lisa.analysis.AbstractLattice;
import it.unive.lisa.checks.Check;

/**
 * A {@link Check} that is able to exploit both the syntactic structure of the
 * program and the semantic information produced with the fixpoint iteration.
 * Instances of this interface will use a {@link CheckToolWithAnalysisResults}
 * as auxiliary tool during the inspection.
 * 
 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
 * 
 * @param <A> the kind of {@link AbstractLattice} produced by the domain
 *                {@code D}
 * @param <D> the kind of {@link AbstractDomain} to run during the analysis
 */
public interface SemanticCheck<A extends AbstractLattice<A>,
		D extends AbstractDomain<A>> extends Check<CheckToolWithAnalysisResults<A, D>> {
}

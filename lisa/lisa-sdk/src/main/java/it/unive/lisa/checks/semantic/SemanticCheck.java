package it.unive.lisa.checks.semantic;

import it.unive.lisa.analysis.AbstractState;
import it.unive.lisa.checks.Check;

/**
 * A {@link Check} that is able to exploit both the syntactic structure of the
 * program and the semantic information produced with the fixpoint iteration.
 * Instances of this interface will use a {@link CheckToolWithAnalysisResults}
 * as auxiliary tool during the inspection.
 * 
 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
 * 
 * @param <A> the type of {@link AbstractState} contained in the results
 */
public interface SemanticCheck<A extends AbstractState<A>> extends Check<CheckToolWithAnalysisResults<A>> {
}

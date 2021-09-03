package it.unive.lisa.checks.semantic;

import it.unive.lisa.checks.Check;

/**
 * A {@link Check} that is able to exploit both the syntactic structure of the
 * program and the semantic information produced with the fixpoint iteration.
 * Instances of this interface will use a {@link CheckToolWithAnalysisResults}
 * as auxiliary tool during the inspection.
 * 
 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
 */
public interface SemanticCheck extends Check<CheckToolWithAnalysisResults<?, ?, ?>> {
}

package it.unive.lisa.checks.syntactic;

import it.unive.lisa.CheckTool;
import it.unive.lisa.checks.Check;

/**
 * A {@link Check} that is able to exploit only the syntactic structure of the
 * program. Instances of this interface will use a {@link CheckTool} as
 * auxiliary tool during the inspection.
 * 
 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
 */
public interface SyntacticCheck
		extends
		Check<CheckTool> {
}

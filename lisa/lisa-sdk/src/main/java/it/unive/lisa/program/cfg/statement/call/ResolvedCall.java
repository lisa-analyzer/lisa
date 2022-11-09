package it.unive.lisa.program.cfg.statement.call;

import it.unive.lisa.program.cfg.CodeMember;
import java.util.Collection;

/**
 * Marker for {@link Call}s that have already been resolved, and that thus know
 * what are their targets.
 * 
 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
 */
public interface ResolvedCall {

	/**
	 * Yields the targets of the {@link Call}.
	 * 
	 * @return the targets
	 */
	Collection<CodeMember> getTargets();
}

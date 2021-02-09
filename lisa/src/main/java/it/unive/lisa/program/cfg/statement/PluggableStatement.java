package it.unive.lisa.program.cfg.statement;

import it.unive.lisa.program.cfg.CFG;
import it.unive.lisa.program.cfg.NativeCFG;

/**
 * A {@link Statement} that can be dynamically plugged into a {@link CFG} in
 * place of another statement. This is used for resolving calls to native
 * constructs ({@link NativeCFG}s) that get rewritten into {@link NativeCall}s.
 * 
 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
 */
public interface PluggableStatement {

	/**
	 * Sets the original {@link Statement} that got rewritten by theis pluggable
	 * statement.
	 * 
	 * @param st the original statement
	 */
	void setOriginatingStatement(Statement st);
}

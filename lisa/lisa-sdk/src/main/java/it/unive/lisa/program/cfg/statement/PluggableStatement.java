package it.unive.lisa.program.cfg.statement;

import it.unive.lisa.program.cfg.CFG;
import it.unive.lisa.program.cfg.CodeLocation;
import it.unive.lisa.program.cfg.NativeCFG;

/**
 * A {@link Statement} that can be dynamically plugged into a {@link CFG} in
 * place of another statement. This is used for resolving calls to native
 * constructs ({@link NativeCFG}s) that get rewritten into
 * {@link NaryExpression}s. <br>
 * <br>
 * <b>Note: NativeCFGs will build instances of this interface assuming that the
 * following contract is satisfied.</b> Classes implementing this interface
 * should provide a public static method named <i>build</i>, returning a
 * {@link NaryExpression} and with the following parameters: {@link CFG},
 * {@link CodeLocation}, {@link Expression}{@code []}.
 * 
 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
 */
@FunctionalInterface
public interface PluggableStatement {

	/**
	 * Sets the original {@link Statement} that got rewritten by theis pluggable
	 * statement.
	 * 
	 * @param st the original statement
	 */
	void setOriginatingStatement(Statement st);
}

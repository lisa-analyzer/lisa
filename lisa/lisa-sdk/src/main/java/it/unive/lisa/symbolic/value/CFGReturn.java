package it.unive.lisa.symbolic.value;

import it.unive.lisa.program.annotations.Annotations;
import it.unive.lisa.program.cfg.CFG;
import it.unive.lisa.program.cfg.CodeLocation;
import it.unive.lisa.type.Type;

/**
 * A special variable that represents the return value of a CFG. This variable
 * is used in {@link it.unive.lisa.program.cfg.statement.Return} statements to
 * denote the value that is returned to the caller CFG.
 * 
 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
 */
public class CFGReturn
		extends
		Variable {

	/**
	 * Builds the return variable of the given CFG.
	 * 
	 * @param cfg        the CFG
	 * @param staticType the static type of the return value
	 * @param location   the code location where this variable is defined
	 */
	public CFGReturn(
			CFG cfg,
			Type staticType,
			CodeLocation location) {
		super(staticType, "ret_value@" + cfg.getDescriptor().getName(), location);
	}

	/**
	 * Builds the return variable of the given CFG.
	 * 
	 * @param cfg         the CFG
	 * @param staticType  the static type of the return value
	 * @param annotations the annotations to attach to the variable
	 * @param location    the code location where this variable is defined
	 */
	public CFGReturn(
			CFG cfg,
			Type staticType,
			Annotations annotations,
			CodeLocation location) {
		super(staticType, "ret_value@" + cfg.getDescriptor().getName(), annotations, location);
	}

}

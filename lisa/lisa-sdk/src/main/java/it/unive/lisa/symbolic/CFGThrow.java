package it.unive.lisa.symbolic;

import it.unive.lisa.analysis.ScopeToken;
import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.program.annotations.Annotations;
import it.unive.lisa.program.cfg.CFG;
import it.unive.lisa.program.cfg.CodeLocation;
import it.unive.lisa.program.cfg.ProgramPoint;
import it.unive.lisa.symbolic.value.Variable;
import it.unive.lisa.type.Type;

/**
 * A special variable that represents the error thrown by a CFG. This variable
 * is used in {@link it.unive.lisa.program.cfg.statement.Throw} statements to
 * denote the value that is thrown as error to the caller CFG. Note that, as
 * this variable should only appear in error states that might be caught later,
 * it cannot be scoped. This is to allow catches in caller CFGs to always access
 * the thrown value.
 *
 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
 */
public class CFGThrow
		extends
		Variable {

	/**
	 * Builds the throw variable of the given CFG.
	 * 
	 * @param cfg        the CFG
	 * @param staticType the static type of the thrown value
	 * @param location   the code location where this variable is defined
	 */
	public CFGThrow(
			CFG cfg,
			Type staticType,
			CodeLocation location) {
		super(staticType, "thrown@" + cfg.getDescriptor().getName(), location);
	}

	/**
	 * Builds the throw variable of the given CFG.
	 * 
	 * @param cfg         the CFG
	 * @param staticType  the static type of the thrown value
	 * @param annotations the annotations to attach to the variable
	 * @param location    the code location where this variable is defined
	 */
	public CFGThrow(
			CFG cfg,
			Type staticType,
			Annotations annotations,
			CodeLocation location) {
		super(staticType, "thrown@" + cfg.getDescriptor().getName(), annotations, location);
	}

	@Override
	public boolean canBeScoped() {
		return false;
	}

	@Override
	public SymbolicExpression popScope(
			ScopeToken token,
			ProgramPoint pp)
			throws SemanticException {
		return this;
	}

	@Override
	public SymbolicExpression pushScope(
			ScopeToken token,
			ProgramPoint pp) {
		return this;
	}

}

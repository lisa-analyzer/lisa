package it.unive.lisa.cfg.statement;

import it.unive.lisa.symbolic.value.Identifier;

/**
 * Objects implementing this interface will produce a meta-variable to represent
 * the value that they produce on the stack during the computation of their
 * semantic.
 * 
 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
 */
public interface MetaVariableCreator {

	/**
	 * Yields the meta variable that is introduced during the evaluation of the
	 * semantics of this object to store information about the value produced by
	 * this object. Since the meta variable simulates a value pushed on the
	 * stack, it should be forgotten after it is consumed.
	 * 
	 * @return the meta variable introduced by this object
	 */
	Identifier getMetaVariable();
}

package it.unive.lisa.program.cfg;

import it.unive.lisa.program.ProgramValidationException;

/**
 * A program member that has code within it. It exposes a method for retrieving
 * a {@link CodeMemberDescriptor} containing its signature.
 * 
 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
 */
public interface CodeMember {

	/**
	 * Yields the {@link CodeMemberDescriptor} containing the signature of this
	 * code member.
	 * 
	 * @return the descriptor of this code member
	 */
	CodeMemberDescriptor getDescriptor();

	/**
	 * Validates this cfg, ensuring that the code contained in it is well
	 * formed.
	 * 
	 * @throws ProgramValidationException if one of the aforementioned checks
	 *                                        fail
	 */
	default public void validate()
			throws ProgramValidationException {
		// nothing to do by default
	}

}

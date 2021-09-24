package it.unive.lisa.program.cfg;

/**
 * A program member that has code within it. It exposes a method for retrieving
 * a {@link CFGDescriptor} containing its signature.
 * 
 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
 */
@FunctionalInterface
public interface CodeMember {

	/**
	 * Yields the {@link CFGDescriptor} containing the signature of this code
	 * member.
	 * 
	 * @return the descriptor of this code member
	 */
	CFGDescriptor getDescriptor();
}

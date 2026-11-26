package it.unive.lisa.triage.inputs;

import it.unive.lisa.program.cfg.NativeCFG;

/**
 * The native code whose unknown results are inputs provided by the user (e.g.,
 * values read from the outside, or nondeterministic choices), as opposed to
 * unknown results that approximate deterministic computations (e.g., library
 * methods whose semantics is not modeled precisely), as a frontend knows them.
 * The verification of the warnings only picks the values of user inputs freely
 * to reproduce a warning (together with the parameters of the entrypoints),
 * while other unknown values make the reproduction imprecise (see
 * {@link InputSource.Kind}).
 *
 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
 */
@FunctionalInterface
public interface UserInputs {

	/**
	 * No native code produces user inputs.
	 */
	UserInputs NONE = target -> false;

	/**
	 * Yields whether the unknown results of the given native cfg are inputs
	 * provided by the user.
	 *
	 * @param target the native cfg
	 *
	 * @return whether its unknown results are user inputs
	 */
	boolean isUserInput(
			NativeCFG target);
}

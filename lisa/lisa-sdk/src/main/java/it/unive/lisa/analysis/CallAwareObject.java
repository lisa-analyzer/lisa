package it.unive.lisa.analysis;

import it.unive.lisa.program.cfg.ProgramPoint;

/**
 * An object that can react to the return from a call, combining the value it
 * has at the end of the callee with the one it had in the caller before the
 * call. This is used by the values stored inside the {@link FixpointInfo} of
 * the analysis states (see
 * {@link FixpointInfo#onCallReturn(FixpointInfo, ProgramPoint)}): by default,
 * the information after a call is the one at the end of the callee, that might
 * be the least upper bound of the information of several calls when the
 * callee's results are shared by different calling contexts. Objects
 * implementing this interface can instead restore the parts of the information
 * that the callee cannot modify from the one of the caller.
 *
 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
 *
 * @param <T> the concrete type of this object, returned by
 *                {@link #onCallReturn(Object, ProgramPoint)}
 */
public interface CallAwareObject<T> {

	/**
	 * Yields the value of this object after returning from a call, given its
	 * value at the end of the callee (i.e., {@code this}) and its value in the
	 * caller before the call.
	 *
	 * @param caller the value of this object in the caller, before the call
	 * @param call   the program point of the call
	 *
	 * @return the value of this object after the call
	 *
	 * @throws SemanticException if an error occurs during the computation
	 */
	T onCallReturn(
			T caller,
			ProgramPoint call)
			throws SemanticException;

}

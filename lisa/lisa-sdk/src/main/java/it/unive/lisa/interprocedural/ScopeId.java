package it.unive.lisa.interprocedural;

import it.unive.lisa.analysis.AbstractLattice;
import it.unive.lisa.program.cfg.CFG;
import it.unive.lisa.program.cfg.fixpoints.CompoundState;
import it.unive.lisa.program.cfg.statement.call.CFGCall;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * An identifier for an {@link InterproceduralAnalysis} to distinguish different
 * results for the same {@link CFG} based on their calling contexts.
 * 
 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
 * 
 * @param <A> the type of {@link AbstractLattice} computed by the analysis
 */
public interface ScopeId<A extends AbstractLattice<A>> {

	/**
	 * Yields the id to use at the start of the analysis, for entrypoints.
	 * 
	 * @return the scope
	 */
	ScopeId<A> startingId();

	/**
	 * Yields whether or not this id is the starting one, that is, if it has
	 * been generated with {@link #startingId()} or if it must be considered
	 * equivalent to the value returned by that method.
	 * 
	 * @return {@code true} if that condition holds
	 */
	boolean isStartingId();

	/**
	 * Transforms the current scope id by appending the given call.
	 * 
	 * @param c     the call to append
	 * @param state the analysis state at the call site
	 *
	 * @return the (optionally) updated id
	 */
	ScopeId<A> push(
			CFGCall c,
			CompoundState<A> state);

	/**
	 * Yields a hash of this id, that can be used to identify it in places where
	 * its full representation would be too long (e.g., names of symbolic
	 * expressions or file names). The hash must be consistent with
	 * {@link #equals(Object)}, that is, equal ids must have equal hashes, and
	 * it must be stable across different executions of the analysis.
	 * Implementations should compute the hash once at construction (e.g.,
	 * through {@link #computeHash(String)}) and return the stored value.
	 * 
	 * @return the hash of this id
	 */
	String getHash();

	/**
	 * Computes the hash of the given string representation of a
	 * {@link ScopeId}, as the hexadecimal encoding of its MD5 digest. The
	 * representation should contain all and only the information used to
	 * determine equality of ids, and should not depend on
	 * {@link Object#hashCode()} implementations that are not stable across
	 * executions.
	 * 
	 * @param representation the string representation of the id
	 * 
	 * @return the hash of the representation
	 * 
	 * @throws IllegalStateException if the MD5 algorithm is not available
	 */
	static String computeHash(
			String representation) {
		try {
			byte[] digest = MessageDigest.getInstance("MD5")
					.digest(representation.getBytes(StandardCharsets.UTF_8));
			StringBuilder sb = new StringBuilder(digest.length * 2);
			for (byte b : digest)
				sb.append(String.format("%02x", b));
			return sb.toString();
		} catch (NoSuchAlgorithmException e) {
			// every java platform is required to support MD5
			throw new IllegalStateException("MD5 is not available", e);
		}
	}

}

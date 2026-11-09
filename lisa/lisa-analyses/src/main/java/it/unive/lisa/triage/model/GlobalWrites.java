package it.unive.lisa.triage.model;

import it.unive.lisa.program.cfg.CodeMember;
import java.util.Set;

/**
 * The global variables (e.g., static fields) that the code of a member of the
 * program writes, as a frontend knows them. Alarm models use them to keep the
 * values of the global variables that the callees of a summarized call cannot
 * write (see {@link ResultCache#globalsWrittenBy(java.util.Collection,
 * it.unive.lisa.interprocedural.callgraph.CallGraph)}): without them, a
 * summarized call might write any global variable.
 *
 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
 */
@FunctionalInterface
public interface GlobalWrites {

	/**
	 * The global writes of no member are known.
	 */
	GlobalWrites UNKNOWN = member -> null;

	/**
	 * Yields the names of the global variables (as the program names them when
	 * accessing them, e.g., {@code C::f}) that the code of the given member
	 * might write, excluding the code of the members it calls, or {@code null}
	 * if they are not known (e.g., for code writing global variables through
	 * reflection).
	 *
	 * @param member the member
	 *
	 * @return the names, or {@code null}
	 */
	Set<String> writtenBy(
			CodeMember member);

	/**
	 * Yields whether the given member initializes global variables before
	 * their first access, and runs at most once (e.g., the static initializer
	 * of a class). The global variables written by the callees of a summarized
	 * call are computed without following the calls to such members: this is
	 * sound if every write of a global variable happens after its initializer
	 * ran (so that, once the model wrote the variable, the initializer does
	 * nothing), as the global variables that the model did not write are
	 * unknown anyway (see {@link HeapModel}). By default, no member is an
	 * initializer.
	 *
	 * @param member the member
	 *
	 * @return whether the member is an initializer
	 */
	default boolean isInitializer(
			CodeMember member) {
		return false;
	}
}

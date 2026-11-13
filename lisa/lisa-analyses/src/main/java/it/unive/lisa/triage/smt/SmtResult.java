package it.unive.lisa.triage.smt;

/**
 * The result of a satisfiability check performed by an {@link SmtSolver}.
 *
 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
 */
public enum SmtResult {

	/**
	 * The assertions are satisfiable.
	 */
	SAT,

	/**
	 * The assertions are unsatisfiable.
	 */
	UNSAT,

	/**
	 * The solver could not decide the satisfiability of the assertions (e.g.,
	 * because of a timeout or of an incomplete theory).
	 */
	UNKNOWN;
}

package it.unive.lisa.witness.v2;

/**
 * The features of the witness format that only some languages support.
 * 
 * @author <a href="mailto:giacomo.zanatta@unive.it">Giacomo Zanatta</a>
 */
public enum LanguageFeature {

	/**
	 * The data model of the task, needed to interpret the sizes of types.
	 */
	DATA_MODEL,

	/**
	 * Ghost variables and their updates, used by correctness witnesses.
	 */
	GHOST_INSTRUMENTATION,

	/**
	 * Invariants relating two consecutive states of the program.
	 */
	TRANSITION_INVARIANTS,

	/**
	 * Preconditions and postconditions of functions.
	 */
	FUNCTION_CONTRACTS
}

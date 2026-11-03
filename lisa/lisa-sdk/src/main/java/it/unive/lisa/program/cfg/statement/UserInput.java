package it.unive.lisa.program.cfg.statement;

/**
 * A marker for statements whose unknown results are inputs provided by the
 * user (e.g., values read from the outside, or nondeterministic choices), as
 * opposed to unknown results that approximate deterministic computations
 * (e.g., library methods whose semantics is not modeled precisely). Everything
 * that counts as user input towards triage (other than the parameters of the
 * entrypoints) should implement this interface: the triage of warnings only
 * picks the values of user inputs freely to reproduce a warning, while other
 * unknown values make the reproduction imprecise. For instance, the
 * constructs of {@link it.unive.lisa.program.cfg.NativeCFG}s modeling
 * nondeterminism should implement this interface.
 *
 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
 */
public interface UserInput {
}

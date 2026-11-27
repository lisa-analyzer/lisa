package it.unive.lisa.checks.verification;

/**
 * A {@link Violation} that happens whenever the statement on which the warning
 * is issued is reached. Instances of this class can be obtained through
 * {@link Violation#reach()}.
 *
 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
 */
public final class ReachViolation
		extends
		Violation {

	/**
	 * The singleton instance of this class.
	 */
	static final ReachViolation INSTANCE = new ReachViolation();

	private ReachViolation() {
	}

	@Override
	public String toString() {
		return "reach";
	}
}

package it.unive.lisa.triage.model;

import it.unive.lisa.program.cfg.CodeLocation;
import it.unive.lisa.type.Type;

/**
 * A {@link FreshValue} modeling a value that a transition does not compute
 * precisely (e.g., the result of a summarized call, a value read from the
 * heap, or the result of an unsupported construct). Unlike {@link InputValue}s,
 * summary values are not part of the inputs of an execution: they are a source
 * of imprecision of the model.
 *
 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
 */
public class SummaryValue
		extends
		FreshValue {

	private final String reason;

	/**
	 * Builds the value.
	 *
	 * @param staticType the static type of the value
	 * @param reason     a human-readable description of why the value is not
	 *                       computed precisely
	 * @param location   the location where the value is produced
	 * @param index      a number that uniquely identifies the value among the
	 *                       fresh values of a model
	 */
	public SummaryValue(
			Type staticType,
			String reason,
			CodeLocation location,
			long index) {
		super(staticType, "$summary#" + index, location);
		this.reason = reason;
	}

	/**
	 * Yields the reason why this value is not computed precisely.
	 *
	 * @return the reason
	 */
	public String getReason() {
		return reason;
	}
}

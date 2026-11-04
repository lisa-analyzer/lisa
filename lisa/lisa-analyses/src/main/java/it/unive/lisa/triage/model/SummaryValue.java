package it.unive.lisa.triage.model;

import it.unive.lisa.program.cfg.CodeLocation;
import it.unive.lisa.type.Type;

/**
 * A {@link FreshValue} modeling a value that a transition does not compute
 * precisely (e.g., the result of a summarized call, a value read from the heap,
 * or the result of an unsupported construct). Unlike {@link InputValue}s,
 * summary values are not part of the inputs of an execution: they are a source
 * of imprecision of the model.
 *
 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
 */
public class SummaryValue
		extends
		FreshValue {

	/**
	 * The kinds of imprecision a summary value can come from.
	 *
	 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
	 */
	public enum Kind {

		/**
		 * The result of a call whose callee is not modeled.
		 */
		CALL_RESULT("call results"),

		/**
		 * A value read from the heap, that is not modeled.
		 */
		HEAP("heap values"),

		/**
		 * A value that differs among merged cases of a transition.
		 */
		MERGE("merged values"),

		/**
		 * The value of an identifier that is no longer constrained (e.g., it
		 * has been forgotten).
		 */
		UNCONSTRAINED("unconstrained values"),

		/**
		 * An invalid value.
		 */
		INVALID("invalid values"),

		/**
		 * The result of an expression that is not supported.
		 */
		UNSUPPORTED("unsupported expressions");

		private final String description;

		private Kind(
				String description) {
			this.description = description;
		}

		/**
		 * Yields a human-readable description of the values of this kind, in
		 * the plural.
		 *
		 * @return the description
		 */
		public String getDescription() {
			return description;
		}
	}

	/**
	 * The reason of the unknown conditions replacing the path conditions lost
	 * when merging cases (see
	 * {@link SymbolicCase#merge(SymbolicCase, SymbolicCase, FreshValueFactory, it.unive.lisa.program.cfg.CodeLocation)}).
	 * Such conditions are encoded as {@code true}, but executions relying on
	 * them are not represented precisely.
	 */
	public static final String MERGED_CONDITIONS = "merged path conditions";

	private final Kind kind;

	private final String reason;

	/**
	 * Builds the value.
	 *
	 * @param staticType the static type of the value
	 * @param kind       the kind of imprecision the value comes from
	 * @param reason     a human-readable description of why the value is not
	 *                       computed precisely
	 * @param location   the location where the value is produced
	 * @param index      a number that uniquely identifies the value among the
	 *                       fresh values of a model
	 */
	public SummaryValue(
			Type staticType,
			Kind kind,
			String reason,
			CodeLocation location,
			long index) {
		super(staticType, "$summary#" + index, location);
		this.kind = kind;
		this.reason = reason;
	}

	/**
	 * Yields the kind of imprecision this value comes from.
	 *
	 * @return the kind
	 */
	public Kind getKind() {
		return kind;
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

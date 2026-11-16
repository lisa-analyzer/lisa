package it.unive.lisa.triage.model;

import it.unive.lisa.program.cfg.CodeLocation;
import it.unive.lisa.symbolic.value.PushAny;
import it.unive.lisa.type.Type;
import java.util.Objects;

/**
 * An unknown value introduced by the replay of a statement (e.g., the result of
 * a summarized call). Abstract domains treat it as any other {@link PushAny},
 * while the symbolic component of a {@link ReplayState} turns it into a
 * {@link SummaryValue} instead of an {@link InputValue}, since it is not a
 * source of nondeterminism of the program.
 *
 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
 */
public class SummaryMarker
		extends
		PushAny {

	private final String reason;

	private final SummaryValue.Kind kind;

	/**
	 * Builds the marker of the result of a summarized call.
	 *
	 * @param staticType the static type of the unknown value
	 * @param reason     a human-readable description of why the value is
	 *                       unknown
	 * @param location   the location where the value is introduced
	 */
	public SummaryMarker(
			Type staticType,
			String reason,
			CodeLocation location) {
		this(staticType, reason, location, SummaryValue.Kind.CALL_RESULT);
	}

	/**
	 * Builds the marker.
	 *
	 * @param staticType the static type of the unknown value
	 * @param reason     a human-readable description of why the value is
	 *                       unknown
	 * @param location   the location where the value is introduced
	 * @param kind       the kind of the summary value replacing the marker
	 */
	public SummaryMarker(
			Type staticType,
			String reason,
			CodeLocation location,
			SummaryValue.Kind kind) {
		super(staticType, location);
		this.reason = reason;
		this.kind = kind;
	}

	/**
	 * Yields the kind of the summary value replacing this marker.
	 *
	 * @return the kind
	 */
	public SummaryValue.Kind getKind() {
		return kind;
	}

	/**
	 * Yields the reason why the value is unknown.
	 *
	 * @return the reason
	 */
	public String getReason() {
		return reason;
	}

	@Override
	public int hashCode() {
		return 31 * (31 * super.hashCode() + Objects.hashCode(reason)) + Objects.hashCode(kind);
	}

	@Override
	public boolean equals(
			Object obj) {
		if (this == obj)
			return true;
		if (!super.equals(obj))
			return false;
		return Objects.equals(reason, ((SummaryMarker) obj).reason) && kind == ((SummaryMarker) obj).kind;
	}
}

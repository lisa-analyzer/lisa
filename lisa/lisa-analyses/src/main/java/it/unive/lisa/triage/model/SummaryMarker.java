package it.unive.lisa.triage.model;

import it.unive.lisa.program.cfg.CodeLocation;
import it.unive.lisa.symbolic.value.PushAny;
import it.unive.lisa.type.Type;
import java.util.Objects;

/**
 * An unknown value introduced by the replay of a statement (e.g., the result
 * of a summarized call). Abstract domains treat it as any other
 * {@link PushAny}, while the symbolic component of a {@link ReplayState} turns
 * it into a {@link SummaryValue} instead of an {@link InputValue}, since it is
 * not a source of nondeterminism of the program.
 *
 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
 */
public class SummaryMarker
		extends
		PushAny {

	private final String reason;

	/**
	 * Builds the marker.
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
		super(staticType, location);
		this.reason = reason;
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
		return 31 * super.hashCode() + Objects.hashCode(reason);
	}

	@Override
	public boolean equals(
			Object obj) {
		if (this == obj)
			return true;
		if (!super.equals(obj))
			return false;
		return Objects.equals(reason, ((SummaryMarker) obj).reason);
	}
}

package it.unive.lisa.lattices.informationFlow;

import it.unive.lisa.analysis.BaseLattice;
import it.unive.lisa.analysis.Lattice;
import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.analysis.informationFlow.NonInterference;
import it.unive.lisa.util.representation.StringRepresentation;
import it.unive.lisa.util.representation.StructuredRepresentation;

/**
 * A pair of integrity and confidentiality bits for the {@link NonInterference}
 * analysis.
 * 
 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
 */
public class NonInterferenceValue
		implements
		BaseLattice<NonInterferenceValue> {

	/**
	 * The value to use for bottom non interference levels.
	 */
	public static final byte NI_BOTTOM = 0;

	/**
	 * The value to use for low non interference levels.
	 */
	public static final byte NI_LOW = -1;

	/**
	 * The value to use for high non interference levels.
	 */
	public static final byte NI_HIGH = 1;

	/**
	 * The non interference level for low confidentiality and high integrity.
	 */
	public static NonInterferenceValue LOW_HIGH = new NonInterferenceValue(NI_LOW, NI_HIGH);

	/**
	 * The non interference level for low confidentiality and low integrity.
	 */
	public static NonInterferenceValue LOW_LOW = new NonInterferenceValue(NI_LOW, NI_LOW);

	/**
	 * The non interference level for high confidentiality and high integrity.
	 */
	public static NonInterferenceValue HIGH_HIGH = new NonInterferenceValue(NI_HIGH, NI_HIGH);

	/**
	 * The non interference level for high confidentiality and low integrity,
	 * corresponding to the top of the lattice.
	 */
	public static NonInterferenceValue HIGH_LOW = new NonInterferenceValue(NI_HIGH, NI_LOW);

	/**
	 * The bottom abstract element.
	 */
	public static NonInterferenceValue BOTTOM = new NonInterferenceValue(NI_BOTTOM, NI_BOTTOM);

	private final byte confidentiality;

	private final byte integrity;

	/**
	 * Builds the abstract value for the given confidentiality and integrity
	 * values. Each of those can be either 0 for bottom ({@link #NI_BOTTOM}), 1
	 * for low ({@link #NI_LOW}), or 2 for high ({@link #NI_HIGH}).
	 * 
	 * @param confidentiality the confidentiality value
	 * @param integrity       the integrity value
	 */
	private NonInterferenceValue(
			byte confidentiality,
			byte integrity) {
		this.confidentiality = confidentiality;
		this.integrity = integrity;
	}

	@Override
	public NonInterferenceValue top() {
		return HIGH_LOW;
	}

	@Override
	public NonInterferenceValue bottom() {
		return BOTTOM;
	}

	/**
	 * Yields {@code true} if and only if this instance represents a
	 * {@code high} value for the confidentiality non interference analysis.
	 * 
	 * @return {@code true} if this is a high confidentiality element
	 */
	public boolean isHighConfidentiality() {
		return confidentiality == NI_HIGH;
	}

	/**
	 * Yields {@code true} if and only if this instance represents a {@code low}
	 * value for the confidentiality non interference analysis.
	 * 
	 * @return {@code true} if this is a low confidentiality element
	 */
	public boolean isLowConfidentiality() {
		return confidentiality == NI_LOW;
	}

	/**
	 * Yields {@code true} if and only if this instance represents a
	 * {@code high} value for the integrity non interference analysis.
	 * 
	 * @return {@code true} if this is a high integrity element
	 */
	public boolean isHighIntegrity() {
		return integrity == NI_HIGH;
	}

	/**
	 * Yields {@code true} if and only if this instance represents a {@code low}
	 * value for the integrity non interference analysis.
	 * 
	 * @return {@code true} if this is a low integrity element
	 */
	public boolean isLowIntegrity() {
		return integrity == NI_LOW;
	}

	@Override
	public NonInterferenceValue lubAux(
			NonInterferenceValue other)
			throws SemanticException {
		// HL
		// | \
		// HH LL
		// | /
		// LH
		// |
		// BB
		byte confidentiality = isHighConfidentiality() || other.isHighConfidentiality() ? NI_HIGH : NI_LOW;
		byte integrity = isLowIntegrity() || other.isLowIntegrity() ? NI_LOW : NI_HIGH;
		if (confidentiality == NI_LOW && integrity == NI_LOW)
			return LOW_LOW;
		if (confidentiality == NI_HIGH && integrity == NI_HIGH)
			return HIGH_HIGH;
		if (confidentiality == NI_HIGH && integrity == NI_LOW)
			return HIGH_LOW;
		// confidentiality == NI_LOW && integrity == NI_HIGH
		return LOW_HIGH;
	}

	@Override
	public boolean lessOrEqualAux(
			NonInterferenceValue other)
			throws SemanticException {
		// HL
		// | \
		// HH LL
		// | /
		// LH
		// |
		// BB
		boolean confidentiality = isLowConfidentiality() || this.confidentiality == other.confidentiality;
		boolean integrity = isHighIntegrity() || this.integrity == other.integrity;
		return confidentiality && integrity;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + confidentiality;
		result = prime * result + integrity;
		return result;
	}

	@Override
	public boolean equals(
			Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		NonInterferenceValue other = (NonInterferenceValue) obj;
		if (confidentiality != other.confidentiality)
			return false;
		if (integrity != other.integrity)
			return false;
		return true;
	}

	@Override
	public String toString() {
		return representation().toString();
	}

	@Override
	public StructuredRepresentation representation() {
		if (isBottom())
			return Lattice.bottomRepresentation();
		return new StringRepresentation((isHighConfidentiality() ? "H" : "L") + (isHighIntegrity() ? "H" : "L"));
	}

}

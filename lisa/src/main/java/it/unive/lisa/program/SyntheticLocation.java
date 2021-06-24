package it.unive.lisa.program;

import it.unive.lisa.program.cfg.CodeLocation;

/**
 * A synthetic code location.
 * 
 * @author <a href="mailto:vincenzo.arceri@unive.it">Vincenzo Arceri</a>
 */
public class SyntheticLocation implements CodeLocation {

	/**
	 * Singleton instance of the synthetic location.
	 */
	public final static SyntheticLocation INSTANCE = new SyntheticLocation();

	private SyntheticLocation() {
	}

	@Override
	public String getCodeLocation() {
		return "<unknown>";
	}

	@Override
	public int compareTo(CodeLocation o) {
		return o instanceof SyntheticLocation ? -1 : 0;
	}
}

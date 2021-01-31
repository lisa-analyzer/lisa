package it.unive.lisa.program.cfg;

public interface ProgramPoint {

	/**
	 * Yields the {@link CFG} that this program point belongs to.
	 * 
	 * @return the containing cfg
	 */
	CFG getCFG();
}

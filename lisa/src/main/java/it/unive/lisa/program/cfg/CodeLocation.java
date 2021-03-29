package it.unive.lisa.program.cfg;

/**
 * A generic interface for representing the location of an element in the source
 * code (e.g., source/line/column, source/offset, ...).
 * 
 * @author <a href="mailto:vincenzo.arceri@unive.it">VincenzoArceri</a>
 */
public interface CodeLocation extends Comparable<CodeLocation> {

	/**
	 * Yields the string code location representation.
	 * 
	 * @return the string code location representation
	 */
	String getCodeLocation();
}

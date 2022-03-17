package it.unive.lisa.interprocedural;

import it.unive.lisa.util.datastructures.graph.algorithms.FixpointException;

public class NoEntryPointException extends FixpointException {

	private static final long serialVersionUID = -7989565407659746161L;

	public NoEntryPointException() {
		super("The program contains no entry points for the analysis");
	}
}

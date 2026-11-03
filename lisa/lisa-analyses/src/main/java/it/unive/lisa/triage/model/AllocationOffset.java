package it.unive.lisa.triage.model;

import it.unive.lisa.program.cfg.CodeLocation;

/**
 * A {@link FreshValue} modeling how many references have been allocated by
 * code that is not modeled (e.g., by the callees of a summarized call), that
 * is, how much the counter of allocations of the {@link HeapModel} grows.
 * Unlike {@link SummaryValue}s, these values are not a source of imprecision:
 * the identifiers of the references allocated afterwards only need to differ
 * from the existing ones, and any non-negative offset yields the same
 * executions up to a renaming of references. Unlike {@link InputValue}s, they
 * are not inputs of the program.
 *
 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
 */
public class AllocationOffset
		extends
		FreshValue {

	/**
	 * Builds the value.
	 *
	 * @param location the location of the code allocating the references
	 * @param index    a number that uniquely identifies the value among the
	 *                     fresh values of a model
	 */
	public AllocationOffset(
			CodeLocation location,
			long index) {
		super(HeapType.COUNTER, "$allocated#" + index, location);
	}
}

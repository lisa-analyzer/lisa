package it.unive.lisa.triage.model;

import it.unive.lisa.triage.encoding.CellKeySort;
import it.unive.lisa.triage.encoding.ValueSort;
import it.unive.lisa.triage.smt.Sort;
import it.unive.lisa.type.Type;
import it.unive.lisa.type.TypeSystem;
import it.unive.lisa.type.Untyped;
import java.util.Collections;
import java.util.Set;

/**
 * The types of the synthetic values used by alarm models to represent the heap
 * (see {@link HeapModel}). They are never seen by the analysis: they only give
 * a sort to the symbols encoding the heap.
 *
 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
 */
public enum HeapType
		implements
		Type {

	/**
	 * The contents of a field of all objects: a map from the identifiers of
	 * references to values.
	 */
	FIELD(Sort.array(Sort.INT, ValueSort.SORT)),

	/**
	 * The cells of all arrays: a map from the keys of cells (pairs of the
	 * identifier of a reference and an index, see {@link CellKeySort}) to
	 * values.
	 */
	CELLS(Sort.array(CellKeySort.SORT, ValueSort.SORT)),

	/**
	 * Which fields of which objects have been written: a map from the
	 * identifiers of references to booleans.
	 */
	WRITTEN_FIELD(Sort.array(Sort.INT, Sort.BOOL)),

	/**
	 * Which cells of which arrays have been written: a map from the keys of
	 * cells to booleans.
	 */
	WRITTEN_CELLS(Sort.array(CellKeySort.SORT, Sort.BOOL)),

	/**
	 * Whether a global variable has been written.
	 */
	DEFINED(Sort.BOOL),

	/**
	 * The key of a cell of an array.
	 */
	CELL_KEY(CellKeySort.SORT),

	/**
	 * The counter of allocations, that is, the identifier of the next reference
	 * to be allocated.
	 */
	COUNTER(Sort.INT),

	/**
	 * A reference.
	 */
	REFERENCE(ValueSort.SORT);

	private final Sort sort;

	private HeapType(
			Sort sort) {
		this.sort = sort;
	}

	/**
	 * Yields the sort encoding the values of this type.
	 *
	 * @return the sort
	 */
	public Sort getSort() {
		return sort;
	}

	@Override
	public boolean canBeAssignedTo(
			Type other) {
		return other == this || other.isUntyped();
	}

	@Override
	public Type commonSupertype(
			Type other) {
		return other == this ? this : Untyped.INSTANCE;
	}

	@Override
	public Set<Type> allInstances(
			TypeSystem types) {
		return Collections.singleton(this);
	}
}

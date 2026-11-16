package it.unive.lisa.triage.encoding;

import it.unive.lisa.triage.smt.Datatype;
import it.unive.lisa.triage.smt.Datatype.Field;
import it.unive.lisa.triage.smt.Sort;
import it.unive.lisa.triage.smt.Term;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * The SMT datatype of the keys of the cells of arrays: pairs of the identifier
 * of the reference to an array and an index. Keying the cells of all arrays by
 * such pairs lets each write of a cell mention the cells once, so that the
 * encoding of many writes grows linearly.
 *
 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
 */
public final class CellKeySort {

	/**
	 * The name of the constructor of keys.
	 */
	public static final String KEY = "CellKey.key";

	/**
	 * The datatype.
	 */
	public static final Datatype DATATYPE;

	static {
		Map<String, List<Field>> constructors = new LinkedHashMap<>();
		constructors.put(KEY, List.of(new Field("CellKey.ref", Sort.INT), new Field("CellKey.index", Sort.INT)));
		DATATYPE = new Datatype("CellKey", constructors);
	}

	/**
	 * The sort of keys.
	 */
	public static final Sort SORT = DATATYPE.getSort();

	private CellKeySort() {
	}

	/**
	 * Yields the key of the cell at the given index of the array referenced by
	 * the reference with the given identifier.
	 *
	 * @param reference the identifier of the reference, an integer term
	 * @param index     the index, an integer term
	 *
	 * @return the key
	 */
	public static Term key(
			Term reference,
			Term index) {
		return DATATYPE.construct(KEY, reference, index);
	}
}

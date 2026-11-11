package it.unive.lisa.triage.bmc;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * The values of the functions that the encoder can compute on concrete values
 * (see
 * {@link it.unive.lisa.triage.encoding.SymbolTable#evaluable(String, it.unive.lisa.triage.smt.Sort, List, java.util.function.Function, it.unive.lisa.symbolic.SymbolicExpression)}),
 * learned from the models of the solver: when the value of an application of
 * such a function in a model is not the one of the function, the right value
 * for the arguments of the model is learned, so that the following encodings
 * (see {@link Unrolling#setEvaluatedFunctions(EvaluatedFunctions)}) give the
 * function its value on those arguments. Arguments and values are kept as
 * literals in SMT-LIB syntax.
 */
public class EvaluatedFunctions {

	private final Map<String, Map<List<String>, String>> values = new LinkedHashMap<>();

	private int size;

	/**
	 * Yields the known values of the given function, by their arguments.
	 *
	 * @param function the function
	 *
	 * @return the values
	 */
	public Map<List<String>, String> valuesOf(
			String function) {
		Map<List<String>, String> known = values.get(function);
		return known == null ? Collections.emptyMap() : Collections.unmodifiableMap(known);
	}

	/**
	 * Records the value of the given function on the given arguments.
	 *
	 * @param function  the function
	 * @param arguments the arguments
	 * @param value     the value
	 *
	 * @return {@code true} if the value was not known
	 */
	public boolean add(
			String function,
			List<String> arguments,
			String value) {
		if (values.computeIfAbsent(function, f -> new LinkedHashMap<>()).putIfAbsent(List.copyOf(arguments),
				value) != null)
			return false;
		size++;
		return true;
	}

	/**
	 * Yields the number of known values, of all the functions.
	 *
	 * @return the number of values
	 */
	public int size() {
		return size;
	}
}

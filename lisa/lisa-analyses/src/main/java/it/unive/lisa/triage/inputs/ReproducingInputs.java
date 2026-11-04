package it.unive.lisa.triage.inputs;

import it.unive.lisa.checks.verification.ReproducingInput;
import it.unive.lisa.interprocedural.ScopeId;
import it.unive.lisa.triage.encoding.ValueSort;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * A builder of the {@link ReproducingInput}s of a counterexample, that collects
 * the values of input sources in the order they are consumed by the execution,
 * numbering the occurrences of each source. Values are given in SMT-LIB syntax,
 * as printed by a solver, and are made readable through
 * {@link ValueSort#display(String)}.
 *
 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
 */
public class ReproducingInputs {

	private final ScopeId<?> context;

	private final List<ReproducingInput> inputs = new ArrayList<>();

	private final Map<InputSource, Integer> occurrences = new LinkedHashMap<>();

	/**
	 * Builds the builder.
	 *
	 * @param context the context where the inputs are consumed
	 */
	public ReproducingInputs(
			ScopeId<?> context) {
		this.context = context;
	}

	/**
	 * Adds the next value consumed from the given source.
	 *
	 * @param source the source
	 * @param value  the value, in SMT-LIB syntax
	 */
	public void add(
			InputSource source,
			String value) {
		int occurrence = occurrences.merge(source, 1, Integer::sum) - 1;
		inputs.add(source.toReproducingInput(context, occurrence, ValueSort.display(value)));
	}

	/**
	 * Yields the inputs collected so far.
	 *
	 * @return the inputs
	 */
	public List<ReproducingInput> get() {
		return Collections.unmodifiableList(inputs);
	}
}

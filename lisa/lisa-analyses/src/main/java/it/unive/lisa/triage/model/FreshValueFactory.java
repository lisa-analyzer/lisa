package it.unive.lisa.triage.model;

import it.unive.lisa.program.cfg.CodeLocation;
import it.unive.lisa.program.cfg.ProgramPoint;
import it.unive.lisa.symbolic.value.PushAny;
import it.unive.lisa.triage.inputs.InputSource;
import it.unive.lisa.triage.inputs.InputSourceResolver;
import it.unive.lisa.type.Type;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * A factory of {@link FreshValue}s, ensuring that each of them is uniquely
 * named. Unknown values introduced by the program are resolved to their
 * {@link InputSource}s through an {@link InputSourceResolver}.
 *
 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
 */
public class FreshValueFactory {

	private final InputSourceResolver resolver;

	private final HeapModel heap;

	private final Map<String, FreshValue> values;

	private long next;

	/**
	 * Builds the factory.
	 *
	 * @param resolver the resolver used to find the sources of the unknown
	 *                     values introduced by the program
	 */
	public FreshValueFactory(
			InputSourceResolver resolver) {
		this(resolver, new HeapModel());
	}

	/**
	 * Builds the factory.
	 *
	 * @param resolver the resolver used to find the sources of the unknown
	 *                     values introduced by the program
	 * @param heap     the heap model of the program, telling which fields are
	 *                     immutable
	 */
	public FreshValueFactory(
			InputSourceResolver resolver,
			HeapModel heap) {
		this.resolver = resolver;
		this.heap = heap;
		this.values = new LinkedHashMap<>();
		this.next = 0;
	}

	/**
	 * Yields the heap model of the program.
	 *
	 * @return the heap model
	 */
	public HeapModel getHeapModel() {
		return heap;
	}

	/**
	 * Yields a fresh value for the given unknown value, introduced at the given
	 * program point: an {@link InputValue} if the unknown value is an input of
	 * the program (a parameter, or the result of a call), or a
	 * {@link SummaryValue} if it is a {@link SummaryMarker}, the result of a
	 * call that is not a genuine input
	 * ({@link InputSource.Kind#IMPRECISE_CALL_RESULT}), or an unknown value of
	 * other origin ({@link InputSource.Kind#UNKNOWN}), that approximates the
	 * code producing it.
	 *
	 * @param value the unknown value
	 * @param pp    the program point where the value is introduced (can be
	 *                  {@code null})
	 *
	 * @return the fresh value
	 */
	public synchronized FreshValue fresh(
			PushAny value,
			ProgramPoint pp) {
		if (value instanceof SummaryMarker)
			return summary(value.getStaticType(), ((SummaryMarker) value).getKind(),
					((SummaryMarker) value).getReason(),
					value.getCodeLocation());
		InputSource source = resolver.resolve(value, pp);
		if (source.getKind() == InputSource.Kind.IMPRECISE_CALL_RESULT)
			return summary(value.getStaticType(), SummaryValue.Kind.CALL_RESULT, "imprecise " + source.getDescription(),
					value.getCodeLocation());
		if (source.getKind() == InputSource.Kind.UNKNOWN)
			// unknown values that are neither parameters nor results of calls
			// are approximations of the code producing them, not inputs
			return summary(value.getStaticType(), SummaryValue.Kind.UNSUPPORTED, source.getDescription(),
					value.getCodeLocation());
		InputValue input = new InputValue(source, next++);
		values.put(input.getName(), input);
		return input;
	}

	/**
	 * Yields a fresh {@link SummaryValue}.
	 *
	 * @param type     the static type of the value
	 * @param kind     the kind of imprecision the value comes from
	 * @param reason   a human-readable description of why the value is not
	 *                     computed precisely
	 * @param location the location where the value is produced
	 *
	 * @return the fresh value
	 */
	public synchronized SummaryValue summary(
			Type type,
			SummaryValue.Kind kind,
			String reason,
			CodeLocation location) {
		SummaryValue summary = new SummaryValue(type, kind, reason, location, next++);
		values.put(summary.getName(), summary);
		return summary;
	}

	/**
	 * Yields a fresh {@link AllocationOffset}.
	 *
	 * @param location the location of the code allocating the references
	 *
	 * @return the fresh value
	 */
	public synchronized AllocationOffset allocationOffset(
			CodeLocation location) {
		AllocationOffset offset = new AllocationOffset(location, next++);
		values.put(offset.getName(), offset);
		return offset;
	}

	/**
	 * Yields all the fresh values created by this factory, indexed by name.
	 *
	 * @return the fresh values
	 */
	public synchronized Map<String, FreshValue> getValues() {
		return Collections.unmodifiableMap(new LinkedHashMap<>(values));
	}
}

package it.unive.lisa.witness.v2;

import it.unive.lisa.witness.v2.correctness.FunctionContract;
import it.unive.lisa.witness.v2.correctness.Invariant;
import it.unive.lisa.witness.v2.correctness.InvariantSet;
import it.unive.lisa.witness.v2.correctness.InvariantSetItem;
import it.unive.lisa.witness.v2.correctness.InvariantType;
import it.unive.lisa.witness.v2.correctness.Label;
import it.unive.lisa.witness.v2.correctness.ghost.Expression;
import it.unive.lisa.witness.v2.correctness.ghost.GhostAssignment;
import it.unive.lisa.witness.v2.correctness.ghost.GhostContent;
import it.unive.lisa.witness.v2.correctness.ghost.GhostInstrumentation;
import it.unive.lisa.witness.v2.correctness.ghost.GhostScope;
import it.unive.lisa.witness.v2.correctness.ghost.GhostUpdate;
import it.unive.lisa.witness.v2.correctness.ghost.GhostVariable;
import it.unive.lisa.witness.v2.metadata.Metadata;
import it.unive.lisa.witness.v2.metadata.Producer;
import it.unive.lisa.witness.v2.metadata.Task;
import it.unive.lisa.witness.v2.metadata.TaskLoader;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

// an expression gets the extended format of the language only if it uses an extension keyword
public final class CorrectnessWitnessBuilder
		extends
		WitnessBuilder {

	private final List<InvariantSetItem> items = new ArrayList<>();

	private final List<GhostVariable> ghostVariables = new ArrayList<>();

	// the assignments of each location, in the order of the calls
	private final Map<Location, List<GhostAssignment>> ghostUpdates = new LinkedHashMap<>();

	public CorrectnessWitnessBuilder(
			Task task,
			Producer producer) {
		super(task, producer);
	}

	public static CorrectnessWitnessBuilder forInputs(
			List<Path> inputs,
			Path propertyFile,
			Language language,
			DataModel dataModel,
			Producer producer)
			throws IOException {
		return new CorrectnessWitnessBuilder(TaskLoader.load(inputs, propertyFile, language, dataModel), producer);
	}

	public CorrectnessWitnessBuilder locationInvariant(
			Location location,
			String value) {
		return invariant(InvariantType.LOCATION_INVARIANT, location, value);
	}

	public CorrectnessWitnessBuilder loopInvariant(
			Location location,
			String value) {
		return invariant(InvariantType.LOOP_INVARIANT, location, value);
	}

	public CorrectnessWitnessBuilder invariant(
			InvariantType type,
			Location location,
			String value,
			Label... labels) {
		return item(new Invariant(type, location, value, task.getLanguage().getFormatFor(value), Set.of(labels)));
	}

	public CorrectnessWitnessBuilder contract(
			Location location,
			String requires,
			String ensures) {
		return item(new FunctionContract(location, requires, ensures,
				task.getLanguage().getFormatFor(requires, ensures), null));
	}

	public CorrectnessWitnessBuilder item(
			InvariantSetItem item) {
		Entry.checkLocation(task, Objects.requireNonNull(item, "item").getLocation());
		item.check(task);
		items.add(item);
		return this;
	}

	// ghost code only uses c_expression: the witness rejects it for the other
	// languages
	public CorrectnessWitnessBuilder ghostVariable(
			String name,
			String type,
			String initial) {
		checkGhost();
		for (GhostVariable declared : ghostVariables)
			if (declared.getName().equals(name))
				throw new InvalidWitnessException("The ghost variable " + name + " is declared twice");
		ghostVariables.add(new GhostVariable(name, type, GhostScope.GLOBAL,
				new Expression(initial, Format.C_EXPRESSION)));
		return this;
	}

	// assignments at the same location form a single update
	public CorrectnessWitnessBuilder ghostUpdate(
			Location location,
			String variable,
			String value) {
		checkGhost();
		Entry.checkLocation(task, Objects.requireNonNull(location, "location"));
		ghostUpdates.computeIfAbsent(location, l -> new ArrayList<>())
				.add(new GhostAssignment(variable, value, Format.C_EXPRESSION));
		return this;
	}

	private void checkGhost() {
		if (!task.getLanguage().supportsGhost())
			throw new InvalidWitnessException("Ghost instrumentation is not supported for " + task.getLanguage());
	}

	// both entries share the same metadata, as in the official examples
	public CorrectnessWitness build() {
		Metadata metadata = Metadata.create(producer, task);
		InvariantSet invariants = new InvariantSet(metadata, items);
		if (ghostVariables.isEmpty() && ghostUpdates.isEmpty())
			return new CorrectnessWitness(invariants);
		List<GhostUpdate> updates = new ArrayList<>();
		for (Map.Entry<Location, List<GhostAssignment>> update : ghostUpdates.entrySet())
			updates.add(new GhostUpdate(update.getKey(), update.getValue()));
		return new CorrectnessWitness(invariants,
				new GhostInstrumentation(metadata, new GhostContent(ghostVariables, updates)));
	}
}

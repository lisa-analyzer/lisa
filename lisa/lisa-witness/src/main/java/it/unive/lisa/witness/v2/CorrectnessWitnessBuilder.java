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

/**
 * A builder of correctness witnesses. Invariants and contracts use the plain
 * expression format of the language, unless they use an extension keyword such
 * as {@code \result}, in which case they use the extended one. Ghost code is
 * only allowed for C, and it always uses {@link Format#C_EXPRESSION}. When the
 * witness has ghost code, its two entries share the same metadata.
 * 
 * @author <a href="mailto:giacomo.zanatta@unive.it">Giacomo Zanatta</a>
 */
public final class CorrectnessWitnessBuilder
		extends
		WitnessBuilder {

	/**
	 * The invariants and contracts of the witness, in the order they are added.
	 */
	private final List<InvariantSetItem> items = new ArrayList<>();

	/**
	 * The ghost variables of the witness, in the order they are declared.
	 */
	private final List<GhostVariable> ghostVariables = new ArrayList<>();

	/**
	 * The ghost assignments of each location, in the order they are added.
	 */
	private final Map<Location, List<GhostAssignment>> ghostUpdates = new LinkedHashMap<>();

	/**
	 * Builds the builder of a correctness witness for the given task.
	 * 
	 * @param task     the task of the witness
	 * @param producer the tool producing the witness
	 */
	public CorrectnessWitnessBuilder(
			Task task,
			Producer producer) {
		super(task, producer);
	}

	/**
	 * Builds the builder of a correctness witness for the task made of the
	 * given input files and property file. The input files are read and hashed
	 * by {@link TaskLoader}.
	 * 
	 * @param inputs       the input files of the task, as given to the verifier
	 * @param propertyFile the file containing the specification of the task
	 * @param language     the language of the task
	 * @param dataModel    the data model of the task, or {@code null} if the
	 *                         language does not need one
	 * @param producer     the tool producing the witness
	 * 
	 * @return the builder
	 * 
	 * @throws IOException if an input file or the property file cannot be read
	 */
	public static CorrectnessWitnessBuilder forInputs(
			List<Path> inputs,
			Path propertyFile,
			Language language,
			DataModel dataModel,
			Producer producer)
			throws IOException {
		return new CorrectnessWitnessBuilder(TaskLoader.load(inputs, propertyFile, language, dataModel), producer);
	}

	/**
	 * Adds a location invariant, holding every time the location is reached.
	 * 
	 * @param location the location of the invariant
	 * @param value    the invariant
	 * 
	 * @return this builder
	 * 
	 * @throws InvalidWitnessException if the location is not in an input file
	 *                                     of the task, or if the invariant
	 *                                     breaks a rule of the format
	 */
	public CorrectnessWitnessBuilder locationInvariant(
			Location location,
			String value) {
		return invariant(InvariantType.LOCATION_INVARIANT, location, value);
	}

	/**
	 * Adds a loop invariant, holding at the beginning of every iteration of the
	 * loop at the location.
	 * 
	 * @param location the location of the loop
	 * @param value    the invariant
	 * 
	 * @return this builder
	 * 
	 * @throws InvalidWitnessException if the location is not in an input file
	 *                                     of the task, or if the invariant
	 *                                     breaks a rule of the format
	 */
	public CorrectnessWitnessBuilder loopInvariant(
			Location location,
			String value) {
		return invariant(InvariantType.LOOP_INVARIANT, location, value);
	}

	/**
	 * Adds an invariant of the given type, with the given labels.
	 * 
	 * @param type     the type of the invariant
	 * @param location the location of the invariant
	 * @param value    the invariant
	 * @param labels   the labels of the invariant, such as
	 *                     {@link Label#INDUCTIVE}
	 * 
	 * @return this builder
	 * 
	 * @throws InvalidWitnessException if the location is not in an input file
	 *                                     of the task, or if the invariant
	 *                                     breaks a rule of the format, for
	 *                                     instance a transition invariant for a
	 *                                     language that does not support them
	 */
	public CorrectnessWitnessBuilder invariant(
			InvariantType type,
			Location location,
			String value,
			Label... labels) {
		return item(new Invariant(type, location, value, task.getLanguage().getFormatFor(value), Set.of(labels)));
	}

	/**
	 * Adds a contract of the function at the location.
	 * 
	 * @param location the location of the function
	 * @param requires the precondition of the function, or {@code null} if it
	 *                     is not given
	 * @param ensures  the postcondition of the function, or {@code null} if it
	 *                     is not given
	 * 
	 * @return this builder
	 * 
	 * @throws InvalidWitnessException if the location is not in an input file
	 *                                     of the task, or if the contract
	 *                                     breaks a rule of the format
	 */
	public CorrectnessWitnessBuilder contract(
			Location location,
			String requires,
			String ensures) {
		return item(new FunctionContract(location, requires, ensures,
				task.getLanguage().getFormatFor(requires, ensures), null));
	}

	/**
	 * Adds the given invariant or contract, after checking it against the task.
	 * 
	 * @param item the invariant or contract
	 * 
	 * @return this builder
	 * 
	 * @throws InvalidWitnessException if the location of the item is not in an
	 *                                     input file of the task, or if the
	 *                                     item breaks a rule of the format
	 */
	public CorrectnessWitnessBuilder item(
			InvariantSetItem item) {
		Entry.checkLocation(task, Objects.requireNonNull(item, "item").getLocation());
		item.check(task);
		items.add(item);
		return this;
	}

	/**
	 * Declares a global ghost variable. Ghost code is only allowed for C.
	 * 
	 * @param name    the name of the variable
	 * @param type    the C type of the variable
	 * @param initial the initial value of the variable
	 * 
	 * @return this builder
	 * 
	 * @throws InvalidWitnessException if the language of the task is not C, or
	 *                                     if a variable with the same name is
	 *                                     already declared
	 */
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

	/**
	 * Adds an assignment of a ghost variable at the location. Assignments at
	 * the same location form a single update, in the order they are added. The
	 * variable can be declared later, since declarations are checked when the
	 * witness is built.
	 * 
	 * @param location the location of the update
	 * @param variable the name of the ghost variable
	 * @param value    the value assigned to the variable
	 * 
	 * @return this builder
	 * 
	 * @throws InvalidWitnessException if the language of the task is not C, or
	 *                                     if the location is not in an input
	 *                                     file of the task
	 */
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

	/**
	 * Checks that the language of the task allows ghost code.
	 * 
	 * @throws InvalidWitnessException if the language of the task does not
	 *                                     support ghost code
	 */
	private void checkGhost() {
		if (!task.getLanguage().supportsGhost())
			throw new InvalidWitnessException("Ghost instrumentation is not supported for " + task.getLanguage());
	}

	/**
	 * Builds the witness made of the invariants, contracts and ghost code added
	 * so far, with new metadata. The ghost instrumentation is only added if the
	 * witness has ghost code. Each call builds a new witness, with its own
	 * identifier and creation time.
	 * 
	 * @return the witness
	 * 
	 * @throws InvalidWitnessException if an update assigns an undeclared ghost
	 *                                     variable, or if the witness breaks a
	 *                                     rule of the format
	 */
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

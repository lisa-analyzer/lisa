package it.unive.lisa.witness.v2;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import it.unive.lisa.witness.v2.violation.ViolationSequence;
import java.util.List;

/**
 * An abstract witness, made of the entries of the witness format. Its
 * implementations are {@link ViolationWitness} and {@link CorrectnessWitness}.
 * When a witness is built, each of its entries is checked against its task, for
 * instance that every location is in one of its input files and that every
 * expression format is allowed for its language.
 * 
 * @author <a href="mailto:giacomo.zanatta@unive.it">Giacomo Zanatta</a>
 */
public abstract class Witness {

	/**
	 * The entries of this witness, in the order they are written.
	 */
	private final List<Entry> entries;

	/**
	 * Builds the witness and checks each of its entries against its task.
	 * 
	 * @param entries the entries of the witness
	 * 
	 * @throws InvalidWitnessException if an entry breaks a rule of the format
	 */
	protected Witness(
			List<Entry> entries) {
		this.entries = List.copyOf(entries);
		for (Entry entry : this.entries)
			entry.check();
	}

	/**
	 * Builds the witness made of the given entries. It is used by
	 * {@link WitnessReader}, since the witness format does not say which kind
	 * of witness a list of entries is. A single violation sequence is a
	 * {@link ViolationWitness}, any other list is read as a
	 * {@link CorrectnessWitness}.
	 * 
	 * @param entries the entries read from the YAML text
	 * 
	 * @return the witness made of the given entries
	 * 
	 * @throws InvalidWitnessException if the entries do not form a witness
	 */
	@JsonCreator(mode = JsonCreator.Mode.DELEGATING)
	static Witness of(
			List<Entry> entries) {
		if (entries == null || entries.isEmpty())
			throw new InvalidWitnessException("The witness has no entries");
		if (entries.size() == 1 && entries.get(0).getEntryType() == EntryType.VIOLATION_SEQUENCE)
			return new ViolationWitness((ViolationSequence) entries.get(0));
		return new CorrectnessWitness(entries);
	}

	/**
	 * Returns the entries of this witness, in the order they are written. A
	 * witness is written as this list.
	 * 
	 * @return the entries of this witness
	 */
	@JsonValue
	public final List<Entry> getEntries() {
		return entries;
	}
}

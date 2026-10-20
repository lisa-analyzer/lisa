package it.unive.lisa.witness.v2;

import java.util.List;

public abstract class Witness {

	private final List<Entry> entries;

	protected Witness(
			List<Entry> entries) {
		this.entries = List.copyOf(entries);
		for (Entry entry : this.entries)
			entry.checkLanguage();
	}

	public final List<Entry> getEntries() {
		return entries;
	}
}

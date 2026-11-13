package it.unive.lisa.triage.model;

import it.unive.lisa.program.cfg.CodeMember;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.function.Predicate;

/**
 * The contents of the heap that the code of the members of a program writes, as
 * the frontend knows them: the calls that the models summarize then keep the
 * contents of the heap that their callees cannot write (see {@link HeapModel}),
 * instead of making all of them unknown. The writes of a member are the ones of
 * its own code, excluding the members it calls: these are found through the
 * call graph, as for {@link GlobalWrites}.
 *
 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
 */
@FunctionalInterface
public interface HeapWrites {

	/**
	 * The heap writes of a program that are not known: the calls that the
	 * models summarize might write any content of the heap.
	 */
	HeapWrites UNKNOWN = member -> null;

	/**
	 * Returns the contents of the heap that the code of the given member might
	 * write, excluding the members it calls, or {@code null} if they are not
	 * known. The writes of fields of new objects (e.g., the ones of their
	 * initialization) count as writes of the fields.
	 *
	 * @param member the member
	 *
	 * @return the written contents, or {@code null}
	 */
	Written writtenBy(
			CodeMember member);

	/**
	 * Contents of the heap that some code might write: fields, by the names of
	 * the identifiers of their contents (see {@link HeapModel#field(String)}:
	 * qualified by the classes declaring them, or not), and the cells of
	 * arrays. The fields are told by a set of tests, each telling some fields
	 * that might be written: the contents written by several pieces of code
	 * keep the tests of each of them, so that the union of many contents (e.g.,
	 * the ones of all the members called by a call) does not nest the tests.
	 */
	public static final class Written {

		/**
		 * The contents written by code that does not write the heap.
		 */
		public static final Written NOTHING = new Written(Collections.emptySet(), false);

		private final Set<Predicate<String>> fields;

		private final boolean cells;

		/**
		 * Builds the contents.
		 *
		 * @param fields tells whether a field, by its name, might be written
		 * @param cells  whether the cells of arrays might be written
		 */
		public Written(
				Predicate<String> fields,
				boolean cells) {
			this(Collections.singleton(fields), cells);
		}

		private Written(
				Set<Predicate<String>> fields,
				boolean cells) {
			this.fields = fields;
			this.cells = cells;
		}

		/**
		 * Returns whether the field with the given name (qualified by the class
		 * declaring it, or not) might be written.
		 *
		 * @param name the name of the field
		 *
		 * @return whether the field might be written
		 */
		public boolean writesField(
				String name) {
			for (Predicate<String> test : fields)
				if (test.test(name))
					return true;
			return false;
		}

		/**
		 * Returns whether the cells of arrays might be written.
		 *
		 * @return whether the cells might be written
		 */
		public boolean writesCells() {
			return cells;
		}

		/**
		 * Returns the contents written by the code writing these ones or the
		 * given ones.
		 *
		 * @param other the other contents
		 *
		 * @return the union of the contents
		 */
		public Written union(
				Written other) {
			if (this == NOTHING)
				return other;
			if (other == NOTHING)
				return this;
			// the same tests (e.g., shared by the members of a program) are
			// kept once
			Set<Predicate<String>> union = new LinkedHashSet<>(fields);
			union.addAll(other.fields);
			return new Written(Collections.unmodifiableSet(union), cells || other.cells);
		}
	}
}

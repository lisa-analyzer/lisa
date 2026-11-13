package it.unive.lisa.triage.smt;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * An algebraic datatype of the SMT-LIB language, made of a set of constructors,
 * each with a (possibly empty) list of named fields. Datatypes must be declared
 * through {@link SmtSolver#declareDatatype(Datatype)} before their terms can be
 * used. Terms of a datatype are built and inspected through
 * {@link #construct(String, Term...)}, {@link #test(String, Term)}, and
 * {@link #select(String, Term)}. Note that, as defined by SMT-LIB, selecting a
 * field of a term built with a different constructor yields an unspecified
 * value of the field's sort.
 *
 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
 */
public final class Datatype {

	/**
	 * A field of a constructor.
	 *
	 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
	 */
	public static final class Field {

		private final String name;

		private final Sort sort;

		/**
		 * Builds the field.
		 *
		 * @param name the name of the field, that is also the name of its
		 *                 selector
		 * @param sort the sort of the field
		 */
		public Field(
				String name,
				Sort sort) {
			Objects.requireNonNull(name);
			Objects.requireNonNull(sort);
			this.name = name;
			this.sort = sort;
		}

		/**
		 * Yields the name of this field.
		 *
		 * @return the name
		 */
		public String getName() {
			return name;
		}

		/**
		 * Yields the sort of this field.
		 *
		 * @return the sort
		 */
		public Sort getSort() {
			return sort;
		}
	}

	private final String name;

	private final Sort sort;

	private final Map<String, List<Field>> constructors;

	private final Map<String, Field> fields;

	/**
	 * Builds the datatype.
	 *
	 * @param name         the name of the datatype
	 * @param constructors the constructors of the datatype, mapped to their
	 *                         fields (in order)
	 */
	public Datatype(
			String name,
			Map<String, List<Field>> constructors) {
		Objects.requireNonNull(name);
		if (constructors.isEmpty())
			throw new IllegalArgumentException("A datatype must have at least one constructor");
		this.name = name;
		this.sort = Sort.uninterpreted(name);
		this.constructors = Collections.unmodifiableMap(new LinkedHashMap<>(constructors));
		Map<String, Field> fields = new LinkedHashMap<>();
		for (List<Field> fs : constructors.values())
			for (Field f : fs)
				if (fields.put(f.getName(), f) != null)
					throw new IllegalArgumentException("Duplicate field name in datatype " + name + ": " + f.getName());
		this.fields = Collections.unmodifiableMap(fields);
	}

	/**
	 * Yields the name of this datatype.
	 *
	 * @return the name
	 */
	public String getName() {
		return name;
	}

	/**
	 * Yields the sort of the terms of this datatype.
	 *
	 * @return the sort
	 */
	public Sort getSort() {
		return sort;
	}

	/**
	 * Yields the constructors of this datatype, mapped to their fields.
	 *
	 * @return the constructors
	 */
	public Map<String, List<Field>> getConstructors() {
		return constructors;
	}

	/**
	 * Builds the term obtained by applying the given constructor to the given
	 * arguments.
	 *
	 * @param constructor the name of the constructor
	 * @param args        the arguments, one for each field of the constructor
	 *
	 * @return the term
	 */
	public Term construct(
			String constructor,
			Term... args) {
		List<Field> fs = constructors.get(constructor);
		if (fs == null)
			throw new IllegalArgumentException("Unknown constructor of datatype " + name + ": " + constructor);
		if (fs.size() != args.length)
			throw new IllegalArgumentException("Wrong number of arguments for constructor " + constructor);
		for (int i = 0; i < args.length; i++)
			if (!fs.get(i).getSort().equals(args[i].getSort()))
				throw new IllegalArgumentException("Wrong sort for field " + fs.get(i).getName() + ": "
						+ args[i].getSort());
		return Term.app(Term.symbol(constructor), sort, args);
	}

	/**
	 * Builds the boolean term that holds if the given term has been built with
	 * the given constructor.
	 *
	 * @param constructor the name of the constructor
	 * @param term        the term
	 *
	 * @return the boolean term
	 * 
	 * @throws IllegalArgumentException if the given constructor is not a
	 *                                      constructor of this datatype
	 */
	public Term test(
			String constructor,
			Term term) {
		if (!constructors.containsKey(constructor))
			throw new IllegalArgumentException("Unknown constructor of datatype " + name + ": " + constructor);
		if (term.getSort().equals(sort))
			// terms built by a constructor are decided right away
			for (String c : constructors.keySet()) {
				String symbol = Term.symbol(c);
				String smtlib = term.toSmtLib();
				if (smtlib.equals(symbol) || smtlib.startsWith("(" + symbol + " "))
					return c.equals(constructor) ? Term.TRUE : Term.FALSE;
			}
		return Term.app("(_ is " + Term.symbol(constructor) + ")", Sort.BOOL, term);
	}

	/**
	 * Builds the term selecting the given field of the given term. If the term
	 * has not been built with the constructor owning the field, the result is
	 * an unspecified value of the field's sort.
	 *
	 * @param field the name of the field
	 * @param term  the term
	 *
	 * @return the selected term
	 */
	public Term select(
			String field,
			Term term) {
		Field f = fields.get(field);
		if (f == null)
			throw new IllegalArgumentException("Unknown field of datatype " + name + ": " + field);
		return Term.app(Term.symbol(field), f.getSort(), term);
	}

	/**
	 * Yields the SMT-LIB command declaring this datatype.
	 *
	 * @return the declaration
	 */
	public String toSmtLibDeclaration() {
		StringBuilder sb = new StringBuilder("(declare-datatypes ((").append(sort.toSmtLib()).append(" 0)) ((");
		boolean first = true;
		for (Map.Entry<String, List<Field>> ctor : constructors.entrySet()) {
			if (!first)
				sb.append(" ");
			first = false;
			sb.append("(").append(Term.symbol(ctor.getKey()));
			for (Field f : ctor.getValue())
				sb.append(" (").append(Term.symbol(f.getName())).append(" ").append(f.getSort().toSmtLib())
						.append(")");
			sb.append(")");
		}
		return sb.append(")))").toString();
	}

	@Override
	public String toString() {
		return name;
	}
}

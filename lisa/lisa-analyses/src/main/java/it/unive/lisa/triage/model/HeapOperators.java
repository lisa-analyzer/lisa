package it.unive.lisa.triage.model;

import it.unive.lisa.symbolic.value.operator.binary.BinaryOperator;
import it.unive.lisa.symbolic.value.operator.ternary.TernaryOperator;
import it.unive.lisa.symbolic.value.operator.unary.UnaryOperator;
import it.unive.lisa.type.Type;
import it.unive.lisa.type.TypeSystem;
import it.unive.lisa.type.Untyped;
import java.util.Collections;
import java.util.Set;

/**
 * The operators used by alarm models to read and write the heap (see
 * {@link HeapModel}). They are never seen by the analysis: they only appear in
 * the symbolic transitions of alarm models, and they are encoded as reads
 * ({@code select}) and writes ({@code store}) of SMT arrays.
 *
 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
 */
public final class HeapOperators {

	private HeapOperators() {
	}

	/**
	 * The map obtained by writing, in a map (the left operand), a value (the
	 * right operand) at a key (the middle operand, a reference for the contents
	 * of fields, or the key of a cell for the cells of arrays).
	 *
	 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
	 */
	public static final class ReferenceStore
			implements
			TernaryOperator {

		/**
		 * The singleton instance of this class.
		 */
		public static final ReferenceStore INSTANCE = new ReferenceStore();

		private ReferenceStore() {
		}

		@Override
		public Set<Type> typeInference(
				TypeSystem types,
				Set<Type> left,
				Set<Type> middle,
				Set<Type> right) {
			return Collections.singleton(Untyped.INSTANCE);
		}

		@Override
		public String toString() {
			return "store";
		}
	}

	/**
	 * The value of a map (the left operand) at a key (the right operand, a
	 * reference for the contents of fields, or the key of a cell for the cells
	 * of arrays), as read by the program: the middle operand tells which
	 * locations have been written, and the read is precise only if the location
	 * has been written.
	 *
	 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
	 */
	public static final class ReferenceRead
			implements
			TernaryOperator {

		/**
		 * The singleton instance of this class.
		 */
		public static final ReferenceRead INSTANCE = new ReferenceRead();

		private ReferenceRead() {
		}

		@Override
		public Set<Type> typeInference(
				TypeSystem types,
				Set<Type> left,
				Set<Type> middle,
				Set<Type> right) {
			return Collections.singleton(Untyped.INSTANCE);
		}

		@Override
		public String toString() {
			return "read";
		}
	}

	/**
	 * The key of the cell of an array (see
	 * {@link it.unive.lisa.triage.encoding.CellKeySort}) at an index (the right
	 * operand) of the array referenced by a reference (the left operand).
	 *
	 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
	 */
	public static final class CellKey
			implements
			BinaryOperator {

		/**
		 * The singleton instance of this class.
		 */
		public static final CellKey INSTANCE = new CellKey();

		private CellKey() {
		}

		@Override
		public Set<Type> typeInference(
				TypeSystem types,
				Set<Type> left,
				Set<Type> right) {
			return Collections.singleton(Untyped.INSTANCE);
		}

		@Override
		public String toString() {
			return "cell";
		}
	}

	/**
	 * Whether a location has been written: the left operand is a map of written
	 * locations (see {@link ReferenceRead}), and the right one is the key of
	 * the location (a reference for the contents of fields, or the key of a
	 * cell for the cells of arrays).
	 *
	 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
	 */
	public static final class WrittenAt
			implements
			BinaryOperator {

		/**
		 * The singleton instance of this class.
		 */
		public static final WrittenAt INSTANCE = new WrittenAt();

		private WrittenAt() {
		}

		@Override
		public Set<Type> typeInference(
				TypeSystem types,
				Set<Type> left,
				Set<Type> right) {
			return Collections.singleton(types.getBooleanType());
		}

		@Override
		public String toString() {
			return "written";
		}
	}

	/**
	 * The reference whose identifier is the operand (an integer).
	 *
	 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
	 */
	public static final class NewReference
			implements
			UnaryOperator {

		/**
		 * The singleton instance of this class.
		 */
		public static final NewReference INSTANCE = new NewReference();

		private NewReference() {
		}

		@Override
		public Set<Type> typeInference(
				TypeSystem types,
				Set<Type> argument) {
			return Collections.singleton(Untyped.INSTANCE);
		}

		@Override
		public String toString() {
			return "ref";
		}
	}
}

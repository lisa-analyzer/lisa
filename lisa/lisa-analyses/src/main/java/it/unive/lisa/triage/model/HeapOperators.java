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
	 * The value of a map (the left operand, of {@link HeapType#FIELD} or
	 * {@link HeapType#CELLS} type) at a reference (the right operand).
	 *
	 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
	 */
	public static final class ReferenceSelect
			implements
			BinaryOperator {

		/**
		 * The singleton instance of this class.
		 */
		public static final ReferenceSelect INSTANCE = new ReferenceSelect();

		private ReferenceSelect() {
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
			return "select";
		}
	}

	/**
	 * The value of the cells of an array (the left operand, of
	 * {@link HeapType#FIELD} type) at an index (the right operand).
	 *
	 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
	 */
	public static final class IndexSelect
			implements
			BinaryOperator {

		/**
		 * The singleton instance of this class.
		 */
		public static final IndexSelect INSTANCE = new IndexSelect();

		private IndexSelect() {
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
			return "select-index";
		}
	}

	/**
	 * The map obtained by writing, in a map (the left operand), a value (the
	 * right operand) at a reference (the middle operand).
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
	 * The cells obtained by writing, in the cells of an array (the left
	 * operand), a value (the right operand) at an index (the middle operand).
	 *
	 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
	 */
	public static final class IndexStore
			implements
			TernaryOperator {

		/**
		 * The singleton instance of this class.
		 */
		public static final IndexStore INSTANCE = new IndexStore();

		private IndexStore() {
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
			return "store-index";
		}
	}

	/**
	 * The value of a map (the left operand) at a reference (the right
	 * operand), as read by the program: the middle operand tells which
	 * locations have been written, and the read is precise only if the
	 * location has been written.
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
	 * The value of the cells of an array (the left operand) at an index (the
	 * right operand), as read by the program: the middle operand tells which
	 * cells have been written, and the read is precise only if the cell has
	 * been written.
	 *
	 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
	 */
	public static final class IndexRead
			implements
			TernaryOperator {

		/**
		 * The singleton instance of this class.
		 */
		public static final IndexRead INSTANCE = new IndexRead();

		private IndexRead() {
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
			return "read-index";
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

package it.unive.lisa.analysis.heap;

import it.unive.lisa.analysis.Lattice;
import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.analysis.SemanticOracle;
import it.unive.lisa.analysis.lattices.ExpressionSet;
import it.unive.lisa.analysis.lattices.Satisfiability;
import it.unive.lisa.program.cfg.ProgramPoint;
import it.unive.lisa.symbolic.SymbolicExpression;
import it.unive.lisa.symbolic.heap.AccessChild;
import it.unive.lisa.symbolic.heap.HeapDereference;
import it.unive.lisa.symbolic.heap.HeapExpression;
import it.unive.lisa.symbolic.heap.HeapReference;
import it.unive.lisa.symbolic.heap.MemoryAllocation;
import it.unive.lisa.symbolic.value.HeapLocation;
import it.unive.lisa.symbolic.value.Identifier;
import it.unive.lisa.symbolic.value.MemoryPointer;
import it.unive.lisa.type.ReferenceType;
import it.unive.lisa.type.Type;
import it.unive.lisa.type.Untyped;
import it.unive.lisa.util.representation.StringRepresentation;
import it.unive.lisa.util.representation.StructuredRepresentation;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;

/**
 * A monolithic heap implementation that abstracts all heap locations to a
 * unique identifier.
 * 
 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
 */
public class MonolithicHeap implements BaseHeapDomain<MonolithicHeap> {

	private static final MonolithicHeap TOP = new MonolithicHeap();

	private static final MonolithicHeap BOTTOM = new MonolithicHeap();

	private static final String MONOLITH_NAME = "heap";

	private static final StructuredRepresentation REPR = new StringRepresentation("monolith");

	@Override
	public ExpressionSet rewrite(
			SymbolicExpression expression,
			ProgramPoint pp,
			SemanticOracle oracle)
			throws SemanticException {
		return expression.accept(Rewriter.SINGLETON, pp);
	}

	@Override
	public List<HeapReplacement> getSubstitution() {
		return Collections.emptyList();
	}

	@Override
	public MonolithicHeap assign(
			Identifier id,
			SymbolicExpression expression,
			ProgramPoint pp,
			SemanticOracle oracle)
			throws SemanticException {
		return this;
	}

	@Override
	public MonolithicHeap mk(
			MonolithicHeap reference) {
		return TOP;
	}

	@Override
	public MonolithicHeap mk(
			MonolithicHeap reference,
			List<HeapReplacement> replacements) {
		return TOP;
	}

	@Override
	public MonolithicHeap semanticsOf(
			HeapExpression expression,
			ProgramPoint pp,
			SemanticOracle oracle) {
		return this;
	}

	@Override
	public MonolithicHeap assume(
			SymbolicExpression expression,
			ProgramPoint src,
			ProgramPoint dest,
			SemanticOracle oracle)
			throws SemanticException {
		return this;
	}

	@Override
	public MonolithicHeap forgetIdentifier(
			Identifier id)
			throws SemanticException {
		return this;
	}

	@Override
	public MonolithicHeap forgetIdentifiersIf(
			Predicate<Identifier> test)
			throws SemanticException {
		return this;
	}

	@Override
	public MonolithicHeap lubAux(
			MonolithicHeap other)
			throws SemanticException {
		return TOP;
	}

	@Override
	public MonolithicHeap glbAux(
			MonolithicHeap other)
			throws SemanticException {
		return TOP;
	}

	@Override
	public boolean lessOrEqualAux(
			MonolithicHeap other)
			throws SemanticException {
		return true;
	}

	@Override
	public MonolithicHeap top() {
		return TOP;
	}

	@Override
	public MonolithicHeap bottom() {
		return BOTTOM;
	}

	@Override
	public StructuredRepresentation representation() {
		return isBottom() ? Lattice.bottomRepresentation() : REPR;
	}

	@Override
	public int hashCode() {
		return MonolithicHeap.class.hashCode();
	}

	@Override
	public boolean equals(
			Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		MonolithicHeap other = (MonolithicHeap) obj;
		return isTop() == other.isTop() && isBottom() == other.isBottom();
	}

	/**
	 * A {@link it.unive.lisa.analysis.heap.BaseHeapDomain.Rewriter} for the
	 * {@link MonolithicHeap} domain.
	 * 
	 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
	 */
	public static class Rewriter extends BaseHeapDomain.Rewriter {

		/**
		 * The singleton instance of this rewriter.
		 */
		public static final Rewriter SINGLETON = new Rewriter();

		@Override
		public ExpressionSet visit(
				AccessChild expression,
				ExpressionSet receiver,
				ExpressionSet child,
				Object... params)
				throws SemanticException {
			// any expression accessing an area of the heap or instantiating a
			// new one is modeled through the monolith
			Set<Type> acc = new HashSet<>();
			child.forEach(e -> acc.add(e.getStaticType()));
			Type refType = Type.commonSupertype(acc, Untyped.INSTANCE);

			HeapLocation e = new HeapLocation(refType, MONOLITH_NAME, true,
					expression.getCodeLocation());
			return new ExpressionSet(e);
		}

		@Override
		public ExpressionSet visit(
				MemoryAllocation expression,
				Object... params)
				throws SemanticException {
			// any expression accessing an area of the heap or instantiating a
			// new one is modeled through the monolith
			HeapLocation e = new HeapLocation(expression.getStaticType(), MONOLITH_NAME, true,
					expression.getCodeLocation());
			return new ExpressionSet(e);
		}

		@Override
		public ExpressionSet visit(
				HeapReference expression,
				ExpressionSet ref,
				Object... params)
				throws SemanticException {
			// any expression accessing an area of the heap or instantiating a
			// new one is modeled through the monolith
			Set<Type> acc = new HashSet<>();
			ref.forEach(e -> acc.add(e.getStaticType()));
			Type refType = Type.commonSupertype(acc, Untyped.INSTANCE);

			HeapLocation loc = new HeapLocation(refType, MONOLITH_NAME, true,
					expression.getCodeLocation());
			MemoryPointer e = new MemoryPointer(new ReferenceType(refType), loc, expression.getCodeLocation());
			return new ExpressionSet(e);
		}

		@Override
		public ExpressionSet visit(
				HeapDereference expression,
				ExpressionSet deref,
				Object... params)
				throws SemanticException {
			// any expression accessing an area of the heap or instantiating a
			// new one is modeled through the monolith
			return deref;
		}
	}

	@Override
	public boolean knowsIdentifier(
			Identifier id) {
		return false;
	}

	@Override
	public Satisfiability alias(
			SymbolicExpression x,
			SymbolicExpression y,
			ProgramPoint pp,
			SemanticOracle oracle)
			throws SemanticException {
		return Satisfiability.UNKNOWN;
	}

	@Override
	public Satisfiability isReachableFrom(
			SymbolicExpression x,
			SymbolicExpression y,
			ProgramPoint pp,
			SemanticOracle oracle)
			throws SemanticException {
		return Satisfiability.UNKNOWN;
	}
}

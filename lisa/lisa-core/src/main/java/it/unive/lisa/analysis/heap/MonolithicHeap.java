package it.unive.lisa.analysis.heap;

import it.unive.lisa.analysis.Lattice;
import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.analysis.lattices.ExpressionSet;
import it.unive.lisa.analysis.representation.DomainRepresentation;
import it.unive.lisa.analysis.representation.StringRepresentation;
import it.unive.lisa.program.cfg.ProgramPoint;
import it.unive.lisa.symbolic.SymbolicExpression;
import it.unive.lisa.symbolic.heap.AccessChild;
import it.unive.lisa.symbolic.heap.HeapAllocation;
import it.unive.lisa.symbolic.heap.HeapDereference;
import it.unive.lisa.symbolic.heap.HeapExpression;
import it.unive.lisa.symbolic.heap.HeapReference;
import it.unive.lisa.symbolic.value.HeapLocation;
import it.unive.lisa.symbolic.value.Identifier;
import it.unive.lisa.symbolic.value.MemoryPointer;
import it.unive.lisa.symbolic.value.ValueExpression;
import it.unive.lisa.type.ReferenceType;
import it.unive.lisa.type.Type;
import it.unive.lisa.type.Untyped;
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
public class MonolithicHeap extends BaseHeapDomain<MonolithicHeap> {

	private static final MonolithicHeap TOP = new MonolithicHeap();

	private static final MonolithicHeap BOTTOM = new MonolithicHeap();

	private static final String MONOLITH_NAME = "heap";

	private static final DomainRepresentation REPR = new StringRepresentation("monolith");

	@Override
	public ExpressionSet<ValueExpression> rewrite(SymbolicExpression expression, ProgramPoint pp)
			throws SemanticException {
		return expression.accept(new Rewriter(), pp);
	}

	@Override
	public List<HeapReplacement> getSubstitution() {
		return Collections.emptyList();
	}

	@Override
	public MonolithicHeap assign(Identifier id, SymbolicExpression expression, ProgramPoint pp)
			throws SemanticException {
		return this;
	}

	@Override
	public MonolithicHeap mk(MonolithicHeap reference) {
		return TOP;
	}

	@Override
	public MonolithicHeap semanticsOf(HeapExpression expression, ProgramPoint pp) {
		return this;
	}

	@Override
	public MonolithicHeap assume(SymbolicExpression expression, ProgramPoint pp) throws SemanticException {
		return this;
	}

	@Override
	public Satisfiability satisfies(SymbolicExpression expression, ProgramPoint pp) throws SemanticException {
		// we leave the decision to the value domain
		return Satisfiability.UNKNOWN;
	}

	@Override
	public MonolithicHeap forgetIdentifier(Identifier id) throws SemanticException {
		return this;
	}

	@Override
	public MonolithicHeap forgetIdentifiersIf(Predicate<Identifier> test) throws SemanticException {
		return this;
	}

	@Override
	public MonolithicHeap lubAux(MonolithicHeap other) throws SemanticException {
		return TOP;
	}

	@Override
	public MonolithicHeap wideningAux(MonolithicHeap other) throws SemanticException {
		return TOP;
	}

	@Override
	public boolean lessOrEqualAux(MonolithicHeap other) throws SemanticException {
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
	public DomainRepresentation representation() {
		return isBottom() ? Lattice.bottomRepresentation() : REPR;
	}

	@Override
	public int hashCode() {
		return MonolithicHeap.class.hashCode();
	}

	@Override
	public boolean equals(Object obj) {
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

		@Override
		public ExpressionSet<ValueExpression> visit(AccessChild expression, ExpressionSet<ValueExpression> receiver,
				ExpressionSet<ValueExpression> child, Object... params) throws SemanticException {
			// any expression accessing an area of the heap or instantiating a
			// new one is modeled through the monolith
			Set<Type> acc = new HashSet<>();
			child.forEach(e -> acc.add(e.getStaticType()));
			Type refType = Type.commonSupertype(acc, Untyped.INSTANCE);

			HeapLocation e = new HeapLocation(refType, MONOLITH_NAME, true,
					expression.getCodeLocation());
			if (expression.hasRuntimeTypes())
				e.setRuntimeTypes(expression.getRuntimeTypes(null));
			return new ExpressionSet<>(e);
		}

		@Override
		public ExpressionSet<ValueExpression> visit(HeapAllocation expression, Object... params)
				throws SemanticException {
			// any expression accessing an area of the heap or instantiating a
			// new one is modeled through the monolith
			HeapLocation e = new HeapLocation(expression.getStaticType(), MONOLITH_NAME, true,
					expression.getCodeLocation());
			if (expression.hasRuntimeTypes())
				e.setRuntimeTypes(expression.getRuntimeTypes(null));
			return new ExpressionSet<>(e);
		}

		@Override
		public ExpressionSet<ValueExpression> visit(HeapReference expression, ExpressionSet<ValueExpression> ref,
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
			if (expression.hasRuntimeTypes())
				e.setRuntimeTypes(expression.getRuntimeTypes(null));
			return new ExpressionSet<>(e);
		}

		@Override
		public ExpressionSet<ValueExpression> visit(HeapDereference expression, ExpressionSet<ValueExpression> deref,
				Object... params)
				throws SemanticException {
			// any expression accessing an area of the heap or instantiating a
			// new one is modeled through the monolith
			return deref;
		}
	}
}

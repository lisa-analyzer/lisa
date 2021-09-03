package it.unive.lisa.analysis.impl.heap;

import it.unive.lisa.analysis.Lattice;
import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.analysis.heap.BaseHeapDomain;
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
import java.util.Collections;
import java.util.List;

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
	protected MonolithicHeap mk(MonolithicHeap reference) {
		return TOP;
	}

	@Override
	protected MonolithicHeap semanticsOf(HeapExpression expression, ProgramPoint pp) {
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
	protected MonolithicHeap lubAux(MonolithicHeap other) throws SemanticException {
		return TOP;
	}

	@Override
	protected MonolithicHeap wideningAux(MonolithicHeap other) throws SemanticException {
		return TOP;
	}

	@Override
	protected boolean lessOrEqualAux(MonolithicHeap other) throws SemanticException {
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
		return isBottom() ? Lattice.BOTTOM_REPR : REPR;
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

	private static class Rewriter extends BaseHeapDomain.Rewriter {

		@Override
		public ExpressionSet<ValueExpression> visit(AccessChild expression, ExpressionSet<ValueExpression> receiver,
				ExpressionSet<ValueExpression> child, Object... params) throws SemanticException {
			// any expression accessing an area of the heap or instantiating a
			// new
			// one is modeled through the monolith
			return new ExpressionSet<>(
					new HeapLocation(expression.getTypes(), MONOLITH_NAME, true, expression.getCodeLocation()));
		}

		@Override
		public ExpressionSet<ValueExpression> visit(HeapAllocation expression, Object... params)
				throws SemanticException {
			// any expression accessing an area of the heap or instantiating a
			// new
			// one is modeled through the monolith
			return new ExpressionSet<>(
					new HeapLocation(expression.getTypes(), MONOLITH_NAME, true, expression.getCodeLocation()));
		}

		@Override
		public ExpressionSet<ValueExpression> visit(HeapReference expression, ExpressionSet<ValueExpression> ref,
				Object... params)
				throws SemanticException {
			// any expression accessing an area of the heap or instantiating a
			// new
			// one is modeled through the monolith
			return new ExpressionSet<>(new MemoryPointer(expression.getTypes(),
					new HeapLocation(expression.getTypes(), MONOLITH_NAME, true, expression.getCodeLocation()),
					expression.getCodeLocation()));
		}

		@Override
		public ExpressionSet<ValueExpression> visit(HeapDereference expression, ExpressionSet<ValueExpression> deref,
				Object... params)
				throws SemanticException {
			// any expression accessing an area of the heap or instantiating a
			// new
			// one is modeled through the monolith
			return new ExpressionSet<>(new MemoryPointer(expression.getTypes(),
					new HeapLocation(expression.getTypes(), MONOLITH_NAME, true, expression.getCodeLocation()),
					expression.getCodeLocation()));
		}
	}
}

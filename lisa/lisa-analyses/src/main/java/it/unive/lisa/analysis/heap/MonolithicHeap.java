package it.unive.lisa.analysis.heap;

import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.analysis.SemanticOracle;
import it.unive.lisa.analysis.lattices.ExpressionSet;
import it.unive.lisa.analysis.lattices.Satisfiability;
import it.unive.lisa.lattices.heap.Monolith;
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
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.apache.commons.lang3.tuple.Pair;

/**
 * A monolithic heap implementation that abstracts all heap locations to a
 * unique identifier.
 * 
 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
 */
public class MonolithicHeap implements BaseHeapDomain<Monolith> {

	private static final String MONOLITH_NAME = "heap";

	@Override
	public Monolith makeLattice() {
		return Monolith.SINGLETON;
	}

	@Override
	public ExpressionSet rewrite(
			Monolith state,
			SymbolicExpression expression,
			ProgramPoint pp,
			SemanticOracle oracle)
			throws SemanticException {
		return expression.accept(Rewriter.SINGLETON, pp);
	}

	@Override
	public Pair<Monolith, List<HeapReplacement>> assign(
			Monolith state,
			Identifier id,
			SymbolicExpression expression,
			ProgramPoint pp,
			SemanticOracle oracle)
			throws SemanticException {
		return Pair.of(state, Collections.emptyList());
	}

	@Override
	public Pair<Monolith, List<HeapReplacement>> semanticsOf(
			Monolith state,
			HeapExpression expression,
			ProgramPoint pp,
			SemanticOracle oracle) {
		return Pair.of(state, Collections.emptyList());
	}

	@Override
	public Pair<Monolith, List<HeapReplacement>> assume(
			Monolith state,
			SymbolicExpression expression,
			ProgramPoint src,
			ProgramPoint dest,
			SemanticOracle oracle)
			throws SemanticException {
		return Pair.of(state, Collections.emptyList());
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
			if (receiver.size() != 1)
				throw new SemanticException("Rewriting of receiver led to more than one expression");

			// any expression accessing an area of the heap or instantiating a
			// new one is modeled through the monolith
			Set<Type> acc = new HashSet<>();
			child.forEach(e -> acc.add(e.getStaticType()));
			Type refType = Type.commonSupertype(acc, Untyped.INSTANCE);

			HeapLocation e = new HeapLocation(refType, MONOLITH_NAME, true, expression.getCodeLocation());
			if (receiver.elements.iterator().next() instanceof HeapLocation) {
				HeapLocation loc = (HeapLocation) receiver.elements.iterator().next();
				e.setAllocation(loc.isAllocation());
			}
			return new ExpressionSet(e);
		}

		@Override
		public ExpressionSet visit(
				MemoryAllocation expression,
				Object... params)
				throws SemanticException {
			// any expression accessing an area of the heap or instantiating a
			// new one is modeled through the monolith
			HeapLocation e = new HeapLocation(
				expression.getStaticType(),
				MONOLITH_NAME,
				true,
				expression.getCodeLocation());
			e.setAllocation(true);
			return new ExpressionSet(e);
		}

		@Override
		public ExpressionSet visit(
				HeapReference expression,
				ExpressionSet ref,
				Object... params)
				throws SemanticException {
			if (ref.size() != 1)
				throw new SemanticException("Rewriting of receiver led to more than one expression");

			// any expression accessing an area of the heap or instantiating a
			// new one is modeled through the monolith
			Set<Type> acc = new HashSet<>();
			ref.forEach(e -> acc.add(e.getStaticType()));
			Type refType = Type.commonSupertype(acc, Untyped.INSTANCE);

			HeapLocation loc = (HeapLocation) ref.elements.iterator().next();
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
	public Satisfiability alias(
			Monolith state,
			SymbolicExpression x,
			SymbolicExpression y,
			ProgramPoint pp,
			SemanticOracle oracle)
			throws SemanticException {
		return Satisfiability.UNKNOWN;
	}

	@Override
	public Satisfiability isReachableFrom(
			Monolith state,
			SymbolicExpression x,
			SymbolicExpression y,
			ProgramPoint pp,
			SemanticOracle oracle)
			throws SemanticException {
		return Satisfiability.UNKNOWN;
	}

}

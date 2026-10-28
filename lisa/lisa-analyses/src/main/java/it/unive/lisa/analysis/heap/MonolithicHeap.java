package it.unive.lisa.analysis.heap;

import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.analysis.SemanticOracle;
import it.unive.lisa.lattices.ExpressionSet;
import it.unive.lisa.lattices.Satisfiability;
import it.unive.lisa.lattices.heap.Monolith;
import it.unive.lisa.program.cfg.ProgramPoint;
import it.unive.lisa.symbolic.SymbolicExpression;
import it.unive.lisa.symbolic.heap.AccessChild;
import it.unive.lisa.symbolic.heap.HeapDereference;
import it.unive.lisa.symbolic.heap.HeapExpression;
import it.unive.lisa.symbolic.heap.HeapReference;
import it.unive.lisa.symbolic.heap.MemoryAllocation;
import it.unive.lisa.symbolic.heap.NullConstant;
import it.unive.lisa.symbolic.value.HeapLocation;
import it.unive.lisa.symbolic.value.Identifier;
import it.unive.lisa.symbolic.value.MemoryPointer;
import it.unive.lisa.type.NullType;
import it.unive.lisa.type.Type;
import it.unive.lisa.type.Untyped;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.apache.commons.lang3.tuple.Pair;

/**
 * A monolithic heap implementation that abstracts all heap locations,
 * regardless of where and how they are allocated, to a single unique identifier
 * (named {@value #MONOLITH_NAME}). It does not distinguish between different
 * objects, arrays or fields, and it never answers positively to aliasing or
 * reachability queries other than {@link Satisfiability#UNKNOWN}.
 *
 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
 */
public class MonolithicHeap
		implements
		BaseHeapDomain<Monolith> {

	private static final String MONOLITH_NAME = "heap";

	@Override
	public Monolith makeLattice() {
		return Monolith.SINGLETON;
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

	@Override
	public ExpressionSet rewriteAccessChild(
			AccessChild expression,
			ExpressionSet receiver,
			ExpressionSet child,
			Monolith state,
			ProgramPoint pp,
			SemanticOracle oracle)
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
	public ExpressionSet rewriteMemoryAllocation(
			MemoryAllocation expression,
			Monolith state,
			ProgramPoint pp,
			SemanticOracle oracle)
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
	public ExpressionSet rewriteHeapReference(
			HeapReference expression,
			ExpressionSet ref,
			Monolith state,
			ProgramPoint pp,
			SemanticOracle oracle)
			throws SemanticException {
		if (ref.size() != 1)
			throw new SemanticException("Rewriting of receiver led to more than one expression");

		// any expression accessing an area of the heap or instantiating a
		// new one is modeled through the monolith
		Set<Type> acc = new HashSet<>();
		ref.forEach(e -> acc.add(e.getStaticType()));
		Type refType = Type.commonSupertype(acc, Untyped.INSTANCE);

		HeapLocation loc = (HeapLocation) ref.elements.iterator().next();
		MemoryPointer e = new MemoryPointer(pp.getProgram().getTypes().getReference(refType), loc,
				expression.getCodeLocation());
		return new ExpressionSet(e);
	}

	@Override
	public ExpressionSet rewriteHeapDereference(
			HeapDereference expression,
			ExpressionSet deref,
			Monolith state,
			ProgramPoint pp,
			SemanticOracle oracle)
			throws SemanticException {
		// any expression accessing an area of the heap or instantiating a
		// new one is modeled through the monolith
		return deref;
	}

	@Override
	public ExpressionSet rewriteNullConstant(
			NullConstant expression,
			Monolith state,
			ProgramPoint pp,
			SemanticOracle oracle)
			throws SemanticException {
		HeapLocation e = new HeapLocation(
				expression.getStaticType(),
				MONOLITH_NAME,
				true,
				expression.getCodeLocation());
		e.setAllocation(false);
		MemoryPointer mp = new MemoryPointer(
				pp.getProgram().getTypes().getReference(NullType.INSTANCE),
				e,
				expression.getCodeLocation());
		return new ExpressionSet(mp);
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

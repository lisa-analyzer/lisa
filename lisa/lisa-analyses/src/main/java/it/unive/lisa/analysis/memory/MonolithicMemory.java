package it.unive.lisa.analysis.memory;

import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.analysis.SemanticOracle;
import it.unive.lisa.lattices.ExpressionSet;
import it.unive.lisa.lattices.Satisfiability;
import it.unive.lisa.lattices.memory.Monolith;
import it.unive.lisa.program.cfg.ProgramPoint;
import it.unive.lisa.symbolic.SymbolicExpression;
import it.unive.lisa.symbolic.memory.GetAddress;
import it.unive.lisa.symbolic.memory.MemoryAllocation;
import it.unive.lisa.symbolic.memory.MemoryDereference;
import it.unive.lisa.symbolic.memory.MemoryExpression;
import it.unive.lisa.symbolic.memory.NullConstant;
import it.unive.lisa.symbolic.memory.StaticAccess;
import it.unive.lisa.symbolic.value.Identifier;
import it.unive.lisa.symbolic.value.MemoryLocation;
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
 * A monolithic memory implementation that abstracts all memory locations to a
 * unique identifier.
 * 
 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
 */
public class MonolithicMemory
		implements
		BaseMemoryDomain<Monolith> {

	private static final String MONOLITH_NAME = "memory";

	@Override
	public Monolith makeLattice() {
		return Monolith.SINGLETON;
	}

	@Override
	public Pair<Monolith, List<MemoryReplacement>> assign(
			Monolith state,
			Identifier id,
			SymbolicExpression expression,
			ProgramPoint pp,
			SemanticOracle oracle)
			throws SemanticException {
		return Pair.of(state, Collections.emptyList());
	}

	@Override
	public Pair<Monolith, List<MemoryReplacement>> semanticsOf(
			Monolith state,
			MemoryExpression expression,
			ProgramPoint pp,
			SemanticOracle oracle) {
		return Pair.of(state, Collections.emptyList());
	}

	@Override
	public Pair<Monolith, List<MemoryReplacement>> assume(
			Monolith state,
			SymbolicExpression expression,
			ProgramPoint src,
			ProgramPoint dest,
			SemanticOracle oracle)
			throws SemanticException {
		return Pair.of(state, Collections.emptyList());
	}

	@Override
	public ExpressionSet rewriteStaticAccess(
			StaticAccess expression,
			ExpressionSet receiver,
			ExpressionSet child,
			Monolith state,
			ProgramPoint pp,
			SemanticOracle oracle)
			throws SemanticException {
		if (receiver.size() != 1)
			throw new SemanticException("Rewriting of receiver led to more than one expression");

		// any expression accessing an area of the memory or instantiating a
		// new one is modeled through the monolith
		Set<Type> acc = new HashSet<>();
		child.forEach(e -> acc.add(e.getStaticType()));
		Type refType = Type.commonSupertype(acc, Untyped.INSTANCE);

		MemoryLocation e = new MemoryLocation(refType, MONOLITH_NAME, true, expression.getCodeLocation());
		if (receiver.elements.iterator().next() instanceof MemoryLocation) {
			MemoryLocation loc = (MemoryLocation) receiver.elements.iterator().next();
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
		// any expression accessing an area of the memory or instantiating a
		// new one is modeled through the monolith
		MemoryLocation e = new MemoryLocation(
				expression.getStaticType(),
				MONOLITH_NAME,
				true,
				expression.getCodeLocation());
		e.setAllocation(true);
		return new ExpressionSet(e);
	}

	@Override
	public ExpressionSet rewriteGetAddress(
			GetAddress expression,
			ExpressionSet ref,
			Monolith state,
			ProgramPoint pp,
			SemanticOracle oracle)
			throws SemanticException {
		if (ref.size() != 1)
			throw new SemanticException("Rewriting of receiver led to more than one expression");

		// any expression accessing an area of the memory or instantiating a
		// new one is modeled through the monolith
		Set<Type> acc = new HashSet<>();
		ref.forEach(e -> acc.add(e.getStaticType()));
		Type refType = Type.commonSupertype(acc, Untyped.INSTANCE);

		MemoryLocation loc = (MemoryLocation) ref.elements.iterator().next();
		MemoryPointer e = new MemoryPointer(pp.getProgram().getTypes().getReference(refType), loc,
				expression.getCodeLocation());
		return new ExpressionSet(e);
	}

	@Override
	public ExpressionSet rewriteMemoryDereference(
			MemoryDereference expression,
			ExpressionSet deref,
			Monolith state,
			ProgramPoint pp,
			SemanticOracle oracle)
			throws SemanticException {
		// any expression accessing an area of the memory or instantiating a
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
		MemoryLocation e = new MemoryLocation(
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

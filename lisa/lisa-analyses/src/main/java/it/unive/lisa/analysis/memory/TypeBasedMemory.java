package it.unive.lisa.analysis.memory;

import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.analysis.SemanticOracle;
import it.unive.lisa.lattices.ExpressionSet;
import it.unive.lisa.lattices.Satisfiability;
import it.unive.lisa.lattices.memory.AllocatedTypes;
import it.unive.lisa.program.cfg.ProgramPoint;
import it.unive.lisa.symbolic.SymbolicExpression;
import it.unive.lisa.symbolic.memory.DynamicAccess;
import it.unive.lisa.symbolic.memory.GetAddress;
import it.unive.lisa.symbolic.memory.MemoryAllocation;
import it.unive.lisa.symbolic.memory.MemoryDereference;
import it.unive.lisa.symbolic.memory.MemoryExpression;
import it.unive.lisa.symbolic.memory.NullConstant;
import it.unive.lisa.symbolic.memory.StaticAccess;
import it.unive.lisa.symbolic.value.Identifier;
import it.unive.lisa.symbolic.value.MemoryLocation;
import it.unive.lisa.symbolic.value.MemoryPointer;
import it.unive.lisa.symbolic.value.Variable;
import it.unive.lisa.type.NullType;
import it.unive.lisa.type.Type;
import it.unive.lisa.type.Untyped;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.ListUtils;
import org.apache.commons.lang3.tuple.Pair;

/**
 * A type-based memory implementation that abstracts memory locations depending
 * on their types, i.e., all the memory locations with the same type are
 * abstracted into a single unique identifier.
 *
 * @author <a href="mailto:vincenzo.arceri@unipr.it">Vincenzo Arceri</a>
 */
public class TypeBasedMemory
		implements
		BaseMemoryDomain<AllocatedTypes> {

	@Override
	public AllocatedTypes makeLattice() {
		return new AllocatedTypes();
	}

	@Override
	public Pair<AllocatedTypes, List<MemoryReplacement>> assign(
			AllocatedTypes state,
			Identifier id,
			SymbolicExpression expression,
			ProgramPoint pp,
			SemanticOracle oracle)
			throws SemanticException {
		return Pair.of(state, Collections.emptyList());
	}

	@Override
	public Pair<AllocatedTypes, List<MemoryReplacement>> assume(
			AllocatedTypes state,
			SymbolicExpression expression,
			ProgramPoint src,
			ProgramPoint dest,
			SemanticOracle oracle)
			throws SemanticException {
		return Pair.of(state, Collections.emptyList());
	}

	@Override
	public Pair<AllocatedTypes, List<MemoryReplacement>> semanticsOf(
			AllocatedTypes state,
			MemoryExpression expression,
			ProgramPoint pp,
			SemanticOracle oracle)
			throws SemanticException {
		if (expression instanceof StaticAccess) {
			StaticAccess access = (StaticAccess) expression;
			return smallStepSemantics(state, access.getContainer(), pp, oracle);
		}

		if (expression instanceof DynamicAccess) {
			DynamicAccess access = (DynamicAccess) expression;
			Pair<AllocatedTypes,
					List<MemoryReplacement>> cont = smallStepSemantics(state, access.getContainer(), pp, oracle);
			Pair<AllocatedTypes,
					List<MemoryReplacement>> ch = smallStepSemantics(cont.getLeft(), access.getChild(), pp, oracle);
			return Pair.of(ch.getLeft(), ListUtils.union(cont.getRight(), ch.getRight()));
		}

		if (expression instanceof MemoryAllocation) {
			Set<String> names = new HashSet<>(state.elements);
			for (Type type : oracle.getRuntimeTypesOf(expression, pp))
				if (type.isInMemoryType())
					names.add(type.toString());

			return Pair.of(new AllocatedTypes(names), Collections.emptyList());
		}

		if (expression instanceof GetAddress)
			return smallStepSemantics(state, ((GetAddress) expression).getExpression(), pp, oracle);

		if (expression instanceof MemoryDereference)
			return smallStepSemantics(state, ((MemoryDereference) expression).getExpression(), pp, oracle);

		return Pair.of(state.top(), Collections.emptyList());
	}

	@Override
	public ExpressionSet rewriteStaticAccess(
			StaticAccess expression,
			ExpressionSet receiver,
			String child,
			AllocatedTypes state,
			ProgramPoint pp,
			SemanticOracle oracle)
			throws SemanticException {
		// we use the container because we are not field-sensitive
		Set<SymbolicExpression> result = new HashSet<>();
		for (Type t : oracle.getRuntimeTypesOf(expression, pp)) {
			MemoryLocation e = new MemoryLocation(t, t.toString(), true, expression.getCodeLocation());
			result.add(e);
		}
		return new ExpressionSet(result);
	}

	// TODO not yet implemented: stubbed just to compile.
	@Override
	public ExpressionSet rewriteDynamicAccess(
			DynamicAccess expression,
			ExpressionSet receiver,
			ExpressionSet child,
			AllocatedTypes state,
			ProgramPoint pp,
			SemanticOracle oracle)
			throws SemanticException {
		throw new SemanticException("Rewriting of dynamic field accesses (p[s]) is not yet implemented");
	}

	@Override
	public ExpressionSet rewriteMemoryAllocation(
			MemoryAllocation expression,
			AllocatedTypes state,
			ProgramPoint pp,
			SemanticOracle oracle)
			throws SemanticException {
		Set<SymbolicExpression> result = new HashSet<>();
		Type t = expression.getStaticType();
		if (t.isInMemoryType()) {
			MemoryLocation e = new MemoryLocation(t, t.toString(), true, expression.getCodeLocation());
			e.setAllocation(true);
			result.add(e);
		}
		return new ExpressionSet(result);
	}

	@Override
	public ExpressionSet rewriteGetAddress(
			GetAddress expression,
			ExpressionSet ref,
			AllocatedTypes state,
			ProgramPoint pp,
			SemanticOracle oracle)
			throws SemanticException {
		Set<SymbolicExpression> result = new HashSet<>();
		for (SymbolicExpression refExp : ref) {
			refExp = refExp.removeTypingExpressions();
			if (refExp instanceof MemoryLocation) {
				Set<Type> rt = oracle.getRuntimeTypesOf(refExp, pp);
				Type sup = Type.commonSupertype(rt, Untyped.INSTANCE);
				MemoryPointer e = new MemoryPointer(
						pp.getProgram().getTypes().getReference(sup),
						(MemoryLocation) refExp,
						refExp.getCodeLocation());
				result.add(e);
			}
		}

		return new ExpressionSet(result);
	}

	@Override
	public ExpressionSet rewriteMemoryDereference(
			MemoryDereference expression,
			ExpressionSet deref,
			AllocatedTypes state,
			ProgramPoint pp,
			SemanticOracle oracle)
			throws SemanticException {
		Set<SymbolicExpression> result = new HashSet<>();
		for (SymbolicExpression derefExp : deref) {
			derefExp = derefExp.removeTypingExpressions();
			if (derefExp instanceof Variable) {
				Variable var = (Variable) derefExp;
				for (Type t : oracle.getRuntimeTypesOf(var, pp))
					if (t.isPointerType()) {
						Type inner = t.asPointerType().getInnerType();
						MemoryLocation loc = new MemoryLocation(inner, inner.toString(), true, var.getCodeLocation());
						MemoryPointer pointer = new MemoryPointer(t, loc, var.getCodeLocation());
						result.add(pointer);
					}
			}
		}

		return new ExpressionSet(result);
	}

	@Override
	public ExpressionSet rewriteNullConstant(
			NullConstant expression,
			AllocatedTypes state,
			ProgramPoint pp,
			SemanticOracle oracle)
			throws SemanticException {
		MemoryLocation loc = new MemoryLocation(
				NullType.INSTANCE,
				NullType.INSTANCE.toString(),
				true,
				expression.getCodeLocation());
		MemoryPointer mp = new MemoryPointer(
				pp.getProgram().getTypes().getReference(NullType.INSTANCE),
				loc,
				expression.getCodeLocation());
		return new ExpressionSet(mp);
	}

	@Override
	public Satisfiability alias(
			AllocatedTypes state,
			SymbolicExpression x,
			SymbolicExpression y,
			ProgramPoint pp,
			SemanticOracle oracle)
			throws SemanticException {
		if (state.isTop())
			return Satisfiability.UNKNOWN;
		if (state.isBottom())
			return Satisfiability.BOTTOM;

		Set<Type> ltypes = new HashSet<>();
		for (SymbolicExpression e : rewrite(state, x, pp, oracle))
			ltypes.addAll(oracle.getRuntimeTypesOf(e, pp));
		Set<Type> rtypes = new HashSet<>();
		for (SymbolicExpression e : rewrite(state, y, pp, oracle))
			rtypes.addAll(oracle.getRuntimeTypesOf(e, pp));
		if (CollectionUtils.intersection(ltypes, rtypes).isEmpty())
			// no common types -> they cannot be "smashed" to the same location
			return Satisfiability.NOT_SATISFIED;

		return Satisfiability.UNKNOWN;
	}

	@Override
	public Satisfiability isReachableFrom(
			AllocatedTypes state,
			SymbolicExpression x,
			SymbolicExpression y,
			ProgramPoint pp,
			SemanticOracle oracle)
			throws SemanticException {
		return Satisfiability.UNKNOWN;
	}

}

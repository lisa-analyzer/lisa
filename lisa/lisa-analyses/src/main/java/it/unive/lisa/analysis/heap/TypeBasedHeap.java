package it.unive.lisa.analysis.heap;

import it.unive.lisa.analysis.ScopeToken;
import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.analysis.SemanticOracle;
import it.unive.lisa.analysis.lattices.ExpressionSet;
import it.unive.lisa.analysis.lattices.Satisfiability;
import it.unive.lisa.analysis.lattices.SetLattice;
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
import it.unive.lisa.symbolic.value.Variable;
import it.unive.lisa.type.ReferenceType;
import it.unive.lisa.type.Type;
import it.unive.lisa.type.Untyped;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.ListUtils;
import org.apache.commons.lang3.tuple.Pair;

/**
 * A type-based heap implementation that abstracts heap locations depending on
 * their types, i.e., all the heap locations with the same type are abstracted
 * into a single unique identifier.
 *
 * @author <a href="mailto:vincenzo.arceri@unipr.it">Vincenzo Arceri</a>
 */
public class TypeBasedHeap
		implements
		BaseHeapDomain<TypeBasedHeap.Types> {

	/**
	 * A heap lattice that contains the types of the heap locations. These are
	 * modelled as a set of types that have been allocated in the heap.
	 * 
	 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
	 */
	public static class Types
			extends
			SetLattice<Types, String>
			implements
			HeapLattice<Types> {

		/**
		 * Builds an empty set of types.
		 */
		public Types() {
			super(Collections.emptySet(), true);
		}

		private Types(
				Set<String> elements) {
			super(elements, true);
		}

		private Types(
				Set<String> elements,
				boolean isTop) {
			super(elements, isTop);
		}

		@Override
		public Types top() {
			return new Types(Collections.emptySet(), true);
		}

		@Override
		public Types bottom() {
			return new Types(Collections.emptySet(), false);
		}

		@Override
		public Pair<Types, List<HeapReplacement>> pushScope(
				ScopeToken token,
				ProgramPoint pp)
				throws SemanticException {
			return Pair.of(this, Collections.emptyList());
		}

		@Override
		public Pair<Types, List<HeapReplacement>> popScope(
				ScopeToken token,
				ProgramPoint pp)
				throws SemanticException {
			return Pair.of(this, Collections.emptyList());
		}

		@Override
		public boolean knowsIdentifier(
				Identifier id) {
			return false;
		}

		@Override
		public Pair<Types, List<HeapReplacement>> forgetIdentifier(
				Identifier id,
				ProgramPoint pp)
				throws SemanticException {
			return Pair.of(this, Collections.emptyList());
		}

		@Override
		public Pair<Types, List<HeapReplacement>> forgetIdentifiers(
				Iterable<Identifier> ids,
				ProgramPoint pp)
				throws SemanticException {
			return Pair.of(this, Collections.emptyList());
		}

		@Override
		public Pair<Types, List<HeapReplacement>> forgetIdentifiersIf(
				Predicate<Identifier> test,
				ProgramPoint pp)
				throws SemanticException {
			return Pair.of(this, Collections.emptyList());
		}

		@Override
		public Types mk(
				Set<String> set) {
			return new Types(set);
		}

		@Override
		public List<HeapReplacement> expand(
				HeapReplacement base)
				throws SemanticException {
			return List.of(base);
		}

	}

	@Override
	public Types makeLattice() {
		return new Types();
	}

	@Override
	public ExpressionSet rewrite(
			Types state,
			SymbolicExpression expression,
			ProgramPoint pp,
			SemanticOracle oracle)
			throws SemanticException {
		return expression.accept(Rewriter.SINGLETON, pp, oracle);
	}

	@Override
	public Pair<Types, List<HeapReplacement>> assign(
			Types state,
			Identifier id,
			SymbolicExpression expression,
			ProgramPoint pp,
			SemanticOracle oracle)
			throws SemanticException {
		return Pair.of(state, Collections.emptyList());
	}

	@Override
	public Pair<Types, List<HeapReplacement>> assume(
			Types state,
			SymbolicExpression expression,
			ProgramPoint src,
			ProgramPoint dest,
			SemanticOracle oracle)
			throws SemanticException {
		return Pair.of(state, Collections.emptyList());
	}

	@Override
	public Pair<Types, List<HeapReplacement>> semanticsOf(
			Types state,
			HeapExpression expression,
			ProgramPoint pp,
			SemanticOracle oracle)
			throws SemanticException {
		if (expression instanceof AccessChild) {
			AccessChild access = (AccessChild) expression;
			Pair<Types, List<HeapReplacement>> cont = smallStepSemantics(state, access.getContainer(), pp, oracle);
			Pair<Types, List<HeapReplacement>> ch = smallStepSemantics(cont.getLeft(), access.getChild(), pp, oracle);
			return Pair.of(ch.getLeft(), ListUtils.union(cont.getRight(), ch.getRight()));
		}

		if (expression instanceof MemoryAllocation) {
			Set<String> names = new HashSet<>(state.elements);
			for (Type type : oracle.getRuntimeTypesOf(expression, pp))
				if (type.isInMemoryType())
					names.add(type.toString());

			return Pair.of(new Types(names), Collections.emptyList());
		}

		if (expression instanceof HeapReference)
			return smallStepSemantics(state, ((HeapReference) expression).getExpression(), pp, oracle);

		if (expression instanceof HeapDereference)
			return smallStepSemantics(state, ((HeapDereference) expression).getExpression(), pp, oracle);

		return Pair.of(state.top(), Collections.emptyList());
	}

	/**
	 * A {@link it.unive.lisa.analysis.heap.BaseHeapDomain.Rewriter} for the
	 * {@link TypeBasedHeap} domain.
	 * 
	 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
	 */
	public static class Rewriter
			extends
			BaseHeapDomain.Rewriter {

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
			// we use the container because we are not field-sensitive
			Set<SymbolicExpression> result = new HashSet<>();

			ProgramPoint pp = (ProgramPoint) params[0];
			SemanticOracle oracle = (SemanticOracle) params[1];

			for (SymbolicExpression ch : child)
				for (Type t : oracle.getRuntimeTypesOf(ch, pp)) {
					HeapLocation e = new HeapLocation(t, t.toString(), true, expression.getCodeLocation());
					result.add(e);
				}
			return new ExpressionSet(result);
		}

		@Override
		public ExpressionSet visit(
				MemoryAllocation expression,
				Object... params)
				throws SemanticException {
			Set<SymbolicExpression> result = new HashSet<>();
			Type t = expression.getStaticType();
			if (t.isInMemoryType()) {
				HeapLocation e = new HeapLocation(t, t.toString(), true, expression.getCodeLocation());
				e.setAllocation(true);
				result.add(e);
			}
			return new ExpressionSet(result);
		}

		@Override
		public ExpressionSet visit(
				HeapReference expression,
				ExpressionSet ref,
				Object... params)
				throws SemanticException {
			Set<SymbolicExpression> result = new HashSet<>();

			ProgramPoint pp = (ProgramPoint) params[0];
			SemanticOracle oracle = (SemanticOracle) params[1];

			for (SymbolicExpression refExp : ref) {
				refExp = refExp.removeTypingExpressions();
				if (refExp instanceof HeapLocation) {
					Set<Type> rt = oracle.getRuntimeTypesOf(refExp, pp);
					Type sup = Type.commonSupertype(rt, Untyped.INSTANCE);
					MemoryPointer e = new MemoryPointer(
							new ReferenceType(sup),
							(HeapLocation) refExp,
							refExp.getCodeLocation());
					result.add(e);
				}
			}

			return new ExpressionSet(result);
		}

		@Override
		public ExpressionSet visit(
				HeapDereference expression,
				ExpressionSet deref,
				Object... params)
				throws SemanticException {
			Set<SymbolicExpression> result = new HashSet<>();
			ProgramPoint pp = (ProgramPoint) params[0];
			SemanticOracle oracle = (SemanticOracle) params[1];

			for (SymbolicExpression derefExp : deref) {
				derefExp = derefExp.removeTypingExpressions();
				if (derefExp instanceof Variable) {
					Variable var = (Variable) derefExp;
					for (Type t : oracle.getRuntimeTypesOf(var, pp))
						if (t.isPointerType()) {
							Type inner = t.asPointerType().getInnerType();
							HeapLocation loc = new HeapLocation(inner, inner.toString(), true, var.getCodeLocation());
							MemoryPointer pointer = new MemoryPointer(t, loc, var.getCodeLocation());
							result.add(pointer);
						}
				}
			}

			return new ExpressionSet(result);
		}

	}

	@Override
	public Satisfiability alias(
			Types state,
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
			Types state,
			SymbolicExpression x,
			SymbolicExpression y,
			ProgramPoint pp,
			SemanticOracle oracle)
			throws SemanticException {
		return Satisfiability.UNKNOWN;
	}

}

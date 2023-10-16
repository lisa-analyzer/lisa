package it.unive.lisa.analysis.heap;

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
import it.unive.lisa.symbolic.value.Variable;
import it.unive.lisa.type.ReferenceType;
import it.unive.lisa.type.Type;
import it.unive.lisa.type.Untyped;
import it.unive.lisa.util.representation.SetRepresentation;
import it.unive.lisa.util.representation.StringRepresentation;
import it.unive.lisa.util.representation.StructuredRepresentation;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.SetUtils;

/**
 * A type-based heap implementation that abstracts heap locations depending on
 * their types, i.e., all the heap locations with the same type are abstracted
 * into a single unique identifier.
 *
 * @author <a href="mailto:vincenzo.arceri@unive.it">Vincenzo Arceri</a>
 */
public class TypeBasedHeap implements BaseHeapDomain<TypeBasedHeap> {

	private static final TypeBasedHeap TOP = new TypeBasedHeap();

	private static final TypeBasedHeap BOTTOM = new TypeBasedHeap();

	private final Set<String> names;

	/**
	 * Builds a new empty instance of {@link TypeBasedHeap}.
	 */
	public TypeBasedHeap() {
		this(new HashSet<>());
	}

	/**
	 * Builds a new instance of {@link TypeBasedHeap} knowing the given types.
	 * 
	 * @param names the name of the known types
	 */
	public TypeBasedHeap(
			Set<String> names) {
		this.names = names;
	}

	/**
	 * Yields the name of the types known to this domain instance.
	 * 
	 * @return the names
	 */
	public Set<String> getKnownTypes() {
		return names;
	}

	@Override
	public ExpressionSet rewrite(
			SymbolicExpression expression,
			ProgramPoint pp,
			SemanticOracle oracle)
			throws SemanticException {
		return expression.accept(Rewriter.SINGLETON, pp, oracle);
	}

	@Override
	public TypeBasedHeap assign(
			Identifier id,
			SymbolicExpression expression,
			ProgramPoint pp,
			SemanticOracle oracle)
			throws SemanticException {
		return this;
	}

	@Override
	public TypeBasedHeap assume(
			SymbolicExpression expression,
			ProgramPoint src,
			ProgramPoint dest,
			SemanticOracle oracle)
			throws SemanticException {
		return this;
	}

	@Override
	public TypeBasedHeap forgetIdentifier(
			Identifier id)
			throws SemanticException {
		return this;
	}

	@Override
	public TypeBasedHeap forgetIdentifiersIf(
			Predicate<Identifier> test)
			throws SemanticException {
		return this;
	}

	@Override
	public Satisfiability satisfies(
			SymbolicExpression expression,
			ProgramPoint pp,
			SemanticOracle oracle)
			throws SemanticException {
		// we leave the decision to the value domain
		return Satisfiability.UNKNOWN;
	}

	@Override
	public StructuredRepresentation representation() {
		return new SetRepresentation(names, StringRepresentation::new);
	}

	@Override
	public TypeBasedHeap top() {
		return TOP;
	}

	@Override
	public TypeBasedHeap bottom() {
		return BOTTOM;
	}

	@Override
	public List<HeapReplacement> getSubstitution() {
		return Collections.emptyList();
	}

	@Override
	public TypeBasedHeap mk(
			TypeBasedHeap reference) {
		return new TypeBasedHeap(reference.names);
	}

	@Override
	public TypeBasedHeap mk(
			TypeBasedHeap reference,
			List<HeapReplacement> replacements) {
		return new TypeBasedHeap(reference.names);
	}

	@Override
	public TypeBasedHeap semanticsOf(
			HeapExpression expression,
			ProgramPoint pp,
			SemanticOracle oracle)
			throws SemanticException {
		if (expression instanceof AccessChild) {
			AccessChild access = (AccessChild) expression;
			TypeBasedHeap containerState = smallStepSemantics(access.getContainer(), pp, oracle);
			return containerState.smallStepSemantics(access.getChild(), pp, oracle);
		}

		if (expression instanceof MemoryAllocation) {
			Set<String> names = new HashSet<>(this.names);
			for (Type type : oracle.getRuntimeTypesOf(expression, pp, oracle))
				if (type.isInMemoryType())
					names.add(type.toString());

			return new TypeBasedHeap(names);
		}

		if (expression instanceof HeapReference)
			return smallStepSemantics(((HeapReference) expression).getExpression(), pp, oracle);

		if (expression instanceof HeapDereference)
			return smallStepSemantics(((HeapDereference) expression).getExpression(), pp, oracle);

		return top();
	}

	@Override
	public TypeBasedHeap lubAux(
			TypeBasedHeap other)
			throws SemanticException {
		return new TypeBasedHeap(SetUtils.union(names, other.names));
	}

	@Override
	public TypeBasedHeap glbAux(
			TypeBasedHeap other)
			throws SemanticException {
		return new TypeBasedHeap(SetUtils.intersection(names, other.names));
	}

	@Override
	public boolean lessOrEqualAux(
			TypeBasedHeap other)
			throws SemanticException {
		return other.names.containsAll(names);
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((names == null) ? 0 : names.hashCode());
		return result;
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
		TypeBasedHeap other = (TypeBasedHeap) obj;
		if (names == null) {
			if (other.names != null)
				return false;
		} else if (!names.equals(other.names))
			return false;
		return true;
	}

	/**
	 * A {@link it.unive.lisa.analysis.heap.BaseHeapDomain.Rewriter} for the
	 * {@link TypeBasedHeap} domain.
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
			// we use the container because we are not field-sensitive
			Set<SymbolicExpression> result = new HashSet<>();

			ProgramPoint pp = (ProgramPoint) params[0];
			SemanticOracle oracle = (SemanticOracle) params[1];

			for (SymbolicExpression rec : receiver)
				if (rec instanceof MemoryPointer) {
					MemoryPointer pid = (MemoryPointer) rec;
					for (Type t : oracle.getRuntimeTypesOf(pid, pp, oracle))
						if (t.isPointerType()) {
							Type inner = t.asPointerType().getInnerType();
							HeapLocation e = new HeapLocation(inner, inner.toString(), true,
									expression.getCodeLocation());
							result.add(e);
						}
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

			for (SymbolicExpression refExp : ref)
				if (refExp instanceof HeapLocation) {
					Set<Type> rt = oracle.getRuntimeTypesOf(refExp, pp, oracle);
					Type sup = Type.commonSupertype(rt, Untyped.INSTANCE);
					MemoryPointer e = new MemoryPointer(
							new ReferenceType(sup),
							(HeapLocation) refExp,
							refExp.getCodeLocation());
					result.add(e);
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
				if (derefExp instanceof Variable) {
					Variable var = (Variable) derefExp;
					for (Type t : oracle.getRuntimeTypesOf(var, pp, oracle))
						if (t.isPointerType()) {
							Type inner = t.asPointerType().getInnerType();
							HeapLocation loc = new HeapLocation(inner, inner.toString(), true,
									var.getCodeLocation());
							MemoryPointer pointer = new MemoryPointer(t, loc, var.getCodeLocation());
							result.add(pointer);
						}
				}
			}

			return new ExpressionSet(result);
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
		if (isTop())
			return Satisfiability.UNKNOWN;
		if (isBottom())
			return Satisfiability.BOTTOM;

		Set<Type> ltypes = new HashSet<>();
		for (SymbolicExpression e : rewrite(x, pp, oracle))
			ltypes.addAll(oracle.getRuntimeTypesOf(e, pp, oracle));
		Set<Type> rtypes = new HashSet<>();
		for (SymbolicExpression e : rewrite(y, pp, oracle))
			rtypes.addAll(oracle.getRuntimeTypesOf(e, pp, oracle));
		if (CollectionUtils.intersection(ltypes, rtypes).isEmpty())
			// no common types -> they cannot be "smashed" to the same location
			return Satisfiability.NOT_SATISFIED;

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

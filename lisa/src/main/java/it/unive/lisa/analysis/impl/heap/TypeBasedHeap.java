package it.unive.lisa.analysis.impl.heap;

import it.unive.lisa.analysis.ScopeToken;
import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.analysis.heap.BaseHeapDomain;
import it.unive.lisa.analysis.lattices.ExpressionSet;
import it.unive.lisa.analysis.representation.DomainRepresentation;
import it.unive.lisa.analysis.representation.SetRepresentation;
import it.unive.lisa.analysis.representation.StringRepresentation;
import it.unive.lisa.program.cfg.ProgramPoint;
import it.unive.lisa.symbolic.SymbolicExpression;
import it.unive.lisa.symbolic.heap.AccessChild;
import it.unive.lisa.symbolic.heap.HeapAllocation;
import it.unive.lisa.symbolic.heap.HeapExpression;
import it.unive.lisa.symbolic.value.HeapLocation;
import it.unive.lisa.symbolic.value.Identifier;
import it.unive.lisa.symbolic.value.Skip;
import it.unive.lisa.symbolic.value.ValueExpression;
import it.unive.lisa.type.Type;
import it.unive.lisa.util.collections.externalSet.ExternalSet;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.apache.commons.collections4.SetUtils;

/**
 * A type-based heap implementation that abstracts heap locations depending on
 * their types, i.e., all the heap locations with the same type are abstracted
 * into a single unique identifier.
 *
 * @author <a href="mailto:vincenzo.arceri@unive.it">Vincenzo Arceri</a>
 */
public class TypeBasedHeap extends BaseHeapDomain<TypeBasedHeap> {

	private static final TypeBasedHeap TOP = new TypeBasedHeap();

	private static final TypeBasedHeap BOTTOM = new TypeBasedHeap();

	private final Set<String> names;

	/**
	 * Builds a new instance of TypeBasedHeap, with an unique rewritten
	 * expression {@link Skip}.
	 */
	public TypeBasedHeap() {
		this(new HashSet<>());
	}

	private TypeBasedHeap(Set<String> names) {
		this.names = names;
	}

	@Override
	public ExpressionSet<ValueExpression> rewrite(SymbolicExpression expression, ProgramPoint pp)
			throws SemanticException {
		return expression.accept(new Rewriter());
	}

	@Override
	public TypeBasedHeap assign(Identifier id, SymbolicExpression expression, ProgramPoint pp)
			throws SemanticException {
		return this;
	}

	@Override
	public TypeBasedHeap assume(SymbolicExpression expression, ProgramPoint pp) throws SemanticException {
		return this;
	}

	@Override
	public TypeBasedHeap forgetIdentifier(Identifier id) throws SemanticException {
		return this;
	}

	@Override
	public Satisfiability satisfies(SymbolicExpression expression, ProgramPoint pp) throws SemanticException {
		// we leave the decision to the value domain
		return Satisfiability.UNKNOWN;
	}

	@Override
	public DomainRepresentation representation() {
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
	protected TypeBasedHeap mk(TypeBasedHeap reference) {
		return this;
	}

	@Override
	protected TypeBasedHeap semanticsOf(HeapExpression expression, ProgramPoint pp) throws SemanticException {
		if (expression instanceof AccessChild) {
			AccessChild access = (AccessChild) expression;
			TypeBasedHeap containerState = smallStepSemantics(access.getContainer(), pp);
			TypeBasedHeap childState = containerState.smallStepSemantics(access.getChild(), pp);

			Set<String> names = new HashSet<>(childState.names);

			for (ValueExpression cont : containerState.rewrite(access.getContainer(), pp))
				for (Type type : cont.getTypes())
					if (type.isPointerType())
						names.add(type.toString());

			return new TypeBasedHeap(names);
		}

		if (expression instanceof HeapAllocation) {
			Set<String> names = new HashSet<>(this.names);
			for (Type type : expression.getTypes())
				if (type.isPointerType())
					names.add(type.toString());

			return new TypeBasedHeap(names);
		}

		return top();
	}

	@Override
	protected TypeBasedHeap lubAux(TypeBasedHeap other) throws SemanticException {
		return new TypeBasedHeap(SetUtils.union(names, other.names));
	}

	@Override
	protected TypeBasedHeap wideningAux(TypeBasedHeap other) throws SemanticException {
		return lubAux(other);
	}

	@Override
	protected boolean lessOrEqualAux(TypeBasedHeap other) throws SemanticException {
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
	public boolean equals(Object obj) {
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

	@Override
	public TypeBasedHeap pushScope(ScopeToken scope) throws SemanticException {
		return this;
	}

	@Override
	public TypeBasedHeap popScope(ScopeToken scope) throws SemanticException {
		return this;
	}

	private static class Rewriter extends BaseHeapDomain.Rewriter {

		@Override
		public ExpressionSet<ValueExpression> visit(AccessChild expression, ExpressionSet<ValueExpression> receiver,
				ExpressionSet<ValueExpression> child, Object... params) throws SemanticException {
			// we use the container because we are not field-sensitive
			ExternalSet<Type> types = expression.getTypes();
			Set<ValueExpression> result = new HashSet<>();
			for (ValueExpression rec : receiver)
				for (Type t : rec.getTypes())
					if (t.isPointerType())
						result.add(new HeapLocation(types, t.toString(), true));
			return new ExpressionSet<>(result);
		}

		@Override
		public ExpressionSet<ValueExpression> visit(HeapAllocation expression, Object... params)
				throws SemanticException {
			ExternalSet<Type> types = expression.getTypes();
			Set<ValueExpression> result = new HashSet<>();
			for (Type t : types)
				if (t.isPointerType())
					result.add(new HeapLocation(types, t.toString(), true));
			return new ExpressionSet<>(result);
		}
	}
}

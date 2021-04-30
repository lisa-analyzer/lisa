package it.unive.lisa.analysis.impl.heap;

import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.analysis.heap.BaseHeapDomain;
import it.unive.lisa.analysis.lattices.ExpressionSet;
import it.unive.lisa.analysis.representation.DomainRepresentation;
import it.unive.lisa.analysis.representation.SetRepresentation;
import it.unive.lisa.analysis.representation.StringRepresentation;
import it.unive.lisa.caches.Caches;
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

	private final ExpressionSet<ValueExpression> rewritten;

	private final Set<String> names;

	/**
	 * Builds a new instance of TypeBasedHeap, with an unique rewritten
	 * expression {@link Skip}.
	 */
	public TypeBasedHeap() {
		this(new Skip());
	}

	private TypeBasedHeap(ValueExpression rewritten) {
		this(new ExpressionSet<ValueExpression>(rewritten), new HashSet<>());
	}

	private TypeBasedHeap(ExpressionSet<ValueExpression> rewritten, Set<String> names) {
		this.rewritten = rewritten;
		this.names = names;
	}

	@Override
	public TypeBasedHeap assign(Identifier id, SymbolicExpression expression, ProgramPoint pp)
			throws SemanticException {
		// we just rewrite the expression if needed
		return smallStepSemantics(expression, pp);
	}

	@Override
	public TypeBasedHeap assume(SymbolicExpression expression, ProgramPoint pp) throws SemanticException {
		// we just rewrite the expression if needed
		return smallStepSemantics(expression, pp);
	}

	@Override
	public TypeBasedHeap forgetIdentifier(Identifier id) throws SemanticException {
		return new TypeBasedHeap(rewritten, names);
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
	public ExpressionSet<ValueExpression> getRewrittenExpressions() {
		return rewritten;
	}

	@Override
	public List<HeapReplacement> getSubstitution() {
		return Collections.emptyList();
	}

	@Override
	protected TypeBasedHeap mk(TypeBasedHeap reference, ValueExpression expression) {
		return new TypeBasedHeap(new ExpressionSet<ValueExpression>(expression), reference.names);
	}

	@Override
	protected TypeBasedHeap semanticsOf(HeapExpression expression, ProgramPoint pp) throws SemanticException {
		if (expression instanceof AccessChild) {
			AccessChild access = (AccessChild) expression;
			TypeBasedHeap containerState = smallStepSemantics(access.getContainer(), pp);
			TypeBasedHeap childState = containerState.smallStepSemantics(access.getChild(), pp);

			Set<ValueExpression> ids = new HashSet<>();
			Set<String> names = new HashSet<>(childState.names);

			for (SymbolicExpression o : containerState.getRewrittenExpressions())
				for (Type type : o.getTypes()) {
					if (type.isPointerType()) {
						ids.add(new HeapLocation(access.getTypes(), type.toString(), true));
						names.add(type.toString());
					}
				}

			return new TypeBasedHeap(new ExpressionSet<ValueExpression>(ids), names);
		}

		if (expression instanceof HeapAllocation) {
			Set<ValueExpression> ids = new HashSet<>();
			Set<String> names = new HashSet<>(this.names);
			for (Type type : expression.getTypes())
				if (type.isPointerType()) {
					ids.add(new HeapLocation(Caches.types().mkSingletonSet(type), type.toString(), true));
					names.add(type.toString());
				}

			return new TypeBasedHeap(new ExpressionSet<ValueExpression>(ids), names);
		}

		return top();
	}

	@Override
	protected TypeBasedHeap lubAux(TypeBasedHeap other) throws SemanticException {
		return new TypeBasedHeap(rewritten.lub(other.rewritten), SetUtils.union(names, other.names));
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
		result = prime * result + ((rewritten == null) ? 0 : rewritten.hashCode());
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
		if (rewritten == null) {
			if (other.rewritten != null)
				return false;
		} else if (!rewritten.equals(other.rewritten))
			return false;
		return true;
	}
}

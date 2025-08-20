package it.unive.lisa.analysis.dataflow;

import it.unive.lisa.analysis.ScopeToken;
import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.program.cfg.ProgramPoint;
import it.unive.lisa.symbolic.SymbolicExpression;
import it.unive.lisa.symbolic.value.Identifier;
import it.unive.lisa.symbolic.value.ValueExpression;
import it.unive.lisa.util.representation.StringRepresentation;
import it.unive.lisa.util.representation.StructuredRepresentation;
import java.util.Collection;
import java.util.Collections;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * An implementation of the liveness dataflow analysis, that determines which
 * values might be used later on in the program.
 * 
 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
 */
public class Liveness
		extends
		DataflowDomain<PossibleSet<Liveness.Liv>, Liveness.Liv> {

	@Override
	public PossibleSet<Liv> makeLattice() {
		return new PossibleSet<>();
	}

	@Override
	public Set<Liv> gen(
			PossibleSet<Liv> state,
			Identifier id,
			ValueExpression expression,
			ProgramPoint pp)
			throws SemanticException {
		Collection<Identifier> ids = expression.accept(new AvailableExpressions.IDCollector(), new Object[0]);
		return ids.stream().map(Liv::new).collect(Collectors.toSet());
	}

	@Override
	public Set<Liv> gen(
			PossibleSet<Liv> state,
			ValueExpression expression,
			ProgramPoint pp)
			throws SemanticException {
		Collection<Identifier> ids = expression.accept(new AvailableExpressions.IDCollector(), new Object[0]);
		return ids.stream().map(Liv::new).collect(Collectors.toSet());
	}

	@Override
	public Set<Liv> kill(
			PossibleSet<Liv> state,
			Identifier id,
			ValueExpression expression,
			ProgramPoint pp)
			throws SemanticException {
		return Collections.singleton(new Liv(id));
	}

	@Override
	public Set<Liv> kill(
			PossibleSet<Liv> state,
			ValueExpression expression,
			ProgramPoint pp)
			throws SemanticException {
		return Collections.emptySet();
	}

	/**
	 * A liveness dataflow element, that is, an identifier that is live at a
	 * given program point.
	 * 
	 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
	 */
	public static class Liv
			implements
			DataflowElement<Liv> {

		private final Identifier id;

		private Liv(
				Identifier id) {
			this.id = id;
		}

		@Override
		public Liv pushScope(
				ScopeToken token,
				ProgramPoint pp)
				throws SemanticException {
			return new Liv((Identifier) id.pushScope(token, pp));
		}

		@Override
		public Liv popScope(
				ScopeToken token,
				ProgramPoint pp)
				throws SemanticException {
			return new Liv((Identifier) id.popScope(token, pp));
		}

		@Override
		public Collection<Identifier> getInvolvedIdentifiers() {
			return Collections.singleton(id);
		}

		@Override
		public StructuredRepresentation representation() {
			return new StringRepresentation(id);
		}

		@Override
		public String toString() {
			return representation().toString();
		}

		@Override
		public int hashCode() {
			return Objects.hash(id);
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
			Liv other = (Liv) obj;
			return Objects.equals(id, other.id);
		}

		@Override
		public Liv replaceIdentifier(
				Identifier source,
				Identifier target) {
			SymbolicExpression e = id.replace(source, target);
			if (e == id)
				return this;
			return new Liv((Identifier) e);
		}

	}

}

package it.unive.lisa.analysis.dataflow;

import it.unive.lisa.analysis.ScopeToken;
import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.program.cfg.ProgramPoint;
import it.unive.lisa.symbolic.SymbolicExpression;
import it.unive.lisa.symbolic.value.Identifier;
import it.unive.lisa.symbolic.value.ValueExpression;
import it.unive.lisa.util.representation.ListRepresentation;
import it.unive.lisa.util.representation.StringRepresentation;
import it.unive.lisa.util.representation.StructuredRepresentation;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * An implementation of the reaching definition dataflow analysis.
 * 
 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
 */
public class ReachingDefinitions extends DataflowDomain<PossibleSet<ReachingDefinitions.RD>, ReachingDefinitions.RD> {

	@Override
	public PossibleSet<RD> makeLattice() {
		return new PossibleSet<>();
	}

	@Override
	public Set<RD> gen(
			PossibleSet<RD> state,
			Identifier id,
			ValueExpression expression,
			ProgramPoint pp) {
		return Collections.singleton(new RD(id, pp));
	}

	@Override
	public Set<RD> gen(
			PossibleSet<RD> state,
			ValueExpression expression,
			ProgramPoint pp) {
		return Collections.emptySet();
	}

	@Override
	public Set<RD> kill(
			PossibleSet<RD> state,
			Identifier id,
			ValueExpression expression,
			ProgramPoint pp) {
		Set<RD> result = new HashSet<>();

		for (RD rd : state.getDataflowElements())
			if (rd.variable.equals(id))
				result.add(rd);

		return result;
	}

	@Override
	public Set<RD> kill(
			PossibleSet<RD> state,
			ValueExpression expression,
			ProgramPoint pp) {
		return Collections.emptySet();
	}

	/**
	 * A reaching definition dataflow element, that is, a pair of an identifier
	 * and a program point where the definition of the identifier happens.
	 * 
	 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
	 */
	public static class RD implements DataflowElement<RD> {

		private final Identifier variable;

		private final ProgramPoint programPoint;

		private RD(
				Identifier variable,
				ProgramPoint programPoint) {
			this.variable = variable;
			this.programPoint = programPoint;
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + ((programPoint == null) ? 0 : programPoint.hashCode());
			result = prime * result + ((variable == null) ? 0 : variable.hashCode());
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
			RD other = (RD) obj;
			if (programPoint == null) {
				if (other.programPoint != null)
					return false;
			} else if (!programPoint.equals(other.programPoint))
				return false;
			if (variable == null) {
				if (other.variable != null)
					return false;
			} else if (!variable.equals(other.variable))
				return false;
			return true;
		}

		@Override
		public StructuredRepresentation representation() {
			return new ListRepresentation(new StringRepresentation(variable), new StringRepresentation(programPoint));
		}

		@Override
		public String toString() {
			return representation().toString();
		}

		@Override
		public Collection<Identifier> getInvolvedIdentifiers() {
			return Collections.singleton(variable);
		}

		@Override
		public RD pushScope(
				ScopeToken scope,
				ProgramPoint pp)
				throws SemanticException {
			return new RD((Identifier) variable.pushScope(scope, pp), programPoint);
		}

		@Override
		public RD popScope(
				ScopeToken scope,
				ProgramPoint pp)
				throws SemanticException {
			if (!variable.canBeScoped())
				return this;

			SymbolicExpression popped = variable.popScope(scope, pp);
			if (popped == null)
				return null;

			return new RD((Identifier) popped, programPoint);
		}

		@Override
		public RD replaceIdentifier(
				Identifier source,
				Identifier target) {
			SymbolicExpression e = variable.replace(source, target);
			if (e == variable)
				return this;
			return new RD((Identifier) e, programPoint);
		}

	}

}

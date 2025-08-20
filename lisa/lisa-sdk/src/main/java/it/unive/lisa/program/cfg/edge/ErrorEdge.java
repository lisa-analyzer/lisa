package it.unive.lisa.program.cfg.edge;

import it.unive.lisa.analysis.AbstractDomain;
import it.unive.lisa.analysis.AbstractLattice;
import it.unive.lisa.analysis.Analysis;
import it.unive.lisa.analysis.AnalysisState;
import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.program.cfg.statement.Statement;
import it.unive.lisa.program.cfg.statement.VariableRef;
import it.unive.lisa.type.Type;
import java.util.Arrays;
import java.util.Set;
import java.util.TreeSet;
import org.apache.commons.lang3.StringUtils;

/**
 * An edge that transfers control flow to its destination only if an exception
 * of specific types has been thrown previously and it reached its source.
 * 
 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
 */
public class ErrorEdge
		extends
		Edge {

	private final VariableRef variable;

	private final Type[] types;

	/**
	 * Builds the edge.
	 * 
	 * @param source      the source statement
	 * @param destination the destination statement
	 * @param variable    the variable that is being caught by this edge, or
	 *                        {@code null} if this edge does not catch any
	 *                        variable
	 * @param types       the types of exceptions that are caught by this edge
	 */
	public ErrorEdge(
			Statement source,
			Statement destination,
			VariableRef variable,
			Type... types) {
		super(source, destination);
		this.variable = variable;
		this.types = types;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = super.hashCode();
		result = prime * result + ((variable == null) ? 0 : variable.hashCode());
		result = prime * result + Arrays.hashCode(types);
		return result;
	}

	@Override
	public boolean equals(
			Object obj) {
		if (this == obj)
			return true;
		if (!super.equals(obj))
			return false;
		if (getClass() != obj.getClass())
			return false;
		ErrorEdge other = (ErrorEdge) obj;
		if (variable == null) {
			if (other.variable != null)
				return false;
		} else if (!variable.equals(other.variable))
			return false;
		if (!Arrays.equals(types, other.types))
			return false;
		return true;
	}

	@Override
	public String toString() {
		Set<String> typeNames = new TreeSet<>();
		for (Type type : types)
			typeNames.add(type.toString());
		return "[ "
				+ getSource()
				+ " ] -("
				+ StringUtils.join(typeNames, ", ")
				+ " = "
				+ (variable == null ? "<no-var>" : variable.getName())
				+ ")-> [ "
				+ getDestination()
				+ " ]";
	}

	@Override
	public String getLabel() {
		Set<String> typeNames = new TreeSet<>();
		for (Type type : types)
			typeNames.add(type.toString());
		return StringUtils.join(typeNames, ", ") + (variable == null ? "" : ", variable: " + variable.getName());
	}

	@Override
	public <A extends AbstractLattice<A>, D extends AbstractDomain<A>> AnalysisState<A> traverseForward(
			AnalysisState<A> state,
			Analysis<A, D> analysis)
			throws SemanticException {
		// TODO implement the semantics
		// TODO take into account also other error edges from the same source,
		// to ensure that an error is caught only by the most specific catch
		return state;
	}

	@Override
	public <A extends AbstractLattice<A>, D extends AbstractDomain<A>> AnalysisState<A> traverseBackwards(
			AnalysisState<A> state,
			Analysis<A, D> analysis)
			throws SemanticException {
		return traverseForward(state, analysis);
	}

	@Override
	public boolean isUnconditional() {
		return false;
	}

	@Override
	public boolean isErrorHandling() {
		return true;
	}

	@Override
	public boolean isFinallyRelated() {
		return false;
	}

	@Override
	public ErrorEdge newInstance(
			Statement source,
			Statement destination) {
		return new ErrorEdge(source, destination, variable, types);
	}

}

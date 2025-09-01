package it.unive.lisa.program.cfg.edge;

import it.unive.lisa.analysis.AbstractDomain;
import it.unive.lisa.analysis.AbstractLattice;
import it.unive.lisa.analysis.Analysis;
import it.unive.lisa.analysis.AnalysisState;
import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.program.cfg.protection.ProtectedBlock;
import it.unive.lisa.program.cfg.statement.Statement;
import it.unive.lisa.program.cfg.statement.VariableRef;
import it.unive.lisa.type.Type;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
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

	private final ProtectedBlock protectedBlock;

	/**
	 * Builds the edge.
	 * 
	 * @param source      the source statement
	 * @param destination the destination statement
	 * @param variable    the variable that is being caught by this edge, or
	 *                        {@code null} if this edge does not catch any
	 *                        variable
	 * @param protectedBlock the block that is protected by this edge
	 * @param types       the types of exceptions that are caught by this edge
	 */
	public ErrorEdge(
			Statement source,
			Statement destination,
			VariableRef variable,
			ProtectedBlock protectedBlock,
			Type... types) {
		super(source, destination);
		this.variable = variable;
		this.types = types;
		this.protectedBlock = protectedBlock;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = super.hashCode();
		result = prime * result + ((variable == null) ? 0 : variable.hashCode());
		result = prime * result + ((protectedBlock == null) ? 0 : protectedBlock.hashCode());
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
		if (protectedBlock == null) {
			if (other.protectedBlock != null)
				return false;
		} else if (!protectedBlock.equals(other.protectedBlock))
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
		Collection<Type> excluded = new HashSet<>();
		for (Edge other : getSource().getCFG().getOutgoingEdges(getSource()))
			if (other instanceof ErrorEdge && this != other)
				for (Type ex : ((ErrorEdge) other).types)
					for (Type e : types)
						if (!e.equals(ex) && ex.canBeAssignedTo(e))
							// ex is a more specific error than e,
							// so we leave it to the other catch
							excluded.add(ex);

		return analysis.moveErrorsToExecution(state, protectedBlock, Arrays.asList(types), excluded, variable);
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
	public ErrorEdge newInstance(
			Statement source,
			Statement destination) {
		return new ErrorEdge(source, destination, variable, protectedBlock, types);
	}

}

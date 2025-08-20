package it.unive.lisa.program.cfg.statement;

import it.unive.lisa.analysis.AbstractDomain;
import it.unive.lisa.analysis.AbstractLattice;
import it.unive.lisa.analysis.AnalysisState;
import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.analysis.StatementStore;
import it.unive.lisa.interprocedural.InterproceduralAnalysis;
import it.unive.lisa.program.cfg.CFG;
import it.unive.lisa.program.cfg.CodeLocation;
import it.unive.lisa.program.cfg.edge.Edge;
import it.unive.lisa.util.collections.CollectionUtilities;
import it.unive.lisa.util.datastructures.graph.GraphVisitor;

/**
 * A continue statement, which is used to skip the rest of the current iteration
 * of a loop.
 * 
 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
 */
public class Continue
		extends
		Statement {

	private final String label;

	/**
	 * Builds a continue statement.
	 * 
	 * @param cfg      the control flow graph to which this statement belongs
	 * @param location the location of this statement in the source code
	 * @param label    the label that this continue statement refers to, if any
	 *                     (if {@code null}, this continue statement does not
	 *                     refer to any label)
	 */
	public Continue(
			CFG cfg,
			CodeLocation location,
			String label) {
		super(cfg, location);
		this.label = label;
	}

	/**
	 * Yields whether this continue statement has a target label.
	 * 
	 * @return {@code true} if this continue statement has a target label,
	 *             {@code false} otherwise
	 */
	public boolean hasLabel() {
		return label != null;
	}

	/**
	 * Yields the label that this continue statement refers to, if any.
	 * 
	 * @return the label, or {@code null}
	 */
	public String getLabel() {
		return label;
	}

	@Override
	public boolean continuesControlFlow() {
		return true;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = super.hashCode();
		result = prime * result + ((label == null) ? 0 : label.hashCode());
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
		Continue other = (Continue) obj;
		if (label == null) {
			if (other.label != null)
				return false;
		} else if (!label.equals(other.label))
			return false;
		return true;
	}

	@Override
	public String toString() {
		return "continue" + (label != null ? " " + label : "");
	}

	@Override
	public <V> boolean accept(
			GraphVisitor<CFG, Statement, Edge, V> visitor,
			V tool) {
		return visitor.visit(tool, getCFG(), this);
	}

	@Override
	public <A extends AbstractLattice<A>, D extends AbstractDomain<A>> AnalysisState<A> forwardSemantics(
			AnalysisState<A> entryState,
			InterproceduralAnalysis<A, D> interprocedural,
			StatementStore<A> expressions)
			throws SemanticException {
		return entryState;
	}

	@Override
	protected int compareSameClass(
			Statement o) {
		return CollectionUtilities.nullSafeCompare(true, label, ((Continue) o).label, String::compareTo);
	}

}

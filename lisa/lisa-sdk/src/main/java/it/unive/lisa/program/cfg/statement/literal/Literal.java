package it.unive.lisa.program.cfg.statement.literal;

import it.unive.lisa.analysis.AbstractState;
import it.unive.lisa.analysis.AnalysisState;
import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.analysis.StatementStore;
import it.unive.lisa.analysis.heap.HeapDomain;
import it.unive.lisa.analysis.value.TypeDomain;
import it.unive.lisa.analysis.value.ValueDomain;
import it.unive.lisa.interprocedural.InterproceduralAnalysis;
import it.unive.lisa.program.cfg.CFG;
import it.unive.lisa.program.cfg.CodeLocation;
import it.unive.lisa.program.cfg.edge.Edge;
import it.unive.lisa.program.cfg.statement.Expression;
import it.unive.lisa.program.cfg.statement.Statement;
import it.unive.lisa.symbolic.value.Constant;
import it.unive.lisa.type.Type;
import it.unive.lisa.util.datastructures.graph.GraphVisitor;

/**
 * A literal, representing a constant value.
 * 
 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
 * 
 * @param <E> the type of constant represented by this literal
 */
public abstract class Literal<E> extends Expression {

	/**
	 * The value of this literal
	 */
	private final E value;

	/**
	 * Builds a typed literal, consisting of a constant value, happening at the
	 * given location in the program.
	 * 
	 * @param cfg        the cfg that this expression belongs to
	 * @param location   the location where the expression is defined within the
	 *                       program
	 * @param value      the value of this literal
	 * @param staticType the type of this literal
	 */
	public Literal(CFG cfg, CodeLocation location, E value, Type staticType) {
		super(cfg, location, staticType);
		this.value = value;
	}

	/**
	 * Yields the value of this literal.
	 * 
	 * @return the value of this literal
	 */
	public E getValue() {
		return value;
	}

	@Override
	public int setOffset(int offset) {
		return this.offset = offset;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = super.hashCode();
		result = prime * result + ((value == null) ? 0 : value.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (!super.equals(obj))
			return false;
		if (getClass() != obj.getClass())
			return false;
		Literal<?> other = (Literal<?>) obj;
		if (value == null) {
			if (other.value != null)
				return false;
		} else if (!value.equals(other.value))
			return false;
		return true;
	}

	@Override
	public String toString() {
		return String.valueOf(value);
	}

	@Override
	public <A extends AbstractState<A, H, V, T>,
			H extends HeapDomain<H>,
			V extends ValueDomain<V>,
			T extends TypeDomain<T>> AnalysisState<A, H, V, T> semantics(
					AnalysisState<A, H, V, T> entryState,
					InterproceduralAnalysis<A, H, V, T> interprocedural,
					StatementStore<A, H, V, T> expressions)
					throws SemanticException {
		return entryState.smallStepSemantics(new Constant(getStaticType(), getValue(), getLocation()), this);
	}

	@Override
	public <V> boolean accept(GraphVisitor<CFG, Statement, Edge, V> visitor, V tool) {
		return visitor.visit(tool, getCFG(), this);
	}
}

package it.unive.lisa.program.cfg.statement;

import it.unive.lisa.analysis.AbstractState;
import it.unive.lisa.analysis.AnalysisState;
import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.analysis.StatementStore;
import it.unive.lisa.analysis.heap.HeapDomain;
import it.unive.lisa.analysis.value.TypeDomain;
import it.unive.lisa.analysis.value.ValueDomain;
import it.unive.lisa.interprocedural.InterproceduralAnalysis;
import it.unive.lisa.program.annotations.Annotation;
import it.unive.lisa.program.annotations.Annotations;
import it.unive.lisa.program.cfg.CFG;
import it.unive.lisa.program.cfg.CodeLocation;
import it.unive.lisa.program.cfg.VariableTableEntry;
import it.unive.lisa.program.cfg.edge.Edge;
import it.unive.lisa.symbolic.SymbolicExpression;
import it.unive.lisa.symbolic.value.Variable;
import it.unive.lisa.type.Type;
import it.unive.lisa.type.Untyped;
import it.unive.lisa.util.datastructures.graph.GraphVisitor;
import java.util.Objects;

/**
 * A reference to a variable of the current CFG, identified by its name.
 * 
 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
 */
public class VariableRef extends Expression {

	/**
	 * The name of this variable
	 */
	private final String name;

	/**
	 * Builds the untyped variable reference, identified by its name. The type
	 * of this variable reference is {@link Untyped#INSTANCE}.
	 * 
	 * @param cfg      the cfg that this expression belongs to
	 * @param location the location of this variable reference
	 * @param name     the name of this variable reference
	 */
	public VariableRef(CFG cfg, CodeLocation location, String name) {
		this(cfg, location, name, Untyped.INSTANCE);
	}

	/**
	 * Builds the variable reference, identified by its name, happening at the
	 * given location in the program.
	 * 
	 * @param cfg      the cfg that this expression belongs to
	 * @param location the location where the expression is defined within the
	 *                     program
	 * @param name     the name of this variable
	 * @param type     the type of this variable
	 */
	public VariableRef(CFG cfg, CodeLocation location, String name, Type type) {
		super(cfg, location, type);
		Objects.requireNonNull(name, "The name of a variable cannot be null");
		this.name = name;
	}

	@Override
	public int setOffset(int offset) {
		return this.offset = offset;
	}

	/**
	 * Yields the name of this variable.
	 * 
	 * @return the name of this variable
	 */
	public String getName() {
		return name;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = super.hashCode();
		result = prime * result + ((name == null) ? 0 : name.hashCode());
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
		VariableRef other = (VariableRef) obj;
		if (name == null) {
			if (other.name != null)
				return false;
		} else if (!name.equals(other.name))
			return false;
		return true;
	}

	@Override
	public String toString() {
		return name;
	}

	/**
	 * Yields a {@link Variable} representing the referenced variable.
	 * 
	 * @return the expression representing the variable
	 */
	public Variable getVariable() {
		Variable v = new Variable(getStaticType(), getName(), getLocation());
		for (Annotation ann : getAnnotations())
			v.addAnnotation(ann);
		return v;
	}

	@Override
	public <A extends AbstractState<A, H, V, T>,
			H extends HeapDomain<H>,
			V extends ValueDomain<V>,
			T extends TypeDomain<T>> AnalysisState<A, H, V, T> semantics(
					AnalysisState<A, H, V, T> entryState, InterproceduralAnalysis<A, H, V, T> interprocedural,
					StatementStore<A, H, V, T> expressions)
					throws SemanticException {
		SymbolicExpression expr = getVariable();
		return entryState.smallStepSemantics(expr, this);
	}

	@Override
	public <V> boolean accept(GraphVisitor<CFG, Statement, Edge, V> visitor, V tool) {
		return visitor.visit(tool, getCFG(), this);
	}

	/**
	 * Yields the annotations of this variable, retrieved from the variable
	 * table of the cfg this variable belongs to.
	 * 
	 * @return the annotations of this variable.
	 */
	public Annotations getAnnotations() {
		// FIXME the iteration should be performed inside the descriptor
		for (VariableTableEntry entry : getCFG().getDescriptor().getVariables())
			if (entry.getName().equals(getName()))
				return entry.getAnnotations();
		return new Annotations();
	}
}

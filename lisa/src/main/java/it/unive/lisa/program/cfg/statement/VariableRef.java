package it.unive.lisa.program.cfg.statement;

import java.util.Objects;

import it.unive.lisa.analysis.AbstractState;
import it.unive.lisa.analysis.AnalysisState;
import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.analysis.StatementStore;
import it.unive.lisa.analysis.heap.HeapDomain;
import it.unive.lisa.analysis.value.ValueDomain;
import it.unive.lisa.interprocedural.InterproceduralAnalysis;
import it.unive.lisa.program.cfg.CFG;
import it.unive.lisa.program.cfg.CodeLocation;
import it.unive.lisa.program.cfg.edge.Edge;
import it.unive.lisa.symbolic.SymbolicExpression;
import it.unive.lisa.symbolic.heap.HeapReference;
import it.unive.lisa.symbolic.value.ValueIdentifier;
import it.unive.lisa.type.Type;
import it.unive.lisa.type.Untyped;
import it.unive.lisa.util.datastructures.graph.GraphVisitor;

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
	 * Builds the untyped variable reference, identified by its name. The
	 * location where this variable reference happens is unknown (i.e. no source
	 * file/line/column is available) and its type is {@link Untyped#INSTANCE}.
	 * 
	 * @param cfg  the cfg that this expression belongs to
	 * @param name the name of this variable
	 */
	public VariableRef(CFG cfg, String name) {
		this(cfg, name, Untyped.INSTANCE);
	}

	/**
	 * Builds a typed variable reference, identified by its name and its type.
	 * The location where this variable reference happens is unknown (i.e. no
	 * source file/line/column is available).
	 * 
	 * @param cfg  the cfg that this expression belongs to
	 * @param name the name of this variable
	 * @param type the type of this variable
	 */
	public VariableRef(CFG cfg, String name, Type type) {
		this(cfg, null, name, type);
	}

	/**
	 * Builds the variable reference, identified by its name, happening at the
	 * given location in the program.
	 * 
	 * @param cfg      the cfg that this expression belongs to
	 * @param location the location where the expression is defined within the
	 *                     source file. If unknown, use {@code null}
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
	public boolean isEqualTo(Statement st) {
		if (this == st)
			return true;
		if (getClass() != st.getClass())
			return false;
		if (!super.isEqualTo(st))
			return false;
		VariableRef other = (VariableRef) st;
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
	 * Yields a {@link SymbolicExpression} representing the referenced variable.
	 * 
	 * @return the expression representing the variable
	 */
	public SymbolicExpression getVariable() {
		SymbolicExpression expr;
		if (getStaticType().isPointerType())
			// the smallStepSemantics will take care of converting that
			// reference to a variable identifier
			// setting also the identifier as computed expression
			expr = new HeapReference(getRuntimeTypes(), getName());
		else
			expr = new ValueIdentifier(getRuntimeTypes(), getName());
		return expr;
	}

	@Override
	public <A extends AbstractState<A, H, V>,
			H extends HeapDomain<H>,
			V extends ValueDomain<V>> AnalysisState<A, H, V> semantics(
					AnalysisState<A, H, V> entryState, InterproceduralAnalysis<A, H, V> interprocedural,
					StatementStore<A, H, V> expressions)
					throws SemanticException {
		SymbolicExpression expr = getVariable();
		return entryState.smallStepSemantics(expr, this);
	}

	@Override
	public <V> boolean accept(GraphVisitor<CFG, Statement, Edge, V> visitor, V tool) {
		return visitor.visit(tool, getCFG(), this);
	}
}

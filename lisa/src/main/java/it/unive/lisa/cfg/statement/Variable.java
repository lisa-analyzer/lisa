package it.unive.lisa.cfg.statement;

import java.util.Objects;

import it.unive.lisa.analysis.AbstractState;
import it.unive.lisa.analysis.AnalysisState;
import it.unive.lisa.analysis.HeapDomain;
import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.analysis.StatementStore;
import it.unive.lisa.analysis.ValueDomain;
import it.unive.lisa.analysis.impl.types.TypeEnvironment;
import it.unive.lisa.callgraph.CallGraph;
import it.unive.lisa.cfg.CFG;
import it.unive.lisa.cfg.type.Type;
import it.unive.lisa.cfg.type.Untyped;
import it.unive.lisa.symbolic.SymbolicExpression;
import it.unive.lisa.symbolic.heap.HeapReference;
import it.unive.lisa.symbolic.value.ValueIdentifier;

/**
 * A reference to a variable of the current CFG, identified by its name.
 * 
 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
 */
public class Variable extends Expression {

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
	public Variable(CFG cfg, String name) {
		this(cfg, null, -1, -1, name, Untyped.INSTANCE);
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
	public Variable(CFG cfg, String name, Type type) {
		this(cfg, null, -1, -1, name, type);
	}

	/**
	 * Builds the variable reference, identified by its name, happening at the
	 * given location in the program.
	 * 
	 * @param cfg        the cfg that this expression belongs to
	 * @param sourceFile the source file where this expression happens. If
	 *                       unknown, use {@code null}
	 * @param line       the line number where this expression happens in the
	 *                       source file. If unknown, use {@code -1}
	 * @param col        the column where this expression happens in the source
	 *                       file. If unknown, use {@code -1}
	 * @param name       the name of this variable
	 * @param type       the type of this variable
	 */
	public Variable(CFG cfg, String sourceFile, int line, int col, String name, Type type) {
		super(cfg, sourceFile, line, col, type);
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
		Variable other = (Variable) st;
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

	private SymbolicExpression getVariable() {
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
	public <A extends AbstractState<A, H, TypeEnvironment>, H extends HeapDomain<H>> AnalysisState<A, H, TypeEnvironment> typeInference(
			AnalysisState<A, H, TypeEnvironment> entryState, CallGraph callGraph,
			StatementStore<A, H, TypeEnvironment> expressions) throws SemanticException {
		AnalysisState<A, H, TypeEnvironment> typing = entryState.smallStepSemantics(getVariable());
		setRuntimeTypes(typing.getState().getValueState().getLastComputedTypes().getRuntimeTypes());
		// we have to recreate the variable for it to have the correct typing
		// information
		return typing.smallStepSemantics(getVariable());
	}

	@Override
	public <A extends AbstractState<A, H, V>,H extends HeapDomain<H>, V extends ValueDomain<V>> AnalysisState<A, H, V> semantics(
			AnalysisState<A, H, V> entryState, CallGraph callGraph, StatementStore<A, H, V> expressions)
			throws SemanticException {
		SymbolicExpression expr = getVariable();
		return entryState.smallStepSemantics(expr);
	}
}

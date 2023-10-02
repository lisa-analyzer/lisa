package it.unive.lisa.program.cfg.statement;

import it.unive.lisa.analysis.AbstractState;
import it.unive.lisa.analysis.AnalysisState;
import it.unive.lisa.analysis.Lattice;
import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.analysis.StatementStore;
import it.unive.lisa.interprocedural.InterproceduralAnalysis;
import it.unive.lisa.program.cfg.CFG;
import it.unive.lisa.program.cfg.CodeLocation;
import it.unive.lisa.program.cfg.edge.Edge;
import it.unive.lisa.symbolic.value.PushAny;
import it.unive.lisa.type.Type;
import it.unive.lisa.util.datastructures.graph.GraphVisitor;

/**
 * An {@link Expression} that can be used on the right-hand side of an
 * assignment to initialize a variable or parameter of a given type to a
 * statically unknown value. To do this, this class' semantics simply leaves a
 * {@link PushAny} on the stack. Instances of this class can be returned by
 * {@link Type#unknownValue(CFG, CodeLocation)}, but should not be directly
 * placed inside {@link CFG}s at parse time.
 * 
 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
 */
public class DefaultParamInitialization extends Expression {

	/**
	 * Builds the initializing expression.
	 * 
	 * @param cfg      the {@link CFG} where the initialization will happen
	 * @param location the {@link CodeLocation} where the initialization will
	 *                     happen
	 * @param type     the static type of the variable or parameter to
	 *                     initialize
	 */
	public DefaultParamInitialization(
			CFG cfg,
			CodeLocation location,
			Type type) {
		super(cfg, location, type);
	}

	@Override
	public int setOffset(
			int offset) {
		throw new UnsupportedOperationException();
	}

	@Override
	public <V> boolean accept(
			GraphVisitor<CFG, Statement, Edge, V> visitor,
			V tool) {
		return visitor.visit(tool, getCFG(), this);
	}

	@Override
	public String toString() {
		return Lattice.TOP_STRING;
	}

	@Override
	public <A extends AbstractState<A>> AnalysisState<A> semantics(
			AnalysisState<A> entryState,
			InterproceduralAnalysis<A> interprocedural,
			StatementStore<A> expressions)
			throws SemanticException {
		return entryState.smallStepSemantics(new PushAny(getStaticType(), getLocation()), this);
	}
}

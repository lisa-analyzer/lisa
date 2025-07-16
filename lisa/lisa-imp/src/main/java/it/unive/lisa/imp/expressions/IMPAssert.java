package it.unive.lisa.imp.expressions;

import it.unive.lisa.analysis.AbstractDomain;
import it.unive.lisa.analysis.AbstractLattice;
import it.unive.lisa.analysis.Analysis;
import it.unive.lisa.analysis.AnalysisState;
import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.analysis.StatementStore;
import it.unive.lisa.interprocedural.InterproceduralAnalysis;
import it.unive.lisa.program.SourceCodeLocation;
import it.unive.lisa.program.cfg.CFG;
import it.unive.lisa.program.cfg.statement.Expression;
import it.unive.lisa.program.cfg.statement.Statement;
import it.unive.lisa.program.cfg.statement.UnaryStatement;
import it.unive.lisa.symbolic.SymbolicExpression;
import it.unive.lisa.symbolic.value.Skip;
import it.unive.lisa.type.Type;
import java.util.Set;

/**
 * An assertion in an IMP program.
 * 
 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
 */
public class IMPAssert
		extends
		UnaryStatement {

	/**
	 * Builds the assertion.
	 * 
	 * @param cfg        the {@link CFG} where this operation lies
	 * @param sourceFile the source file name where this operation is defined
	 * @param line       the line number where this operation is defined
	 * @param col        the column where this operation is defined
	 * @param expression the expression being asserted
	 */
	public IMPAssert(
			CFG cfg,
			String sourceFile,
			int line,
			int col,
			Expression expression) {
		super(cfg, new SourceCodeLocation(sourceFile, line, col), "assert", expression);
	}

	@Override
	protected int compareSameClassAndParams(
			Statement o) {
		return 0; // no extra fields to compare
	}

	@Override
	public <A extends AbstractLattice<A>,
			D extends AbstractDomain<A>> AnalysisState<A> fwdUnarySemantics(
					InterproceduralAnalysis<A, D> interprocedural,
					AnalysisState<A> state,
					SymbolicExpression expr,
					StatementStore<A> expressions)
					throws SemanticException {
		Analysis<A, D> analysis = interprocedural.getAnalysis();
		Set<Type> types = analysis.getRuntimeTypesOf(state, expr, this);
		if (types.stream().noneMatch(Type::isBooleanType))
			return state.bottom();
		return analysis.smallStepSemantics(state, new Skip(getLocation()), this);
	}

}

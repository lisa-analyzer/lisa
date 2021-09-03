package it.unive.lisa.program.cfg.statement;

import it.unive.lisa.analysis.AbstractState;
import it.unive.lisa.analysis.AnalysisState;
import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.analysis.StatementStore;
import it.unive.lisa.analysis.heap.HeapDomain;
import it.unive.lisa.analysis.value.ValueDomain;
import it.unive.lisa.interprocedural.InterproceduralAnalysis;
import it.unive.lisa.program.cfg.CFG;
import it.unive.lisa.program.cfg.CodeLocation;
import it.unive.lisa.program.cfg.ProgramPoint;
import it.unive.lisa.symbolic.SymbolicExpression;
import it.unive.lisa.symbolic.value.Identifier;

/**
 * A statement assigning the result of an expression to an assignable
 * expression.
 * 
 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
 */
public class Assignment extends BinaryExpression {

	/**
	 * Builds the assignment, assigning {@code expression} to {@code target},
	 * happening at the given location in the program.
	 * 
	 * @param cfg        the cfg that this statement belongs to
	 * @param location   the location where this statement is defined within the
	 *                       source file. If unknown, use {@code null}
	 * @param target     the target of the assignment
	 * @param expression the expression to assign to {@code target}
	 */
	public Assignment(CFG cfg, CodeLocation location, Expression target, Expression expression) {
		super(cfg, location, target, expression);
	}

	@Override
	public final String toString() {
		return getLeft() + " = " + getRight();
	}

	/**
	 * Semantics of an assignment ({@code left = right}) is evaluated as
	 * follows:
	 * <ol>
	 * <li>the semantic of the {@code right} is evaluated using the given
	 * {@code entryState}, returning a new analysis state
	 * {@code as_r = <state_r, expr_r>}</li>
	 * <li>the semantic of the {@code left} is evaluated using {@code as_r},
	 * returning a new analysis state {@code as_l = <state_l, expr_l>}</li>
	 * <li>the final post-state is evaluated through
	 * {@link AnalysisState#assign(Identifier, SymbolicExpression, ProgramPoint)},
	 * using {@code expr_l} as {@code id} and {@code expr_r} as
	 * {@code value}</li>
	 * </ol>
	 * This means that all side effects from {@code right} are evaluated before
	 * the ones from {@code left}.<br>
	 * <br>
	 * {@inheritDoc}
	 */
	@Override
	public final <A extends AbstractState<A, H, V>,
			H extends HeapDomain<H>,
			V extends ValueDomain<V>> AnalysisState<A, H, V> semantics(
					AnalysisState<A, H, V> entryState, InterproceduralAnalysis<A, H, V> interprocedural,
					StatementStore<A, H, V> expressions)
					throws SemanticException {
		AnalysisState<A, H, V> right = getRight().semantics(entryState, interprocedural, expressions);
		AnalysisState<A, H, V> left = getLeft().semantics(right, interprocedural, expressions);
		expressions.put(getRight(), right);
		expressions.put(getLeft(), left);

		AnalysisState<A, H, V> result = right.bottom();
		for (SymbolicExpression expr1 : left.getComputedExpressions())
			for (SymbolicExpression expr2 : right.getComputedExpressions()) {
				AnalysisState<A, H, V> tmp = left.assign(expr1, expr2, this);
				result = result.lub(tmp);
			}

		if (!getRight().getMetaVariables().isEmpty())
			result = result.forgetIdentifiers(getRight().getMetaVariables());
		if (!getLeft().getMetaVariables().isEmpty())
			result = result.forgetIdentifiers(getLeft().getMetaVariables());
		return result;
	}
}

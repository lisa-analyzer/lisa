package it.unive.lisa.program.language.parameterassignment;

import it.unive.lisa.analysis.AbstractState;
import it.unive.lisa.analysis.AnalysisState;
import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.analysis.StatementStore;
import it.unive.lisa.analysis.heap.HeapDomain;
import it.unive.lisa.analysis.lattices.ExpressionSet;
import it.unive.lisa.analysis.value.TypeDomain;
import it.unive.lisa.analysis.value.ValueDomain;
import it.unive.lisa.interprocedural.InterproceduralAnalysis;
import it.unive.lisa.program.cfg.Parameter;
import it.unive.lisa.program.cfg.statement.Expression;
import it.unive.lisa.program.cfg.statement.call.Call;
import it.unive.lisa.program.cfg.statement.call.NamedParameterExpression;
import it.unive.lisa.program.language.resolution.PythonLikeMatchingStrategy;
import it.unive.lisa.symbolic.SymbolicExpression;
import it.unive.lisa.type.Type;
import java.util.Set;
import org.apache.commons.lang3.tuple.Pair;

/**
 * A Python-like assigning strategy. Specifically:<br>
 * <i>If keyword arguments are present, they are first converted to positional
 * arguments, as follows. First, a list of unfilled slots is created for the
 * formal parameters. If there are N positional arguments, they are placed in
 * the first N slots. Next, for each keyword argument, the identifier is used to
 * determine the corresponding slot (if the identifier is the same as the first
 * formal parameter name, the first slot is used, and so on). If the slot is
 * already filled, a TypeError exception is raised. Otherwise, the value of the
 * argument is placed in the slot, filling it (even if the expression is None,
 * it fills the slot). When all arguments have been processed, the slots that
 * are still unfilled are filled with the corresponding default value from the
 * function definition. [...] If there are any unfilled slots for which no
 * default value is specified, a TypeError exception is raised. Otherwise, the
 * list of filled slots is used as the argument list for the call. [...] If
 * there are more positional arguments than there are formal parameter slots, a
 * TypeError exception is raised [...]. If any keyword argument does not
 * correspond to a formal parameter name, a TypeError exception is raised
 * [...].</i><br>
 * The only difference w.r.t. this specification is that whenever a TypeError
 * exception should be raised, this strategy returns the bottom state without
 * throwing errors.<br>
 * <br>
 * Keyword parameters are identified by actuals (i.e. {@link Expression}s) that
 * are instance of {@link NamedParameterExpression}.
 * 
 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
 * 
 * @see <a href=
 *          "https://docs.python.org/3/reference/expressions.html#calls">Python
 *          Language Reference: calls</a>
 */
public class PythonLikeAssigningStrategy implements ParameterAssigningStrategy {

	/**
	 * The singleton instance of this class.
	 */
	public static final PythonLikeAssigningStrategy INSTANCE = new PythonLikeAssigningStrategy();

	private PythonLikeAssigningStrategy() {
	}

	@Override
	@SuppressWarnings("unchecked")
	public <A extends AbstractState<A, H, V, T>,
			H extends HeapDomain<H>,
			V extends ValueDomain<V>,
			T extends TypeDomain<T>> Pair<AnalysisState<A, H, V, T>, ExpressionSet<SymbolicExpression>[]> prepare(
					Call call,
					AnalysisState<A, H, V, T> callState,
					InterproceduralAnalysis<A, H, V, T> interprocedural,
					StatementStore<A, H, V, T> expressions,
					Parameter[] formals,
					ExpressionSet<SymbolicExpression>[] parameters)
					throws SemanticException {

		ExpressionSet<SymbolicExpression>[] slots = new ExpressionSet[formals.length];
		Set<Type>[] slotsTypes = new Set[formals.length];
		Expression[] actuals = call.getParameters();

		ExpressionSet<SymbolicExpression>[] defaults = new ExpressionSet[formals.length];
		Set<Type>[] defaultTypes = new Set[formals.length];
		for (int pos = 0; pos < slots.length; pos++) {
			Expression def = formals[pos].getDefaultValue();
			if (def != null) {
				callState = def.semantics(callState, interprocedural, expressions);
				expressions.put(def, callState);
				defaults[pos] = callState.getComputedExpressions();
				defaultTypes[pos] = callState.getDomainInstance(TypeDomain.class).getInferredRuntimeTypes();
			}
		}

		AnalysisState<A, H, V, T> logic = PythonLikeMatchingStrategy.pythonLogic(
				formals,
				actuals,
				parameters,
				call.parameterTypes(expressions),
				defaults,
				defaultTypes,
				slots,
				slotsTypes,
				callState.bottom());
		if (logic != null)
			return Pair.of(logic, parameters);

		// prepare the state for the call: assign the value to each parameter
		AnalysisState<A, H, V, T> prepared = callState;
		for (int i = 0; i < formals.length; i++) {
			AnalysisState<A, H, V, T> temp = prepared.bottom();
			for (SymbolicExpression exp : slots[i])
				temp = temp.lub(prepared.assign(formals[i].toSymbolicVariable(), exp, call));
			prepared = temp;
		}

		return Pair.of(prepared, slots);
	}

}

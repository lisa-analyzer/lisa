package it.unive.lisa.imp.expressions;

import it.unive.lisa.analysis.AbstractState;
import it.unive.lisa.analysis.AnalysisState;
import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.analysis.StatementStore;
import it.unive.lisa.analysis.lattices.ExpressionSet;
import it.unive.lisa.interprocedural.InterproceduralAnalysis;
import it.unive.lisa.program.SourceCodeLocation;
import it.unive.lisa.program.cfg.CFG;
import it.unive.lisa.program.cfg.statement.Expression;
import it.unive.lisa.program.cfg.statement.InstrumentedReceiverRef;
import it.unive.lisa.program.cfg.statement.NaryExpression;
import it.unive.lisa.program.cfg.statement.Statement;
import it.unive.lisa.program.cfg.statement.call.Call.CallType;
import it.unive.lisa.program.cfg.statement.call.UnresolvedCall;
import it.unive.lisa.symbolic.SymbolicExpression;
import it.unive.lisa.symbolic.heap.HeapReference;
import it.unive.lisa.symbolic.heap.MemoryAllocation;
import it.unive.lisa.symbolic.value.Identifier;
import it.unive.lisa.type.ReferenceType;
import it.unive.lisa.type.Type;
import it.unive.lisa.type.UnitType;
import java.util.Objects;
import org.apache.commons.lang3.ArrayUtils;

/**
 * An expression modeling the object allocation and initialization operation
 * ({@code new className(...)}). The type of this expression is the
 * {@link UnitType} representing the created class. This expression corresponds
 * to a {@link MemoryAllocation} that is used as first parameter (i.e.,
 * {@code this}) for the {@link UnresolvedCall} targeting the invoked
 * constructor. All parameters of the constructor call are provided to the
 * {@link UnresolvedCall}.
 * 
 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
 */
public class IMPNewObj extends NaryExpression {

	private final boolean staticallyAllocated;

	/**
	 * Builds the object allocation and initialization.
	 * 
	 * @param cfg                 the {@link CFG} where this operation lies
	 * @param sourceFile          the source file name where this operation is
	 *                                defined
	 * @param line                the line number where this operation is
	 *                                defined
	 * @param col                 the column where this operation is defined
	 * @param type                the type of the object that is being created
	 * @param staticallyAllocated if this allocation is static or not
	 * @param parameters          the parameters of the constructor call
	 */
	public IMPNewObj(
			CFG cfg,
			String sourceFile,
			int line,
			int col,
			Type type,
			boolean staticallyAllocated,
			Expression... parameters) {
		super(cfg, new SourceCodeLocation(sourceFile, line, col), (staticallyAllocated ? "" : "new ") + type, type,
				parameters);
		this.staticallyAllocated = staticallyAllocated;
	}

	@Override
	protected int compareSameClassAndParams(
			Statement o) {
		return Boolean.compare(staticallyAllocated, ((IMPNewObj) o).staticallyAllocated);
	}

	@Override
	public <A extends AbstractState<A>> AnalysisState<A> forwardSemanticsAux(
			InterproceduralAnalysis<A> interprocedural,
			AnalysisState<A> state,
			ExpressionSet[] params,
			StatementStore<A> expressions)
			throws SemanticException {
		Type type = getStaticType();
		ReferenceType reftype = new ReferenceType(type);
		MemoryAllocation creation = new MemoryAllocation(type, getLocation(), staticallyAllocated);
		HeapReference ref = new HeapReference(reftype, creation, getLocation());

		// we start by allocating the memory region
		AnalysisState<A> allocated = state.smallStepSemantics(creation, this);

		// we need to add the receiver to the parameters of the constructor call
		InstrumentedReceiverRef paramThis = new InstrumentedReceiverRef(getCFG(), getLocation(), false, reftype);
		Expression[] fullExpressions = ArrayUtils.insert(0, getSubExpressions(), paramThis);

		// we also have to add the receiver inside the state
		AnalysisState<A> callstate = paramThis.forwardSemantics(allocated, interprocedural, expressions);
		ExpressionSet[] fullParams = ArrayUtils.insert(0, params, callstate.getComputedExpressions());

		// we store a reference to the newly created region in the receiver
		AnalysisState<A> tmp = state.bottom();
		for (SymbolicExpression rec : callstate.getComputedExpressions())
			tmp = tmp.lub(callstate.assign(rec, ref, paramThis));
		// we store the approximation of the receiver in the sub-expressions
		expressions.put(paramThis, tmp);

		// constructor call
		UnresolvedCall call = new UnresolvedCall(
				getCFG(),
				getLocation(),
				CallType.INSTANCE,
				type.toString(),
				type.toString(),
				fullExpressions);
		AnalysisState<A> sem = call.forwardSemanticsAux(interprocedural, tmp, fullParams, expressions);

		// now remove the instrumented receiver
		expressions.forget(paramThis);
		for (SymbolicExpression v : callstate.getComputedExpressions())
			if (v instanceof Identifier)
				// we leave the instrumented receiver in the program variables
				// until it is popped from the stack to keep a reference to the
				// newly created object and its fields
				getMetaVariables().add((Identifier) v);

		// finally, we leave a reference to the newly created object on the
		// stack; this correponds to the state after the constructor call
		// but with the receiver left on the stack
		return new AnalysisState<>(
				sem.getState(),
				callstate.getComputedExpressions(),
				sem.getFixpointInformation());
	}

	@Override
	public <A extends AbstractState<A>> AnalysisState<A> backwardSemanticsAux(
			InterproceduralAnalysis<A> interprocedural,
			AnalysisState<A> state,
			ExpressionSet[] params,
			StatementStore<A> expressions)
			throws SemanticException {
		// TODO implement this when backward analysis will be out of
		// beta
		throw new UnsupportedOperationException();
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = super.hashCode();
		result = prime * result + Objects.hash(staticallyAllocated);
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
		IMPNewObj other = (IMPNewObj) obj;
		return staticallyAllocated == other.staticallyAllocated;
	}
}

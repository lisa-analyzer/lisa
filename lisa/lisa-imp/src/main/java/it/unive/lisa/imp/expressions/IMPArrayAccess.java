package it.unive.lisa.imp.expressions;

import it.unive.lisa.analysis.AbstractState;
import it.unive.lisa.analysis.AnalysisState;
import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.analysis.StatementStore;
import it.unive.lisa.analysis.heap.HeapDomain;
import it.unive.lisa.analysis.value.TypeDomain;
import it.unive.lisa.analysis.value.ValueDomain;
import it.unive.lisa.interprocedural.InterproceduralAnalysis;
import it.unive.lisa.program.SourceCodeLocation;
import it.unive.lisa.program.cfg.CFG;
import it.unive.lisa.program.cfg.statement.BinaryExpression;
import it.unive.lisa.program.cfg.statement.Expression;
import it.unive.lisa.symbolic.SymbolicExpression;
import it.unive.lisa.symbolic.heap.AccessChild;
import it.unive.lisa.symbolic.heap.HeapDereference;
import it.unive.lisa.type.ArrayType;
import it.unive.lisa.type.Type;
import it.unive.lisa.type.TypeSystem;
import java.util.HashSet;
import java.util.Set;

/**
 * An expression modeling the array element access operation
 * ({@code array[index]}). The type of this expression is the one of the
 * resolved array element.
 * 
 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
 */
public class IMPArrayAccess extends BinaryExpression {

	/**
	 * Builds the array access.
	 * 
	 * @param cfg        the {@link CFG} where this operation lies
	 * @param sourceFile the source file name where this operation is defined
	 * @param line       the line number where this operation is defined
	 * @param col        the column where this operation is defined
	 * @param container  the expression representing the array reference that
	 *                       will receive the access
	 * @param location   the expression representing the accessed element
	 */
	public IMPArrayAccess(CFG cfg, String sourceFile, int line, int col, Expression container, Expression location) {
		super(cfg, new SourceCodeLocation(sourceFile, line, col), "[]", container, location);
	}

	@Override
	public <A extends AbstractState<A, H, V, T>,
			H extends HeapDomain<H>,
			V extends ValueDomain<V>,
			T extends TypeDomain<T>> AnalysisState<A, H, V, T> binarySemantics(
					InterproceduralAnalysis<A, H, V, T> interprocedural,
					AnalysisState<A, H, V, T> state,
					SymbolicExpression left,
					SymbolicExpression right,
					StatementStore<A, H, V, T> expressions)
					throws SemanticException {
		Set<Type> arraytypes = new HashSet<>();
		TypeSystem types = getProgram().getTypes();
		for (Type t : left.getRuntimeTypes(types))
			if (t.isPointerType() && t.asPointerType().getInnerType().isArrayType())
				arraytypes.add(t.asPointerType().getInnerType().asArrayType());

		if (arraytypes.isEmpty())
			return state.bottom();

		ArrayType arraytype = Type.commonSupertype(arraytypes, getStaticType()).asArrayType();
		HeapDereference container = new HeapDereference(arraytype, left, getLocation());
		container.setRuntimeTypes(arraytypes);

		return state.smallStepSemantics(new AccessChild(arraytype.getInnerType(), container, right, getLocation()),
				this);
	}
}

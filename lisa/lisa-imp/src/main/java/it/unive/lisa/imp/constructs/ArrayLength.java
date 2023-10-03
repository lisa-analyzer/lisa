package it.unive.lisa.imp.constructs;

import it.unive.lisa.analysis.AbstractState;
import it.unive.lisa.analysis.AnalysisState;
import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.analysis.StatementStore;
import it.unive.lisa.interprocedural.InterproceduralAnalysis;
import it.unive.lisa.program.Program;
import it.unive.lisa.program.SourceCodeLocation;
import it.unive.lisa.program.cfg.CFG;
import it.unive.lisa.program.cfg.CodeLocation;
import it.unive.lisa.program.cfg.CodeMemberDescriptor;
import it.unive.lisa.program.cfg.NativeCFG;
import it.unive.lisa.program.cfg.Parameter;
import it.unive.lisa.program.cfg.statement.Expression;
import it.unive.lisa.program.cfg.statement.PluggableStatement;
import it.unive.lisa.program.cfg.statement.Statement;
import it.unive.lisa.program.cfg.statement.UnaryExpression;
import it.unive.lisa.program.type.Int32Type;
import it.unive.lisa.symbolic.SymbolicExpression;
import it.unive.lisa.symbolic.heap.AccessChild;
import it.unive.lisa.symbolic.heap.HeapDereference;
import it.unive.lisa.symbolic.value.Variable;
import it.unive.lisa.type.ArrayType;
import it.unive.lisa.type.Type;
import it.unive.lisa.type.Untyped;
import java.util.HashSet;
import java.util.Set;

/**
 * The native construct representing the array length operation. This construct
 * can be invoked on an array variable {@code x} with {@code arraylen(x)}.
 * 
 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
 */
public class ArrayLength extends NativeCFG {

	/**
	 * Builds the construct.
	 * 
	 * @param location the location where this construct is defined
	 * @param program  the program of the analysis
	 */
	public ArrayLength(
			CodeLocation location,
			Program program) {
		super(new CodeMemberDescriptor(location, program, false, "arraylen", Int32Type.INSTANCE,
				new Parameter(location, "a", Untyped.INSTANCE)),
				IMPArrayLength.class);
	}

	/**
	 * An expression modeling the array length operation. The type of the
	 * operand must be {@link ArrayType}. The type of this expression is the
	 * {@link Int32Type}.
	 * 
	 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
	 */
	public static class IMPArrayLength extends UnaryExpression implements PluggableStatement {

		/**
		 * The statement that originated this one.
		 */
		protected Statement originating;

		/**
		 * Builds a new instance of this native call, according to the
		 * {@link PluggableStatement} contract.
		 * 
		 * @param cfg      the cfg where the native call happens
		 * @param location the location where the native call happens
		 * @param params   the parameters of the native call
		 * 
		 * @return the newly-built call
		 */
		public static IMPArrayLength build(
				CFG cfg,
				CodeLocation location,
				Expression... params) {
			return new IMPArrayLength(cfg, location, params[0]);
		}

		@Override
		public void setOriginatingStatement(
				Statement st) {
			originating = st;
		}

		/**
		 * Builds the length.
		 * 
		 * @param cfg        the {@link CFG} where this operation lies
		 * @param sourceFile the source file name where this operation is
		 *                       defined
		 * @param line       the line number where this operation is defined
		 * @param col        the column where this operation is defined
		 * @param parameter  the operand of this operation
		 */
		public IMPArrayLength(
				CFG cfg,
				String sourceFile,
				int line,
				int col,
				Expression parameter) {
			this(cfg, new SourceCodeLocation(sourceFile, line, col), parameter);
		}

		/**
		 * Builds the length.
		 * 
		 * @param cfg       the {@link CFG} where this operation lies
		 * @param location  the code location where this operation is defined
		 * @param parameter the operand of this operation
		 */
		public IMPArrayLength(
				CFG cfg,
				CodeLocation location,
				Expression parameter) {
			super(cfg, location, "arraylen", parameter);
		}

		@Override
		public <A extends AbstractState<A>> AnalysisState<A> unaryFwdSemantics(
				InterproceduralAnalysis<A> interprocedural,
				AnalysisState<A> state,
				SymbolicExpression expr,
				StatementStore<A> expressions)
				throws SemanticException {
			Set<Type> arraytypes = new HashSet<>();
			Set<Type> types = state.getState().getRuntimeTypesOf(expr, this, state.getState());
			for (Type t : types)
				if (t.isPointerType() && t.asPointerType().getInnerType().isArrayType())
					arraytypes.add(t.asPointerType().getInnerType());

			if (arraytypes.isEmpty())
				return state.bottom();

			ArrayType arraytype = Type.commonSupertype(arraytypes, getStaticType()).asArrayType();
			HeapDereference container = new HeapDereference(arraytype, expr, getLocation());
			AccessChild len = new AccessChild(
					Int32Type.INSTANCE,
					container,
					new Variable(Untyped.INSTANCE, "len", getLocation()),
					getLocation());

			return state.smallStepSemantics(len, this);
		}
	}
}
package it.unive.lisa.imp.constructs;

import it.unive.lisa.program.ClassUnit;
import it.unive.lisa.program.SourceCodeLocation;
import it.unive.lisa.program.cfg.CFG;
import it.unive.lisa.program.cfg.CodeLocation;
import it.unive.lisa.program.cfg.CodeMemberDescriptor;
import it.unive.lisa.program.cfg.NativeCFG;
import it.unive.lisa.program.cfg.Parameter;
import it.unive.lisa.program.cfg.statement.Expression;
import it.unive.lisa.program.cfg.statement.PluggableStatement;
import it.unive.lisa.program.cfg.statement.Statement;
import it.unive.lisa.program.cfg.statement.string.Substring;
import it.unive.lisa.type.common.BoolType;
import it.unive.lisa.type.common.Int32Type;
import it.unive.lisa.type.common.StringType;

/**
 * The native construct representing the substring operation. This construct can
 * be invoked on a string variable {@code x} with
 * {@code x.substring(start, end)}, where {@code start} is an integer
 * representing the index where the substring starts (inclusive) and {@code end}
 * is an integer representing the index where the substring ends (exclusive).
 * 
 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
 */
public class StringSubstring extends NativeCFG {

	/**
	 * Builds the construct.
	 * 
	 * @param location   the location where this construct is defined
	 * @param stringUnit the unit where this construct is defined
	 */
	public StringSubstring(CodeLocation location, ClassUnit stringUnit) {
		super(new CodeMemberDescriptor(location, stringUnit, true, "substring", BoolType.INSTANCE,
				new Parameter(location, "this", StringType.INSTANCE),
				new Parameter(location, "start", Int32Type.INSTANCE),
				new Parameter(location, "end", Int32Type.INSTANCE)),
				IMPStringSubstring.class);
	}

	/**
	 * An expression modeling the string substring operation. The type of the
	 * first operand must be {@link StringType}, while the other two operands'
	 * types must be {@link Int32Type}. The type of this expression is the
	 * {@link StringType}.
	 * 
	 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
	 */
	public static class IMPStringSubstring extends Substring implements PluggableStatement {

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
		public static IMPStringSubstring build(CFG cfg, CodeLocation location, Expression... params) {
			return new IMPStringSubstring(cfg, location, params[0], params[1], params[2]);
		}

		@Override
		public void setOriginatingStatement(Statement st) {
			originating = st;
		}

		/**
		 * Builds the substring.
		 * 
		 * @param cfg        the {@link CFG} where this operation lies
		 * @param sourceFile the source file name where this operation is
		 *                       defined
		 * @param line       the line number where this operation is defined
		 * @param col        the column where this operation is defined
		 * @param left       the left-hand side of this operation
		 * @param middle     the middle operand of this operation
		 * @param right      the right-hand side of this operation
		 */
		public IMPStringSubstring(CFG cfg, String sourceFile, int line, int col, Expression left,
				Expression middle, Expression right) {
			this(cfg, new SourceCodeLocation(sourceFile, line, col), left, middle, right);
		}

		/**
		 * Builds the substring.
		 * 
		 * @param cfg      the {@link CFG} where this operation lies
		 * @param location the code location where this operation is defined
		 * @param left     the left-hand side of this operation
		 * @param middle   the middle operand of this operation
		 * @param right    the right-hand side of this operation
		 */
		public IMPStringSubstring(CFG cfg, CodeLocation location, Expression left, Expression middle,
				Expression right) {
			super(cfg, location, left, middle, right);
		}
	}
}

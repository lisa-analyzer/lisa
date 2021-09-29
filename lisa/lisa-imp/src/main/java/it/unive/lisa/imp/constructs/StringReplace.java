package it.unive.lisa.imp.constructs;

import it.unive.lisa.program.CompilationUnit;
import it.unive.lisa.program.SourceCodeLocation;
import it.unive.lisa.program.cfg.CFG;
import it.unive.lisa.program.cfg.CFGDescriptor;
import it.unive.lisa.program.cfg.CodeLocation;
import it.unive.lisa.program.cfg.NativeCFG;
import it.unive.lisa.program.cfg.Parameter;
import it.unive.lisa.program.cfg.statement.Expression;
import it.unive.lisa.program.cfg.statement.PluggableStatement;
import it.unive.lisa.program.cfg.statement.Statement;
import it.unive.lisa.program.cfg.statement.string.Replace;
import it.unive.lisa.type.common.BoolType;
import it.unive.lisa.type.common.StringType;

/**
 * The native construct representing the replace operation. This construct can
 * be invoked on a string variable {@code x} with
 * {@code x.replace(search, replacement)}, where {@code search} is the string to
 * replace and {@code replacement} is the string to use as a replacement.
 * 
 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
 */
public class StringReplace extends NativeCFG {

	/**
	 * Builds the construct.
	 * 
	 * @param location   the location where this construct is defined
	 * @param stringUnit the unit where this construct is defined
	 */
	public StringReplace(CodeLocation location, CompilationUnit stringUnit) {
		super(new CFGDescriptor(location, stringUnit, true, "replace", BoolType.INSTANCE,
				new Parameter(location, "this", StringType.INSTANCE),
				new Parameter(location, "search", StringType.INSTANCE),
				new Parameter(location, "replacement", StringType.INSTANCE)),
				IMPStringReplace.class);
	}

	/**
	 * An expression modeling the string replace operation. The type of all
	 * three operands must be {@link StringType}. The type of this expression is
	 * the {@link StringType}.
	 * 
	 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
	 */
	public static class IMPStringReplace extends Replace implements PluggableStatement {

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
		public static IMPStringReplace build(CFG cfg, CodeLocation location, Expression... params) {
			return new IMPStringReplace(cfg, location, params[0], params[1], params[2]);
		}

		@Override
		public void setOriginatingStatement(Statement st) {
			originating = st;
		}

		/**
		 * Builds the replace.
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
		public IMPStringReplace(CFG cfg, String sourceFile, int line, int col, Expression left,
				Expression middle, Expression right) {
			this(cfg, new SourceCodeLocation(sourceFile, line, col), left, middle, right);
		}

		/**
		 * Builds the replace.
		 * 
		 * @param cfg      the {@link CFG} where this operation lies
		 * @param location the code location where this operation is defined
		 * @param left     the left-hand side of this operation
		 * @param middle   the middle operand of this operation
		 * @param right    the right-hand side of this operation
		 */
		public IMPStringReplace(CFG cfg, CodeLocation location, Expression left,
				Expression middle, Expression right) {
			super(cfg, location, left, middle, right);
		}
	}
}

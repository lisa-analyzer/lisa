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
import it.unive.lisa.program.cfg.statement.string.Contains;
import it.unive.lisa.type.common.BoolType;
import it.unive.lisa.type.common.StringType;

/**
 * The native construct representing the contains operation. This construct can
 * be invoked on a string variable {@code x} with {@code x.contains(other)},
 * where {@code other} is the string that will be checked against substrings of
 * {@code x}.
 * 
 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
 */
public class StringContains extends NativeCFG {

	/**
	 * Builds the construct.
	 * 
	 * @param location   the location where this construct is defined
	 * @param stringUnit the unit where this construct is defined
	 */
	public StringContains(CodeLocation location, ClassUnit stringUnit) {
		super(new CodeMemberDescriptor(location, stringUnit, true, "contains", BoolType.INSTANCE,
				new Parameter(location, "this", StringType.INSTANCE),
				new Parameter(location, "other", StringType.INSTANCE)),
				IMPStringContains.class);
	}

	/**
	 * An expression modeling the string contains operation. The type of both
	 * operands must be {@link StringType}. The type of this expression is the
	 * {@link BoolType}.
	 * 
	 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
	 */
	public static class IMPStringContains extends Contains implements PluggableStatement {

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
		public static IMPStringContains build(CFG cfg, CodeLocation location, Expression... params) {
			return new IMPStringContains(cfg, location, params[0], params[1]);
		}

		@Override
		public void setOriginatingStatement(Statement st) {
			originating = st;
		}

		/**
		 * Builds the contains.
		 * 
		 * @param cfg        the {@link CFG} where this operation lies
		 * @param sourceFile the source file name where this operation is
		 *                       defined
		 * @param line       the line number where this operation is defined
		 * @param col        the column where this operation is defined
		 * @param left       the left-hand side of this operation
		 * @param right      the right-hand side of this operation
		 */
		public IMPStringContains(CFG cfg, String sourceFile, int line, int col, Expression left,
				Expression right) {
			this(cfg, new SourceCodeLocation(sourceFile, line, col), left, right);
		}

		/**
		 * Builds the contains.
		 * 
		 * @param cfg      the {@link CFG} where this operation lies
		 * @param location the code location where this operation is defined
		 * @param left     the left-hand side of this operation
		 * @param right    the right-hand side of this operation
		 */
		public IMPStringContains(CFG cfg, CodeLocation location, Expression left, Expression right) {
			super(cfg, location, left, right);
		}
	}
}

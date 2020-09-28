package it.unive.lisa.cfg.statement;

import java.util.Objects;

import org.apache.commons.lang3.StringUtils;

import it.unive.lisa.cfg.CFG;

/**
 * A native call, modeling the usage of one of the native constructs of the
 * language.
 * 
 * @author @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
 */
public class NativeCall extends Call {

	/**
	 * The target of this call
	 */
	private final String constructName;

	/**
	 * Builds the native call. The location where this call happens is unknown (i.e.
	 * no source file/line/column is available).
	 * 
	 * @param cfg           the cfg that this expression belongs to
	 * @param constructName the name of the construct invoked by this native call
	 */
	public NativeCall(CFG cfg, String constructName) {
		this(cfg, null, -1, -1, constructName);
	}

	/**
	 * Builds the CFG call, happening at the given location in the program.
	 * 
	 * @param cfg           the cfg that this expression belongs to
	 * @param sourceFile    the source file where this expression happens. If
	 *                      unknown, use {@code null}
	 * @param line          the line number where this expression happens in the
	 *                      source file. If unknown, use {@code -1}
	 * @param col           the column where this expression happens in the source
	 *                      file. If unknown, use {@code -1}
	 * @param constructName the name of the construct invoked by this native call
	 */
	public NativeCall(CFG cfg, String sourceFile, int line, int col, String constructName) {
		super(cfg, sourceFile, line, col);
		Objects.requireNonNull(constructName, "The name of the native construct of a native call cannot be null");
		this.constructName = constructName;
	}

	/**
	 * Yields the CFG that is targeted by this CFG call.
	 * 
	 * @return the target CFG
	 */
	public String getConstructName() {
		return constructName;
	}

	@Override
	public String toString() {
		return constructName + "(" + StringUtils.join(getParameters(), ", ") + ")";
	}
}

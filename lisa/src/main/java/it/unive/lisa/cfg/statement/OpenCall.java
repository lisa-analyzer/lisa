package it.unive.lisa.cfg.statement;

import java.util.Objects;

import org.apache.commons.lang3.StringUtils;

import it.unive.lisa.cfg.CFG;

/**
 * A call to a CFG that is not under analysis.
 * 
 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
 */
public class OpenCall extends Call {

	/**
	 * The name of the target of this call
	 */
	private final String targetName;

	/**
	 * Builds the open call. The location where this call happens is unknown (i.e.
	 * no source file/line/column is available).
	 * 
	 * @param cfg        the cfg that this expression belongs to
	 * @param targetName the name of the target of this open call
	 */
	public OpenCall(CFG cfg, String targetName) {
		this(cfg, null, -1, -1, targetName);
	}

	/**
	 * Builds the open call, happening at the given location in the program.
	 * 
	 * @param cfg        the cfg that this expression belongs to
	 * @param sourceFile the source file where this expression happens. If unknown,
	 *                   use {@code null}
	 * @param line       the line number where this expression happens in the source
	 *                   file. If unknown, use {@code -1}
	 * @param col        the column where this expression happens in the source
	 *                   file. If unknown, use {@code -1}
	 * @param targetName the name of the target of this open call
	 */
	public OpenCall(CFG cfg, String sourceFile, int line, int col, String targetName) {
		super(cfg, sourceFile, line, col);
		Objects.requireNonNull(targetName, "The name of the target of an open call cannot be null");
		this.targetName = targetName;
	}

	/**
	 * Yields the name of the target of this open call.
	 * 
	 * @return the name of the target
	 */
	public String getTargetName() {
		return targetName;
	}

	@Override
	public String toString() {
		return targetName + "(" + StringUtils.join(getParameters(), ", ") + ")";
	}
}

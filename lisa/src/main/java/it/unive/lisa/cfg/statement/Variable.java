package it.unive.lisa.cfg.statement;

import java.util.Objects;

import it.unive.lisa.cfg.CFG;

/**
 * A reference to a variable of the current CFG, identified by its name.
 * 
 * @author @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
 */
public class Variable extends Expression {

	/**
	 * The name of this variable
	 */
	private final String name;
	
	/**
	 * Builds the variable reference, identified by its name. The location where
	 * this variable reference happens is unknown (i.e. no source file/line/column is
	 * available).
	 * 
	 * @param cfg        the cfg that this expression belongs to
	 * @param name       the name of this variable
	 */
	public Variable(CFG cfg, String name) {
		this(cfg, null, -1, -1, name);
	}

	/**
	 * Builds the variable reference, identified by its name, happening at the given
	 * location in the program.
	 * 
	 * @param cfg        the cfg that this expression belongs to
	 * @param sourceFile the source file where this expression happens. If unknown,
	 *                   use {@code null}
	 * @param line       the line number where this expression happens in the source
	 *                   file. If unknown, use {@code -1}
	 * @param col        the column where this expression happens in the source
	 *                   file. If unknown, use {@code -1}
	 * @param name       the name of this variable
	 */
	public Variable(CFG cfg, String sourceFile, int line, int col, String name) {
		super(cfg, sourceFile, line, col);
		Objects.requireNonNull(name, "The name of a variable cannot be null");
		this.name = name;
	}

	/**
	 * Yields the name of this variable.
	 * 
	 * @return the name of this variable
	 */
	public String getName() {
		return name;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = super.hashCode();
		result = prime * result + ((name == null) ? 0 : name.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (!super.equals(obj))
			return false;
		if (getClass() != obj.getClass())
			return false;
		Variable other = (Variable) obj;
		if (name == null) {
			if (other.name != null)
				return false;
		} else if (!name.equals(other.name))
			return false;
		return true;
	}

	@Override
	public String toString() {
		return name;
	}
}

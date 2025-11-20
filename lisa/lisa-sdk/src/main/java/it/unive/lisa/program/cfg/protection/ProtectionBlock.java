package it.unive.lisa.program.cfg.protection;

import it.unive.lisa.program.cfg.statement.Statement;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import org.apache.commons.lang3.StringUtils;

/**
 * A protection block, which is used to handle errors that might occur during
 * the execution of a program. It consists of a try block, one or more catch
 * blocks, an optional else block, and an optional finally block.
 * 
 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
 */
public class ProtectionBlock {

	private final ProtectedBlock tryBlock;

	private final List<CatchBlock> catchBlocks;

	private final ProtectedBlock elseBlock;

	private final ProtectedBlock finallyBlock;

	private Statement closing;

	/**
	 * Builds the protection block.
	 * 
	 * @param tryBlock     the try block of this protection block
	 * @param catchBlocks  the list of catch blocks of this protection block
	 * @param elseBlock    the else block of this protection block, if any
	 * @param finallyBlock the finally block of this protection block, if any
	 * @param closing      the closing statement of this protection block, if it
	 *                         is unique; if this protection block can end in
	 *                         multiple statements, this should be {@code null}
	 */
	public ProtectionBlock(
			ProtectedBlock tryBlock,
			List<CatchBlock> catchBlocks,
			ProtectedBlock elseBlock,
			ProtectedBlock finallyBlock,
			Statement closing) {
		Objects.requireNonNull(tryBlock, "The try block of a protection block cannot be null");
		Objects.requireNonNull(catchBlocks, "The list of catch blocks of a protection block cannot be null");
		if (catchBlocks.isEmpty())
			throw new IllegalArgumentException("A protection block must have at least one catch block");
		this.tryBlock = tryBlock;
		this.catchBlocks = catchBlocks;
		this.elseBlock = elseBlock;
		this.finallyBlock = finallyBlock;
		this.closing = closing;
	}

	/**
	 * Yields the try block of this protection block.
	 * 
	 * @return the try block
	 */
	public ProtectedBlock getTryBlock() {
		return tryBlock;
	}

	/**
	 * Yields the list of {@link CatchBlock}s of this protection block.
	 * 
	 * @return the list of catch blocks
	 */
	public List<CatchBlock> getCatchBlocks() {
		return catchBlocks;
	}

	/**
	 * Yields the else block of this protection block, if any. If this
	 * protection block does not have an else block, this method returns
	 * {@code null}.
	 * 
	 * @return the else block, or {@code null} if there is no else block
	 */
	public ProtectedBlock getElseBlock() {
		return elseBlock;
	}

	/**
	 * Yields the finally block of this protection block, if any. If this
	 * protection block does not have a finally block, this method returns
	 * {@code null}.
	 * 
	 * @return the finally block, or {@code null} if there is no finally block
	 */
	public ProtectedBlock getFinallyBlock() {
		return finallyBlock;
	}

	/**
	 * Yields the last statement to be executed as part of this protection
	 * block, if it is unique. If this protection block can end in multiple
	 * statements (e.g., returns, throws, breaks, continues), this method
	 * returns {@code null}.
	 * 
	 * @return the closing statement, or {@code null} if there is no unique
	 *             closing statement
	 */
	public Statement getClosing() {
		return closing;
	}

	/**
	 * Sets the last statement to be executed as part of this protection block,
	 * if it is unique. If this protection block can end in multiple statements
	 * (e.g., returns, throws, breaks, continues), this method should be invoked
	 * with {@code null} to indicate that there is no unique closing statement.
	 * 
	 * @param closing the closing statement, or {@code null} if there is no
	 *                    unique closing statement
	 */
	public void setClosing(
			Statement closing) {
		this.closing = closing;
	}

	/**
	 * Yields the full body of this protection block, that is, the body of the
	 * try block, the catch blocks, the else block (if any), and the finally
	 * block (if any and if requested).
	 * 
	 * @param includeFinally whether to include the body of the finally block,
	 *                           if present
	 * 
	 * @return the full body of this protection block
	 */
	public Collection<Statement> getFullBody(
			boolean includeFinally) {
		Collection<Statement> all = new LinkedList<>(tryBlock.getBody());
		all.addAll(catchBlocks.stream().flatMap(cb -> cb.getBody().getBody().stream()).collect(Collectors.toList()));
		if (elseBlock != null)
			all.addAll(elseBlock.getBody());
		if (finallyBlock != null && includeFinally)
			all.addAll(finallyBlock.getBody());
		return all;
	}

	/**
	 * Simplifies this block, removing the given targets from its body.
	 * 
	 * @param targets the set of statements that must be simplified
	 */
	public void simplify(
			Set<Statement> targets) {
		tryBlock.getBody().removeIf(targets::contains);
		for (CatchBlock cb : catchBlocks)
			cb.simplify(targets);
		if (elseBlock != null && !elseBlock.getBody().isEmpty())
			elseBlock.getBody().removeIf(targets::contains);
		if (finallyBlock != null && !finallyBlock.getBody().isEmpty())
			finallyBlock.getBody().removeIf(targets::contains);
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((tryBlock == null) ? 0 : tryBlock.hashCode());
		result = prime * result + ((catchBlocks == null) ? 0 : catchBlocks.hashCode());
		result = prime * result + ((elseBlock == null) ? 0 : elseBlock.hashCode());
		result = prime * result + ((finallyBlock == null) ? 0 : finallyBlock.hashCode());
		result = prime * result + ((closing == null) ? 0 : closing.hashCode());
		return result;
	}

	@Override
	public boolean equals(
			Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		ProtectionBlock other = (ProtectionBlock) obj;
		if (tryBlock == null) {
			if (other.tryBlock != null)
				return false;
		} else if (!tryBlock.equals(other.tryBlock))
			return false;
		if (catchBlocks == null) {
			if (other.catchBlocks != null)
				return false;
		} else if (!catchBlocks.equals(other.catchBlocks))
			return false;
		if (elseBlock == null) {
			if (other.elseBlock != null)
				return false;
		} else if (!elseBlock.equals(other.elseBlock))
			return false;
		if (finallyBlock == null) {
			if (other.finallyBlock != null)
				return false;
		} else if (!finallyBlock.equals(other.finallyBlock))
			return false;
		if (closing == null) {
			if (other.closing != null)
				return false;
		} else if (!closing.equals(other.closing))
			return false;
		return true;
	}

	@Override
	public String toString() {
		Collection<String> components = new LinkedList<>();
		if (elseBlock != null)
			components.add("else");
		if (finallyBlock != null)
			components.add("finally");
		String comp = StringUtils.join(components, "-");
		if (!comp.isEmpty())
			comp = "-" + comp;
		return "ProtectionBlock [try" + comp + ", " + StringUtils.join(catchBlocks, ", ") + "]";
	}

}

package it.unive.lisa.program.cfg.protection;

import it.unive.lisa.program.cfg.statement.NoOp;
import it.unive.lisa.program.cfg.statement.Statement;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import org.apache.commons.lang3.StringUtils;

public class ProtectionBlock {

	private final ProtectedBlock tryBlock;

	private final List<CatchBlock> catchBlocks;

	private final ProtectedBlock elseBlock;

	private final ProtectedBlock finallyBlock;

	private Statement closing;

	public ProtectionBlock(
			ProtectedBlock tryBlock,
			List<CatchBlock> catchBlocks,
			ProtectedBlock elseBlock,
			ProtectedBlock finallyBlock,
			Statement closing) {
		this.tryBlock = tryBlock;
		this.catchBlocks = catchBlocks;
		this.elseBlock = elseBlock;
		this.finallyBlock = finallyBlock;
		this.closing = closing;
	}

	public ProtectedBlock getTryBlock() {
		return tryBlock;
	}

	public List<CatchBlock> getCatchBlocks() {
		return catchBlocks;
	}

	public ProtectedBlock getElseBlock() {
		return elseBlock;
	}

	public ProtectedBlock getFinallyBlock() {
		return finallyBlock;
	}

	public Statement getClosing() {
		return closing;
	}

	public void setClosing(
			Statement closing) {
		this.closing = closing;
	}

	public Collection<Statement> getFullBody(
			boolean includeFinally) {
		Collection<Statement> all = new LinkedList<>(tryBlock.getBody());
		all.addAll(catchBlocks.stream().flatMap(cb -> cb.getBody().getBody().stream()).toList());
		if (elseBlock != null)
			all.addAll(elseBlock.getBody());
		if (finallyBlock != null && includeFinally)
			all.addAll(finallyBlock.getBody());
		return all;
	}

	public void simplify() {
		tryBlock.getBody().removeIf(NoOp.class::isInstance);
		for (CatchBlock cb : catchBlocks)
			cb.simplify();
		if (elseBlock != null && !elseBlock.getBody().isEmpty())
			elseBlock.getBody().removeIf(NoOp.class::isInstance);
		if (finallyBlock != null && !finallyBlock.getBody().isEmpty())
			finallyBlock.getBody().removeIf(NoOp.class::isInstance);
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((tryBlock == null) ? 0 : tryBlock.hashCode());
		result = prime * result + ((catchBlocks == null) ? 0 : catchBlocks.hashCode());
		result = prime * result + ((elseBlock == null) ? 0 : elseBlock.hashCode());
		result = prime * result + ((finallyBlock == null) ? 0 : finallyBlock.hashCode());
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

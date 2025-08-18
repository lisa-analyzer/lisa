package it.unive.lisa.util.frontend;

import it.unive.lisa.program.cfg.CFG;
import it.unive.lisa.program.cfg.edge.Edge;
import it.unive.lisa.program.cfg.statement.Statement;
import it.unive.lisa.util.datastructures.graph.code.NodeList;

/**
 * Utility class for frontends, that can be used to represent a block of code
 * that has been parsed while visiting the AST.
 * 
 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
 */
public class ParsedBlock {

	private final Statement begin;

	private final NodeList<CFG, Statement, Edge> body;

	private final Statement end;

	/**
	 * Builds the parsed block.
	 * 
	 * @param begin the first instruction of this block
	 * @param body  the body of the block
	 * @param end   the last instruction of this block
	 */
	public ParsedBlock(
			Statement begin,
			NodeList<CFG, Statement, Edge> body,
			Statement end) {
		this.begin = begin;
		this.body = body;
		this.end = end;
	}

	/**
	 * Yields the first instruction of this block, which is the one that is
	 * executed when this block is entered.
	 * 
	 * @return the first instruction of this block
	 */
	public Statement getBegin() {
		return begin;
	}

	/**
	 * Yields the body of the block.
	 * 
	 * @return the body of the block
	 */
	public NodeList<CFG, Statement, Edge> getBody() {
		return body;
	}

	/**
	 * Yields the last instruction of this block, if any. This method might
	 * return {@code null} if the block does not have a last instruction that
	 * can be continued with other code.
	 * 
	 * @return the last instruction of this block, or {@code null}
	 */
	public Statement getEnd() {
		return end;
	}

	/**
	 * Checks if this block can be continued, meaning that it contains at least
	 * one path that does not stop execution, does not break control flow, and
	 * does not continue control flow.
	 * 
	 * @return {@code true} if this block can be continued, {@code false}
	 *             otherwise
	 */
	public boolean canBeContinued() {
		return end != null && !end.stopsExecution() && !end.breaksControlFlow() && !end.continuesControlFlow();
	}

	/**
	 * Checks if this block always continues, meaning that it does not contain
	 * any statements that stop execution, break control flow, or continue
	 * control flow.
	 * 
	 * @return {@code true} if this block always continues, {@code false}
	 *             otherwise
	 */
	public boolean alwaysContinues() {
		return canBeContinued()
				&& body.getNodes()
					.stream()
					.noneMatch(st -> st.stopsExecution() || st.breaksControlFlow() || st.continuesControlFlow());
	}

	@Override
	public String toString() {
		return body.toString() + " (begin: " + begin + ", end: " + end + ")";
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((begin == null) ? 0 : begin.hashCode());
		result = prime * result + ((body == null) ? 0 : body.hashCode());
		result = prime * result + ((end == null) ? 0 : end.hashCode());
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
		ParsedBlock other = (ParsedBlock) obj;
		if (begin == null) {
			if (other.begin != null)
				return false;
		} else if (!begin.equals(other.begin))
			return false;
		if (body == null) {
			if (other.body != null)
				return false;
		} else if (!body.equals(other.body))
			return false;
		if (end == null) {
			if (other.end != null)
				return false;
		} else if (!end.equals(other.end))
			return false;
		return true;
	}

}

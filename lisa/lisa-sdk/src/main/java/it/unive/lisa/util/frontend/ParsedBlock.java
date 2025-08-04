package it.unive.lisa.util.frontend;

import it.unive.lisa.program.cfg.CFG;
import it.unive.lisa.program.cfg.edge.Edge;
import it.unive.lisa.program.cfg.statement.Statement;
import it.unive.lisa.util.datastructures.graph.code.NodeList;

public class ParsedBlock {

    private final Statement begin;

    private final NodeList<CFG, Statement, Edge> body;

    private final Statement end;

    public ParsedBlock(Statement begin, NodeList<CFG, Statement, Edge> body, Statement end) {
        this.begin = begin;
        this.body = body;
        this.end = end;
    }

    public Statement getBegin() {
        return begin;
    }

    public NodeList<CFG, Statement, Edge> getBody() {
        return body;
    }

    public Statement getEnd() {
        return end;
    }

    public boolean canBeContinued() {
        return end != null && !end.stopsExecution();
    }

    @Override
    public String toString() {
        return body.toString() + " (begin: " + begin + ", end: " + end + ")";
    }
}

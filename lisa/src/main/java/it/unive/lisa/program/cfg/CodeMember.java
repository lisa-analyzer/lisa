package it.unive.lisa.program.cfg;

import it.unive.lisa.program.cfg.edge.Edge;
import it.unive.lisa.program.cfg.statement.Statement;

/**
 * A control flow graph, that has {@link Statement}s as nodes and {@link Edge}s
 * as edges.
 * 
 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
 */
public interface CodeMember {

	CFGDescriptor getDescriptor();
}

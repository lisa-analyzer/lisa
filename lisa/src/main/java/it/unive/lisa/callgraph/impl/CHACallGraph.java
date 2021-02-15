package it.unive.lisa.callgraph.impl;

import it.unive.lisa.analysis.*;
import it.unive.lisa.program.Program;
import it.unive.lisa.program.cfg.CFG;
import it.unive.lisa.program.cfg.statement.CFGCall;
import it.unive.lisa.program.cfg.statement.Call;
import it.unive.lisa.program.cfg.statement.Expression;
import it.unive.lisa.program.cfg.statement.UnresolvedCall;
import it.unive.lisa.symbolic.SymbolicExpression;
import it.unive.lisa.type.Type;
import it.unive.lisa.type.UnitType;
import it.unive.lisa.util.datastructures.graph.FixpointException;

import java.util.ArrayList;
import java.util.Collection;

/**
 * A call graph constructed following the Class Hierarchy Analysis as defined in:
 *
 * Frank Tip and Jens Palsberg. 2000. Scalable propagation-based call graph construction algorithms.
 * In Proceedings of the 15th ACM SIGPLAN conference on Object-oriented programming, systems, languages,
 * and applications (OOPSLA '00). Association for Computing Machinery, New York, NY, USA, 281–293.
 * DOI:https://doi.org/10.1145/353171.353190
 *
 * @author <a href="mailto:pietro.ferrara@unive.it">Pietro Ferrara</a>
 *
 */
public final class CHACallGraph extends AbstractBaseCallGraph {

    @Override
    protected Collection<Type> getPossibleTypesOfReceiver(Expression receiver) {
        return receiver.getStaticType().allInstances();
    }

}

package it.unive.lisa.interprocedural.callgraph;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import it.unive.lisa.TestLanguageFeatures;
import it.unive.lisa.TestTypeSystem;
import it.unive.lisa.analysis.symbols.SymbolAliasing;
import it.unive.lisa.program.Application;
import it.unive.lisa.program.Program;
import it.unive.lisa.program.ProgramValidationException;
import it.unive.lisa.program.SourceCodeLocation;
import it.unive.lisa.program.cfg.CFG;
import it.unive.lisa.program.cfg.CodeMember;
import it.unive.lisa.program.cfg.CodeMemberDescriptor;
import it.unive.lisa.program.cfg.edge.SequentialEdge;
import it.unive.lisa.program.cfg.statement.Expression;
import it.unive.lisa.program.cfg.statement.Ret;
import it.unive.lisa.program.cfg.statement.call.CFGCall;
import it.unive.lisa.program.cfg.statement.call.Call;
import it.unive.lisa.program.cfg.statement.call.Call.CallType;
import it.unive.lisa.program.cfg.statement.call.UnresolvedCall;
import it.unive.lisa.type.Type;
import java.util.Collection;
import java.util.Set;
import org.junit.Test;

public class CallRegisteringTest {

	/**
	 * @see <a href="https://github.com/lisa-analyzer/lisa/issues/145">#145</a>
	 */
	@Test
	public void issue145() throws CallResolutionException, ProgramValidationException, CallGraphConstructionException {
		CallGraph cg = new BaseCallGraph() {

			@Override
			public Collection<Type> getPossibleTypesOfReceiver(Expression receiver, Set<Type> types)
					throws CallResolutionException {
				return receiver.getStaticType().allInstances(receiver.getProgram().getTypes());
			}

		};

		Program p = new Program(new TestLanguageFeatures(), new TestTypeSystem());

		CFG cfg1 = new CFG(new CodeMemberDescriptor(new SourceCodeLocation("fake1", 0, 0), p, false, "cfg1"));
		UnresolvedCall call = new UnresolvedCall(cfg1, new SourceCodeLocation("fake1", 1, 0), CallType.STATIC,
				p.getName(), "cfg2");
		cfg1.addNode(call, true);
		Ret ret = new Ret(cfg1, new SourceCodeLocation("fake1", 2, 0));
		cfg1.addNode(ret, false);
		cfg1.addEdge(new SequentialEdge(call, ret));

		CFG cfg2 = new CFG(new CodeMemberDescriptor(new SourceCodeLocation("fake2", 0, 0), p, false, "cfg2"));
		cfg2.addNode(new Ret(cfg2, new SourceCodeLocation("fake2", 1, 0)), true);

		p.addCodeMember(cfg2);
		p.addCodeMember(cfg1);
		p.getFeatures().getProgramValidationLogic().validateAndFinalize(p);

		Application app = new Application(p);
		cg.init(app);
		@SuppressWarnings("unchecked")
		CFGCall resolved = (CFGCall) cg.resolve(call, new Set[0], new SymbolAliasing());
		cg.registerCall(resolved);

		Collection<CodeMember> callees = cg.getCallees(cfg1);
		assertEquals(1, callees.size());
		assertSame(cfg2, callees.iterator().next());
		assertTrue(cg.getCallees(cfg2).isEmpty());

		Collection<CodeMember> callers = cg.getCallers(cfg2);
		assertEquals(1, callers.size());
		assertSame(cfg1, callers.iterator().next());
		assertTrue(cg.getCallers(cfg1).isEmpty());

		Collection<Call> callSites = cg.getCallSites(cfg2);
		assertEquals(1, callSites.size());
		assertSame(call, callSites.iterator().next());
		assertTrue(cg.getCallSites(cfg1).isEmpty());
	}
}

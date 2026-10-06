package it.unive.lisa.interprocedural.callgraph;

import static org.junit.jupiter.api.Assertions.assertEquals;

import it.unive.lisa.imp.IMPFrontend;
import it.unive.lisa.imp.ParsingException;
import it.unive.lisa.imp.types.ClassType;
import it.unive.lisa.program.Program;
import it.unive.lisa.program.SyntheticLocation;
import it.unive.lisa.program.cfg.CFG;
import it.unive.lisa.program.cfg.statement.Expression;
import it.unive.lisa.program.cfg.statement.VariableRef;
import it.unive.lisa.type.Type;
import java.util.HashSet;
import java.util.Set;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

public class CHACallGraphTest {

	private static Program program;
	private static CFG cfg;
	private static ClassType base;
	private static ClassType sub;
	private static ClassType unrelated;

	@BeforeAll
	public static void init()
			throws ParsingException {
		program = IMPFrontend.processText(
				"class Base { } class Sub extends Base { } class Unrelated { } class C { foo() { } }");
		cfg = program.getAllCFGs().iterator().next();
		base = ClassType.lookup("Base");
		sub = ClassType.lookup("Sub");
		unrelated = ClassType.lookup("Unrelated");
	}

	private Expression receiverOfType(
			ClassType type) {
		return new VariableRef(cfg, SyntheticLocation.INSTANCE, "recv", type);
	}

	@Test
	public void resolvesToEveryInstanceOfTheStaticTypeAccordingToTheHierarchy() {
		CHACallGraph cha = new CHACallGraph();
		Expression receiver = receiverOfType(base);

		Set<Type> result = new HashSet<>(cha.getPossibleTypesOfReceiver(receiver, Set.of(unrelated)));

		assertEquals(base.allInstances(program.getTypes()), result);
	}

	@Test
	public void ignoresWhateverTypesTheCallerBelievesAreInstantiated() {
		CHACallGraph cha = new CHACallGraph();
		Expression receiver = receiverOfType(base);

		// the passed-in "already known" types set is deliberately unrelated
		// to Base's hierarchy (and even empty in the second call): CHA must
		// not be influenced by it at all, since it resolves purely through
		// the class hierarchy
		Set<Type> withUnrelatedHint = new HashSet<>(cha.getPossibleTypesOfReceiver(receiver, Set.of(unrelated)));
		Set<Type> withNoHint = new HashSet<>(cha.getPossibleTypesOfReceiver(receiver, Set.of()));

		assertEquals(withNoHint, withUnrelatedHint);
		assertEquals(base.allInstances(program.getTypes()), withUnrelatedHint);
	}

	@Test
	public void aTypeWithNoSubtypesResolvesOnlyToItself() {
		CHACallGraph cha = new CHACallGraph();
		Expression receiver = receiverOfType(sub);

		Set<Type> result = new HashSet<>(cha.getPossibleTypesOfReceiver(receiver, Set.of()));

		assertEquals(sub.allInstances(program.getTypes()), result);
	}

}

package it.unive.lisa.checks.warnings;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

import it.unive.lisa.program.ClassUnit;
import it.unive.lisa.program.Global;
import it.unive.lisa.program.SourceCodeLocation;
import it.unive.lisa.program.cfg.CFG;
import it.unive.lisa.program.cfg.CodeMemberDescriptor;
import it.unive.lisa.program.cfg.statement.Expression;
import it.unive.lisa.program.cfg.statement.NoOp;
import it.unive.lisa.program.cfg.statement.Statement;
import it.unive.lisa.program.cfg.statement.literal.Int32Literal;
import java.util.List;
import org.junit.Test;

public class WarningsTest {

	private static final ClassUnit unit1 = new ClassUnit(new SourceCodeLocation("fake", 1, 0), "fake1",
			false);
	private static final ClassUnit unit2 = new ClassUnit(new SourceCodeLocation("fake", 1, 1), "fake2",
			false);

	private static final Global global1 = new Global(new SourceCodeLocation("fake", 15, 0), unit1, "fake1", false);
	private static final Global global2 = new Global(new SourceCodeLocation("fake", 15, 1), unit2, "fake2", false);

	private static final CodeMemberDescriptor descriptor1 = new CodeMemberDescriptor(new SourceCodeLocation("fake", 2, 0), unit1,
			false, "foo1");
	private static final CFG cfg1 = new CFG(descriptor1);
	private static final Statement st1 = new NoOp(cfg1, new SourceCodeLocation("fake", 3, 0));
	private static final Expression e1 = new Int32Literal(cfg1, new SourceCodeLocation("fake", 4, 0), 5);

	private static final CodeMemberDescriptor descriptor2 = new CodeMemberDescriptor(new SourceCodeLocation("fake", 2, 1), unit2,
			false, "foo2");
	private static final CFG cfg2 = new CFG(descriptor2);
	private static final Statement st2 = new NoOp(cfg2, new SourceCodeLocation("fake", 3, 1));
	private static final Expression e2 = new Int32Literal(cfg2, new SourceCodeLocation("fake", 4, 1), 5);

	@Test
	public void testSameTypeDifferentMessage() {
		Warning w1 = new Warning("foo");
		Warning w2 = new Warning("bar");
		assertEquals(0, w1.compareTo(w1));
		assertTrue(w1.compareTo(w2) > 0);
		assertTrue(w2.compareTo(w1) < 0);

		UnitWarning uw1 = new UnitWarning(unit1, "foo");
		UnitWarning uw2 = new UnitWarning(unit1, "bar");
		assertEquals(0, uw1.compareTo(uw1));
		assertTrue(uw1.compareTo(uw2) > 0);
		assertTrue(uw2.compareTo(uw1) < 0);

		GlobalWarning gw1 = new GlobalWarning(unit1, global1, "foo");
		GlobalWarning gw2 = new GlobalWarning(unit1, global1, "bar");
		assertEquals(0, gw1.compareTo(gw1));
		assertTrue(gw1.compareTo(gw2) > 0);
		assertTrue(gw2.compareTo(gw1) < 0);

		CFGWarning cfgw1 = new CFGWarning(cfg1, "foo");
		CFGWarning cfgw2 = new CFGWarning(cfg1, "bar");
		assertEquals(0, cfgw1.compareTo(cfgw1));
		assertTrue(cfgw1.compareTo(cfgw2) > 0);
		assertTrue(cfgw2.compareTo(cfgw1) < 0);

		CFGDescriptorWarning cfgdw1 = new CFGDescriptorWarning(descriptor1, "foo");
		CFGDescriptorWarning cfgdw2 = new CFGDescriptorWarning(descriptor1, "bar");
		assertEquals(0, cfgdw1.compareTo(cfgdw1));
		assertTrue(cfgdw1.compareTo(cfgdw2) > 0);
		assertTrue(cfgdw2.compareTo(cfgdw1) < 0);

		ExpressionWarning ew1 = new ExpressionWarning(e1, "foo");
		ExpressionWarning ew2 = new ExpressionWarning(e1, "bar");
		assertEquals(0, ew1.compareTo(ew1));
		assertTrue(ew1.compareTo(ew2) > 0);
		assertTrue(ew2.compareTo(ew1) < 0);

		StatementWarning sw1 = new StatementWarning(st1, "foo");
		StatementWarning sw2 = new StatementWarning(st1, "bar");
		assertEquals(0, sw1.compareTo(sw1));
		assertTrue(sw1.compareTo(sw2) > 0);
		assertTrue(sw2.compareTo(sw1) < 0);
	}

	@Test
	public void testSameTypeDifferentElement() {
		UnitWarning uw1 = new UnitWarning(unit1, "foo");
		UnitWarning uw2 = new UnitWarning(unit2, "bar");
		assertEquals(0, uw1.compareTo(uw1));
		assertTrue(uw1.compareTo(uw2) < 0);
		assertTrue(uw2.compareTo(uw1) > 0);

		GlobalWarning gw1 = new GlobalWarning(unit1, global1, "foo");
		GlobalWarning gw2 = new GlobalWarning(unit2, global2, "bar");
		assertEquals(0, gw1.compareTo(gw1));
		assertTrue(gw1.compareTo(gw2) < 0);
		assertTrue(gw2.compareTo(gw1) > 0);

		CFGWarning cfgw1 = new CFGWarning(cfg1, "foo");
		CFGWarning cfgw2 = new CFGWarning(cfg2, "bar");
		assertEquals(0, cfgw1.compareTo(cfgw1));
		assertTrue(cfgw1.compareTo(cfgw2) < 0);
		assertTrue(cfgw2.compareTo(cfgw1) > 0);

		CFGDescriptorWarning cfgdw1 = new CFGDescriptorWarning(descriptor1, "foo");
		CFGDescriptorWarning cfgdw2 = new CFGDescriptorWarning(descriptor2, "bar");
		assertEquals(0, cfgdw1.compareTo(cfgdw1));
		assertTrue(cfgdw1.compareTo(cfgdw2) < 0);
		assertTrue(cfgdw2.compareTo(cfgdw1) > 0);

		ExpressionWarning ew1 = new ExpressionWarning(e1, "foo");
		ExpressionWarning ew2 = new ExpressionWarning(e2, "bar");
		assertEquals(0, ew1.compareTo(ew1));
		assertTrue(ew1.compareTo(ew2) < 0);
		assertTrue(ew2.compareTo(ew1) > 0);

		StatementWarning sw1 = new StatementWarning(st1, "foo");
		StatementWarning sw2 = new StatementWarning(st2, "bar");
		assertEquals(0, sw1.compareTo(sw1));
		assertTrue(sw1.compareTo(sw2) < 0);
		assertTrue(sw2.compareTo(sw1) > 0);
	}

	@Test
	public void testDifferentType() {
		List<Warning> warns = List.of(
				new Warning("bar"),
				new UnitWarning(unit1, "foo"),
				new GlobalWarning(unit1, global1, "foo"),
				new CFGWarning(cfg1, "foo"),
				new CFGDescriptorWarning(descriptor1, "foo"),
				new ExpressionWarning(e1, "foo"),
				new StatementWarning(st1, "foo"));

		for (int i = 0; i < warns.size(); i++)
			for (int j = 0; j < warns.size(); j++)
				if (i != j) {
					Warning w1 = warns.get(i), w2 = warns.get(j);
					assertNotEquals(w1.getClass().getSimpleName() + " == " + w2.getClass().getSimpleName(), 0,
							w1.compareTo(w2));
					assertNotEquals(w2.getClass().getSimpleName() + " == " + w1.getClass().getSimpleName(), 0,
							w2.compareTo(w1));

					// these are here just to ensure that they don't throw
					// exceptions
					w1.toString();
					w2.toString();
				}
	}
}

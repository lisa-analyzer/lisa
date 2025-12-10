package it.unive.lisa.checks.warnings;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

import it.unive.lisa.TestLanguageFeatures;
import it.unive.lisa.TestTypeSystem;
import it.unive.lisa.outputs.messages.CFGDescriptorMessage;
import it.unive.lisa.outputs.messages.CFGMessage;
import it.unive.lisa.outputs.messages.ExpressionMessage;
import it.unive.lisa.outputs.messages.GlobalMessage;
import it.unive.lisa.outputs.messages.Message;
import it.unive.lisa.outputs.messages.StatementMessage;
import it.unive.lisa.outputs.messages.UnitMessage;
import it.unive.lisa.program.ClassUnit;
import it.unive.lisa.program.Global;
import it.unive.lisa.program.Program;
import it.unive.lisa.program.SourceCodeLocation;
import it.unive.lisa.program.cfg.CFG;
import it.unive.lisa.program.cfg.CodeMemberDescriptor;
import it.unive.lisa.program.cfg.statement.Expression;
import it.unive.lisa.program.cfg.statement.NoOp;
import it.unive.lisa.program.cfg.statement.Statement;
import it.unive.lisa.program.cfg.statement.VariableRef;
import java.util.List;
import org.junit.Test;

public class WarningsTest {

	private static final ClassUnit unit1 = new ClassUnit(
			new SourceCodeLocation("fake", 1, 0),
			new Program(new TestLanguageFeatures(), new TestTypeSystem()),
			"fake1",
			false);

	private static final ClassUnit unit2 = new ClassUnit(
			new SourceCodeLocation("fake", 1, 1),
			new Program(new TestLanguageFeatures(), new TestTypeSystem()),
			"fake2",
			false);

	private static final Global global1 = new Global(new SourceCodeLocation("fake", 15, 0), unit1, "fake1", false);

	private static final Global global2 = new Global(new SourceCodeLocation("fake", 15, 1), unit2, "fake2", false);

	private static final CodeMemberDescriptor descriptor1 = new CodeMemberDescriptor(
			new SourceCodeLocation("fake", 2, 0),
			unit1,
			false,
			"foo1");

	private static final CFG cfg1 = new CFG(descriptor1);

	private static final Statement st1 = new NoOp(cfg1, new SourceCodeLocation("fake", 3, 0));

	private static final Expression e1 = new VariableRef(cfg1, new SourceCodeLocation("fake", 4, 0), "x");

	private static final CodeMemberDescriptor descriptor2 = new CodeMemberDescriptor(
			new SourceCodeLocation("fake", 2, 1),
			unit2,
			false,
			"foo2");

	private static final CFG cfg2 = new CFG(descriptor2);

	private static final Statement st2 = new NoOp(cfg2, new SourceCodeLocation("fake", 3, 1));

	private static final Expression e2 = new VariableRef(cfg2, new SourceCodeLocation("fake", 4, 1), "x");

	@Test
	public void testSameTypeDifferentMessage() {
		Message w1 = new Message("foo");
		Message w2 = new Message("bar");
		assertEquals(0, w1.compareTo(w1));
		assertTrue(w1.compareTo(w2) > 0);
		assertTrue(w2.compareTo(w1) < 0);

		UnitMessage uw1 = new UnitMessage(unit1, "foo");
		UnitMessage uw2 = new UnitMessage(unit1, "bar");
		assertEquals(0, uw1.compareTo(uw1));
		assertTrue(uw1.compareTo(uw2) > 0);
		assertTrue(uw2.compareTo(uw1) < 0);

		GlobalMessage gw1 = new GlobalMessage(unit1, global1, "foo");
		GlobalMessage gw2 = new GlobalMessage(unit1, global1, "bar");
		assertEquals(0, gw1.compareTo(gw1));
		assertTrue(gw1.compareTo(gw2) > 0);
		assertTrue(gw2.compareTo(gw1) < 0);

		CFGMessage cfgw1 = new CFGMessage(cfg1, "foo");
		CFGMessage cfgw2 = new CFGMessage(cfg1, "bar");
		assertEquals(0, cfgw1.compareTo(cfgw1));
		assertTrue(cfgw1.compareTo(cfgw2) > 0);
		assertTrue(cfgw2.compareTo(cfgw1) < 0);

		CFGDescriptorMessage cfgdw1 = new CFGDescriptorMessage(descriptor1, "foo");
		CFGDescriptorMessage cfgdw2 = new CFGDescriptorMessage(descriptor1, "bar");
		assertEquals(0, cfgdw1.compareTo(cfgdw1));
		assertTrue(cfgdw1.compareTo(cfgdw2) > 0);
		assertTrue(cfgdw2.compareTo(cfgdw1) < 0);

		ExpressionMessage ew1 = new ExpressionMessage(e1, "foo");
		ExpressionMessage ew2 = new ExpressionMessage(e1, "bar");
		assertEquals(0, ew1.compareTo(ew1));
		assertTrue(ew1.compareTo(ew2) > 0);
		assertTrue(ew2.compareTo(ew1) < 0);

		StatementMessage sw1 = new StatementMessage(st1, "foo");
		StatementMessage sw2 = new StatementMessage(st1, "bar");
		assertEquals(0, sw1.compareTo(sw1));
		assertTrue(sw1.compareTo(sw2) > 0);
		assertTrue(sw2.compareTo(sw1) < 0);
	}

	@Test
	public void testSameTypeDifferentElement() {
		UnitMessage uw1 = new UnitMessage(unit1, "foo");
		UnitMessage uw2 = new UnitMessage(unit2, "bar");
		assertEquals(0, uw1.compareTo(uw1));
		assertTrue(uw1.compareTo(uw2) < 0);
		assertTrue(uw2.compareTo(uw1) > 0);

		GlobalMessage gw1 = new GlobalMessage(unit1, global1, "foo");
		GlobalMessage gw2 = new GlobalMessage(unit2, global2, "bar");
		assertEquals(0, gw1.compareTo(gw1));
		assertTrue(gw1.compareTo(gw2) < 0);
		assertTrue(gw2.compareTo(gw1) > 0);

		CFGMessage cfgw1 = new CFGMessage(cfg1, "foo");
		CFGMessage cfgw2 = new CFGMessage(cfg2, "bar");
		assertEquals(0, cfgw1.compareTo(cfgw1));
		assertTrue(cfgw1.compareTo(cfgw2) < 0);
		assertTrue(cfgw2.compareTo(cfgw1) > 0);

		CFGDescriptorMessage cfgdw1 = new CFGDescriptorMessage(descriptor1, "foo");
		CFGDescriptorMessage cfgdw2 = new CFGDescriptorMessage(descriptor2, "bar");
		assertEquals(0, cfgdw1.compareTo(cfgdw1));
		assertTrue(cfgdw1.compareTo(cfgdw2) < 0);
		assertTrue(cfgdw2.compareTo(cfgdw1) > 0);

		ExpressionMessage ew1 = new ExpressionMessage(e1, "foo");
		ExpressionMessage ew2 = new ExpressionMessage(e2, "bar");
		assertEquals(0, ew1.compareTo(ew1));
		assertTrue(ew1.compareTo(ew2) < 0);
		assertTrue(ew2.compareTo(ew1) > 0);

		StatementMessage sw1 = new StatementMessage(st1, "foo");
		StatementMessage sw2 = new StatementMessage(st2, "bar");
		assertEquals(0, sw1.compareTo(sw1));
		assertTrue(sw1.compareTo(sw2) < 0);
		assertTrue(sw2.compareTo(sw1) > 0);
	}

	@Test
	public void testDifferentType() {
		List<Message> warns = List.of(
				new Message("bar"),
				new UnitMessage(unit1, "foo"),
				new GlobalMessage(unit1, global1, "foo"),
				new CFGMessage(cfg1, "foo"),
				new CFGDescriptorMessage(descriptor1, "foo"),
				new ExpressionMessage(e1, "foo"),
				new StatementMessage(st1, "foo"));

		for (int i = 0; i < warns.size(); i++)
			for (int j = 0; j < warns.size(); j++)
				if (i != j) {
					Message w1 = warns.get(i), w2 = warns.get(j);
					assertNotEquals(
							w1.getClass().getSimpleName() + " == " + w2.getClass().getSimpleName(),
							0,
							w1.compareTo(w2));
					assertNotEquals(
							w2.getClass().getSimpleName() + " == " + w1.getClass().getSimpleName(),
							0,
							w2.compareTo(w1));

					// these are here just to ensure that they don't throw
					// exceptions
					w1.toString();
					w2.toString();
				}
	}

}

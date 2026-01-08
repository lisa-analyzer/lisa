package it.unive.lisa.checks.syntactic;

import static org.junit.Assert.assertTrue;

import java.util.Collection;
import java.util.HashSet;

import org.apache.commons.collections4.CollectionUtils;
import org.junit.Test;

import it.unive.lisa.ReportingTool;
import it.unive.lisa.TestLanguageFeatures;
import it.unive.lisa.TestTypeSystem;
import it.unive.lisa.conf.LiSAConfiguration;
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
import it.unive.lisa.program.Unit;
import it.unive.lisa.program.cfg.CFG;
import it.unive.lisa.program.cfg.CodeMemberDescriptor;
import it.unive.lisa.program.cfg.statement.Expression;
import it.unive.lisa.program.cfg.statement.NoOp;
import it.unive.lisa.program.cfg.statement.Statement;
import it.unive.lisa.program.cfg.statement.VariableRef;
import it.unive.lisa.util.file.FileManager;

public class CheckToolTest {

	private static final ClassUnit unit = new ClassUnit(
			new SourceCodeLocation("fake", 1, 0),
			new Program(new TestLanguageFeatures(), new TestTypeSystem()),
			"fake",
			false);

	private static final Global global = new Global(new SourceCodeLocation("fake", 15, 0), unit, "fake", false);

	private static final CodeMemberDescriptor descriptor = new CodeMemberDescriptor(
			new SourceCodeLocation("fake", 2, 0),
			unit,
			false,
			"foo");

	private static final CFG cfg = new CFG(descriptor);

	private static Message build(
			ReportingTool tool,
			Object target,
			String message) {
		if (target == null) {
			tool.warn(message);
			return new Message(message);
		} else if (target instanceof Unit) {
			tool.warnOn((Unit) target, message);
			return new UnitMessage((Unit) target, message);
		} else if (target instanceof Global) {
			tool.warnOn(unit, (Global) target, message);
			return new GlobalMessage(unit, (Global) target, message);
		} else if (target instanceof CFG) {
			tool.warnOn((CFG) target, message);
			return new CFGMessage((CFG) target, message);
		} else if (target instanceof CodeMemberDescriptor) {
			tool.warnOn((CodeMemberDescriptor) target, message);
			return new CFGDescriptorMessage((CodeMemberDescriptor) target, message);
		} else if (target instanceof Expression) {
			tool.warnOn((Expression) target, message);
			return new ExpressionMessage((Expression) target, message);
		} else if (target instanceof Statement) {
			tool.warnOn((Statement) target, message);
			return new StatementMessage((Statement) target, message);
		}
		return null;
	}

	@Test
	public void testCopy() {
		ReportingTool tool = new ReportingTool(new LiSAConfiguration(), new FileManager("foo"));
		Collection<Message> exp = new HashSet<>();

		exp.add(build(tool, null, "foo"));
		exp.add(build(tool, cfg, "foo"));
		exp.add(build(tool, descriptor, "foo"));
		exp.add(build(tool, unit, "foo"));
		exp.add(build(tool, global, "foo"));
		exp.add(build(tool, new NoOp(cfg, new SourceCodeLocation("fake", 3, 0)), "foo"));
		exp.add(build(tool, new VariableRef(cfg, new SourceCodeLocation("fake", 4, 0), "x"), "foo"));
		exp.add(build(tool, new NoOp(cfg, new SourceCodeLocation("fake", 3, 0)), "foo"));
		exp.add(build(tool, new NoOp(cfg, new SourceCodeLocation("fake", 4, 0)), "foo"));
		exp.add(build(tool, new NoOp(cfg, new SourceCodeLocation("fake", 5, 0)), "foo"));
		exp.add(build(tool, new NoOp(cfg, new SourceCodeLocation("fake", 5, 0)), "faa"));
		exp.add(build(tool, new NoOp(cfg, new SourceCodeLocation("fake", 3, 0)), "foo"));
		exp.add(build(tool, new NoOp(cfg, new SourceCodeLocation("fake", 3, 0)), "foo"));
		exp.add(build(tool, new NoOp(cfg, new SourceCodeLocation("fake", 4, 0)), "foo"));
		exp.add(build(tool, new NoOp(cfg, new SourceCodeLocation("fake", 4, 0)), "foo"));
		exp.add(build(tool, new NoOp(cfg, new SourceCodeLocation("fake", 4, 0)), "foo"));
		exp.add(build(tool, new NoOp(cfg, new SourceCodeLocation("fake", 5, 0)), "foo"));
		exp.add(build(tool, new NoOp(cfg, new SourceCodeLocation("fake", 5, 0)), "faa"));

		assertTrue("Wrong set of warnings", CollectionUtils.isEqualCollection(exp, tool.getWarnings()));
		assertTrue("Wrong set of warnings", CollectionUtils.isEqualCollection(exp, new ReportingTool(tool).getWarnings()));
	}

	@Test
	public void testSimpleFill() {
		ReportingTool tool = new ReportingTool(new LiSAConfiguration(), new FileManager("foo"));
		Collection<Message> exp = new HashSet<>();

		exp.add(build(tool, null, "foo"));
		exp.add(build(tool, cfg, "foo"));
		exp.add(build(tool, descriptor, "foo"));
		exp.add(build(tool, global, "foo"));
		exp.add(build(tool, new NoOp(cfg, new SourceCodeLocation("fake", 3, 0)), "foo"));
		exp.add(build(tool, new VariableRef(cfg, new SourceCodeLocation("fake", 4, 0), "x"), "foo"));

		assertTrue("Wrong set of warnings", CollectionUtils.isEqualCollection(exp, tool.getWarnings()));
	}

	@Test
	public void testDisjointMessages() {
		ReportingTool tool = new ReportingTool(new LiSAConfiguration(), new FileManager("foo"));
		Collection<Message> exp = new HashSet<>();

		exp.add(build(tool, new NoOp(cfg, new SourceCodeLocation("fake", 3, 0)), "foo"));
		exp.add(build(tool, new NoOp(cfg, new SourceCodeLocation("fake", 4, 0)), "foo"));
		exp.add(build(tool, new NoOp(cfg, new SourceCodeLocation("fake", 5, 0)), "foo"));
		exp.add(build(tool, new NoOp(cfg, new SourceCodeLocation("fake", 5, 0)), "faa"));

		assertTrue("Wrong set of warnings", CollectionUtils.isEqualCollection(exp, tool.getWarnings()));
	}

	@Test
	public void testDuplicateMessages() {
		ReportingTool tool = new ReportingTool(new LiSAConfiguration(), new FileManager("foo"));
		Collection<Message> exp = new HashSet<>();

		exp.add(build(tool, new NoOp(cfg, new SourceCodeLocation("fake", 3, 0)), "foo"));
		exp.add(build(tool, new NoOp(cfg, new SourceCodeLocation("fake", 3, 0)), "foo"));
		exp.add(build(tool, new NoOp(cfg, new SourceCodeLocation("fake", 4, 0)), "foo"));
		exp.add(build(tool, new NoOp(cfg, new SourceCodeLocation("fake", 4, 0)), "foo"));
		exp.add(build(tool, new NoOp(cfg, new SourceCodeLocation("fake", 4, 0)), "foo"));
		exp.add(build(tool, new NoOp(cfg, new SourceCodeLocation("fake", 5, 0)), "foo"));
		exp.add(build(tool, new NoOp(cfg, new SourceCodeLocation("fake", 5, 0)), "faa"));

		assertTrue("Wrong set of warnings", CollectionUtils.isEqualCollection(exp, tool.getWarnings()));
	}

}

package it.unive.lisa;

import static org.junit.jupiter.api.Assertions.assertTrue;

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
import java.util.Collection;
import java.util.HashSet;
import org.apache.commons.collections4.CollectionUtils;
import org.junit.jupiter.api.Test;

public class ReportingToolTest {

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
		return build(tool, target, message, true);
	}

	// mirrors build(...), but exercises the notice(...)/noticeOn(...) family
	// instead of warn(...)/warnOn(...), since the two families are meant to
	// behave identically and only differ in which collection they populate
	private static Message buildNotice(
			ReportingTool tool,
			Object target,
			String message) {
		return build(tool, target, message, false);
	}

	private static Message build(
			ReportingTool tool,
			Object target,
			String message,
			boolean warn) {
		if (target == null) {
			if (warn)
				tool.warn(message);
			else
				tool.notice(message);
			return new Message(message);
		} else if (target instanceof Unit) {
			if (warn)
				tool.warnOn((Unit) target, message);
			else
				tool.noticeOn((Unit) target, message);
			return new UnitMessage((Unit) target, message);
		} else if (target instanceof Global) {
			if (warn)
				tool.warnOn(unit, (Global) target, message);
			else
				tool.noticeOn(unit, (Global) target, message);
			return new GlobalMessage(unit, (Global) target, message);
		} else if (target instanceof CFG) {
			if (warn)
				tool.warnOn((CFG) target, message);
			else
				tool.noticeOn((CFG) target, message);
			return new CFGMessage((CFG) target, message);
		} else if (target instanceof CodeMemberDescriptor) {
			if (warn)
				tool.warnOn((CodeMemberDescriptor) target, message);
			else
				tool.noticeOn((CodeMemberDescriptor) target, message);
			return new CFGDescriptorMessage((CodeMemberDescriptor) target, message);
		} else if (target instanceof Expression) {
			if (warn)
				tool.warnOn((Expression) target, message);
			else
				tool.noticeOn((Expression) target, message);
			return new ExpressionMessage((Expression) target, message);
		} else if (target instanceof Statement) {
			if (warn)
				tool.warnOn((Statement) target, message);
			else
				tool.noticeOn((Statement) target, message);
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

		assertTrue(CollectionUtils.isEqualCollection(exp, tool.getWarnings()), "Wrong set of warnings");
		assertTrue(CollectionUtils.isEqualCollection(exp, new ReportingTool(tool).getWarnings()),
				"Wrong set of warnings");
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

		assertTrue(CollectionUtils.isEqualCollection(exp, tool.getWarnings()), "Wrong set of warnings");
	}

	@Test
	public void testDisjointMessages() {
		ReportingTool tool = new ReportingTool(new LiSAConfiguration(), new FileManager("foo"));
		Collection<Message> exp = new HashSet<>();

		exp.add(build(tool, new NoOp(cfg, new SourceCodeLocation("fake", 3, 0)), "foo"));
		exp.add(build(tool, new NoOp(cfg, new SourceCodeLocation("fake", 4, 0)), "foo"));
		exp.add(build(tool, new NoOp(cfg, new SourceCodeLocation("fake", 5, 0)), "foo"));
		exp.add(build(tool, new NoOp(cfg, new SourceCodeLocation("fake", 5, 0)), "faa"));

		assertTrue(CollectionUtils.isEqualCollection(exp, tool.getWarnings()), "Wrong set of warnings");
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

		assertTrue(CollectionUtils.isEqualCollection(exp, tool.getWarnings()), "Wrong set of warnings");
	}

	@Test
	public void testNoticeCopy() {
		ReportingTool tool = new ReportingTool(new LiSAConfiguration(), new FileManager("foo"));
		Collection<Message> exp = new HashSet<>();

		exp.add(buildNotice(tool, null, "foo"));
		exp.add(buildNotice(tool, cfg, "foo"));
		exp.add(buildNotice(tool, descriptor, "foo"));
		exp.add(buildNotice(tool, unit, "foo"));
		exp.add(buildNotice(tool, global, "foo"));
		exp.add(buildNotice(tool, new NoOp(cfg, new SourceCodeLocation("fake", 3, 0)), "foo"));
		exp.add(buildNotice(tool, new VariableRef(cfg, new SourceCodeLocation("fake", 4, 0), "x"), "foo"));
		exp.add(buildNotice(tool, new NoOp(cfg, new SourceCodeLocation("fake", 5, 0)), "faa"));

		assertTrue(CollectionUtils.isEqualCollection(exp, tool.getNotices()), "Wrong set of notices");
		assertTrue(CollectionUtils.isEqualCollection(exp, new ReportingTool(tool).getNotices()),
				"Wrong set of notices");
		// notices and warnings are tracked independently
		assertTrue(tool.getWarnings().isEmpty(), "Notices should not be recorded as warnings");
	}

	@Test
	public void testNoticeSimpleFill() {
		ReportingTool tool = new ReportingTool(new LiSAConfiguration(), new FileManager("foo"));
		Collection<Message> exp = new HashSet<>();

		exp.add(buildNotice(tool, null, "foo"));
		exp.add(buildNotice(tool, cfg, "foo"));
		exp.add(buildNotice(tool, descriptor, "foo"));
		exp.add(buildNotice(tool, global, "foo"));
		exp.add(buildNotice(tool, new NoOp(cfg, new SourceCodeLocation("fake", 3, 0)), "foo"));
		exp.add(buildNotice(tool, new VariableRef(cfg, new SourceCodeLocation("fake", 4, 0), "x"), "foo"));

		assertTrue(CollectionUtils.isEqualCollection(exp, tool.getNotices()), "Wrong set of notices");
	}

	@Test
	public void testNoticeDuplicateMessages() {
		ReportingTool tool = new ReportingTool(new LiSAConfiguration(), new FileManager("foo"));
		Collection<Message> exp = new HashSet<>();

		exp.add(buildNotice(tool, new NoOp(cfg, new SourceCodeLocation("fake", 3, 0)), "foo"));
		exp.add(buildNotice(tool, new NoOp(cfg, new SourceCodeLocation("fake", 3, 0)), "foo"));
		exp.add(buildNotice(tool, new NoOp(cfg, new SourceCodeLocation("fake", 4, 0)), "foo"));

		assertTrue(CollectionUtils.isEqualCollection(exp, tool.getNotices()), "Wrong set of notices");
	}

}

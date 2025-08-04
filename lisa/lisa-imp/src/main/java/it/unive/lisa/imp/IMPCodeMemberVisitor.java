package it.unive.lisa.imp;

import static it.unive.lisa.imp.Antlr4Util.getCol;
import static it.unive.lisa.imp.Antlr4Util.getLine;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;

import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.tree.TerminalNode;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.tuple.Pair;

import it.unive.lisa.imp.antlr.IMPParser.ArgContext;
import it.unive.lisa.imp.antlr.IMPParser.ArgumentsContext;
import it.unive.lisa.imp.antlr.IMPParser.ArrayAccessContext;
import it.unive.lisa.imp.antlr.IMPParser.ArrayCreatorRestContext;
import it.unive.lisa.imp.antlr.IMPParser.ArrayExprContext;
import it.unive.lisa.imp.antlr.IMPParser.AssignmentContext;
import it.unive.lisa.imp.antlr.IMPParser.BasicExprContext;
import it.unive.lisa.imp.antlr.IMPParser.BinaryStringExprContext;
import it.unive.lisa.imp.antlr.IMPParser.BlockContext;
import it.unive.lisa.imp.antlr.IMPParser.BlockOrStatementContext;
import it.unive.lisa.imp.antlr.IMPParser.CatchBlockContext;
import it.unive.lisa.imp.antlr.IMPParser.ExpressionContext;
import it.unive.lisa.imp.antlr.IMPParser.FieldAccessContext;
import it.unive.lisa.imp.antlr.IMPParser.ForLoopContext;
import it.unive.lisa.imp.antlr.IMPParser.IndexContext;
import it.unive.lisa.imp.antlr.IMPParser.LiteralContext;
import it.unive.lisa.imp.antlr.IMPParser.LocalDeclarationContext;
import it.unive.lisa.imp.antlr.IMPParser.LoopContext;
import it.unive.lisa.imp.antlr.IMPParser.MethodCallContext;
import it.unive.lisa.imp.antlr.IMPParser.NewBasicArrayExprContext;
import it.unive.lisa.imp.antlr.IMPParser.NewReferenceTypeContext;
import it.unive.lisa.imp.antlr.IMPParser.ParExprContext;
import it.unive.lisa.imp.antlr.IMPParser.PrimitiveTypeContext;
import it.unive.lisa.imp.antlr.IMPParser.ReceiverContext;
import it.unive.lisa.imp.antlr.IMPParser.StatementContext;
import it.unive.lisa.imp.antlr.IMPParser.StringExprContext;
import it.unive.lisa.imp.antlr.IMPParser.TernaryStringExprContext;
import it.unive.lisa.imp.antlr.IMPParser.TryContext;
import it.unive.lisa.imp.antlr.IMPParser.UnaryStringExprContext;
import it.unive.lisa.imp.antlr.IMPParser.UnitNameContext;
import it.unive.lisa.imp.antlr.IMPParser.WhileLoopContext;
import it.unive.lisa.imp.antlr.IMPParserBaseVisitor;
import it.unive.lisa.imp.constructs.ArrayLength;
import it.unive.lisa.imp.constructs.StringConcat;
import it.unive.lisa.imp.constructs.StringContains;
import it.unive.lisa.imp.constructs.StringEndsWith;
import it.unive.lisa.imp.constructs.StringEquals;
import it.unive.lisa.imp.constructs.StringIndexOf;
import it.unive.lisa.imp.constructs.StringLength;
import it.unive.lisa.imp.constructs.StringReplace;
import it.unive.lisa.imp.constructs.StringStartsWith;
import it.unive.lisa.imp.constructs.StringSubstring;
import it.unive.lisa.imp.expressions.IMPAddOrConcat;
import it.unive.lisa.imp.expressions.IMPArrayAccess;
import it.unive.lisa.imp.expressions.IMPAssert;
import it.unive.lisa.imp.expressions.IMPNewArray;
import it.unive.lisa.imp.expressions.IMPNewObj;
import it.unive.lisa.imp.types.ArrayType;
import it.unive.lisa.imp.types.ClassType;
import it.unive.lisa.imp.types.InterfaceType;
import it.unive.lisa.program.SourceCodeLocation;
import it.unive.lisa.program.annotations.Annotations;
import it.unive.lisa.program.cfg.CFG;
import it.unive.lisa.program.cfg.CodeMemberDescriptor;
import it.unive.lisa.program.cfg.controlFlow.IfThenElse;
import it.unive.lisa.program.cfg.controlFlow.Loop;
import it.unive.lisa.program.cfg.edge.BeginFinallyEdge;
import it.unive.lisa.program.cfg.edge.Edge;
import it.unive.lisa.program.cfg.edge.EndFinallyEdge;
import it.unive.lisa.program.cfg.edge.ErrorEdge;
import it.unive.lisa.program.cfg.edge.FalseEdge;
import it.unive.lisa.program.cfg.edge.SequentialEdge;
import it.unive.lisa.program.cfg.edge.TrueEdge;
import it.unive.lisa.program.cfg.protection.CatchBlock;
import it.unive.lisa.program.cfg.protection.ProtectionBlock;
import it.unive.lisa.program.cfg.statement.Assignment;
import it.unive.lisa.program.cfg.statement.Expression;
import it.unive.lisa.program.cfg.statement.NoOp;
import it.unive.lisa.program.cfg.statement.PluggableStatement;
import it.unive.lisa.program.cfg.statement.Ret;
import it.unive.lisa.program.cfg.statement.Return;
import it.unive.lisa.program.cfg.statement.Statement;
import it.unive.lisa.program.cfg.statement.Throw;
import it.unive.lisa.program.cfg.statement.VariableRef;
import it.unive.lisa.program.cfg.statement.call.Call.CallType;
import it.unive.lisa.program.cfg.statement.call.UnresolvedCall;
import it.unive.lisa.program.cfg.statement.comparison.Equal;
import it.unive.lisa.program.cfg.statement.comparison.GreaterOrEqual;
import it.unive.lisa.program.cfg.statement.comparison.GreaterThan;
import it.unive.lisa.program.cfg.statement.comparison.LessOrEqual;
import it.unive.lisa.program.cfg.statement.comparison.LessThan;
import it.unive.lisa.program.cfg.statement.comparison.NotEqual;
import it.unive.lisa.program.cfg.statement.global.AccessInstanceGlobal;
import it.unive.lisa.program.cfg.statement.literal.FalseLiteral;
import it.unive.lisa.program.cfg.statement.literal.Float32Literal;
import it.unive.lisa.program.cfg.statement.literal.Int32Literal;
import it.unive.lisa.program.cfg.statement.literal.Literal;
import it.unive.lisa.program.cfg.statement.literal.NullLiteral;
import it.unive.lisa.program.cfg.statement.literal.StringLiteral;
import it.unive.lisa.program.cfg.statement.literal.TrueLiteral;
import it.unive.lisa.program.cfg.statement.literal.TypeLiteral;
import it.unive.lisa.program.cfg.statement.logic.And;
import it.unive.lisa.program.cfg.statement.logic.Not;
import it.unive.lisa.program.cfg.statement.logic.Or;
import it.unive.lisa.program.cfg.statement.numeric.Division;
import it.unive.lisa.program.cfg.statement.numeric.Multiplication;
import it.unive.lisa.program.cfg.statement.numeric.Negation;
import it.unive.lisa.program.cfg.statement.numeric.Remainder;
import it.unive.lisa.program.cfg.statement.numeric.Subtraction;
import it.unive.lisa.program.cfg.statement.types.Cast;
import it.unive.lisa.program.cfg.statement.types.IsInstance;
import it.unive.lisa.program.type.BoolType;
import it.unive.lisa.program.type.Float32Type;
import it.unive.lisa.program.type.Int32Type;
import it.unive.lisa.program.type.StringType;
import it.unive.lisa.type.Type;
import it.unive.lisa.type.Untyped;
import it.unive.lisa.util.datastructures.graph.code.NodeList;
import it.unive.lisa.util.frontend.CFGTweaker;
import it.unive.lisa.util.frontend.LocalVariableTracker;
import it.unive.lisa.util.frontend.ParsedBlock;

/**
 * An {@link IMPParserBaseVisitor} that will parse the code of an IMP method or
 * constructor.
 * 
 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
 */
class IMPCodeMemberVisitor
		extends
		IMPParserBaseVisitor<Object> {

	private final String file;

	private final NodeList<CFG, Statement, Edge> list;

	private final Collection<Statement> entrypoints;

	private final LocalVariableTracker tracker;

	private final CFG cfg;

	private final CodeMemberDescriptor descriptor;

	/**
	 * Builds the visitor of an IMP method or constructor.
	 * 
	 * @param file       the path of the file where the method or constructor
	 *                       appears
	 * @param descriptor the descriptor of the method or constructor
	 */
	IMPCodeMemberVisitor(
			String file,
			CodeMemberDescriptor descriptor) {
		this.file = file;
		this.descriptor = descriptor;
		list = new NodeList<>(new SequentialEdge());
		entrypoints = new HashSet<>();
		// side effects on entrypoints and matrix will affect the cfg
		cfg = new CFG(descriptor, entrypoints, list);
		tracker = new LocalVariableTracker(cfg, descriptor);
	}

	/**
	 * Visits the code of a {@link BlockContext} representing the code block of
	 * a method or constructor.
	 * 
	 * @param ctx the block context
	 * 
	 * @return the {@link CFG} built from the block
	 */
	CFG visitCodeMember(
			BlockContext ctx) {
		ParsedBlock visited = visitBlock(ctx);
		entrypoints.add(visited.getBegin());
		list.mergeWith(visited.getBody());

		CFGTweaker.addReturns(cfg, IMPSyntaxException::new);
		CFGTweaker.fixFinallyEdges(cfg, IMPSyntaxException::new);

		cfg.simplify();

		return cfg;
	}

	@Override
	public ParsedBlock visitBlock(
			BlockContext ctx) {
		tracker.enterScope();
		NodeList<CFG, Statement, Edge> block = new NodeList<>(new SequentialEdge());

		Statement first = null, last = null;
		boolean canProceed = true;
		for (int i = 0; i < ctx.blockOrStatement().size(); i++) {
			if (!canProceed)
				throw new IMPSyntaxException("Statement '" + last.toString() + "' at " + last.getLocation() + " stops execution, so it cannot be followed by another statement in the same block");
			ParsedBlock st = visitBlockOrStatement(ctx.blockOrStatement(i));
			block.mergeWith(st.getBody());
			if (first == null)
				first = st.getBegin();
			if (last != null)
				block.addEdge(new SequentialEdge(last, st.getBegin()));
			last = st.getEnd();
			canProceed = st.canBeContinued();
		}

		tracker.exitScope(last);

		if (first == null && last == null) {
			// empty block: instrument it with a noop
			NoOp instrumented = new NoOp(cfg, new SourceCodeLocation(file, getLine(ctx), getCol(ctx)));
			first = last = instrumented;
			block.addNode(instrumented);
		}

		return new ParsedBlock(first, block, last);
	}

	@Override
	public ParsedBlock visitBlockOrStatement(
			BlockOrStatementContext ctx) {
		if (ctx.statement() != null)
			return visitStatement(ctx.statement());
		else
			return visitBlock(ctx.block());
	}

	@Override
	public ParsedBlock visitStatement(
			StatementContext ctx) {
		Statement st;
		if (ctx.localDeclaration() != null)
			st = visitLocalDeclaration(ctx.localDeclaration());
		else if (ctx.ASSERT() != null)
			st = new IMPAssert(cfg, file, getLine(ctx), getCol(ctx), visitExpression(ctx.expression()));
		else if (ctx.RETURN() != null)
			if (ctx.expression() != null)
				st = new Return(
						cfg,
						new SourceCodeLocation(file, getLine(ctx), getCol(ctx)),
						visitExpression(ctx.expression()));
			else
				st = new Ret(cfg, new SourceCodeLocation(file, getLine(ctx), getCol(ctx)));
		else if (ctx.THROW() != null)
			st = new Throw(
					cfg,
					new SourceCodeLocation(file, getLine(ctx), getCol(ctx)),
					visitExpression(ctx.expression()));
		else if (ctx.skip != null)
			st = new NoOp(cfg, new SourceCodeLocation(file, getLine(ctx), getCol(ctx)));
		else if (ctx.IF() != null)
			return visitIf(ctx);
		else if (ctx.loop() != null)
			return visitLoop(ctx.loop());
		else if (ctx.try_() != null)
			return visitTry(ctx.try_());
		else if (ctx.command != null)
			st = visitExpression(ctx.command);
		else
			throw new IllegalArgumentException(
					"Statement '" + ctx.toString() + "' cannot be parsed");

		NodeList<CFG, Statement, Edge> adj = new NodeList<>(new SequentialEdge());
		adj.addNode(st);
		return new ParsedBlock(st, adj, st);
	}

	private ParsedBlock visitIf(
			StatementContext ctx) {
		NodeList<CFG, Statement, Edge> ite = new NodeList<>(new SequentialEdge());

		Statement condition = visitParExpr(ctx.parExpr());
		ite.addNode(condition);

		ParsedBlock then = visitBlockOrStatement(ctx.then);
		ite.mergeWith(then.getBody());
		ite.addEdge(new TrueEdge(condition, then.getBegin()));

		ParsedBlock otherwise = null;
		if (ctx.otherwise != null) {
			otherwise = visitBlockOrStatement(ctx.otherwise);
			ite.mergeWith(otherwise.getBody());
			ite.addEdge(new FalseEdge(condition, otherwise.getBegin()));
		}

		boolean needsNoop = then.canBeContinued()
			|| otherwise == null
			|| otherwise.canBeContinued();
		Statement noop = new NoOp(cfg, condition.getLocation());
		if (needsNoop)
			ite.addNode(noop);

		if (then.canBeContinued())
			ite.addEdge(new SequentialEdge(then.getEnd(), noop));
		if (otherwise != null) {
			if (otherwise.canBeContinued())
				ite.addEdge(new SequentialEdge(otherwise.getEnd(), noop));
		} else
			ite.addEdge(new FalseEdge(condition, noop));

		descriptor.addControlFlowStructure(new IfThenElse(
				list,
				condition,
				needsNoop ? noop : null,
				then.getBody().getNodes(),
				otherwise == null ? Collections.emptyList() : otherwise.getBody().getNodes()));

		return new ParsedBlock(condition, ite, needsNoop ? noop : null);
	}

	@Override
	public Expression visitParExpr(
			ParExprContext ctx) {
		return visitExpression(ctx.expression());
	}

	@Override
	public Assignment visitLocalDeclaration(
			LocalDeclarationContext ctx) {
		Expression expression = visitExpression(ctx.expression());
		VariableRef ref = visitVar(ctx.IDENTIFIER(), false);

		if (tracker.hasVariable(ref.getName()))
			throw new IMPSyntaxException(
					"Duplicate variable '" + ref.getName() + "' declared at " + ref.getLocation());

		tracker.addVariable(ref.getName(), ref, new IMPAnnotationVisitor().visitAnnotations(ctx.annotations()));

		// the variable table entry will be generated at the end of the
		// containing block

		return new Assignment(cfg, new SourceCodeLocation(file, getLine(ctx), getCol(ctx)), ref, expression);
	}

	@Override
	public ParsedBlock visitLoop(
			LoopContext ctx) {
		if (ctx.whileLoop() != null)
			return visitWhileLoop(ctx.whileLoop());
		else
			return visitForLoop(ctx.forLoop());
	}

	@Override
	public ParsedBlock visitWhileLoop(
			WhileLoopContext ctx) {
		NodeList<CFG, Statement, Edge> loop = new NodeList<>(new SequentialEdge());
		Statement condition = visitParExpr(ctx.parExpr());
		loop.addNode(condition);

		ParsedBlock body = visitBlockOrStatement(ctx.blockOrStatement());
		loop.mergeWith(body.getBody());
		loop.addEdge(new TrueEdge(condition, body.getBegin()));
		if (body.canBeContinued())
			loop.addEdge(new SequentialEdge(body.getEnd(), condition));

		Statement noop = new NoOp(cfg, condition.getLocation());
		loop.addNode(noop);
		loop.addEdge(new FalseEdge(condition, noop));

		descriptor.addControlFlowStructure(new Loop(list, condition, noop, body.getBody().getNodes()));

		return new ParsedBlock(condition, loop, noop);
	}

	@Override
	public ParsedBlock visitForLoop(
			ForLoopContext ctx) {
		NodeList<CFG, Statement, Edge> loop = new NodeList<>(new SequentialEdge());
		LocalDeclarationContext initDecl = ctx.forDeclaration().initDecl;
		ExpressionContext initExpr = ctx.forDeclaration().initExpr;
		ExpressionContext cond = ctx.forDeclaration().condition;
		ExpressionContext post = ctx.forDeclaration().post;

		Statement first = null, last = null;
		if (initDecl != null) {
			Statement init = visitLocalDeclaration(initDecl);
			loop.addNode(init);
			first = init;
		} else if (initExpr != null) {
			Statement init = visitExpression(initExpr);
			loop.addNode(init);
			first = init;
		}

		Statement condition;
		if (cond != null)
			condition = visitExpression(cond);
		else
			condition = new TrueLiteral(cfg, new SourceCodeLocation(file, getLine(ctx), getCol(ctx)));
		loop.addNode(condition);
		if (first == null)
			first = condition;
		else
			loop.addEdge(new SequentialEdge(first, condition));

		ParsedBlock body = visitBlockOrStatement(ctx.blockOrStatement());
		loop.mergeWith(body.getBody());
		loop.addEdge(new TrueEdge(condition, body.getBegin()));
		last = body.getEnd();

		if (post != null) {
			Statement inc = visitExpression(post);
			loop.addNode(inc);
			if (body.canBeContinued())
				loop.addEdge(new SequentialEdge(body.getEnd(), inc));
			last = inc;
		}

		if (last != null && !last.stopsExecution())
			loop.addEdge(new SequentialEdge(last, condition));

		Statement noop = new NoOp(cfg, condition.getLocation());
		loop.addNode(noop);
		loop.addEdge(new FalseEdge(condition, noop));

		if (post == null)
			descriptor.addControlFlowStructure(new Loop(list, condition, noop, body.getBody().getNodes()));
		else {
			NodeList<CFG, Statement, Edge> tmp = new NodeList<>(body.getBody());
			if (last != null) {
				tmp.addNode(last);
				if (body.canBeContinued())
					loop.addEdge(new SequentialEdge(body.getEnd(), last));
			}
			descriptor.addControlFlowStructure(new Loop(list, condition, noop, tmp.getNodes()));
		}

		return new ParsedBlock(first, loop, noop);
	}

	@Override
	public Assignment visitAssignment(
			AssignmentContext ctx) {
		Expression expression = visitExpression(ctx.expression());
		Expression target = null;
		if (ctx.IDENTIFIER() != null)
			target = visitVar(ctx.IDENTIFIER(), true);
		else if (ctx.fieldAccess() != null)
			target = visitFieldAccess(ctx.fieldAccess());
		else if (ctx.arrayAccess() != null)
			target = visitArrayAccess(ctx.arrayAccess());
		else
			throw new IMPSyntaxException(
					"Target of the assignment at " + expression.getLocation()
							+ " is neither an identifier, a field access or an array access");

		return new Assignment(cfg, new SourceCodeLocation(file, getLine(ctx), getCol(ctx)), target, expression);
	}

	private VariableRef visitVar(
			TerminalNode identifier,
			boolean localReference) {
		VariableRef ref = new VariableRef(
				cfg,
				new SourceCodeLocation(file, getLine(identifier.getSymbol()), getCol(identifier.getSymbol())),
				identifier.getText(),
				Untyped.INSTANCE);
		if (localReference && !tracker.hasVariable(ref.getName()))
			throw new IMPSyntaxException(
					"Referencing undeclared variable '" + ref.getName() + "' at " + ref.getLocation());
		return ref;
	}

	@Override
	public AccessInstanceGlobal visitFieldAccess(
			FieldAccessContext ctx) {
		Expression receiver = visitReceiver(ctx.receiver());
		return new AccessInstanceGlobal(
				cfg,
				new SourceCodeLocation(file, getLine(ctx), getCol(ctx)),
				receiver,
				ctx.name.getText());
	}

	@Override
	public Expression visitReceiver(
			ReceiverContext ctx) {
		if (ctx.THIS() != null)
			return visitVar(ctx.THIS(), false);
		else if (ctx.SUPER() != null)
			return visitVar(ctx.SUPER(), false);
		else
			return visitVar(ctx.IDENTIFIER(), true);
	}

	@Override
	public IMPArrayAccess visitArrayAccess(
			ArrayAccessContext ctx) {
		Expression receiver = visitReceiver(ctx.receiver());
		Expression result = receiver;
		for (IndexContext i : ctx.index())
			result = new IMPArrayAccess(cfg, file, getLine(i), getCol(i), result, visitIndex(i));

		return (IMPArrayAccess) result;
	}

	@Override
	public Expression visitIndex(
			IndexContext ctx) {
		if (ctx.IDENTIFIER() != null)
			return visitVar(ctx.IDENTIFIER(), true);
		else
			return new Int32Literal(
					cfg,
					new SourceCodeLocation(file, getLine(ctx), getCol(ctx)),
					Integer.parseInt(ctx.LITERAL_DECIMAL().getText()));
	}

	@Override
	public Expression visitExpression(
			ExpressionContext ctx) {
		int line = getLine(ctx);
		int col = getCol(ctx);
		if (ctx.paren != null)
			return visitExpression(ctx.paren);
		else if (ctx.assignment() != null)
			return visitAssignment(ctx.assignment());
		else if (ctx.basicExpr() != null)
			return visitBasicExpr(ctx.basicExpr());
		else if (ctx.nested != null)
			if (ctx.NOT() != null)
				return new Not(cfg, new SourceCodeLocation(file, line, col), visitExpression(ctx.nested));
			else
				return new Negation(cfg, new SourceCodeLocation(file, line, col), visitExpression(ctx.nested));
		else if (ctx.left != null && ctx.right != null)
			if (ctx.MUL() != null)
				return new Multiplication(
						cfg,
						new SourceCodeLocation(file, line, col),
						visitExpression(ctx.left),
						visitExpression(ctx.right));
			else if (ctx.DIV() != null)
				return new Division(
						cfg,
						new SourceCodeLocation(file, line, col),
						visitExpression(ctx.left),
						visitExpression(ctx.right));
			else if (ctx.MOD() != null)
				return new Remainder(
						cfg,
						new SourceCodeLocation(file, line, col),
						visitExpression(ctx.left),
						visitExpression(ctx.right));
			else if (ctx.ADD() != null)
				return new IMPAddOrConcat(cfg, file, line, col, visitExpression(ctx.left), visitExpression(ctx.right));
			else if (ctx.SUB() != null)
				return new Subtraction(
						cfg,
						new SourceCodeLocation(file, line, col),
						visitExpression(ctx.left),
						visitExpression(ctx.right));
			else if (ctx.GT() != null)
				return new GreaterThan(
						cfg,
						new SourceCodeLocation(file, line, col),
						visitExpression(ctx.left),
						visitExpression(ctx.right));
			else if (ctx.GE() != null)
				return new GreaterOrEqual(
						cfg,
						new SourceCodeLocation(file, line, col),
						visitExpression(ctx.left),
						visitExpression(ctx.right));
			else if (ctx.LT() != null)
				return new LessThan(
						cfg,
						new SourceCodeLocation(file, line, col),
						visitExpression(ctx.left),
						visitExpression(ctx.right));
			else if (ctx.LE() != null)
				return new LessOrEqual(
						cfg,
						new SourceCodeLocation(file, line, col),
						visitExpression(ctx.left),
						visitExpression(ctx.right));
			else if (ctx.EQUAL() != null)
				return new Equal(
						cfg,
						new SourceCodeLocation(file, line, col),
						visitExpression(ctx.left),
						visitExpression(ctx.right));
			else if (ctx.NOTEQUAL() != null)
				return new NotEqual(
						cfg,
						new SourceCodeLocation(file, line, col),
						visitExpression(ctx.left),
						visitExpression(ctx.right));
			else if (ctx.AND() != null)
				return new And(
						cfg,
						new SourceCodeLocation(file, line, col),
						visitExpression(ctx.left),
						visitExpression(ctx.right));
			else
				return new Or(
						cfg,
						new SourceCodeLocation(file, line, col),
						visitExpression(ctx.left),
						visitExpression(ctx.right));
		else if (ctx.left != null && ctx.type != null)
			if (ctx.IS() != null)
				return new IsInstance(
						cfg,
						new SourceCodeLocation(file, line, col),
						visitExpression(ctx.left),
						new TypeLiteral(
								cfg,
								new SourceCodeLocation(file, line, col),
								makeType(ctx.type.getText(), ctx.type)));
			else
				return new Cast(
						cfg,
						new SourceCodeLocation(file, line, col),
						visitExpression(ctx.left),
						new TypeLiteral(
								cfg,
								new SourceCodeLocation(file, line, col),
								makeType(ctx.type.getText(), ctx.type)));
		else if (ctx.NEW() != null)
			if (ctx.newBasicArrayExpr() != null)
				return visitNewBasicArrayExpr(ctx.newBasicArrayExpr());
			else
				return visitNewReferenceType(ctx.newReferenceType());
		else if (ctx.BUMP() != null)
			if (ctx.newBasicArrayExpr() != null)
				return visitBumpBasicArrayExpr(ctx.newBasicArrayExpr());
			else
				return visitBumpReferenceType(ctx.newReferenceType());
		else if (ctx.arrayAccess() != null)
			return visitArrayAccess(ctx.arrayAccess());
		else if (ctx.fieldAccess() != null)
			return visitFieldAccess(ctx.fieldAccess());
		else if (ctx.methodCall() != null)
			return visitMethodCall(ctx.methodCall());
		else if (ctx.stringExpr() != null)
			return visitStringExpr(ctx.stringExpr());
		else if (ctx.arrayExpr() != null)
			return visitArrayExpr(ctx.arrayExpr());

		throw new UnsupportedOperationException(
				"Type of expression not supported: " + ctx);
	}

	private Type makeType(
			String name, 
			ParserRuleContext ctx) {
		SourceCodeLocation location = new SourceCodeLocation(file, getLine(ctx), getCol(ctx));
		switch (name) {
		case "int":
			return Int32Type.INSTANCE;
		case "float":
			return Float32Type.INSTANCE;
		case "bool":
			return BoolType.INSTANCE;
		case "string":
			return StringType.INSTANCE;
		default:
			Type t = ClassType.lookup(name);
			if (t != null)
				return t;
			t = InterfaceType.lookup(name);
			if (t != null)
				return t;
			if (name.contains("[")) {
				t = ArrayType.lookup(makeType(name.substring(0, name.indexOf("[")), ctx), 1);
				if (t != null)
					return t;
			}
			throw new IMPSyntaxException("Type '" + name + "' not found at " + location);
		}
	}

	@Override
	public Expression visitStringExpr(
			StringExprContext ctx) {
		Expression returned = null;
		if (ctx.unaryStringExpr() != null)
			returned = visitUnaryStringExpr(ctx.unaryStringExpr());
		else if (ctx.binaryStringExpr() != null)
			returned = visitBinaryStringExpr(ctx.binaryStringExpr());
		else if (ctx.ternaryStringExpr() != null)
			returned = visitTernaryStringExpr(ctx.ternaryStringExpr());
		else
			throw new UnsupportedOperationException(
					"Type of string expression not supported: " + ctx);

		if (returned instanceof PluggableStatement)
			// These string operations are also native constructs
			((PluggableStatement) returned).setOriginatingStatement(returned);

		return returned;
	}

	@Override
	public Expression visitArrayExpr(
			ArrayExprContext ctx) {
		return new ArrayLength.IMPArrayLength(cfg, file, getLine(ctx), getCol(ctx), visitExpression(ctx.op));
	}

	@Override
	public Expression visitUnaryStringExpr(
			UnaryStringExprContext ctx) {
		if (ctx.STRLEN() != null)
			return new StringLength.IMPStringLength(cfg, file, getLine(ctx), getCol(ctx), visitExpression(ctx.op));
		throw new UnsupportedOperationException(
				"Type of string expression not supported: " + ctx);
	}

	@Override
	public Expression visitBinaryStringExpr(
			BinaryStringExprContext ctx) {
		if (ctx.STRCAT() != null)
			return new StringConcat.IMPStringConcat(
					cfg,
					file,
					getLine(ctx),
					getCol(ctx),
					visitExpression(ctx.left),
					visitExpression(ctx.right));
		else if (ctx.STRCONTAINS() != null)
			return new StringContains.IMPStringContains(
					cfg,
					file,
					getLine(ctx),
					getCol(ctx),
					visitExpression(ctx.left),
					visitExpression(ctx.right));
		else if (ctx.STRENDS() != null)
			return new StringEndsWith.IMPStringEndsWith(
					cfg,
					file,
					getLine(ctx),
					getCol(ctx),
					visitExpression(ctx.left),
					visitExpression(ctx.right));
		else if (ctx.STREQ() != null)
			return new StringEquals.IMPStringEquals(
					cfg,
					file,
					getLine(ctx),
					getCol(ctx),
					visitExpression(ctx.left),
					visitExpression(ctx.right));
		else if (ctx.STRINDEXOF() != null)
			return new StringIndexOf.IMPStringIndexOf(
					cfg,
					file,
					getLine(ctx),
					getCol(ctx),
					visitExpression(ctx.left),
					visitExpression(ctx.right));
		else if (ctx.STRSTARTS() != null)
			return new StringStartsWith.IMPStringStartsWith(
					cfg,
					file,
					getLine(ctx),
					getCol(ctx),
					visitExpression(ctx.left),
					visitExpression(ctx.right));

		throw new UnsupportedOperationException(
				"Type of string expression not supported: " + ctx);
	}

	@Override
	public Expression visitTernaryStringExpr(
			TernaryStringExprContext ctx) {
		if (ctx.STRREPLACE() != null)
			return new StringReplace.IMPStringReplace(
					cfg,
					file,
					getLine(ctx),
					getCol(ctx),
					visitExpression(ctx.left),
					visitExpression(ctx.middle),
					visitExpression(ctx.right));
		else if (ctx.STRSUB() != null)
			return new StringSubstring.IMPStringSubstring(
					cfg,
					file,
					getLine(ctx),
					getCol(ctx),
					visitExpression(ctx.left),
					visitExpression(ctx.middle),
					visitExpression(ctx.right));

		throw new UnsupportedOperationException(
				"Type of string expression not supported: " + ctx);
	}

	private Expression visitBumpBasicArrayExpr(
			NewBasicArrayExprContext ctx) {
		return new IMPNewArray(
				cfg,
				file,
				getLine(ctx),
				getCol(ctx),
				visitPrimitiveType(ctx.primitiveType()),
				true,
				visitArrayCreatorRest(ctx.arrayCreatorRest()));
	}

	@Override
	public Expression visitNewBasicArrayExpr(
			NewBasicArrayExprContext ctx) {
		return new IMPNewArray(
				cfg,
				file,
				getLine(ctx),
				getCol(ctx),
				visitPrimitiveType(ctx.primitiveType()),
				false,
				visitArrayCreatorRest(ctx.arrayCreatorRest()));
	}

	@Override
	public Expression[] visitArrayCreatorRest(
			ArrayCreatorRestContext ctx) {
		Expression[] result = new Expression[ctx.index().size()];
		for (int i = 0; i < result.length; i++)
			result[i] = visitIndex(ctx.index(i));
		return result;
	}

	@Override
	public Type visitPrimitiveType(
			PrimitiveTypeContext ctx) {
		if (ctx.BOOLEAN() != null)
			return BoolType.INSTANCE;
		else if (ctx.INT() != null)
			return Int32Type.INSTANCE;
		else
			return Float32Type.INSTANCE;
	}

	private Expression visitBumpReferenceType(
			NewReferenceTypeContext ctx) {
		// null since we do not want to create a new one, class types should
		// have been created during the preprocessing
		Type base = ClassType.lookup(ctx.IDENTIFIER().getText());
		if (ctx.arrayCreatorRest() != null)
			return new IMPNewArray(
					cfg,
					file,
					getLine(ctx),
					getCol(ctx),
					base,
					true,
					visitArrayCreatorRest(ctx.arrayCreatorRest()));
		else
			return new IMPNewObj(cfg, file, getLine(ctx), getCol(ctx), base, true, visitArguments(ctx.arguments()));
	}

	@Override
	public Expression visitNewReferenceType(
			NewReferenceTypeContext ctx) {
		// null since we do not want to create a new one, class types should
		// have been created during the preprocessing
		Type base = ClassType.lookup(ctx.IDENTIFIER().getText());
		if (ctx.arrayCreatorRest() != null)
			return new IMPNewArray(
					cfg,
					file,
					getLine(ctx),
					getCol(ctx),
					base,
					false,
					visitArrayCreatorRest(ctx.arrayCreatorRest()));
		else
			return new IMPNewObj(cfg, file, getLine(ctx), getCol(ctx), base, false, visitArguments(ctx.arguments()));
	}

	@Override
	public Expression[] visitArguments(
			ArgumentsContext ctx) {
		Expression[] args = new Expression[ctx.arg().size()];
		int i = 0;
		for (ArgContext arg : ctx.arg())
			args[i++] = visitArg(arg);
		return args;
	}

	@Override
	public Expression visitArg(
			ArgContext ctx) {
		if (ctx.literal() != null)
			return visitLiteral(ctx.literal());
		else if (ctx.fieldAccess() != null)
			return visitFieldAccess(ctx.fieldAccess());
		else if (ctx.arrayAccess() != null)
			return visitArrayAccess(ctx.arrayAccess());
		else if (ctx.methodCall() != null)
			return visitMethodCall(ctx.methodCall());
		else if (ctx.IDENTIFIER() != null)
			return visitVar(ctx.IDENTIFIER(), true);
		else
			return visitVar(ctx.THIS(), false);
	}

	@Override
	public Expression visitMethodCall(
			MethodCallContext ctx) {
		Expression receiver = visitReceiver(ctx.receiver());
		String name = ctx.name.getText();
		Expression[] args = ArrayUtils.insert(0, visitArguments(ctx.arguments()), receiver);
		return new UnresolvedCall(
				cfg,
				new SourceCodeLocation(file, getLine(ctx), getCol(ctx)),
				CallType.INSTANCE,
				null,
				name,
				args);
	}

	@Override
	public Expression visitBasicExpr(
			BasicExprContext ctx) {
		if (ctx.literal() != null)
			return visitLiteral(ctx.literal());
		else if (ctx.THIS() != null)
			return visitVar(ctx.THIS(), false);
		else if (ctx.SUPER() != null)
			return visitVar(ctx.SUPER(), false);
		else
			return visitVar(ctx.IDENTIFIER(), true);
	}

	@Override
	public Literal<?> visitLiteral(
			LiteralContext ctx) {
		int line = getLine(ctx);
		int col = getCol(ctx);
		if (ctx.LITERAL_NULL() != null)
			return new NullLiteral(cfg, new SourceCodeLocation(file, line, col));
		else if (ctx.LITERAL_BOOL() != null)
			if (ctx.LITERAL_BOOL().getText().equals("true"))
				return new TrueLiteral(cfg, new SourceCodeLocation(file, line, col));
			else
				return new FalseLiteral(cfg, new SourceCodeLocation(file, line, col));
		else if (ctx.LITERAL_STRING() != null)
			return new StringLiteral(cfg, new SourceCodeLocation(file, line, col), clean(ctx));
		else if (ctx.LITERAL_FLOAT() != null)
			if (ctx.SUB() != null)
				return new Float32Literal(
						cfg,
						new SourceCodeLocation(file, line, col),
						-Float.parseFloat(ctx.LITERAL_FLOAT().getText()));
			else
				return new Float32Literal(
						cfg,
						new SourceCodeLocation(file, line, col),
						Float.parseFloat(ctx.LITERAL_FLOAT().getText()));
		else if (ctx.LITERAL_DECIMAL() != null)
			if (ctx.SUB() != null)
				return new Int32Literal(
						cfg,
						new SourceCodeLocation(file, line, col),
						-Integer.parseInt(ctx.LITERAL_DECIMAL().getText()));
			else
				return new Int32Literal(
						cfg,
						new SourceCodeLocation(file, line, col),
						Integer.parseInt(ctx.LITERAL_DECIMAL().getText()));

		throw new UnsupportedOperationException(
				"Type of literal not supported: " + ctx);
	}

	/**
	 * Strips the string in the given context of its wrapping quotes, if any.
	 * 
	 * @param ctx the source context
	 * 
	 * @return the cleaned string
	 */
	static String clean(
			LiteralContext ctx) {
		String text = ctx.LITERAL_STRING().getText();
		if (text.startsWith("\"") && text.endsWith("\""))
			return text.substring(1, text.length() - 1);
		return text;
	}

	@Override
	public ParsedBlock visitTry(TryContext ctx) {
		NodeList<CFG, Statement, Edge> trycatch = new NodeList<>(new SequentialEdge());

		// normal exit points of the try-catch in case there is no finally block:
		// in this case, we have to add a noop at the end of the whole try-catch
		// to use it as unique exit point in the returned triple
		Collection<Statement> normalExits = new LinkedList<>();

		// we parse the body of the try block normally
		ParsedBlock body = visitBlock(ctx.body);
		trycatch.mergeWith(body.getBody());

		// if there is an else block, we parse it immediately and connect it
		// to the end of the try block *only* if it does not end with a return/throw
		// (as it would be deadcode)
		ParsedBlock elseBlock = null;
		if (body.canBeContinued())
			if (ctx.else_ == null)
				// non-stopping last statement with no else
				normalExits.add(body.getEnd());
			else {
				elseBlock = visitBlock(ctx.else_);
				trycatch.mergeWith(elseBlock.getBody());
				trycatch.addEdge(new SequentialEdge(body.getEnd(), elseBlock.getBegin()));
				if (elseBlock.canBeContinued())
					// non-stopping last statement
					normalExits.add(elseBlock.getEnd());
			} 
		else if (ctx.else_ != null)
			// the else is deadcode
			throw new IMPSyntaxException("Statement '" + body.getEnd().toString() + "' at " + body.getEnd().getLocation() + " stops execution, so it cannot be followed by an else block");

		// we then parse each catch block, and we connect *every* instruction
		// in the body of the try to the beginning of each catch block
		List<CatchBlock> catches = new LinkedList<>();
		List<ParsedBlock> catchBodies = new LinkedList<>();
		for (CatchBlockContext ex : ctx.catchBlock()) {
			Pair<CatchBlock, ParsedBlock> tmp = visitCatchBlock(ex);
			CatchBlock block = tmp.getLeft();
			ParsedBlock visit = tmp.getRight();
			catches.add(block);
			catchBodies.add(visit);
			trycatch.mergeWith(visit.getBody());
			for (Statement st : body.getBody().getNodes())
				trycatch.addEdge(new ErrorEdge(st, visit.getBegin(), block.getIdentifier(), block.getExceptions()));
			if (visit.canBeContinued())
				// non-stopping last statement
				normalExits.add(visit.getEnd());
		}

		// this is the noop closing the whole try-catch, only if there is at least one path that does 
		// not return/throw anything
		Statement noop = new NoOp(cfg, body.getBegin().getLocation());
		trycatch.addNode(noop);
		boolean usedNoop = false;

		// lastly, we parse the finally block and
		// we connect it with the body (or the else block if it exists) and with each catch block
		ParsedBlock finalBlock = null;
		if (ctx.final_ != null) {
			finalBlock = visitBlock(ctx.final_);
			trycatch.mergeWith(finalBlock.getBody());

			if (elseBlock == null) {
				trycatch.addEdge(new BeginFinallyEdge(body.getEnd(), finalBlock.getBegin(), body.getEnd()));
				if (finalBlock.canBeContinued()) {
					// TODO need to smash the continuation into the normal one at returns
					trycatch.addEdge(new EndFinallyEdge(finalBlock.getEnd(), noop, body.getEnd()));
					usedNoop = true;
				}
			} else {
				trycatch.addEdge(new BeginFinallyEdge(elseBlock.getEnd(), finalBlock.getBegin(), elseBlock.getEnd()));
				if (finalBlock.canBeContinued()) {
					// TODO need to smash the continuation into the normal one at returns
					trycatch.addEdge(new EndFinallyEdge(finalBlock.getEnd(), noop, elseBlock.getEnd()));
					usedNoop = true;
				}
			}

			for (ParsedBlock catchBody : catchBodies) {
				trycatch.addEdge(new BeginFinallyEdge(catchBody.getEnd(), finalBlock.getBegin(), catchBody.getEnd()));
				if (finalBlock.canBeContinued()) {
					// TODO need to smash the continuation into the normal one at returns
					trycatch.addEdge(new EndFinallyEdge(finalBlock.getEnd(), noop, catchBody.getEnd()));
					usedNoop = true;
				}
			}
		}

		if (finalBlock != null && !usedNoop) 
			trycatch.removeNode(noop);
		else if (finalBlock == null && !normalExits.isEmpty()) {
			for (Statement st : normalExits)
				trycatch.addEdge(new SequentialEdge(st, noop));
			usedNoop = true;
		}

		// build protection block
		descriptor.addProtectionBlock(new ProtectionBlock(
			body.getBegin(),
			body.getBody().getNodes(),
			catches,
			elseBlock == null ? null : elseBlock.getBegin(),
			elseBlock == null ? List.of() : elseBlock.getBody().getNodes(), 
			finalBlock == null ? null : finalBlock.getBegin(),
			finalBlock == null ? List.of() : finalBlock.getBody().getNodes(),
			usedNoop ? noop : null));

		return new ParsedBlock(body.getBegin(), trycatch, usedNoop ? noop : null);
	}

	@Override
	public Pair<
			CatchBlock, 
			ParsedBlock
			> visitCatchBlock(CatchBlockContext ctx) {
		List<Type> exceptions = new LinkedList<>();
		for (UnitNameContext name : ctx.unitNames().unitName()) {
			Type t = ClassType.lookup(name.getText());
			if (t == null)
				throw new IMPSyntaxException(
						"Type '" + name.getText() + "' not found at " + new SourceCodeLocation(file, getLine(ctx), getCol(ctx)));
			exceptions.add(t);
		}

		VariableRef variable = null;
		tracker.enterScope();
		if (ctx.IDENTIFIER() != null) {
			variable = visitVar(ctx.IDENTIFIER(), false);
			tracker.addVariable(variable.getName(), variable, new Annotations());
		}


		ParsedBlock body = visitBlock(ctx.body);
		Statement last = body.getEnd();

		tracker.exitScope(last);
		
		CatchBlock catchBlock = new CatchBlock(
				variable,
				body.getBegin(),
				body.getBody().getNodes(),
				exceptions.toArray(Type[]::new));

		Pair<CatchBlock, ParsedBlock> result = Pair.of(catchBlock, body);
		return result;
	}

}

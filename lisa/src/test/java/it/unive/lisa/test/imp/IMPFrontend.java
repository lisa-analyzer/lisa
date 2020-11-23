package it.unive.lisa.test.imp;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;

import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.Token;
import org.antlr.v4.runtime.tree.TerminalNode;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.tuple.Triple;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import it.unive.lisa.cfg.AdjacencyMatrix;
import it.unive.lisa.cfg.CFG;
import it.unive.lisa.cfg.CFGDescriptor;
import it.unive.lisa.cfg.Parameter;
import it.unive.lisa.cfg.edge.FalseEdge;
import it.unive.lisa.cfg.edge.SequentialEdge;
import it.unive.lisa.cfg.edge.TrueEdge;
import it.unive.lisa.cfg.statement.Assignment;
import it.unive.lisa.cfg.statement.Expression;
import it.unive.lisa.cfg.statement.Literal;
import it.unive.lisa.cfg.statement.NoOp;
import it.unive.lisa.cfg.statement.NullLiteral;
import it.unive.lisa.cfg.statement.Ret;
import it.unive.lisa.cfg.statement.Return;
import it.unive.lisa.cfg.statement.Statement;
import it.unive.lisa.cfg.statement.Throw;
import it.unive.lisa.cfg.statement.UnresolvedCall;
import it.unive.lisa.cfg.statement.Variable;
import it.unive.lisa.cfg.type.Type;
import it.unive.lisa.cfg.type.Untyped;
import it.unive.lisa.test.antlr.IMPLexer;
import it.unive.lisa.test.antlr.IMPParser;
import it.unive.lisa.test.antlr.IMPParser.ArgContext;
import it.unive.lisa.test.antlr.IMPParser.ArgumentsContext;
import it.unive.lisa.test.antlr.IMPParser.ArrayAccessContext;
import it.unive.lisa.test.antlr.IMPParser.AssignmentContext;
import it.unive.lisa.test.antlr.IMPParser.BasicExprContext;
import it.unive.lisa.test.antlr.IMPParser.BlockContext;
import it.unive.lisa.test.antlr.IMPParser.BlockOrStatementContext;
import it.unive.lisa.test.antlr.IMPParser.ExpressionContext;
import it.unive.lisa.test.antlr.IMPParser.FieldAccessContext;
import it.unive.lisa.test.antlr.IMPParser.FileContext;
import it.unive.lisa.test.antlr.IMPParser.ForLoopContext;
import it.unive.lisa.test.antlr.IMPParser.FormalContext;
import it.unive.lisa.test.antlr.IMPParser.FormalsContext;
import it.unive.lisa.test.antlr.IMPParser.IndexContext;
import it.unive.lisa.test.antlr.IMPParser.LiteralContext;
import it.unive.lisa.test.antlr.IMPParser.LoopContext;
import it.unive.lisa.test.antlr.IMPParser.MethodCallContext;
import it.unive.lisa.test.antlr.IMPParser.MethodDeclarationContext;
import it.unive.lisa.test.antlr.IMPParser.NewBasicArrayExprContext;
import it.unive.lisa.test.antlr.IMPParser.NewReferenceTypeContext;
import it.unive.lisa.test.antlr.IMPParser.ParExprContext;
import it.unive.lisa.test.antlr.IMPParser.PrimitiveTypeContext;
import it.unive.lisa.test.antlr.IMPParser.ReceiverContext;
import it.unive.lisa.test.antlr.IMPParser.StatementContext;
import it.unive.lisa.test.antlr.IMPParser.WhileLoopContext;
import it.unive.lisa.test.antlr.IMPParserBaseVisitor;
import it.unive.lisa.test.imp.expressions.IMPAdd;
import it.unive.lisa.test.imp.expressions.IMPAnd;
import it.unive.lisa.test.imp.expressions.IMPArrayAccess;
import it.unive.lisa.test.imp.expressions.IMPAssert;
import it.unive.lisa.test.imp.expressions.IMPDiv;
import it.unive.lisa.test.imp.expressions.IMPEqual;
import it.unive.lisa.test.imp.expressions.IMPFalseLiteral;
import it.unive.lisa.test.imp.expressions.IMPFieldAccess;
import it.unive.lisa.test.imp.expressions.IMPFloatLiteral;
import it.unive.lisa.test.imp.expressions.IMPGreaterOrEqual;
import it.unive.lisa.test.imp.expressions.IMPGreaterThan;
import it.unive.lisa.test.imp.expressions.IMPIntLiteral;
import it.unive.lisa.test.imp.expressions.IMPLessOrEqual;
import it.unive.lisa.test.imp.expressions.IMPLessThan;
import it.unive.lisa.test.imp.expressions.IMPMod;
import it.unive.lisa.test.imp.expressions.IMPMul;
import it.unive.lisa.test.imp.expressions.IMPNeg;
import it.unive.lisa.test.imp.expressions.IMPNewArray;
import it.unive.lisa.test.imp.expressions.IMPNewObj;
import it.unive.lisa.test.imp.expressions.IMPNot;
import it.unive.lisa.test.imp.expressions.IMPNotEqual;
import it.unive.lisa.test.imp.expressions.IMPOr;
import it.unive.lisa.test.imp.expressions.IMPStringLiteral;
import it.unive.lisa.test.imp.expressions.IMPSub;
import it.unive.lisa.test.imp.expressions.IMPTrueLiteral;
import it.unive.lisa.test.imp.types.BoolType;
import it.unive.lisa.test.imp.types.ClassType;
import it.unive.lisa.test.imp.types.FloatType;
import it.unive.lisa.test.imp.types.IntType;

public class IMPFrontend extends IMPParserBaseVisitor<Object> {

	private static final Logger log = LogManager.getLogger(IMPFrontend.class);

	public static Collection<CFG> processFile(String file) throws IOException {
		return new IMPFrontend(file).work();
	}

	private final String file;

	private Collection<CFG> cfgs;

	private AdjacencyMatrix matrix;

	private Collection<Statement> entrypoints;

	private CFG currentCFG;

	private Statement lastStatement;

	private IMPFrontend(String file) {
		this.cfgs = new HashSet<CFG>();
		this.file = file;
	}

	private Collection<CFG> work() throws IOException {
		log.info("Reading file... " + file);
		InputStream stream;
		try {
			stream = new FileInputStream(file);
		} catch (FileNotFoundException e) {
			System.err.println(file + " does not exist. Exiting.");
			return new ArrayList<>();
		}

		IMPLexer lexer = new IMPLexer(CharStreams.fromStream(stream, StandardCharsets.UTF_8));
		IMPParser parser = new IMPParser(new CommonTokenStream(lexer));
		FileContext file = parser.file();
		visitFile(file);
		stream.close();

		return cfgs;
	}

	private int getLine(ParserRuleContext ctx) {
		return ctx.getStart().getLine();
	}

	private int getCol(ParserRuleContext ctx) {
		return ctx.getStop().getCharPositionInLine();
	}

	private int getCol(Token ctx) {
		return ctx.getCharPositionInLine();
	}

	private int getLine(Token ctx) {
		return ctx.getLine();
	}

	@Override
	public CFG visitMethodDeclaration(MethodDeclarationContext ctx) {
		entrypoints = new HashSet<>();
		matrix = new AdjacencyMatrix();
		// side effects on entrypoints and matrix will affect the cfg
		currentCFG = new CFG(mkDescriptor(ctx), entrypoints, matrix);

		lastStatement = null;
		Triple<AdjacencyMatrix, Statement, Statement> visited = visitBlock(ctx.block());
		entrypoints.add(visited.getMiddle());
		matrix.mergeWith(visited.getMiddle(), visited.getLeft());

		currentCFG.simplify();
		cfgs.add(currentCFG);
		return currentCFG;
	}

	private CFGDescriptor mkDescriptor(MethodDeclarationContext ctx) {
		return new CFGDescriptor(file, getLine(ctx), getCol(ctx), ctx.name.getText(), visitFormals(ctx.formals()));
	}

	@Override
	public Parameter[] visitFormals(FormalsContext ctx) {
		Parameter[] formals = new Parameter[ctx.formal().size()];
		int i = 0;
		for (FormalContext f : ctx.formal())
			formals[i++] = visitFormal(f);
		return formals;
	}

	@Override
	public Parameter visitFormal(FormalContext ctx) {
		return new Parameter(file, getLine(ctx), getCol(ctx), ctx.name.getText(), Untyped.INSTANCE);
	}

	@Override
	public Triple<AdjacencyMatrix, Statement, Statement> visitBlock(BlockContext ctx) {
		Statement first = null, last = null;
		AdjacencyMatrix matrix = new AdjacencyMatrix();
		for (int i = 0; i < ctx.blockOrStatement().size(); i++) {
			Triple<AdjacencyMatrix, Statement, Statement> st = visitBlockOrStatement(ctx.blockOrStatement(i));
			if (first == null)
				first = st.getMiddle();
			matrix.addNode(st.getMiddle());
			if (lastStatement != null)
				matrix.addEdge(new SequentialEdge(lastStatement, st.getMiddle()));
			if (st.getLeft() != null)
				matrix.mergeWith(st.getMiddle(), st.getLeft());
			lastStatement = st.getRight();
		}

		return Triple.of(matrix, first, last);
	}

	@Override
	public Triple<AdjacencyMatrix, Statement, Statement> visitBlockOrStatement(BlockOrStatementContext ctx) {
		Statement tmp;
		if (ctx.statement() != null)
			return Triple.of(null, tmp = visitStatement(ctx.statement()), tmp);
		else
			return visitBlock(ctx.block());
	}

	@Override
	public Statement visitStatement(StatementContext ctx) {
		if (ctx.assignment() != null)
			return visitAssignment(ctx.assignment());
		else if (ctx.ASSERT() != null)
			return new IMPAssert(currentCFG, file, getLine(ctx), getCol(ctx), visitExpression(ctx.expression()));
		else if (ctx.RETURN() != null)
			if (ctx.expression() != null)
				return new Return(currentCFG, file, getLine(ctx), getCol(ctx), visitExpression(ctx.expression()));
			else
				return new Ret(currentCFG, file, getLine(ctx), getCol(ctx));
		else if (ctx.THROW() != null)
			return new Throw(currentCFG, file, getLine(ctx), getCol(ctx), visitExpression(ctx.expression()));
		else if (ctx.skip != null)
			return new NoOp(currentCFG, file, getLine(ctx), getCol(ctx));
		else if (ctx.IF() != null)
			return visitIf(ctx);
		else if (ctx.loop() != null)
			return visitLoop(ctx.loop());
		else if (ctx.command != null)
			return visitExpression(ctx.command);
		else
			throw new IllegalArgumentException("Statement '" + ctx.toString() + "' cannot be parsed");
	}

	private Statement visitIf(StatementContext ctx) {
		Statement condition = visitParExpr(ctx.parExpr());
		matrix.addNode(condition);
		matrix.addEdge(new SequentialEdge(lastStatement, condition));

		lastStatement = null; // this prevents the sequential edge between condition and next to be created
		Triple<AdjacencyMatrix, Statement, Statement> then = visitBlockOrStatement(ctx.then);
		matrix.addEdge(new TrueEdge(condition, then.getMiddle()));
		if (then.getLeft() != null)
			matrix.mergeWith(then.getMiddle(), then.getLeft());

		Triple<AdjacencyMatrix, Statement, Statement> otherwise = null;
		if (ctx.otherwise != null) {
			lastStatement = null; // this prevents the sequential edge between condition and next to be created
			otherwise = visitBlockOrStatement(ctx.otherwise);
			matrix.addEdge(new FalseEdge(condition, otherwise.getMiddle()));
			if (otherwise.getLeft() != null)
				matrix.mergeWith(otherwise.getMiddle(), otherwise.getLeft());
		}

		Statement noop = new NoOp(currentCFG, file, condition.getLine(), condition.getCol());
		matrix.addNode(noop);
		matrix.addEdge(new SequentialEdge(then.getRight(), noop));
		if (otherwise != null)
			matrix.addEdge(new SequentialEdge(otherwise.getRight(), noop));
		lastStatement = noop;
		return noop;
	}

	@Override
	public Expression visitParExpr(ParExprContext ctx) {
		return visitExpression(ctx.expression());
	}

	@Override
	public Statement visitLoop(LoopContext ctx) {
		if (ctx.whileLoop() != null)
			return visitWhileLoop(ctx.whileLoop());
		else
			return visitForLoop(ctx.forLoop());
	}

	@Override
	public Statement visitWhileLoop(WhileLoopContext ctx) {
		Statement condition = visitParExpr(ctx.parExpr());
		matrix.addNode(condition);
		matrix.addEdge(new SequentialEdge(lastStatement, condition));

		lastStatement = null; // this prevents the sequential edge between condition and next to be created
		Triple<AdjacencyMatrix, Statement, Statement> body = visitBlockOrStatement(ctx.blockOrStatement());
		matrix.addEdge(new TrueEdge(condition, body.getMiddle()));
		matrix.addEdge(new SequentialEdge(body.getRight(), condition));
		if (body.getLeft() != null)
			matrix.mergeWith(body.getMiddle(), body.getLeft());

		Statement noop = new NoOp(currentCFG, file, condition.getLine(), condition.getCol());
		matrix.addNode(noop);
		matrix.addEdge(new FalseEdge(condition, noop));
		lastStatement = noop;
		return noop;
	}

	@Override
	public Statement visitForLoop(ForLoopContext ctx) {
		AssignmentContext init = ctx.forDeclaration().init;
		ExpressionContext cond = ctx.forDeclaration().condition;
		AssignmentContext post = ctx.forDeclaration().post;

		if (init != null) {
			Statement assignment = visitAssignment(init);
			matrix.addNode(assignment);
			matrix.addEdge(new SequentialEdge(lastStatement, assignment));
			lastStatement = assignment;
		}

		Statement condition = visitExpression(cond);
		matrix.addNode(condition);
		matrix.addEdge(new SequentialEdge(lastStatement, condition));

		lastStatement = null; // this prevents the sequential edge between condition and next to be created
		Triple<AdjacencyMatrix, Statement, Statement> body = visitBlockOrStatement(ctx.blockOrStatement());
		matrix.addEdge(new TrueEdge(condition, body.getMiddle()));
		if (body.getLeft() != null)
			matrix.mergeWith(body.getMiddle(), body.getLeft());

		lastStatement = body.getRight();
		if (post != null) {
			Statement assignment = visitAssignment(post);
			matrix.addNode(assignment);
			matrix.addEdge(new SequentialEdge(lastStatement, assignment));
			lastStatement = assignment;
		}
		matrix.addEdge(new SequentialEdge(lastStatement, condition));

		Statement noop = new NoOp(currentCFG, file, condition.getLine(), condition.getCol());
		matrix.addNode(noop);
		matrix.addEdge(new FalseEdge(condition, noop));
		lastStatement = noop;
		return noop;
	}

	@Override
	public Assignment visitAssignment(AssignmentContext ctx) {
		Expression expression = visitExpression(ctx.expression());
		Expression target = null;
		if (ctx.IDENTIFIER() != null)
			target = visitVar(ctx.IDENTIFIER());
		else if (ctx.fieldAccess() != null)
			target = visitFieldAccess(ctx.fieldAccess());
		else if (ctx.arrayAccess() != null)
			target = visitArrayAccess(ctx.arrayAccess());

		return new Assignment(currentCFG, file, getLine(ctx), getCol(ctx), target, expression);
	}

	private Variable visitVar(TerminalNode identifier) {
		return new Variable(currentCFG, file, getLine(identifier.getSymbol()), getCol(identifier.getSymbol()),
				identifier.getText(), Untyped.INSTANCE);
	}

	@Override
	public IMPFieldAccess visitFieldAccess(FieldAccessContext ctx) {
		Expression receiver = visitReceiver(ctx.receiver());
		IMPStringLiteral id = new IMPStringLiteral(currentCFG, file, getLine(ctx.name), getCol(ctx.name),
				ctx.name.getText());
		return new IMPFieldAccess(currentCFG, file, getLine(ctx), getCol(ctx), receiver, id);
	}

	@Override
	public Expression visitReceiver(ReceiverContext ctx) {
		if (ctx.THIS() != null)
			return visitVar(ctx.THIS());
		else if (ctx.SUPER() != null)
			return visitVar(ctx.SUPER());
		else
			return visitVar(ctx.IDENTIFIER());
	}

	@Override
	public IMPFieldAccess visitArrayAccess(ArrayAccessContext ctx) {
		Variable receiver = visitVar(ctx.IDENTIFIER());
		Expression result = receiver;
		for (IndexContext i : ctx.index())
			result = new IMPArrayAccess(currentCFG, file, getLine(i), getCol(i), result, visitIndex(i));

		return (IMPFieldAccess) result;
	}

	@Override
	public Expression visitIndex(IndexContext ctx) {
		if (ctx.IDENTIFIER() != null)
			return visitVar(ctx.IDENTIFIER());
		else
			return new IMPIntLiteral(currentCFG, file, getLine(ctx), getCol(ctx),
					Integer.parseInt(ctx.LITERAL_DECIMAL().getText()));
	}

	@Override
	public Expression visitExpression(ExpressionContext ctx) {
		int line = getLine(ctx);
		int col = getCol(ctx);
		if (ctx.paren != null)
			return visitExpression(ctx.paren);
		else if (ctx.basicExpr() != null)
			return visitBasicExpr(ctx.basicExpr());
		else if (ctx.nested != null)
			if (ctx.NOT() != null)
				return new IMPNot(currentCFG, file, line, col, visitExpression(ctx.nested));
			else
				return new IMPNeg(currentCFG, file, line, col, visitExpression(ctx.nested));
		else if (ctx.left != null && ctx.right != null)
			if (ctx.MUL() != null)
				return new IMPMul(currentCFG, file, line, col, visitExpression(ctx.left), visitExpression(ctx.right));
			else if (ctx.MUL() != null)
				return new IMPDiv(currentCFG, file, line, col, visitExpression(ctx.left), visitExpression(ctx.right));
			else if (ctx.MUL() != null)
				return new IMPMod(currentCFG, file, line, col, visitExpression(ctx.left), visitExpression(ctx.right));
			else if (ctx.MUL() != null)
				return new IMPAdd(currentCFG, file, line, col, visitExpression(ctx.left), visitExpression(ctx.right));
			else if (ctx.MUL() != null)
				return new IMPSub(currentCFG, file, line, col, visitExpression(ctx.left), visitExpression(ctx.right));
			else if (ctx.MUL() != null)
				return new IMPGreaterThan(currentCFG, file, line, col, visitExpression(ctx.left),
						visitExpression(ctx.right));
			else if (ctx.MUL() != null)
				return new IMPGreaterOrEqual(currentCFG, file, line, col, visitExpression(ctx.left),
						visitExpression(ctx.right));
			else if (ctx.MUL() != null)
				return new IMPLessThan(currentCFG, file, line, col, visitExpression(ctx.left),
						visitExpression(ctx.right));
			else if (ctx.MUL() != null)
				return new IMPLessOrEqual(currentCFG, file, line, col, visitExpression(ctx.left),
						visitExpression(ctx.right));
			else if (ctx.MUL() != null)
				return new IMPEqual(currentCFG, file, line, col, visitExpression(ctx.left), visitExpression(ctx.right));
			else if (ctx.MUL() != null)
				return new IMPNotEqual(currentCFG, file, line, col, visitExpression(ctx.left),
						visitExpression(ctx.right));
			else if (ctx.MUL() != null)
				return new IMPAnd(currentCFG, file, line, col, visitExpression(ctx.left), visitExpression(ctx.right));
			else
				return new IMPOr(currentCFG, file, line, col, visitExpression(ctx.left), visitExpression(ctx.right));
		else if (ctx.NEW() != null)
			if (ctx.newBasicArrayExpr() != null)
				return visitNewBasicArrayExpr(ctx.newBasicArrayExpr());
			else
				return visitNewReferenceType(ctx.newReferenceType());
		else if (ctx.arrayAccess() != null)
			return visitArrayAccess(ctx.arrayAccess());
		else if (ctx.fieldAccess() != null)
			return visitFieldAccess(ctx.fieldAccess());
		else if (ctx.methodCall() != null)
			return visitMethodCall(ctx.methodCall());

		throw new UnsupportedOperationException("Type of expression not supported: " + ctx);
	}

	@Override
	public Expression visitNewBasicArrayExpr(NewBasicArrayExprContext ctx) {
		return new IMPNewArray(currentCFG, file, getLine(ctx), getCol(ctx), visitPrimitiveType(ctx.primitiveType()));
	}

	@Override
	public Type visitPrimitiveType(PrimitiveTypeContext ctx) {
		if (ctx.BOOLEAN() != null)
			return BoolType.INSTANCE;
		else if (ctx.INT() != null)
			return IntType.INSTANCE;
		else
			return FloatType.INSTANCE;
	}

	@Override
	public Expression visitNewReferenceType(NewReferenceTypeContext ctx) {
		Type base = ClassType.lookup(ctx.IDENTIFIER().getText(), null);
		if (ctx.arrayCreatorRest() != null)
			return new IMPNewArray(currentCFG, file, getLine(ctx), getCol(ctx), base);
		else
			return new IMPNewObj(currentCFG, file, getLine(ctx), getCol(ctx), base, visitArguments(ctx.arguments()));
	}

	@Override
	public Expression[] visitArguments(ArgumentsContext ctx) {
		Expression[] args = new Expression[ctx.arg().size()];
		int i = 0;
		for (ArgContext arg : ctx.arg())
			args[i++] = visitArg(arg);
		return args;
	}

	@Override
	public Expression visitArg(ArgContext ctx) {
		if (ctx.literal() != null)
			return visitLiteral(ctx.literal());
		else if (ctx.fieldAccess() != null)
			return visitFieldAccess(ctx.fieldAccess());
		else if (ctx.arrayAccess() != null)
			return visitArrayAccess(ctx.arrayAccess());
		else if (ctx.methodCall() != null)
			return visitMethodCall(ctx.methodCall());
		else if (ctx.IDENTIFIER() != null)
			return visitVar(ctx.IDENTIFIER());
		else
			return visitVar(ctx.THIS());
	}

	@Override
	public Expression visitMethodCall(MethodCallContext ctx) {
		Expression receiver = visitReceiver(ctx.receiver());
		String name = ctx.name.getText();
		Expression[] args = ArrayUtils.insert(0, visitArguments(ctx.arguments()), receiver);
		return new UnresolvedCall(currentCFG, file, getLine(ctx), getCol(ctx), name, args);
	}

	@Override
	public Expression visitBasicExpr(BasicExprContext ctx) {
		if (ctx.literal() != null)
			return visitLiteral(ctx.literal());
		else if (ctx.THIS() != null)
			return visitVar(ctx.THIS());
		else if (ctx.SUPER() != null)
			return visitVar(ctx.SUPER());
		else
			return visitVar(ctx.IDENTIFIER());
	}

	@Override
	public Literal visitLiteral(LiteralContext ctx) {
		int line = getLine(ctx);
		int col = getCol(ctx);
		if (ctx.LITERAL_NULL() != null)
			return new NullLiteral(currentCFG, file, line, col);
		else if (ctx.LITERAL_BOOL() != null)
			if (ctx.LITERAL_BOOL().getText().equals("true"))
				return new IMPTrueLiteral(currentCFG, file, line, col);
			else
				return new IMPFalseLiteral(currentCFG, file, line, col);
		else if (ctx.LITERAL_STRING() != null)
			return new IMPStringLiteral(currentCFG, file, line, col, ctx.LITERAL_STRING().getText());
		else if (ctx.LITERAL_FLOAT() != null)
			if (ctx.SUB() != null)
				return new IMPFloatLiteral(currentCFG, file, line, col,
						-Float.parseFloat(ctx.LITERAL_FLOAT().getText()));
			else
				return new IMPFloatLiteral(currentCFG, file, line, col,
						Float.parseFloat(ctx.LITERAL_FLOAT().getText()));
		else if (ctx.LITERAL_DECIMAL() != null)
			if (ctx.SUB() != null)
				return new IMPIntLiteral(currentCFG, file, line, col,
						-Integer.parseInt(ctx.LITERAL_DECIMAL().getText()));
			else
				return new IMPIntLiteral(currentCFG, file, line, col,
						Integer.parseInt(ctx.LITERAL_DECIMAL().getText()));

		throw new UnsupportedOperationException("Type of literal not supported: " + ctx);
	}
}

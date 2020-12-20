package it.unive.lisa.test.imp;

import it.unive.lisa.cfg.CFG;
import it.unive.lisa.cfg.CFGDescriptor;
import it.unive.lisa.cfg.Parameter;
import it.unive.lisa.cfg.edge.Edge;
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
import it.unive.lisa.test.antlr.IMPParser.ArrayCreatorRestContext;
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
import it.unive.lisa.util.datastructures.graph.AdjacencyMatrix;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.HashSet;
import org.antlr.v4.runtime.BailErrorStrategy;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.RecognitionException;
import org.antlr.v4.runtime.Token;
import org.antlr.v4.runtime.atn.PredictionMode;
import org.antlr.v4.runtime.tree.TerminalNode;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * An {@link IMPParserBaseVisitor} that will parse the IMP code building a
 * representation that can be analyzed through LiSA. Methods overridden in this
 * class return a {@link Pair} of {@link Statement}s to uniquely mark the entry-
 * and exit-points of the code that has been generated by vising an AST node.
 * 
 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
 */
public class IMPFrontend extends IMPParserBaseVisitor<Object> {

	private static final Logger log = LogManager.getLogger(IMPFrontend.class);

	/**
	 * Parses a file using the {@link IMPLexer} and the {@link IMPParser}
	 * produced by compiling the ANTLR4 grammar, and yields the collection of
	 * {@link CFG}s that corresponds to the methods parsed from that file.
	 * 
	 * @param file the complete path (relative or absolute) of the file to parse
	 * 
	 * @return the collection of {@link CFG}s parsed from that file
	 * 
	 * @throws ParsingException if this frontend is unable to parse the file
	 */
	public static Collection<CFG> processFile(String file) throws ParsingException {
		return new IMPFrontend(file).work();
	}

	private final String file;

	private Collection<CFG> cfgs;

	private AdjacencyMatrix<Statement, Edge> matrix;

	private CFG currentCFG;

	private IMPFrontend(String file) {
		this.cfgs = new HashSet<CFG>();
		this.file = file;
	}

	private Collection<CFG> work() throws ParsingException {
		log.info("Reading file... " + file);
		try (InputStream stream = new FileInputStream(file)) {
			// common antlr4 initialization
			IMPLexer lexer = new IMPLexer(CharStreams.fromStream(stream, StandardCharsets.UTF_8));
			IMPParser parser = new IMPParser(new CommonTokenStream(lexer));

			// this is needed to get an exception on malformed input
			// otherwise an error is dumped to stderr and the partial
			// parsing result is returned
			parser.setErrorHandler(new BailErrorStrategy());
			parser.getInterpreter().setPredictionMode(PredictionMode.SLL);

			FileContext file = parser.file();
			visitFile(file);
			return cfgs;
		} catch (FileNotFoundException e) {
			log.fatal(file + " does not exist", e);
			throw new ParsingException("Target file '" + file + "' does not exist", e);
		} catch (RecognitionException e) {
			throw Antlr4Util.handleRecognitionException(file, e);
		} catch (Exception e) {
			if (e.getCause() instanceof RecognitionException)
				throw Antlr4Util.handleRecognitionException(file, (RecognitionException) e.getCause());
			else {
				log.error("Parser thrown an exception while parsing " + file, e);
				throw new ParsingException("Parser thrown an exception while parsing " + file, e);
			}
		}
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
		Collection<Statement> entrypoints = new HashSet<>();
		matrix = new AdjacencyMatrix<>();
		// side effects on entrypoints and matrix will affect the cfg
		currentCFG = new CFG(mkDescriptor(ctx), entrypoints, matrix);

		Pair<Statement, Statement> visited = visitBlock(ctx.block());
		entrypoints.add(visited.getLeft());

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
	public Pair<Statement, Statement> visitBlock(BlockContext ctx) {
		Statement first = null, last = null;
		for (int i = 0; i < ctx.blockOrStatement().size(); i++) {
			Pair<Statement, Statement> st = visitBlockOrStatement(ctx.blockOrStatement(i));
			if (first == null)
				first = st.getLeft();
			if (last != null)
				matrix.addEdge(new SequentialEdge(last, st.getLeft()));
			last = st.getRight();
		}

		return Pair.of(first, last);
	}

	@Override
	public Pair<Statement, Statement> visitBlockOrStatement(BlockOrStatementContext ctx) {
		if (ctx.statement() != null)
			return visitStatement(ctx.statement());
		else
			return visitBlock(ctx.block());
	}

	@Override
	public Pair<Statement, Statement> visitStatement(StatementContext ctx) {
		Statement st;
		if (ctx.assignment() != null)
			st = visitAssignment(ctx.assignment());
		else if (ctx.ASSERT() != null)
			st = new IMPAssert(currentCFG, file, getLine(ctx), getCol(ctx), visitExpression(ctx.expression()));
		else if (ctx.RETURN() != null)
			if (ctx.expression() != null)
				st = new Return(currentCFG, file, getLine(ctx), getCol(ctx),
						visitExpression(ctx.expression()));
			else
				st = new Ret(currentCFG, file, getLine(ctx), getCol(ctx));
		else if (ctx.THROW() != null)
			st = new Throw(currentCFG, file, getLine(ctx), getCol(ctx), visitExpression(ctx.expression()));
		else if (ctx.skip != null)
			st = new NoOp(currentCFG, file, getLine(ctx), getCol(ctx));
		else if (ctx.IF() != null)
			return visitIf(ctx);
		else if (ctx.loop() != null)
			return visitLoop(ctx.loop());
		else if (ctx.command != null)
			st = visitExpression(ctx.command);
		else
			throw new IllegalArgumentException("Statement '" + ctx.toString() + "' cannot be parsed");

		matrix.addNode(st);
		return Pair.of(st, st);
	}

	private Pair<Statement, Statement> visitIf(StatementContext ctx) {
		Statement condition = visitParExpr(ctx.parExpr());
		matrix.addNode(condition);

		Pair<Statement, Statement> then = visitBlockOrStatement(ctx.then);
		matrix.addEdge(new TrueEdge(condition, then.getLeft()));

		Pair<Statement, Statement> otherwise = null;
		if (ctx.otherwise != null) {
			otherwise = visitBlockOrStatement(ctx.otherwise);
			matrix.addEdge(new FalseEdge(condition, otherwise.getLeft()));
		}

		Statement noop = new NoOp(currentCFG, file, condition.getLine(), condition.getCol());
		matrix.addNode(noop);
		matrix.addEdge(new SequentialEdge(then.getRight(), noop));
		if (otherwise != null)
			matrix.addEdge(new SequentialEdge(otherwise.getRight(), noop));
		else
			matrix.addEdge(new FalseEdge(condition, noop));

		return Pair.of(condition, noop);
	}

	@Override
	public Expression visitParExpr(ParExprContext ctx) {
		return visitExpression(ctx.expression());
	}

	@Override
	public Pair<Statement, Statement> visitLoop(LoopContext ctx) {
		if (ctx.whileLoop() != null)
			return visitWhileLoop(ctx.whileLoop());
		else
			return visitForLoop(ctx.forLoop());
	}

	@Override
	public Pair<Statement, Statement> visitWhileLoop(WhileLoopContext ctx) {
		Statement condition = visitParExpr(ctx.parExpr());
		matrix.addNode(condition);

		Pair<Statement, Statement> body = visitBlockOrStatement(ctx.blockOrStatement());
		matrix.addEdge(new TrueEdge(condition, body.getLeft()));
		matrix.addEdge(new SequentialEdge(body.getRight(), condition));

		Statement noop = new NoOp(currentCFG, file, condition.getLine(), condition.getCol());
		matrix.addNode(noop);
		matrix.addEdge(new FalseEdge(condition, noop));

		return Pair.of(condition, noop);
	}

	@Override
	public Pair<Statement, Statement> visitForLoop(ForLoopContext ctx) {
		AssignmentContext init = ctx.forDeclaration().init;
		ExpressionContext cond = ctx.forDeclaration().condition;
		AssignmentContext post = ctx.forDeclaration().post;

		Statement first = null, last = null;
		if (init != null) {
			Statement assignment = visitAssignment(init);
			matrix.addNode(assignment);
			first = assignment;
		}

		Statement condition = visitExpression(cond);
		matrix.addNode(condition);
		if (first == null)
			first = condition;
		else
			matrix.addEdge(new SequentialEdge(first, condition));

		Pair<Statement, Statement> body = visitBlockOrStatement(ctx.blockOrStatement());
		matrix.addEdge(new TrueEdge(condition, body.getLeft()));
		last = body.getRight();

		if (post != null) {
			Assignment inc = visitAssignment(post);
			matrix.addNode(inc);
			matrix.addEdge(new SequentialEdge(body.getRight(), inc));
			last = inc;
		}

		matrix.addEdge(new SequentialEdge(last, condition));

		Statement noop = new NoOp(currentCFG, file, condition.getLine(), condition.getCol());
		matrix.addNode(noop);
		matrix.addEdge(new FalseEdge(condition, noop));

		return Pair.of(first, noop);
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
	public IMPArrayAccess visitArrayAccess(ArrayAccessContext ctx) {
		Variable receiver = visitVar(ctx.IDENTIFIER());
		Expression result = receiver;
		for (IndexContext i : ctx.index())
			result = new IMPArrayAccess(currentCFG, file, getLine(i), getCol(i), result, visitIndex(i));

		return (IMPArrayAccess) result;
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
			else if (ctx.DIV() != null)
				return new IMPDiv(currentCFG, file, line, col, visitExpression(ctx.left), visitExpression(ctx.right));
			else if (ctx.MOD() != null)
				return new IMPMod(currentCFG, file, line, col, visitExpression(ctx.left), visitExpression(ctx.right));
			else if (ctx.ADD() != null)
				return new IMPAdd(currentCFG, file, line, col, visitExpression(ctx.left), visitExpression(ctx.right));
			else if (ctx.SUB() != null)
				return new IMPSub(currentCFG, file, line, col, visitExpression(ctx.left), visitExpression(ctx.right));
			else if (ctx.GT() != null)
				return new IMPGreaterThan(currentCFG, file, line, col, visitExpression(ctx.left),
						visitExpression(ctx.right));
			else if (ctx.GE() != null)
				return new IMPGreaterOrEqual(currentCFG, file, line, col, visitExpression(ctx.left),
						visitExpression(ctx.right));
			else if (ctx.LT() != null)
				return new IMPLessThan(currentCFG, file, line, col, visitExpression(ctx.left),
						visitExpression(ctx.right));
			else if (ctx.LE() != null)
				return new IMPLessOrEqual(currentCFG, file, line, col, visitExpression(ctx.left),
						visitExpression(ctx.right));
			else if (ctx.EQUAL() != null)
				return new IMPEqual(currentCFG, file, line, col, visitExpression(ctx.left), visitExpression(ctx.right));
			else if (ctx.NOTEQUAL() != null)
				return new IMPNotEqual(currentCFG, file, line, col, visitExpression(ctx.left),
						visitExpression(ctx.right));
			else if (ctx.AND() != null)
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
		return new IMPNewArray(currentCFG, file, getLine(ctx), getCol(ctx), visitPrimitiveType(ctx.primitiveType()),
				visitArrayCreatorRest(ctx.arrayCreatorRest()));
	}

	@Override
	public Expression[] visitArrayCreatorRest(ArrayCreatorRestContext ctx) {
		Expression[] result = new Expression[ctx.index().size()];
		for (int i = 0; i < result.length; i++)
			result[i] = visitIndex(ctx.index(i));
		return result;
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
			return new IMPNewArray(currentCFG, file, getLine(ctx), getCol(ctx), base,
					visitArrayCreatorRest(ctx.arrayCreatorRest()));
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

package it.unive.lisa.test.imp;

import static it.unive.lisa.test.imp.Antlr4Util.getCol;
import static it.unive.lisa.test.imp.Antlr4Util.getLine;

import it.unive.lisa.program.Global;
import it.unive.lisa.program.cfg.CFG;
import it.unive.lisa.program.cfg.CFGDescriptor;
import it.unive.lisa.program.cfg.Parameter;
import it.unive.lisa.program.cfg.VariableTableEntry;
import it.unive.lisa.program.cfg.edge.Edge;
import it.unive.lisa.program.cfg.edge.FalseEdge;
import it.unive.lisa.program.cfg.edge.SequentialEdge;
import it.unive.lisa.program.cfg.edge.TrueEdge;
import it.unive.lisa.program.cfg.statement.AccessUnitGlobal;
import it.unive.lisa.program.cfg.statement.Assignment;
import it.unive.lisa.program.cfg.statement.Expression;
import it.unive.lisa.program.cfg.statement.Literal;
import it.unive.lisa.program.cfg.statement.NoOp;
import it.unive.lisa.program.cfg.statement.NullLiteral;
import it.unive.lisa.program.cfg.statement.Ret;
import it.unive.lisa.program.cfg.statement.Return;
import it.unive.lisa.program.cfg.statement.Statement;
import it.unive.lisa.program.cfg.statement.Throw;
import it.unive.lisa.program.cfg.statement.UnresolvedCall;
import it.unive.lisa.program.cfg.statement.VariableRef;
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
import it.unive.lisa.test.antlr.IMPParser.ForLoopContext;
import it.unive.lisa.test.antlr.IMPParser.FormalContext;
import it.unive.lisa.test.antlr.IMPParser.FormalsContext;
import it.unive.lisa.test.antlr.IMPParser.IndexContext;
import it.unive.lisa.test.antlr.IMPParser.LiteralContext;
import it.unive.lisa.test.antlr.IMPParser.LocalDeclarationContext;
import it.unive.lisa.test.antlr.IMPParser.LoopContext;
import it.unive.lisa.test.antlr.IMPParser.MethodCallContext;
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
import it.unive.lisa.type.Type;
import it.unive.lisa.type.Untyped;
import it.unive.lisa.util.datastructures.graph.AdjacencyMatrix;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Map;
import java.util.Map.Entry;
import org.antlr.v4.runtime.tree.TerminalNode;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.tuple.Pair;

/**
 * An {@link IMPParserBaseVisitor} that will parse the code of an IMP method or
 * constructor.
 * 
 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
 */
class IMPCodeMemberVisitor extends IMPParserBaseVisitor<Object> {

	private final String file;

	private final AdjacencyMatrix<Statement, Edge, CFG> matrix;

	private final Collection<Statement> entrypoints;

	private final Map<String, VariableRef> visibleIds;

	private final CFG cfg;

	private final CFGDescriptor descriptor;

	/**
	 * Builds the visitor of an IMP method or constructor.
	 * 
	 * @param file       the path of the file where the method or constructor
	 *                       appears
	 * @param descriptor the descriptor of the method or constructor
	 */
	IMPCodeMemberVisitor(String file, CFGDescriptor descriptor) {
		this.file = file;
		this.descriptor = descriptor;
		matrix = new AdjacencyMatrix<>();
		entrypoints = new HashSet<>();
		// side effects on entrypoints and matrix will affect the cfg
		cfg = new CFG(descriptor, entrypoints, matrix);

		visibleIds = new HashMap<>();
		for (VariableTableEntry par : descriptor.getVariables())
			visibleIds.put(par.getName(), par.createReference(cfg));
	}

	/**
	 * Visits the code of a {@link BlockContext} representing the code block of
	 * a method or constructor.
	 * 
	 * @param ctx the block context
	 * 
	 * @return the {@link CFG} built from the block
	 */
	CFG visitCodeMember(BlockContext ctx) {
		Pair<Statement, Statement> visited = visitBlock(ctx);
		entrypoints.add(visited.getLeft());

		if (cfg.getAllExitpoints().isEmpty()) {
			Ret ret = new Ret(cfg, file, descriptor.getLine(), descriptor.getCol());
			if (cfg.getNodesCount() == 0) {
				// empty method, so the ret is also the entrypoint
				matrix.addNode(ret);
				entrypoints.add(ret);
			} else {
				// every non-throwing instruction that does not have a follower
				// is ending the method
				Collection<Statement> preExits = new LinkedList<>();
				for (Statement st : matrix.getNodes())
					if (!st.stopsExecution() && matrix.followersOf(st).isEmpty())
						preExits.add(st);
				matrix.addNode(ret);
				for (Statement st : preExits)
					matrix.addEdge(new SequentialEdge(st, ret));

				for (VariableTableEntry entry : descriptor.getVariables())
					if (preExits.contains(entry.getScopeEnd()))
						entry.setScopeEnd(ret);
			}
		}

		cfg.simplify();
		return cfg;
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
		Map<String, VariableRef> backup = new HashMap<>(visibleIds);

		Statement first = null, last = null;
		for (int i = 0; i < ctx.blockOrStatement().size(); i++) {
			Pair<Statement, Statement> st = visitBlockOrStatement(ctx.blockOrStatement(i));
			if (first == null)
				first = st.getLeft();
			if (last != null)
				matrix.addEdge(new SequentialEdge(last, st.getLeft()));
			last = st.getRight();
		}

		Collection<String> toRemove = new HashSet<>();
		for (Entry<String, VariableRef> id : visibleIds.entrySet())
			if (!backup.containsKey(id.getKey())) {
				VariableRef ref = id.getValue();
				descriptor.addVariable(new VariableTableEntry(ref.getSourceFile(), ref.getLine(), ref.getCol(),
						0, ref.getRootStatement(), last, id.getKey(), Untyped.INSTANCE));
				toRemove.add(id.getKey());
			}

		if (!toRemove.isEmpty())
			toRemove.forEach(visibleIds::remove);

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
		if (ctx.localDeclaration() != null)
			st = visitLocalDeclaration(ctx.localDeclaration());
		else if (ctx.ASSERT() != null)
			st = new IMPAssert(cfg, file, getLine(ctx), getCol(ctx), visitExpression(ctx.expression()));
		else if (ctx.RETURN() != null)
			if (ctx.expression() != null)
				st = new Return(cfg, file, getLine(ctx), getCol(ctx),
						visitExpression(ctx.expression()));
			else
				st = new Ret(cfg, file, getLine(ctx), getCol(ctx));
		else if (ctx.THROW() != null)
			st = new Throw(cfg, file, getLine(ctx), getCol(ctx), visitExpression(ctx.expression()));
		else if (ctx.skip != null)
			st = new NoOp(cfg, file, getLine(ctx), getCol(ctx));
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

		Statement noop = new NoOp(cfg, file, condition.getLine(), condition.getCol());
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
	public Assignment visitLocalDeclaration(LocalDeclarationContext ctx) {
		Expression expression = visitExpression(ctx.expression());
		VariableRef ref = visitVar(ctx.IDENTIFIER(), false);

		if (visibleIds.containsKey(ref.getName()))
			throw new IMPSyntaxException(
					"Duplicate variable '" + ref.getName() + "' declared at " + toCodeLocation(ref));

		visibleIds.put(ref.getName(), ref);
		// the variable table entry will be generated at the end of the
		// containing block

		return new Assignment(cfg, file, getLine(ctx), getCol(ctx), ref, expression);
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

		Statement noop = new NoOp(cfg, file, condition.getLine(), condition.getCol());
		matrix.addNode(noop);
		matrix.addEdge(new FalseEdge(condition, noop));

		return Pair.of(condition, noop);
	}

	@Override
	public Pair<Statement, Statement> visitForLoop(ForLoopContext ctx) {
		LocalDeclarationContext initDecl = ctx.forDeclaration().initDecl;
		ExpressionContext initExpr = ctx.forDeclaration().initExpr;
		ExpressionContext cond = ctx.forDeclaration().condition;
		ExpressionContext post = ctx.forDeclaration().post;

		Statement first = null, last = null;
		if (initDecl != null) {
			Statement init = visitLocalDeclaration(initDecl);
			matrix.addNode(init);
			first = init;
		} else if (initExpr != null) {
			Statement init = visitExpression(initExpr);
			matrix.addNode(init);
			first = init;
		}

		Statement condition;
		if (cond != null)
			condition = visitExpression(cond);
		else
			condition = new IMPTrueLiteral(cfg, file, getLine(ctx), getCol(ctx));
		matrix.addNode(condition);
		if (first == null)
			first = condition;
		else
			matrix.addEdge(new SequentialEdge(first, condition));

		Pair<Statement, Statement> body = visitBlockOrStatement(ctx.blockOrStatement());
		matrix.addEdge(new TrueEdge(condition, body.getLeft()));
		last = body.getRight();

		if (post != null) {
			Statement inc = visitExpression(post);
			matrix.addNode(inc);
			matrix.addEdge(new SequentialEdge(body.getRight(), inc));
			last = inc;
		}

		matrix.addEdge(new SequentialEdge(last, condition));

		Statement noop = new NoOp(cfg, file, condition.getLine(), condition.getCol());
		matrix.addNode(noop);
		matrix.addEdge(new FalseEdge(condition, noop));

		return Pair.of(first, noop);
	}

	@Override
	public Assignment visitAssignment(AssignmentContext ctx) {
		Expression expression = visitExpression(ctx.expression());
		Expression target = null;
		if (ctx.IDENTIFIER() != null)
			target = visitVar(ctx.IDENTIFIER(), true);
		else if (ctx.fieldAccess() != null)
			target = visitFieldAccess(ctx.fieldAccess());
		else if (ctx.arrayAccess() != null)
			target = visitArrayAccess(ctx.arrayAccess());

		return new Assignment(cfg, file, getLine(ctx), getCol(ctx), target, expression);
	}

	private VariableRef visitVar(TerminalNode identifier, boolean localReference) {
		VariableRef ref = new VariableRef(cfg, file, getLine(identifier.getSymbol()), getCol(identifier.getSymbol()),
				identifier.getText(), Untyped.INSTANCE);
		if (localReference && !visibleIds.containsKey(ref.getName()))
			throw new IMPSyntaxException(
					"Referencing undeclared variable '" + ref.getName() + "' at " + toCodeLocation(ref));
		return ref;
	}

	private String toCodeLocation(Statement st) {
		return st.getSourceFile() + ":" + st.getLine() + ":" + st.getCol();
	}

	@Override
	public AccessUnitGlobal visitFieldAccess(FieldAccessContext ctx) {
		Expression receiver = visitReceiver(ctx.receiver());
		Global id = new Global(file, getLine(ctx.name), getCol(ctx.name), ctx.name.getText(), Untyped.INSTANCE);
		return new AccessUnitGlobal(cfg, file, getLine(ctx), getCol(ctx), receiver, id);
	}

	@Override
	public Expression visitReceiver(ReceiverContext ctx) {
		if (ctx.THIS() != null)
			return visitVar(ctx.THIS(), false);
		else if (ctx.SUPER() != null)
			return visitVar(ctx.SUPER(), false);
		else
			return visitVar(ctx.IDENTIFIER(), true);
	}

	@Override
	public IMPArrayAccess visitArrayAccess(ArrayAccessContext ctx) {
		Expression receiver = visitReceiver(ctx.receiver());
		Expression result = receiver;
		for (IndexContext i : ctx.index())
			result = new IMPArrayAccess(cfg, file, getLine(i), getCol(i), result, visitIndex(i));

		return (IMPArrayAccess) result;
	}

	@Override
	public Expression visitIndex(IndexContext ctx) {
		if (ctx.IDENTIFIER() != null)
			return visitVar(ctx.IDENTIFIER(), true);
		else
			return new IMPIntLiteral(cfg, file, getLine(ctx), getCol(ctx),
					Integer.parseInt(ctx.LITERAL_DECIMAL().getText()));
	}

	@Override
	public Expression visitExpression(ExpressionContext ctx) {
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
				return new IMPNot(cfg, file, line, col, visitExpression(ctx.nested));
			else
				return new IMPNeg(cfg, file, line, col, visitExpression(ctx.nested));
		else if (ctx.left != null && ctx.right != null)
			if (ctx.MUL() != null)
				return new IMPMul(cfg, file, line, col, visitExpression(ctx.left), visitExpression(ctx.right));
			else if (ctx.DIV() != null)
				return new IMPDiv(cfg, file, line, col, visitExpression(ctx.left), visitExpression(ctx.right));
			else if (ctx.MOD() != null)
				return new IMPMod(cfg, file, line, col, visitExpression(ctx.left), visitExpression(ctx.right));
			else if (ctx.ADD() != null)
				return new IMPAdd(cfg, file, line, col, visitExpression(ctx.left), visitExpression(ctx.right));
			else if (ctx.SUB() != null)
				return new IMPSub(cfg, file, line, col, visitExpression(ctx.left), visitExpression(ctx.right));
			else if (ctx.GT() != null)
				return new IMPGreaterThan(cfg, file, line, col, visitExpression(ctx.left),
						visitExpression(ctx.right));
			else if (ctx.GE() != null)
				return new IMPGreaterOrEqual(cfg, file, line, col, visitExpression(ctx.left),
						visitExpression(ctx.right));
			else if (ctx.LT() != null)
				return new IMPLessThan(cfg, file, line, col, visitExpression(ctx.left),
						visitExpression(ctx.right));
			else if (ctx.LE() != null)
				return new IMPLessOrEqual(cfg, file, line, col, visitExpression(ctx.left),
						visitExpression(ctx.right));
			else if (ctx.EQUAL() != null)
				return new IMPEqual(cfg, file, line, col, visitExpression(ctx.left), visitExpression(ctx.right));
			else if (ctx.NOTEQUAL() != null)
				return new IMPNotEqual(cfg, file, line, col, visitExpression(ctx.left),
						visitExpression(ctx.right));
			else if (ctx.AND() != null)
				return new IMPAnd(cfg, file, line, col, visitExpression(ctx.left), visitExpression(ctx.right));
			else
				return new IMPOr(cfg, file, line, col, visitExpression(ctx.left), visitExpression(ctx.right));
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
		return new IMPNewArray(cfg, file, getLine(ctx), getCol(ctx), visitPrimitiveType(ctx.primitiveType()),
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
		// null since we do not want to create a new one, class types should
		// have been created during the preprocessing
		Type base = ClassType.lookup(ctx.IDENTIFIER().getText(), null);
		if (ctx.arrayCreatorRest() != null)
			return new IMPNewArray(cfg, file, getLine(ctx), getCol(ctx), base,
					visitArrayCreatorRest(ctx.arrayCreatorRest()));
		else
			return new IMPNewObj(cfg, file, getLine(ctx), getCol(ctx), base, visitArguments(ctx.arguments()));
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
			return visitVar(ctx.IDENTIFIER(), true);
		else
			return visitVar(ctx.THIS(), false);
	}

	@Override
	public Expression visitMethodCall(MethodCallContext ctx) {
		Expression receiver = visitReceiver(ctx.receiver());
		String name = ctx.name.getText();
		Expression[] args = ArrayUtils.insert(0, visitArguments(ctx.arguments()), receiver);
		return new UnresolvedCall(cfg, file, getLine(ctx), getCol(ctx), IMPFrontend.CALL_STRATEGY, true, name, args);
	}

	@Override
	public Expression visitBasicExpr(BasicExprContext ctx) {
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
	public Literal visitLiteral(LiteralContext ctx) {
		int line = getLine(ctx);
		int col = getCol(ctx);
		if (ctx.LITERAL_NULL() != null)
			return new NullLiteral(cfg, file, line, col);
		else if (ctx.LITERAL_BOOL() != null)
			if (ctx.LITERAL_BOOL().getText().equals("true"))
				return new IMPTrueLiteral(cfg, file, line, col);
			else
				return new IMPFalseLiteral(cfg, file, line, col);
		else if (ctx.LITERAL_STRING() != null)
			return new IMPStringLiteral(cfg, file, line, col, ctx.LITERAL_STRING().getText());
		else if (ctx.LITERAL_FLOAT() != null)
			if (ctx.SUB() != null)
				return new IMPFloatLiteral(cfg, file, line, col,
						-Float.parseFloat(ctx.LITERAL_FLOAT().getText()));
			else
				return new IMPFloatLiteral(cfg, file, line, col,
						Float.parseFloat(ctx.LITERAL_FLOAT().getText()));
		else if (ctx.LITERAL_DECIMAL() != null)
			if (ctx.SUB() != null)
				return new IMPIntLiteral(cfg, file, line, col,
						-Integer.parseInt(ctx.LITERAL_DECIMAL().getText()));
			else
				return new IMPIntLiteral(cfg, file, line, col,
						Integer.parseInt(ctx.LITERAL_DECIMAL().getText()));

		throw new UnsupportedOperationException("Type of literal not supported: " + ctx);
	}
}

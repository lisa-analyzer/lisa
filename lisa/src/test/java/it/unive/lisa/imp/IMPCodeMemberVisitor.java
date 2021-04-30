package it.unive.lisa.imp;

import static it.unive.lisa.imp.Antlr4Util.getCol;
import static it.unive.lisa.imp.Antlr4Util.getLine;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Map;
import java.util.Map.Entry;

import org.antlr.v4.runtime.tree.TerminalNode;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.lang3.tuple.Triple;

import it.unive.lisa.imp.constructs.StringConcat;
import it.unive.lisa.imp.constructs.StringContains;
import it.unive.lisa.imp.constructs.StringEndsWith;
import it.unive.lisa.imp.constructs.StringEquals;
import it.unive.lisa.imp.constructs.StringIndexOf;
import it.unive.lisa.imp.constructs.StringLength;
import it.unive.lisa.imp.constructs.StringReplace;
import it.unive.lisa.imp.constructs.StringStartsWith;
import it.unive.lisa.imp.constructs.StringSubstring;
import it.unive.lisa.imp.expressions.IMPAdd;
import it.unive.lisa.imp.expressions.IMPAnd;
import it.unive.lisa.imp.expressions.IMPArrayAccess;
import it.unive.lisa.imp.expressions.IMPAssert;
import it.unive.lisa.imp.expressions.IMPDiv;
import it.unive.lisa.imp.expressions.IMPEqual;
import it.unive.lisa.imp.expressions.IMPFalseLiteral;
import it.unive.lisa.imp.expressions.IMPFloatLiteral;
import it.unive.lisa.imp.expressions.IMPGreaterOrEqual;
import it.unive.lisa.imp.expressions.IMPGreaterThan;
import it.unive.lisa.imp.expressions.IMPIntLiteral;
import it.unive.lisa.imp.expressions.IMPLessOrEqual;
import it.unive.lisa.imp.expressions.IMPLessThan;
import it.unive.lisa.imp.expressions.IMPMod;
import it.unive.lisa.imp.expressions.IMPMul;
import it.unive.lisa.imp.expressions.IMPNeg;
import it.unive.lisa.imp.expressions.IMPNewArray;
import it.unive.lisa.imp.expressions.IMPNewObj;
import it.unive.lisa.imp.expressions.IMPNot;
import it.unive.lisa.imp.expressions.IMPNotEqual;
import it.unive.lisa.imp.expressions.IMPOr;
import it.unive.lisa.imp.expressions.IMPStringLiteral;
import it.unive.lisa.imp.expressions.IMPSub;
import it.unive.lisa.imp.expressions.IMPTrueLiteral;
import it.unive.lisa.imp.types.BoolType;
import it.unive.lisa.imp.types.ClassType;
import it.unive.lisa.imp.types.FloatType;
import it.unive.lisa.imp.types.IntType;
import it.unive.lisa.program.Global;
import it.unive.lisa.program.SourceCodeLocation;
import it.unive.lisa.program.annotations.Annotations;
import it.unive.lisa.program.cfg.CFG;
import it.unive.lisa.program.cfg.CFGDescriptor;
import it.unive.lisa.program.cfg.VariableTableEntry;
import it.unive.lisa.program.cfg.controlFlow.ControlFlowStructure;
import it.unive.lisa.program.cfg.controlFlow.IfThenElse;
import it.unive.lisa.program.cfg.controlFlow.Loop;
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
import it.unive.lisa.program.cfg.statement.PluggableStatement;
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
import it.unive.lisa.test.antlr.IMPParser.BinaryStringExprContext;
import it.unive.lisa.test.antlr.IMPParser.BlockContext;
import it.unive.lisa.test.antlr.IMPParser.BlockOrStatementContext;
import it.unive.lisa.test.antlr.IMPParser.ExpressionContext;
import it.unive.lisa.test.antlr.IMPParser.FieldAccessContext;
import it.unive.lisa.test.antlr.IMPParser.ForLoopContext;
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
import it.unive.lisa.test.antlr.IMPParser.StringExprContext;
import it.unive.lisa.test.antlr.IMPParser.TernaryStringExprContext;
import it.unive.lisa.test.antlr.IMPParser.UnaryStringExprContext;
import it.unive.lisa.test.antlr.IMPParser.WhileLoopContext;
import it.unive.lisa.test.antlr.IMPParserBaseVisitor;
import it.unive.lisa.type.Type;
import it.unive.lisa.type.Untyped;
import it.unive.lisa.util.datastructures.graph.AdjacencyMatrix;

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

	private final Collection<ControlFlowStructure> cfs;

	private final Map<String, Pair<VariableRef, Annotations>> visibleIds;

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
		cfs = new LinkedList<>();
		// side effects on entrypoints and matrix will affect the cfg
		cfg = new CFG(descriptor, entrypoints, matrix);

		visibleIds = new HashMap<>();
		for (VariableTableEntry par : descriptor.getVariables())
			visibleIds.put(par.getName(), Pair.of(par.createReference(cfg), par.getAnnotations()));
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
		Triple<Statement, AdjacencyMatrix<Statement, Edge, CFG>, Statement> visited = visitBlock(ctx);
		entrypoints.add(visited.getLeft());
		matrix.mergeWith(visited.getMiddle());
		cfs.forEach(cf -> cfg.addControlFlowStructure(cf));

		if (cfg.getAllExitpoints().isEmpty()) {
			Ret ret = new Ret(cfg, descriptor.getLocation());
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
	public Triple<Statement, AdjacencyMatrix<Statement, Edge, CFG>, Statement> visitBlock(BlockContext ctx) {
		Map<String, Pair<VariableRef,
				Annotations>> backup = new HashMap<>(visibleIds);
		AdjacencyMatrix<Statement, Edge, CFG> block = new AdjacencyMatrix<>(matrix.getEdgeFactory());

		Statement first = null, last = null;
		for (int i = 0; i < ctx.blockOrStatement().size(); i++) {
			Triple<Statement, AdjacencyMatrix<Statement, Edge, CFG>,
					Statement> st = visitBlockOrStatement(ctx.blockOrStatement(i));
			block.mergeWith(st.getMiddle());
			if (first == null)
				first = st.getLeft();
			if (last != null)
				block.addEdge(new SequentialEdge(last, st.getLeft()));
			last = st.getRight();
		}

		Collection<String> toRemove = new HashSet<>();
		for (Entry<String, Pair<VariableRef, Annotations>> id : visibleIds.entrySet())
			if (!backup.containsKey(id.getKey())) {
				VariableRef ref = id.getValue().getLeft();
				descriptor.addVariable(new VariableTableEntry(ref.getLocation(),
						0, ref.getRootStatement(), last, id.getKey(), Untyped.INSTANCE, id.getValue().getRight()));
				toRemove.add(id.getKey());
			}

		if (!toRemove.isEmpty())
			toRemove.forEach(visibleIds::remove);

		if (first == null && last == null) {
			// empty block: instrument it with a noop
			NoOp instrumented = new NoOp(cfg, new SourceCodeLocation(file, getLine(ctx), getCol(ctx)));
			first = last = instrumented;
			block.addNode(instrumented);
		}

		return Triple.of(first, block, last);
	}

	@Override
	public Triple<Statement, AdjacencyMatrix<Statement, Edge, CFG>, Statement> visitBlockOrStatement(
			BlockOrStatementContext ctx) {
		if (ctx.statement() != null)
			return visitStatement(ctx.statement());
		else
			return visitBlock(ctx.block());
	}

	@Override
	public Triple<Statement, AdjacencyMatrix<Statement, Edge, CFG>, Statement> visitStatement(StatementContext ctx) {
		Statement st;
		if (ctx.localDeclaration() != null)
			st = visitLocalDeclaration(ctx.localDeclaration());
		else if (ctx.ASSERT() != null)
			st = new IMPAssert(cfg, file, getLine(ctx), getCol(ctx), visitExpression(ctx.expression()));
		else if (ctx.RETURN() != null)
			if (ctx.expression() != null)
				st = new Return(cfg, new SourceCodeLocation(file, getLine(ctx), getCol(ctx)),
						visitExpression(ctx.expression()));
			else
				st = new Ret(cfg, new SourceCodeLocation(file, getLine(ctx), getCol(ctx)));
		else if (ctx.THROW() != null)
			st = new Throw(cfg, new SourceCodeLocation(file, getLine(ctx), getCol(ctx)),
					visitExpression(ctx.expression()));
		else if (ctx.skip != null)
			st = new NoOp(cfg, new SourceCodeLocation(file, getLine(ctx), getCol(ctx)));
		else if (ctx.IF() != null)
			return visitIf(ctx);
		else if (ctx.loop() != null)
			return visitLoop(ctx.loop());
		else if (ctx.command != null)
			st = visitExpression(ctx.command);
		else
			throw new IllegalArgumentException("Statement '" + ctx.toString() + "' cannot be parsed");

		AdjacencyMatrix<Statement, Edge, CFG> adj = new AdjacencyMatrix<>(matrix.getEdgeFactory());
		adj.addNode(st);
		return Triple.of(st, adj, st);
	}

	private Triple<Statement, AdjacencyMatrix<Statement, Edge, CFG>, Statement> visitIf(StatementContext ctx) {
		AdjacencyMatrix<Statement, Edge, CFG> ite = new AdjacencyMatrix<>(matrix.getEdgeFactory());

		Statement condition = visitParExpr(ctx.parExpr());
		ite.addNode(condition);

		Triple<Statement, AdjacencyMatrix<Statement, Edge, CFG>, Statement> then = visitBlockOrStatement(ctx.then);
		ite.mergeWith(then.getMiddle());
		ite.addEdge(new TrueEdge(condition, then.getLeft()));

		Triple<Statement, AdjacencyMatrix<Statement, Edge, CFG>, Statement> otherwise = null;
		if (ctx.otherwise != null) {
			otherwise = visitBlockOrStatement(ctx.otherwise);
			ite.mergeWith(otherwise.getMiddle());
			ite.addEdge(new FalseEdge(condition, otherwise.getLeft()));
		}

		Statement noop = new NoOp(cfg, condition.getLocation());
		ite.addNode(noop);
		ite.addEdge(new SequentialEdge(then.getRight(), noop));
		if (otherwise != null)
			ite.addEdge(new SequentialEdge(otherwise.getRight(), noop));
		else
			ite.addEdge(new FalseEdge(condition, noop));

		cfs.add(new IfThenElse(matrix, condition, noop, then.getMiddle().getNodes(),
				otherwise == null ? Collections.emptyList() : otherwise.getMiddle().getNodes()));

		return Triple.of(condition, ite, noop);
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
					"Duplicate variable '" + ref.getName() + "' declared at " + ref.getLocation());

		visibleIds.put(ref.getName(), Pair.of(ref, new IMPAnnotationVisitor().visitAnnotations(ctx.annotations())));
		// the variable table entry will be generated at the end of the
		// containing block

		return new Assignment(cfg, new SourceCodeLocation(file, getLine(ctx), getCol(ctx)), ref, expression);
	}

	@Override
	public Triple<Statement, AdjacencyMatrix<Statement, Edge, CFG>, Statement> visitLoop(LoopContext ctx) {
		if (ctx.whileLoop() != null)
			return visitWhileLoop(ctx.whileLoop());
		else
			return visitForLoop(ctx.forLoop());
	}

	@Override
	public Triple<Statement, AdjacencyMatrix<Statement, Edge, CFG>, Statement> visitWhileLoop(WhileLoopContext ctx) {
		AdjacencyMatrix<Statement, Edge, CFG> loop = new AdjacencyMatrix<>(matrix.getEdgeFactory());
		Statement condition = visitParExpr(ctx.parExpr());
		loop.addNode(condition);

		Triple<Statement, AdjacencyMatrix<Statement, Edge, CFG>,
				Statement> body = visitBlockOrStatement(ctx.blockOrStatement());
		loop.mergeWith(body.getMiddle());
		loop.addEdge(new TrueEdge(condition, body.getLeft()));
		loop.addEdge(new SequentialEdge(body.getRight(), condition));

		Statement noop = new NoOp(cfg, condition.getLocation());
		loop.addNode(noop);
		loop.addEdge(new FalseEdge(condition, noop));

		cfs.add(new Loop(matrix, condition, noop, body.getMiddle().getNodes()));

		return Triple.of(condition, loop, noop);
	}

	@Override
	public Triple<Statement, AdjacencyMatrix<Statement, Edge, CFG>, Statement> visitForLoop(ForLoopContext ctx) {
		AdjacencyMatrix<Statement, Edge, CFG> loop = new AdjacencyMatrix<>(matrix.getEdgeFactory());
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
			condition = new IMPTrueLiteral(cfg, file, getLine(ctx), getCol(ctx));
		loop.addNode(condition);
		if (first == null)
			first = condition;
		else
			loop.addEdge(new SequentialEdge(first, condition));

		Triple<Statement, AdjacencyMatrix<Statement, Edge, CFG>,
				Statement> body = visitBlockOrStatement(ctx.blockOrStatement());
		loop.mergeWith(body.getMiddle());
		loop.addEdge(new TrueEdge(condition, body.getLeft()));
		last = body.getRight();

		if (post != null) {
			Statement inc = visitExpression(post);
			loop.addNode(inc);
			loop.addEdge(new SequentialEdge(body.getRight(), inc));
			last = inc;
		}

		loop.addEdge(new SequentialEdge(last, condition));

		Statement noop = new NoOp(cfg, condition.getLocation());
		loop.addNode(noop);
		loop.addEdge(new FalseEdge(condition, noop));

		if (post == null)
			cfs.add(new Loop(matrix, condition, noop, body.getMiddle().getNodes()));
		else {
			AdjacencyMatrix<Statement, Edge, CFG> tmp = new AdjacencyMatrix<>(body.getMiddle());
			tmp.addNode(last);
			loop.addEdge(new SequentialEdge(body.getRight(), last));
			cfs.add(new Loop(matrix, condition, noop, tmp.getNodes()));
		}

		return Triple.of(first, loop, noop);
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
		else
			throw new IMPSyntaxException("Target of the assignment at " + expression.getLocation()
					+ " is neither an identifier, a field access or an array access");

		return new Assignment(cfg, new SourceCodeLocation(file, getLine(ctx), getCol(ctx)), target, expression);
	}

	private VariableRef visitVar(TerminalNode identifier, boolean localReference) {
		VariableRef ref = new VariableRef(cfg,
				new SourceCodeLocation(file, getLine(identifier.getSymbol()), getCol(identifier.getSymbol())),
				identifier.getText(), Untyped.INSTANCE);
		if (localReference && !visibleIds.containsKey(ref.getName()))
			throw new IMPSyntaxException(
					"Referencing undeclared variable '" + ref.getName() + "' at " + ref.getLocation());
		return ref;
	}

	@Override
	public AccessUnitGlobal visitFieldAccess(FieldAccessContext ctx) {
		Expression receiver = visitReceiver(ctx.receiver());
		Global id = new Global(new SourceCodeLocation(file, getLine(ctx.name), getCol(ctx.name)), ctx.name.getText(),
				Untyped.INSTANCE);
		return new AccessUnitGlobal(cfg, new SourceCodeLocation(file, getLine(ctx), getCol(ctx)), receiver, id);
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
		else if (ctx.stringExpr() != null)
			return visitStringExpr(ctx.stringExpr());

		throw new UnsupportedOperationException("Type of expression not supported: " + ctx);
	}

	@Override
	public Expression visitStringExpr(StringExprContext ctx) {
		Expression returned = null;
		if (ctx.unaryStringExpr() != null)
			returned = visitUnaryStringExpr(ctx.unaryStringExpr());
		else if (ctx.binaryStringExpr() != null)
			returned = visitBinaryStringExpr(ctx.binaryStringExpr());
		else if (ctx.ternaryStringExpr() != null)
			returned = visitTernaryStringExpr(ctx.ternaryStringExpr());
		else
			throw new UnsupportedOperationException("Type of string expression not supported: " + ctx);

		if (returned instanceof PluggableStatement)
			// These string operations are also native constructs
			((PluggableStatement) returned).setOriginatingStatement(returned);

		return returned;
	}

	@Override
	public Expression visitUnaryStringExpr(UnaryStringExprContext ctx) {
		if (ctx.STRLEN() != null)
			return new StringLength.IMPStringLength(cfg, file, getLine(ctx), getCol(ctx), visitExpression(ctx.op));
		throw new UnsupportedOperationException("Type of string expression not supported: " + ctx);
	}

	@Override
	public Expression visitBinaryStringExpr(BinaryStringExprContext ctx) {
		if (ctx.STRCAT() != null)
			return new StringConcat.IMPStringConcat(cfg, file, getLine(ctx), getCol(ctx), visitExpression(ctx.left),
					visitExpression(ctx.right));
		else if (ctx.STRCONTAINS() != null)
			return new StringContains.IMPStringContains(cfg, file, getLine(ctx), getCol(ctx), visitExpression(ctx.left),
					visitExpression(ctx.right));
		else if (ctx.STRENDS() != null)
			return new StringEndsWith.IMPStringEndsWith(cfg, file, getLine(ctx), getCol(ctx), visitExpression(ctx.left),
					visitExpression(ctx.right));
		else if (ctx.STREQ() != null)
			return new StringEquals.IMPStringEquals(cfg, file, getLine(ctx), getCol(ctx), visitExpression(ctx.left),
					visitExpression(ctx.right));
		else if (ctx.STRINDEXOF() != null)
			return new StringIndexOf.IMPStringIndexOf(cfg, file, getLine(ctx), getCol(ctx), visitExpression(ctx.left),
					visitExpression(ctx.right));
		else if (ctx.STRSTARTS() != null)
			return new StringStartsWith.IMPStringStartsWith(cfg, file, getLine(ctx), getCol(ctx),
					visitExpression(ctx.left), visitExpression(ctx.right));

		throw new UnsupportedOperationException("Type of string expression not supported: " + ctx);
	}

	@Override
	public Expression visitTernaryStringExpr(TernaryStringExprContext ctx) {
		if (ctx.STRREPLACE() != null)
			return new StringReplace.IMPStringReplace(cfg, file, getLine(ctx), getCol(ctx), visitExpression(ctx.left),
					visitExpression(ctx.middle), visitExpression(ctx.right));
		else if (ctx.STRSUB() != null)
			return new StringSubstring.IMPStringSubstring(cfg, file, getLine(ctx), getCol(ctx),
					visitExpression(ctx.left),
					visitExpression(ctx.middle), visitExpression(ctx.right));

		throw new UnsupportedOperationException("Type of string expression not supported: " + ctx);
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
		return new UnresolvedCall(cfg, new SourceCodeLocation(file, getLine(ctx), getCol(ctx)),
				IMPFrontend.CALL_STRATEGY, true, name, args);
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
			return new NullLiteral(cfg, new SourceCodeLocation(file, line, col));
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

package it.unive.lisa.imp;

import static it.unive.lisa.imp.Antlr4Util.getCol;
import static it.unive.lisa.imp.Antlr4Util.getLine;

import it.unive.lisa.imp.constructs.StringContains;
import it.unive.lisa.imp.constructs.StringEndsWith;
import it.unive.lisa.imp.constructs.StringEquals;
import it.unive.lisa.imp.constructs.StringIndexOf;
import it.unive.lisa.imp.constructs.StringLength;
import it.unive.lisa.imp.constructs.StringReplace;
import it.unive.lisa.imp.constructs.StringStartsWith;
import it.unive.lisa.imp.constructs.StringSubstring;
import it.unive.lisa.imp.types.ArrayType;
import it.unive.lisa.imp.types.BoolType;
import it.unive.lisa.imp.types.ClassType;
import it.unive.lisa.imp.types.FloatType;
import it.unive.lisa.imp.types.IntType;
import it.unive.lisa.imp.types.StringType;
import it.unive.lisa.program.CompilationUnit;
import it.unive.lisa.program.Global;
import it.unive.lisa.program.Program;
import it.unive.lisa.program.SourceCodeLocation;
import it.unive.lisa.program.cfg.CFG;
import it.unive.lisa.program.cfg.CFGDescriptor;
import it.unive.lisa.program.cfg.Parameter;
import it.unive.lisa.program.cfg.statement.Statement;
import it.unive.lisa.program.cfg.statement.UnresolvedCall.ResolutionStrategy;
import it.unive.lisa.test.antlr.IMPLexer;
import it.unive.lisa.test.antlr.IMPParser;
import it.unive.lisa.test.antlr.IMPParser.ConstructorDeclarationContext;
import it.unive.lisa.test.antlr.IMPParser.FieldDeclarationContext;
import it.unive.lisa.test.antlr.IMPParser.FileContext;
import it.unive.lisa.test.antlr.IMPParser.FormalContext;
import it.unive.lisa.test.antlr.IMPParser.FormalsContext;
import it.unive.lisa.test.antlr.IMPParser.MethodDeclarationContext;
import it.unive.lisa.test.antlr.IMPParser.UnitContext;
import it.unive.lisa.test.antlr.IMPParserBaseVisitor;
import it.unive.lisa.type.Untyped;
import java.io.ByteArrayInputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import org.antlr.v4.runtime.BailErrorStrategy;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.RecognitionException;
import org.antlr.v4.runtime.atn.PredictionMode;
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
	 * The resolution strategy for IMP calling expressions.
	 */
	public static final ResolutionStrategy CALL_STRATEGY = ResolutionStrategy.FIRST_DYNAMIC_THEN_STATIC;

	/**
	 * Parses a file using the {@link IMPLexer} and the {@link IMPParser}
	 * produced by compiling the ANTLR4 grammar, and yields the {@link Program}
	 * that corresponds to the one parsed from that file. Invoking this method
	 * is equivalent to invoking {@link #processFile(String, boolean)} passing
	 * {@code false} as second parameter.
	 * 
	 * @param file the complete path (relative or absolute) of the file to parse
	 * 
	 * @return the resulting {@link Program}
	 * 
	 * @throws ParsingException if this frontend is unable to parse the file
	 */
	public static Program processFile(String file) throws ParsingException {
		return new IMPFrontend(file, false).work(null);
	}

	/**
	 * Parses a file using the {@link IMPLexer} and the {@link IMPParser}
	 * produced by compiling the ANTLR4 grammar, and yields the {@link Program}
	 * that corresponds to the one parsed from that file.
	 * 
	 * @param file     the complete path (relative or absolute) of the file to
	 *                     parse
	 * @param onlyMain true iff the only entry point is the main method
	 * 
	 * @return the resulting {@link Program}
	 * 
	 * @throws ParsingException if this frontend is unable to parse the file
	 */
	public static Program processFile(String file, boolean onlyMain) throws ParsingException {
		return new IMPFrontend(file, onlyMain).work(null);
	}

	/**
	 * Parses a piece of IMP code using the {@link IMPLexer} and the
	 * {@link IMPParser} produced by compiling the ANTLR4 grammar, and yields
	 * the {@link Program} that corresponds to the one parsed from the given
	 * text. Invoking this method is equivalent to invoking
	 * {@link #processText(String, boolean)} passing {@code false} as second
	 * parameter.
	 * 
	 * @param text the IMP program to parse
	 * 
	 * @return the resulting {@link Program}
	 * 
	 * @throws ParsingException if this frontend is unable to parse the text
	 */
	public static Program processText(String text) throws ParsingException {
		return processText(text, false);
	}

	/**
	 * Parses a piece of IMP code using the {@link IMPLexer} and the
	 * {@link IMPParser} produced by compiling the ANTLR4 grammar, and yields
	 * the {@link Program} that corresponds to the one parsed from the given
	 * text.
	 * 
	 * @param text     the IMP program to parse
	 * @param onlyMain true iff the only entry point is the main method
	 * 
	 * @return the resulting {@link Program}
	 * 
	 * @throws ParsingException if this frontend is unable to parse the text
	 */
	public static Program processText(String text, boolean onlyMain) throws ParsingException {
		try (InputStream is = new ByteArrayInputStream(text.getBytes())) {
			return new IMPFrontend("in-memory.imp", onlyMain).work(is);
		} catch (IOException e) {
			throw new ParsingException("Exception while parsing the input text", e);
		}
	}

	private final String file;

	private final Map<String, Pair<CompilationUnit, String>> inheritanceMap;

	private final Program program;

	private CompilationUnit currentUnit;

	private final boolean onlyMain;

	private IMPFrontend(String file, boolean onlyMain) {
		this.file = file;
		inheritanceMap = new HashMap<>();
		program = new Program();
		this.onlyMain = onlyMain;
	}

	private Program work(InputStream inputStream) throws ParsingException {
		// first remove all cached types from previous executions
		ClassType.clearAll();
		ArrayType.clearAll();

		try {
			log.info("Reading file... " + file);
			IMPLexer lexer;
			if (inputStream == null)
				try (InputStream stream = new FileInputStream(file)) {
					lexer = new IMPLexer(CharStreams.fromStream(stream, StandardCharsets.UTF_8));
				}
			else
				lexer = new IMPLexer(CharStreams.fromStream(inputStream, StandardCharsets.UTF_8));

			IMPParser parser = new IMPParser(new CommonTokenStream(lexer));

			// this is needed to get an exception on malformed input
			// otherwise an error is dumped to stderr and the partial
			// parsing result is returned
			parser.setErrorHandler(new BailErrorStrategy());
			parser.getInterpreter().setPredictionMode(PredictionMode.SLL);

			Program p = visitFile(parser.file());

			// add constructs
			SourceCodeLocation unknownLocation = new SourceCodeLocation("imp-runtime", 0, 0);
			CompilationUnit str = new CompilationUnit(unknownLocation, "string", true);
			str.addInstanceConstruct(new StringContains(unknownLocation, str));
			str.addInstanceConstruct(new StringEndsWith(unknownLocation, str));
			str.addInstanceConstruct(new StringEquals(unknownLocation, str));
			str.addInstanceConstruct(new StringIndexOf(unknownLocation, str));
			str.addInstanceConstruct(new StringLength(unknownLocation, str));
			str.addInstanceConstruct(new StringReplace(unknownLocation, str));
			str.addInstanceConstruct(new StringStartsWith(unknownLocation, str));
			str.addInstanceConstruct(new StringSubstring(unknownLocation, str));

			// register all possible types
			p.registerType(BoolType.INSTANCE);
			p.registerType(FloatType.INSTANCE);
			p.registerType(IntType.INSTANCE);
			p.registerType(StringType.INSTANCE);
			ClassType.all().forEach(p::registerType);
			ArrayType.all().forEach(p::registerType);

			return p;
		} catch (FileNotFoundException e) {
			log.fatal(file + " does not exist", e);
			throw new ParsingException("Target file '" + file + "' does not exist", e);
		} catch (IOException e) {
			log.fatal("Unable to open " + file, e);
			throw new ParsingException("Unable to open " + file, e);
		} catch (IMPSyntaxException e) {
			log.fatal(file + " is not well-formed", e);
			throw new ParsingException("Incorrect IMP file: " + file, e);
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

	@Override
	public Program visitFile(FileContext ctx) {
		for (UnitContext unit : ctx.unit()) {
			// we add all the units first, so that type resolution an work
			CompilationUnit u = new CompilationUnit(new SourceCodeLocation(file, getLine(ctx), getCol(ctx)),
					unit.name.getText(), false);
			program.addCompilationUnit(u);
			ClassType.lookup(u.getName(), u);
		}

		for (UnitContext unit : ctx.unit())
			// now we populate each unit
			visitUnit(unit);

		for (Pair<CompilationUnit, String> unit : inheritanceMap.values())
			if (unit.getRight() != null)
				unit.getLeft().addSuperUnit(inheritanceMap.get(unit.getRight()).getLeft());

		return program;
	}

	@Override
	public CompilationUnit visitUnit(UnitContext ctx) {
		currentUnit = program.getUnit(ctx.name.getText());

		if (ctx.superclass != null)
			inheritanceMap.put(currentUnit.getName(), Pair.of(currentUnit, ctx.superclass.getText()));
		else
			inheritanceMap.put(currentUnit.getName(), Pair.of(currentUnit, null));

		for (MethodDeclarationContext decl : ctx.memberDeclarations().methodDeclaration())
			currentUnit.addInstanceCFG(visitMethodDeclaration(decl));

		for (ConstructorDeclarationContext decl : ctx.memberDeclarations().constructorDeclaration())
			currentUnit.addInstanceCFG(visitConstructorDeclaration(decl));

		for (CFG cfg : currentUnit.getInstanceCFGs(false)) {
			if (currentUnit.getInstanceCFGs(false).stream()
					.anyMatch(c -> c != cfg && c.getDescriptor().matchesSignature(cfg.getDescriptor())
							&& cfg.getDescriptor().matchesSignature(c.getDescriptor())))
				throw new IMPSyntaxException("Duplicate cfg: " + cfg);
			if (isEntryPoint(cfg))
				program.addEntryPoint(cfg);
		}

		for (FieldDeclarationContext decl : ctx.memberDeclarations().fieldDeclaration())
			currentUnit.addInstanceGlobal(visitFieldDeclaration(decl));

		for (Global global : currentUnit.getInstanceGlobals(false))
			if (currentUnit.getInstanceGlobals(false).stream()
					.anyMatch(g -> g != global && g.getName().equals(global.getName())))
				throw new IMPSyntaxException("Duplicate global: " + global);

		return currentUnit;
	}

	private boolean isEntryPoint(CFG cfg) {
		if (!onlyMain)
			return true;
		else
			return cfg.getDescriptor().getName().equals("main");
	}

	@Override
	public Global visitFieldDeclaration(FieldDeclarationContext ctx) {
		return new Global(new SourceCodeLocation(file, getLine(ctx), getCol(ctx)), ctx.name.getText(),
				Untyped.INSTANCE, new IMPAnnotationVisitor().visitAnnotations(ctx.annotations()));
	}

	@Override
	public CFG visitConstructorDeclaration(ConstructorDeclarationContext ctx) {
		CFGDescriptor descr = mkDescriptor(ctx);
		if (!currentUnit.getName().equals(descr.getName()))
			throw new IMPSyntaxException("Constructor does not have the same name as its containing class");
		return new IMPCodeMemberVisitor(file, descr).visitCodeMember(ctx.block());
	}

	@Override
	public CFG visitMethodDeclaration(MethodDeclarationContext ctx) {
		CFGDescriptor descr = mkDescriptor(ctx);
		return new IMPCodeMemberVisitor(file, descr).visitCodeMember(ctx.block());
	}

	private CFGDescriptor mkDescriptor(ConstructorDeclarationContext ctx) {
		CFGDescriptor descriptor = new CFGDescriptor(new SourceCodeLocation(file, getLine(ctx), getCol(ctx)),
				currentUnit,
				true, ctx.name.getText(), Untyped.INSTANCE,
				new IMPAnnotationVisitor().visitAnnotations(ctx.annotations()),
				visitFormals(ctx.formals()));
		descriptor.setOverridable(false);
		return descriptor;
	}

	private CFGDescriptor mkDescriptor(MethodDeclarationContext ctx) {
		CFGDescriptor descriptor = new CFGDescriptor(new SourceCodeLocation(file, getLine(ctx), getCol(ctx)),
				currentUnit,
				true, ctx.name.getText(), Untyped.INSTANCE,
				new IMPAnnotationVisitor().visitAnnotations(ctx.annotations()),
				visitFormals(ctx.formals()));

		if (ctx.FINAL() != null)
			descriptor.setOverridable(false);
		else
			descriptor.setOverridable(true);

		return descriptor;
	}

	@Override
	public Parameter[] visitFormals(FormalsContext ctx) {
		Parameter[] formals = new Parameter[ctx.formal().size() + 1];
		formals[0] = new Parameter(new SourceCodeLocation(file, getLine(ctx), getCol(ctx)), "this",
				ClassType.lookup(this.currentUnit.getName(), this.currentUnit));
		int i = 1;
		for (FormalContext f : ctx.formal())
			formals[i++] = visitFormal(f);
		return formals;
	}

	@Override
	public Parameter visitFormal(FormalContext ctx) {
		return new Parameter(new SourceCodeLocation(file, getLine(ctx), getCol(ctx)), ctx.name.getText(),
				Untyped.INSTANCE, new IMPAnnotationVisitor().visitAnnotations(ctx.annotations()));
	}
}
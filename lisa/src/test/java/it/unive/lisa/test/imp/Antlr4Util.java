package it.unive.lisa.test.imp;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import org.antlr.v4.runtime.FailedPredicateException;
import org.antlr.v4.runtime.InputMismatchException;
import org.antlr.v4.runtime.LexerNoViableAltException;
import org.antlr.v4.runtime.NoViableAltException;
import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.RecognitionException;
import org.antlr.v4.runtime.Token;
import org.antlr.v4.runtime.Vocabulary;
import org.antlr.v4.runtime.misc.IntervalSet;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * A class providing utilities regarding ANTLR4 functionalities.
 * 
 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
 */
public class Antlr4Util {

	private static final Logger log = LogManager.getLogger(IMPFrontend.class);

	/**
	 * Builds a {@link ParsingException} starting from a
	 * {@link RecognitionException}. A full message (reporting also the
	 * offending line) will be logged with fatal level.
	 * 
	 * @param file the file being parsed when the exception happened
	 * @param e    the original exception
	 * 
	 * @return a {@link ParsingException} contained more detailed information
	 *             about the one thrown by ANTLR4
	 */
	static ParsingException handleRecognitionException(String file, RecognitionException e) {
		Token problem = e.getOffendingToken();
		StringBuilder message = errorHeader(file, problem);

		if (e instanceof InputMismatchException)
			inputMismatch(e, problem, message);
		else if (e instanceof FailedPredicateException)
			failedPredicate(e, message);
		else if (e instanceof NoViableAltException || e instanceof LexerNoViableAltException)
			missingAlternative(message);
		else
			message.append("an unknown error occurred");

		StringBuilder completeMessage = new StringBuilder(message.toString());
		dumpProblem(file, problem, completeMessage);

		log.error("Error while parsing " + file + ":\n" + completeMessage.toString());
		return new ParsingException("Error while parsing " + file + ":\n" + message.toString());
	}

	private static void dumpProblem(String file, Token problem, StringBuilder message) {
		String base = message.toString();

		message.append(" at: ");
		int begin = message.length();

		try {
			List<String> inputLines = Files.readAllLines(Paths.get(file));
			String errorLine = inputLines.get(problem.getLine() - 1);
			String trimmed = errorLine.trim();
			int offset = Math.max(0, errorLine.indexOf(trimmed));
			message.append(trimmed).append("\n");
			for (int i = 0; i < begin + problem.getCharPositionInLine() - offset; i++)
				message.append(" ");
			message.append("^");
		} catch (IOException e) {
			// something went wrong, roll back
			message.delete(0, message.length());
			message.append(base);
		}
	}

	private static void missingAlternative(StringBuilder message) {
		message.append("could not decide what path to take");
	}

	private static void failedPredicate(RecognitionException e, StringBuilder message) {
		message.append("failed predicate '").append(((FailedPredicateException) e).getPredicate()).append("' ");
	}

	private static void inputMismatch(RecognitionException e, Token problem, StringBuilder message) {
		message.append("matched '").append(problem.getText()).append("' as <")
				.append(tokenName(problem.getType(), e.getRecognizer().getVocabulary())).append(">, expecting <")
				.append(tokenNames(((InputMismatchException) e).getExpectedTokens(), e.getRecognizer().getVocabulary()))
				.append(">");
	}

	private static StringBuilder errorHeader(String file, Token problem) {
		return new StringBuilder().append(file).append(":").append(problem.getLine()).append(":")
				.append(problem.getCharPositionInLine()).append(" - ");
	}

	private static String tokenName(int type, Vocabulary vocabulary) {
		return type < 0 ? "<EOF>" : vocabulary.getSymbolicName(type);
	}

	private static String tokenNames(IntervalSet types, Vocabulary vocabulary) {
		List<Integer> typeList = types.toList();
		StringBuilder expectedBuilder = new StringBuilder();

		for (Integer t : typeList)
			expectedBuilder.append(tokenName(t, vocabulary)).append(" ");

		return expectedBuilder.toString().trim();
	}

	/**
	 * Extracts the line number from an antlr context.
	 * 
	 * @param ctx the context
	 * 
	 * @return the line number where the context appears
	 */
	static int getLine(ParserRuleContext ctx) {
		return ctx.getStart().getLine();
	}

	/**
	 * Extracts the column number from an antlr context.
	 * 
	 * @param ctx the context
	 * 
	 * @return the column number where the context appears
	 */
	static int getCol(ParserRuleContext ctx) {
		return ctx.getStop().getCharPositionInLine();
	}

	/**
	 * Extracts the line number from an antlr token.
	 * 
	 * @param tok the token
	 * 
	 * @return the line number where the token appears
	 */
	static int getLine(Token tok) {
		return tok.getLine();
	}

	/**
	 * Extracts the column number from an antlr token.
	 * 
	 * @param tok the token
	 * 
	 * @return the column number where the token appears
	 */
	static int getCol(Token tok) {
		return tok.getCharPositionInLine();
	}
}

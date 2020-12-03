package it.unive.lisa.outputs.compare;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.Collection;

import org.apache.commons.lang3.tuple.Pair;

import it.unive.lisa.outputs.DotGraph;
import it.unive.lisa.outputs.JsonReport;
import it.unive.lisa.outputs.JsonReport.JsonWarning;
import it.unive.lisa.util.collections.CollectionsDiffBuilder;

public class JsonReportComparer {

	public enum REPORT_TYPE {
		COMMON, ONLY_FIRST, ONLY_SECOND;
	}
	
	public enum REPORTED_COMPONENT {
		WARNINGS, FILES;
	}
	
	public interface DiffReporter {
		void report(REPORTED_COMPONENT component, REPORT_TYPE type, Collection<?> reported);
		void fileDiff(String first, String second, String message);
	}
	
	public static boolean compare(JsonReport first, JsonReport second, File firstFileRoot, File secondFileRoot) throws IOException {
		return compare(first, second, firstFileRoot, secondFileRoot, new BaseDiffReporter());
	}
	
	public static boolean compare(JsonReport first, JsonReport second, File firstFileRoot, File secondFileRoot, DiffReporter reporter) throws IOException {
		CollectionsDiffBuilder<JsonWarning> warnings = new CollectionsDiffBuilder<>(JsonWarning.class, first.getFindings(), second.getFindings());
		warnings.computeDiff(JsonWarning::compareTo);
		
		if (!warnings.getCommons().isEmpty())
			reporter.report(REPORTED_COMPONENT.WARNINGS, REPORT_TYPE.COMMON, warnings.getCommons());
		if (!warnings.getOnlyFirst().isEmpty())
			reporter.report(REPORTED_COMPONENT.WARNINGS, REPORT_TYPE.ONLY_FIRST, warnings.getOnlyFirst());
		if (!warnings.getOnlySecond().isEmpty())
			reporter.report(REPORTED_COMPONENT.WARNINGS, REPORT_TYPE.ONLY_SECOND, warnings.getOnlySecond());
		
		CollectionsDiffBuilder<String> files = new CollectionsDiffBuilder<>(String.class, first.getFiles(), second.getFiles());
		files.computeDiff(String::compareTo);
		
		if (!files.getCommons().isEmpty())
			reporter.report(REPORTED_COMPONENT.FILES, REPORT_TYPE.COMMON, files.getCommons());
		if (!files.getOnlyFirst().isEmpty())
			reporter.report(REPORTED_COMPONENT.FILES, REPORT_TYPE.ONLY_FIRST, files.getOnlyFirst());
		if (!files.getOnlySecond().isEmpty())
			reporter.report(REPORTED_COMPONENT.FILES, REPORT_TYPE.ONLY_SECOND, files.getOnlySecond());
		
		if (!warnings.sameContent() || !files.sameContent())
			return false;
		
		for (Pair<String, String> pair : files.getCommons()) {
			File left = new File(firstFileRoot, pair.getLeft());
			File right = new File(secondFileRoot, pair.getRight());
			
			if (!left.exists())
				throw new FileNotFoundException(pair.getLeft() + " declared as output in the first report does not exist");
			if (!right.exists())
				throw new FileNotFoundException(pair.getRight() + " declared as output in the second report does not exist");
			
			if (left.getName().endsWith(".dot"))
				if (!matchDotGraphs(left, right)) {
					reporter.fileDiff(left.toString(), right.toString(), "Graphs are different");
					return false;
				}
		}
		
		return true;
	}
	
	private static boolean matchDotGraphs(File left, File right) throws IOException {
		DotGraph lDot = DotGraph.readDot(new FileReader(left));
		DotGraph rDot = DotGraph.readDot(new FileReader(right));
		return lDot.equals(rDot);
	}
	
	private static class BaseDiffReporter implements DiffReporter {

		@Override
		public void report(REPORTED_COMPONENT component, REPORT_TYPE type, Collection<?> reported) {
			if (type == REPORT_TYPE.COMMON)
				return;
			
			boolean isFirst = type == REPORT_TYPE.ONLY_FIRST;
			
			switch (component) {
			case FILES:
				if (isFirst)
					System.err.println("Files only in the first report:");
				else
					System.err.println("Files only in the second report:");
				break;
			case WARNINGS:
				if (isFirst)
					System.err.println("Warnings only in the first report:");
				else
					System.err.println("Warnings only in the second report:");
				break;
			default:
				break;
			}
			
			for (Object o : reported)
				System.err.println("\t" + o);
		}

		@Override
		public void fileDiff(String first, String second, String message) {
			System.err.println("['" + first + "', '" + second + "'] " + message);
		}
	}
}

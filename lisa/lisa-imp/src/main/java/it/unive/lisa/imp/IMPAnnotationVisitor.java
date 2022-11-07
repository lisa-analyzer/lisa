package it.unive.lisa.imp;

import java.util.ArrayList;
import java.util.List;

import it.unive.lisa.imp.antlr.IMPParser.AnnotationContext;
import it.unive.lisa.imp.antlr.IMPParser.AnnotationMemberContext;
import it.unive.lisa.imp.antlr.IMPParser.AnnotationMembersContext;
import it.unive.lisa.imp.antlr.IMPParser.AnnotationValueContext;
import it.unive.lisa.imp.antlr.IMPParser.AnnotationsContext;
import it.unive.lisa.imp.antlr.IMPParser.ArrayAnnotationValueContext;
import it.unive.lisa.imp.antlr.IMPParser.BasicAnnotationValueContext;
import it.unive.lisa.imp.antlr.IMPParserBaseVisitor;
import it.unive.lisa.program.annotations.Annotation;
import it.unive.lisa.program.annotations.AnnotationMember;
import it.unive.lisa.program.annotations.Annotations;
import it.unive.lisa.program.annotations.values.AnnotationValue;
import it.unive.lisa.program.annotations.values.ArrayAnnotationValue;
import it.unive.lisa.program.annotations.values.BasicAnnotationValue;
import it.unive.lisa.program.annotations.values.BoolAnnotationValue;
import it.unive.lisa.program.annotations.values.CompilationUnitAnnotationValue;
import it.unive.lisa.program.annotations.values.FloatAnnotationValue;
import it.unive.lisa.program.annotations.values.IntAnnotationValue;
import it.unive.lisa.program.annotations.values.StringAnnotationValue;

/**
 * An {@link IMPParserBaseVisitor} that will parse annotations from IMP code.
 * 
 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
 */
public class IMPAnnotationVisitor extends IMPParserBaseVisitor<Object> {

	@Override
	public Annotations visitAnnotations(AnnotationsContext ctx) {
		if (ctx == null)
			return new Annotations();
		List<Annotation> anns = new ArrayList<>();
		for (int i = 0; i < ctx.annotation().size(); i++)
			anns.add(visitAnnotation(ctx.annotation(i)));
		return new Annotations(anns);
	}

	@Override
	public Annotation visitAnnotation(AnnotationContext ctx) {
		String annotationName = ctx.name.getText();
		if (annotationName.startsWith("Inherited"))
			return new Annotation(annotationName, visitAnnotationMembers(ctx.annotationMembers()), true);
		else
			return new Annotation(annotationName, visitAnnotationMembers(ctx.annotationMembers()), false);
	}

	@Override
	public List<AnnotationMember> visitAnnotationMembers(AnnotationMembersContext ctx) {
		List<AnnotationMember> arr = new ArrayList<>();
		if (ctx == null)
			return arr;
		else
			for (int i = 0; i < ctx.annotationMember().size(); i++)
				arr.add(visitAnnotationMember(ctx.annotationMember(i)));
		return arr;
	}

	@Override
	public AnnotationMember visitAnnotationMember(AnnotationMemberContext ctx) {
		return new AnnotationMember(ctx.IDENTIFIER().getText(), visitAnnotationValue(ctx.annotationValue()));
	}

	@Override
	public AnnotationValue visitAnnotationValue(AnnotationValueContext ctx) {
		if (ctx.basicAnnotationValue() != null)
			return visitBasicAnnotationValue(ctx.basicAnnotationValue());
		else
			return visitArrayAnnotationValue(ctx.arrayAnnotationValue());
	}

	@Override
	public BasicAnnotationValue visitBasicAnnotationValue(BasicAnnotationValueContext ctx) {
		if (ctx.LITERAL_DECIMAL() != null)
			if (ctx.SUB() != null)
				return new IntAnnotationValue(Integer.parseInt(ctx.LITERAL_DECIMAL().getText()));
			else
				return new IntAnnotationValue(-Integer.parseInt(ctx.LITERAL_DECIMAL().getText()));
		else if (ctx.LITERAL_FLOAT() != null)
			if (ctx.SUB() != null)
				return new FloatAnnotationValue(Float.parseFloat(ctx.LITERAL_FLOAT().getText()));
			else
				return new FloatAnnotationValue(-Float.parseFloat(ctx.LITERAL_FLOAT().getText()));
		else if (ctx.LITERAL_BOOL() != null)
			return new BoolAnnotationValue(Boolean.parseBoolean(ctx.LITERAL_BOOL().getText()));
		else if (ctx.LITERAL_STRING() != null)
			return new StringAnnotationValue(ctx.LITERAL_STRING().getText());
		else if (ctx.unit_name != null)
			return new CompilationUnitAnnotationValue(ctx.unit_name.getText());
		throw new UnsupportedOperationException("Annotation value not supported: " + ctx);
	}

	@Override
	public ArrayAnnotationValue visitArrayAnnotationValue(ArrayAnnotationValueContext ctx) {
		if (ctx.basicAnnotationValue() == null)
			return new ArrayAnnotationValue(new BasicAnnotationValue[0]);
		else {
			BasicAnnotationValue[] arr = new BasicAnnotationValue[ctx.basicAnnotationValue().size()];
			for (int i = 0; i < ctx.basicAnnotationValue().size(); i++)
				arr[i] = visitBasicAnnotationValue(ctx.basicAnnotationValue(i));
			return new ArrayAnnotationValue(arr);
		}
	}
}

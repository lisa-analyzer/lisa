package it.unive.lisa.analysis;

import apron.Manager;
import apron.Abstract1;
import apron.Box;
import apron.Octagon;
import apron.Polka;
import apron.PolkaEq;
import apron.PplGrid;
import apron.PplPoly;
import apron.ApronException;
import apron.Pplite;

import it.unive.lisa.analysis.ScopeToken;
import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.analysis.SemanticOracle;
import it.unive.lisa.analysis.lattices.Satisfiability;
import it.unive.lisa.analysis.value.ValueDomain;
import it.unive.lisa.program.cfg.ProgramPoint;
import it.unive.lisa.symbolic.value.BinaryExpression;
import it.unive.lisa.symbolic.value.Identifier;
import it.unive.lisa.symbolic.value.UnaryExpression;
import it.unive.lisa.symbolic.value.ValueExpression;
import it.unive.lisa.util.representation.StringRepresentation;
import it.unive.lisa.util.representation.StructuredRepresentation;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.function.Predicate;

public class Apron implements ValueDomain<Apron> {

	/* Set the Java library path to include the directory containing the Apron library SO file.*/
	static{System.setProperty( "java.library.path", "./src/main/java/it/unive/lisa/apron/japron" );}

	private static Manager manager;

	final Abstract1 state;

	public enum ApronDomain {
		/**
		 * Intervals
		 */
		Box,
		/**
		 * Octagons
		 */
		Octagon,
		/**
		 * Convex polyhedra
		 */
		Polka,
		/**
		 * Linear equalities
		 */
		PolkaEq,
		/**
		 * Reduced product of the Polka convex polyhedra and PplGrid the linear congruence equalities domains
		 * Compile Apron with the specific flag for PPL set to 1 in order to use such domain.
		 */
		PolkaGrid,
		/**
		 * Parma Polyhedra Library linear congruence equalities domain
		 * Compile Apron with the specific flag for PPL set to 1 in order to use such domain.
		 */
		PplGrid,
		/**
		 * The Parma Polyhedra libraryconvex polyhedra domain
		 * Compile Apron with the specific flag for PPL set to 1 in order to use such domain.
		 */
		PplPoly,
		/**
		 * PPLite
		 */
		PPLite;
	}

	public Apron() {
		try {
			String[] vars = {"<ret>"}; // Variable needed to represent the returned value
			state = new Abstract1(manager, new apron.Environment(new String[0], vars));
		}
		catch(ApronException e) {
			throw new UnsupportedOperationException("Apron library crashed", e);
		}
	}
	public Apron(Abstract1 state) {
		this.state = state;
	}

	@Override
	public boolean lessOrEqual(Apron other) throws SemanticException {
		return true;
	}

	@Override
	public Apron lub(Apron other) throws SemanticException {
		return top();
	}

	@Override
	public Apron top() {
		try {
			return new Apron(new Abstract1(manager, new apron.Environment()));
		} catch (ApronException e) {
			throw new UnsupportedOperationException("Apron library crashed", e);
		}
	}

	@Override
	public Apron bottom() {
		try {
			return new Apron(new Abstract1(manager, new apron.Environment(), true));
		} catch (ApronException e) {
			throw new UnsupportedOperationException("Apron library crashed", e);
		}
	}

	@Override
	public Apron pushScope(ScopeToken token) throws SemanticException {
		
		return top();
	}

	@Override
	public Apron popScope(ScopeToken token) throws SemanticException {
		return top();
	}

	@Override
	public Apron assign(Identifier id, ValueExpression expression, ProgramPoint pp, SemanticOracle oracle) throws SemanticException {
		return top();
	}

	@Override
	public Apron smallStepSemantics(ValueExpression expression, ProgramPoint pp, SemanticOracle oracle) throws SemanticException {
		return top();
	}

	@Override
	public Apron assume(ValueExpression expression, ProgramPoint src, ProgramPoint dest, SemanticOracle oracle) throws SemanticException {
		return top();
	}

	@Override
	public boolean knowsIdentifier(Identifier id) {
		return true;
	}

	@Override
	public Apron forgetIdentifier(Identifier id) throws SemanticException {
		return top();
	}

	@Override
	public Apron forgetIdentifiersIf(Predicate<Identifier> test) throws SemanticException {
		return top();
	}

	@Override
	public Satisfiability satisfies(ValueExpression expression, ProgramPoint pp, SemanticOracle oracle) throws SemanticException {
		return Satisfiability.UNKNOWN;
	}

	@Override
	public StructuredRepresentation representation() {
		return new StringRepresentation("TOP_APRON");
	}

	@Override
	public boolean isBottom() {
		return equals(bottom());
	}

	@Override
	public boolean isTop() {
		return equals(top());
	}

	public static void setManager(ApronDomain numericalDomain) {
		switch(numericalDomain) {
			case Box: manager= new apron.Box(); break;
			case Octagon: manager=new Octagon(); break;
			case Polka: manager=new Polka(false); break;
			case PolkaEq: manager=new PolkaEq(); break;
			case PplGrid: manager=new PplGrid(); break;
			case PplPoly: manager=new PplPoly(false); break;
			case PPLite: manager=new Pplite(true); break;
			default: throw new UnsupportedOperationException("Numerical domain "+numericalDomain+" unknown in Apron");
		}
	}



}

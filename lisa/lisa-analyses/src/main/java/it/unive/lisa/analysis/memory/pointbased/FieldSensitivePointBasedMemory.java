package it.unive.lisa.analysis.memory.pointbased;

import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.analysis.SemanticOracle;
import it.unive.lisa.lattices.ExpressionSet;
import it.unive.lisa.lattices.GenericMapLattice;
import it.unive.lisa.lattices.memory.allocations.AllocationSite;
import it.unive.lisa.lattices.memory.allocations.AllocationSites;
import it.unive.lisa.lattices.memory.allocations.HeapAllocationSite;
import it.unive.lisa.lattices.memory.allocations.MemoryEnvWithFields;
import it.unive.lisa.lattices.memory.allocations.NullAllocationSite;
import it.unive.lisa.lattices.memory.allocations.StackAllocationSite;
import it.unive.lisa.program.annotations.Annotation;
import it.unive.lisa.program.cfg.ProgramPoint;
import it.unive.lisa.symbolic.SymbolicExpression;
import it.unive.lisa.symbolic.memory.AccessChild;
import it.unive.lisa.symbolic.memory.MemoryAllocation;
import it.unive.lisa.symbolic.value.Identifier;
import it.unive.lisa.symbolic.value.MemoryPointer;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import org.apache.commons.lang3.tuple.Pair;

/**
 * A field-insensitive program point-based {@link AllocationSiteBasedAnalysis}.
 * The implementation follows X. Rival and K. Yi, "Introduction to Static
 * Analysis An Abstract Interpretation Perspective", Section 8.3.4
 * 
 * @author <a href="mailto:vincenzo.arceri@unipr.it">Vincenzo Arceri</a>
 * 
 * @see <a href=
 *          "https://mitpress.mit.edu/books/introduction-static-analysis">https://mitpress.mit.edu/books/introduction-static-analysis</a>
 */
public class FieldSensitivePointBasedMemory
		extends
		AllocationSiteBasedAnalysis<MemoryEnvWithFields> {

	@Override
	public MemoryEnvWithFields makeLattice() {
		return new MemoryEnvWithFields();
	}

	@Override
	public MemoryEnvWithFields shallowCopy(
			MemoryEnvWithFields state,
			Identifier id,
			StackAllocationSite site,
			List<MemoryReplacement> replacements)
			throws SemanticException {
		// no aliasing: star_y must be cloned and the clone must
		// be assigned to id
		StackAllocationSite clone = new StackAllocationSite(
				site.getStaticType(),
				id.getCodeLocation().toString(),
				site.isWeak(),
				id.getCodeLocation());
		MemoryEnvWithFields memory = store(state, id, clone);

		Map<AllocationSite, ExpressionSet> newFields = new HashMap<>(state.fields.getMap());

		// all the allocation sites fields of star_y
		if (state.fields.getKeys().contains(site)) {
			for (SymbolicExpression field : state.fields.getState(site)) {
				StackAllocationSite cloneWithField = new StackAllocationSite(
						field.getStaticType(),
						id.getCodeLocation().toString(),
						field,
						site.isWeak(),
						id.getCodeLocation());

				StackAllocationSite star_yWithField = new StackAllocationSite(
						field.getStaticType(),
						site.getCodeLocation().toString(),
						field,
						site.isWeak(),
						site.getCodeLocation());
				MemoryReplacement replacement = new MemoryReplacement();
				replacement.addSource(star_yWithField);
				replacement.addTarget(cloneWithField);
				replacement.addTarget(star_yWithField);

				// need to update also the fields of the clone
				addField(clone, field, newFields);

				replacements.add(replacement);
			}
		}

		// need to be replaced also the allocation site (needed for type
		// analysis)
		MemoryReplacement replacement = new MemoryReplacement();
		replacement.addSource(site);
		replacement.addTarget(clone);
		replacement.addTarget(site);
		replacements.add(replacement);

		return new MemoryEnvWithFields(
				memory.lattice,
				memory.function,
				new GenericMapLattice<>(state.fields.lattice, newFields));
	}

	@Override
	public Pair<MemoryEnvWithFields, List<MemoryReplacement>> smallStepSemantics(
			MemoryEnvWithFields state,
			SymbolicExpression expression,
			ProgramPoint pp,
			SemanticOracle oracle)
			throws SemanticException {
		Pair<MemoryEnvWithFields,
				List<MemoryReplacement>> sss = super.smallStepSemantics(state, expression, pp, oracle);
		MemoryEnvWithFields st = sss.getLeft();

		if (expression instanceof AccessChild) {
			AccessChild accessChild = (AccessChild) expression;
			Map<AllocationSite, ExpressionSet> mapping = new HashMap<>(st.fields.getMap());

			ExpressionSet exprs;
			SymbolicExpression cont = accessChild.getContainer();
			if (cont instanceof Identifier)
				exprs = new ExpressionSet(resolveIdentifier(st, (Identifier) cont, pp));
			else if (cont.mightNeedRewriting())
				exprs = rewrite(sss.getLeft(), cont, pp, oracle);
			else
				exprs = new ExpressionSet(cont);

			for (SymbolicExpression rec : exprs)
				if (rec instanceof MemoryPointer) {
					AllocationSite site = (AllocationSite) ((MemoryPointer) rec).getReferencedLocation();
					ExpressionSet childs = rewrite(sss.getLeft(), accessChild.getChild(), pp, oracle);

					for (SymbolicExpression child : childs)
						addField(site, child, mapping);

				} else if (rec instanceof AllocationSite) {
					AllocationSite site = (AllocationSite) rec;
					ExpressionSet childs = rewrite(sss.getLeft(), accessChild.getChild(), pp, oracle);

					for (SymbolicExpression child : childs)
						addField(site, child, mapping);
				}

			return Pair.of(
					new MemoryEnvWithFields(st.lattice, st.function,
							new GenericMapLattice<>(st.fields.lattice, mapping)),
					sss.getRight());
		} else if (expression instanceof MemoryAllocation) {
			String loc = expression.getCodeLocation().getCodeLocation();
			Set<AllocationSite> alreadyAllocated = getAllocatedAt(st, loc);

			if (!alreadyAllocated.isEmpty()) {
				// we must turn all these sites to weak ones, including the one
				// about fields
				List<MemoryReplacement> replacements = new LinkedList<>();
				replacements.addAll(sss.getRight());

				for (AllocationSite site : alreadyAllocated) {
					if (!site.isWeak()) {
						MemoryReplacement replacement = new MemoryReplacement();
						replacement.addSource(site);
						replacement.addTarget(site.toWeak());
						replacements.add(replacement);
					}
					if (st.fields.getKeys().contains(site))
						for (SymbolicExpression field : st.fields.getState(site)) {
							AllocationSite withField = site.withField(field);
							if (!withField.isWeak()) {
								MemoryReplacement replacement = new MemoryReplacement();
								replacement.addSource(withField);
								replacement.addTarget(withField.toWeak());
								replacements.add(replacement);
							}
						}
				}

				if (!replacements.isEmpty()) {
					// we must apply the replacements to our mapping as well
					Map<Identifier, AllocationSites> map = new HashMap<>(st.getMap());
					for (Entry<Identifier, AllocationSites> entry : st) {
						Identifier id = entry.getKey();
						AllocationSites sites = entry.getValue();
						for (MemoryReplacement repl : replacements) {
							if (repl.getSources().contains(id))
								// these are all one-to-one replacements
								id = repl.getTargets().iterator().next();
							sites = sites.applyReplacement(repl, pp);
						}
						map.put(id, sites);
					}
					st = new MemoryEnvWithFields(st.lattice, map, st.fields);
				}

				return Pair.of(st, replacements);
			}
		}

		return sss;
	}

	private void addField(
			AllocationSite site,
			SymbolicExpression field,
			Map<AllocationSite, ExpressionSet> mapping) {
		Set<SymbolicExpression> tmp = new HashSet<>(mapping.getOrDefault(site, new ExpressionSet()).elements());
		tmp.add(field);
		mapping.put(site, new ExpressionSet(tmp));
	}

	@Override
	public ExpressionSet rewriteAccessChild(
			AccessChild expression,
			ExpressionSet receiver,
			ExpressionSet child,
			MemoryEnvWithFields state,
			ProgramPoint pp,
			SemanticOracle oracle)
			throws SemanticException {
		Set<SymbolicExpression> result = new HashSet<>();
		Set<SymbolicExpression> toProcess = new HashSet<>();
		for (SymbolicExpression rec : receiver) {
			rec = rec.removeTypingExpressions();
			if (rec instanceof Identifier)
				toProcess.addAll(resolveIdentifier(state, (Identifier) rec, pp));
			else
				toProcess.add(rec);
		}

		for (SymbolicExpression rec : toProcess) {
			if (rec instanceof MemoryPointer) {
				AllocationSite site = (AllocationSite) ((MemoryPointer) rec).getReferencedLocation();
				if (site.equals(NullAllocationSite.INSTANCE))
					result.add(site);
				else
					populate(expression, child, result, site);
			} else if (rec instanceof AllocationSite) {
				AllocationSite site = (AllocationSite) rec;
				if (site.equals(NullAllocationSite.INSTANCE))
					result.add(site);
				else
					populate(expression, child, result, site);
			}
		}

		return new ExpressionSet(result);
	}

	private void populate(
			AccessChild expression,
			ExpressionSet child,
			Set<SymbolicExpression> result,
			AllocationSite site) {
		for (SymbolicExpression target : child) {
			AllocationSite e;

			if (site instanceof StackAllocationSite)
				e = new StackAllocationSite(
						expression.getStaticType(),
						site.getLocationName(),
						target,
						site.isWeak(),
						site.getCodeLocation());
			else
				e = new HeapAllocationSite(
						expression.getStaticType(),
						site.getLocationName(),
						target,
						site.isWeak(),
						site.getCodeLocation());

			// propagates the annotations of the child value expression to
			// the newly created allocation site
			if (target instanceof Identifier)
				for (Annotation ann : ((Identifier) target).getAnnotations())
					e.addAnnotation(ann);

			result.add(e);
		}
	}

	@Override
	public ExpressionSet rewriteMemoryAllocation(
			MemoryAllocation expression,
			MemoryEnvWithFields state,
			ProgramPoint pp,
			SemanticOracle oracle)
			throws SemanticException {
		String loc = expression.getCodeLocation().getCodeLocation();

		boolean weak;
		if (!getAllocatedAt(state, loc).isEmpty())
			weak = true;
		else
			weak = false;

		AllocationSite e;
		if (expression.isStackAllocation())
			e = new StackAllocationSite(expression.getStaticType(), loc, weak, expression.getCodeLocation());
		else
			e = new HeapAllocationSite(expression.getStaticType(), loc, weak, expression.getCodeLocation());
		e.setAllocation(true);

		// propagates the annotations of expression
		// to the newly created allocation site
		for (Annotation ann : expression.getAnnotations())
			e.getAnnotations().addAnnotation(ann);

		return new ExpressionSet(e);
	}

}

package it.unive.lisa.analysis.memory.pointbased;

import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.analysis.SemanticOracle;
import it.unive.lisa.lattices.ExpressionSet;
import it.unive.lisa.lattices.GenericMapLattice;
import it.unive.lisa.lattices.StringSet;
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
import it.unive.lisa.symbolic.memory.StaticAccess;
import it.unive.lisa.symbolic.value.Identifier;
import it.unive.lisa.symbolic.value.MemoryPointer;
import it.unive.lisa.symbolic.value.Variable;
import it.unive.lisa.type.Untyped;
import it.unive.lisa.util.datastructures.trie.PatriciaTrieMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;
import org.apache.commons.lang3.tuple.Pair;

/**
 * A field-sensitive, allocation-site-based heap domain: in addition to the
 * allocation site of an object or array, heap locations also track, for each
 * allocation site, the fields (or elements) that have been accessed on it and
 * the allocation sites they in turn point to. The implementation follows X.
 * Rival and K. Yi, "Introduction to Static Analysis: An Abstract Interpretation
 * Perspective", Section 8.3.4.
 *
 * @author <a href="mailto:vincenzo.arceri@unipr.it">Vincenzo Arceri</a>
 *
 * @see <a href="https://mitpress.mit.edu/books/introduction-static-analysis">
 *          Xavier Rival, Kwangkeun Yi. Introduction to Static Analysis: An
 *          Abstract Interpretation Perspective. MIT Press, 2020.</a>
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

		PatriciaTrieMap<AllocationSite, StringSet> newFields = state.fields.mkNewFunction(state.fields.function, false);

		// all the allocation sites fields of star_y
		if (state.fields.getKeys().contains(site)) {
			for (String field : state.fields.getState(site)) {
				StackAllocationSite cloneWithField = new StackAllocationSite(
						Untyped.INSTANCE,
						id.getCodeLocation().toString(),
						field,
						site.isWeak(),
						id.getCodeLocation());

				StackAllocationSite star_yWithField = new StackAllocationSite(
						Untyped.INSTANCE,
						site.getCodeLocation().toString(),
						field,
						site.isWeak(),
						site.getCodeLocation());
				MemoryReplacement replacement = new MemoryReplacement();
				replacement.addSource(star_yWithField);
				replacement.addTarget(cloneWithField);
				replacement.addTarget(star_yWithField);

				// need to update also the fields of the clone
				newFields = addField(clone, field, newFields);

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
			AccessChild<?> accessChild = (AccessChild<?>) expression;
			PatriciaTrieMap<AllocationSite, StringSet> mapping = st.fields.mkNewFunction(st.fields.function, false);

			ExpressionSet exprs;
			SymbolicExpression cont = accessChild.getContainer();
			if (cont instanceof Identifier)
				exprs = new ExpressionSet(resolveIdentifier(st, (Identifier) cont, pp));
			else if (cont.mightNeedRewriting())
				exprs = rewrite(sss.getLeft(), cont, pp, oracle);
			else
				exprs = new ExpressionSet(cont);

			String child;
			if (accessChild instanceof StaticAccess)
				child = ((StaticAccess) accessChild).getChild().getName();
			else
				throw new SemanticException("DynamicAccess is not yet supported in FieldSensitivePointBasedMemory");

			for (SymbolicExpression rec : exprs)
				if (rec instanceof MemoryPointer) {
					AllocationSite site = (AllocationSite) ((MemoryPointer) rec).getReferencedLocation();
					mapping = addField(site, child, mapping);
				} else if (rec instanceof AllocationSite) {
					AllocationSite site = (AllocationSite) rec;
					mapping = addField(site, child, mapping);
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
						for (String field : st.fields.getState(site)) {
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
					PatriciaTrieMap<Identifier, AllocationSites> map = st.mkNewFunction(st.function, false);
					for (Entry<Identifier, AllocationSites> entry : st) {
						Identifier id = entry.getKey();
						AllocationSites sites = entry.getValue();
						for (MemoryReplacement repl : replacements) {
							if (repl.getSources().contains(id))
								// these are all one-to-one replacements
								id = repl.getTargets().iterator().next();
							sites = sites.applyReplacement(repl, pp);
						}
						map = map.put(id, sites);
					}
					st = new MemoryEnvWithFields(st.lattice, map, st.fields);
				}

				return Pair.of(st, replacements);
			}
		}

		return sss;
	}

	/**
	 * Tracks a new field for the given allocation site by inserting it in the
	 * given mapping, returning the updated mapping. If the site is already
	 * present in the mapping, the field is added to the existing set of fields,
	 * otherwise a new set is created and added to the mapping.
	 *
	 * @param site    the allocation site
	 * @param field   the field to track
	 * @param mapping the mapping to update
	 * 
	 * @return the updated mapping
	 */
	protected PatriciaTrieMap<AllocationSite, StringSet> addField(
			AllocationSite site,
			String field,
			PatriciaTrieMap<AllocationSite, StringSet> mapping) {
		Set<String> tmp = new HashSet<>(mapping.getOrDefault(site, new StringSet()).elements());
		tmp.add(field);
		return mapping.put(site, new StringSet(tmp));
	}

	@Override
	public ExpressionSet rewriteStaticAccess(
			StaticAccess expression,
			ExpressionSet receiver,
			Variable child,
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
					populate(expression, child.getName(), result, site);
			} else if (rec instanceof AllocationSite) {
				AllocationSite site = (AllocationSite) rec;
				if (site.equals(NullAllocationSite.INSTANCE))
					result.add(site);
				else
					populate(expression, child.getName(), result, site);
			}
		}

		return new ExpressionSet(result);
	}

	private void populate(
			StaticAccess expression,
			String child,
			Set<SymbolicExpression> result,
			AllocationSite site) {
		AllocationSite e;

		if (site instanceof StackAllocationSite)
			e = new StackAllocationSite(
					expression.getStaticType(),
					site.getLocationName(),
					child,
					site.isWeak(),
					site.getCodeLocation());
		else
			e = new HeapAllocationSite(
					expression.getStaticType(),
					site.getLocationName(),
					child,
					site.isWeak(),
					site.getCodeLocation());

		// propagates the annotations of the accessed field to the
		// newly created allocation site
		for (Annotation ann : expression.getAnnotations())
			e.addAnnotation(ann);

		result.add(e);
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

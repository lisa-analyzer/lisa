package it.unive.lisa;

import it.unive.lisa.analysis.AbstractDomain;
import it.unive.lisa.analysis.AbstractLattice;
import it.unive.lisa.analysis.SimpleAbstractDomain;
import it.unive.lisa.analysis.heap.HeapDomain;
import it.unive.lisa.analysis.heap.HeapLattice;
import it.unive.lisa.analysis.heap.MonolithicHeap;
import it.unive.lisa.analysis.nonrelational.type.TypeEnvironment;
import it.unive.lisa.analysis.nonrelational.value.ValueEnvironment;
import it.unive.lisa.analysis.numeric.Interval;
import it.unive.lisa.analysis.type.TypeDomain;
import it.unive.lisa.analysis.type.TypeLattice;
import it.unive.lisa.analysis.types.InferredTypes;
import it.unive.lisa.analysis.value.ValueDomain;
import it.unive.lisa.analysis.value.ValueLattice;
import it.unive.lisa.conf.LiSAConfiguration;
import it.unive.lisa.interprocedural.InterproceduralAnalysis;
import it.unive.lisa.interprocedural.ModularWorstCaseAnalysis;
import it.unive.lisa.interprocedural.callgraph.CallGraph;
import it.unive.lisa.interprocedural.callgraph.RTACallGraph;
import it.unive.lisa.lattices.SimpleAbstractState;
import it.unive.lisa.lattices.heap.Monolith;
import it.unive.lisa.lattices.types.TypeSet;
import it.unive.lisa.util.numeric.IntInterval;

/**
 * A default {@link LiSAConfiguration} that already has a {@link CallGraph} and
 * {@link InterproceduralAnalysis} set. This class also has static methods to
 * instantiate default implementations of other analysis components.
 * 
 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
 */
public class DefaultConfiguration
		extends
		LiSAConfiguration {

	/**
	 * Yields a default {@link CallGraph} that can be used to run analyses.
	 * 
	 * @return the call graph
	 */
	public static RTACallGraph defaultCallGraph() {
		return new RTACallGraph();
	}

	/**
	 * Yields a default {@link InterproceduralAnalysis} that can be used to run
	 * analyses.
	 * 
	 * @param <A> the kind of {@link AbstractLattice} produced by the domain
	 *                {@code D}
	 * @param <D> the kind of {@link AbstractDomain} to run during the analysis
	 * 
	 * @return the interprocedural analysis
	 */
	public static <A extends AbstractLattice<A>,
			D extends AbstractDomain<A>> ModularWorstCaseAnalysis<A, D> defaultInterproceduralAnalysis() {
		return new ModularWorstCaseAnalysis<>();
	}

	/**
	 * Yields a default {@link TypeDomain} that can be used to run analyses.
	 * 
	 * @return the typedomain
	 */
	public static InferredTypes defaultTypeDomain() {
		return new InferredTypes();
	}

	/**
	 * Yields a default {@link ValueDomain} that can be used to run analyses.
	 * 
	 * @return the value domain
	 */
	public static Interval defaultValueDomain() {
		return new Interval();
	}

	/**
	 * Yields a default {@link HeapDomain} that can be used to run analyses.
	 * 
	 * @return the heap domain
	 */
	public static MonolithicHeap defaultHeapDomain() {
		return new MonolithicHeap();
	}

	/**
	 * Yields an instance of {@link SimpleAbstractState} built using the given
	 * sub-domains.
	 * 
	 * @param <H>   the type of {@link HeapLattice} produced by {@code HD}
	 * @param <HD>  the type of {@link HeapDomain}
	 * @param <V>   the type of {@link ValueLattice} produced by {@code VD}
	 * @param <VD>  the type of {@link ValueDomain}
	 * @param <T>   the type of {@link TypeLattice} produced by {@code TD}
	 * @param <TD>  the type of {@link TypeDomain}
	 * @param heap  the {@link HeapDomain} to embed in the returned state
	 * @param value the {@link ValueDomain} to embed in the returned state
	 * @param type  the {@link TypeDomain} to embed in the returned state
	 * 
	 * @return the abstract state
	 */
	public static <H extends HeapLattice<H>,
			HD extends HeapDomain<H>,
			V extends ValueLattice<V>,
			VD extends ValueDomain<V>,
			T extends TypeLattice<T>,
			TD extends TypeDomain<T>> SimpleAbstractDomain<H, V, T> simpleDomain(
					HD heap,
					VD value,
					TD type) {
		return new SimpleAbstractDomain<>(heap, value, type);
	}

	/**
	 * Yields a default {@link AbstractDomain} that can be used to run analyses.
	 * 
	 * @return the abstract state
	 */
	public static SimpleAbstractDomain<Monolith,
			ValueEnvironment<IntInterval>,
			TypeEnvironment<TypeSet>> defaultAbstractDomain() {
		return simpleDomain(defaultHeapDomain(), defaultValueDomain(), defaultTypeDomain());
	}

	/**
	 * Builds a default configuration, having {@link #defaultCallGraph()} as
	 * {@link CallGraph} and {@link #defaultInterproceduralAnalysis()} as
	 * {@link InterproceduralAnalysis}.
	 */
	public DefaultConfiguration() {
		this.callGraph = defaultCallGraph();
		this.interproceduralAnalysis = defaultInterproceduralAnalysis();
	}

}

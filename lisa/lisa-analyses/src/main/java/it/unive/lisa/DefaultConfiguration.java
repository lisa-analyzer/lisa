package it.unive.lisa;

import it.unive.lisa.analysis.AbstractState;
import it.unive.lisa.analysis.SimpleAbstractState;
import it.unive.lisa.analysis.heap.HeapDomain;
import it.unive.lisa.analysis.heap.MonolithicHeap;
import it.unive.lisa.analysis.nonrelational.value.TypeEnvironment;
import it.unive.lisa.analysis.nonrelational.value.ValueEnvironment;
import it.unive.lisa.analysis.numeric.Interval;
import it.unive.lisa.analysis.types.InferredTypes;
import it.unive.lisa.analysis.value.TypeDomain;
import it.unive.lisa.analysis.value.ValueDomain;
import it.unive.lisa.conf.LiSAConfiguration;
import it.unive.lisa.interprocedural.InterproceduralAnalysis;
import it.unive.lisa.interprocedural.ModularWorstCaseAnalysis;
import it.unive.lisa.interprocedural.callgraph.CallGraph;
import it.unive.lisa.interprocedural.callgraph.RTACallGraph;

/**
 * A default {@link LiSAConfiguration} that already has a {@link CallGraph} and
 * {@link InterproceduralAnalysis} set. This class also has static methods to
 * instantiate default implementations of other analysis components.
 * 
 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
 */
public class DefaultConfiguration extends LiSAConfiguration {

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
	 * @param <A> the kind of {@link AbstractState} to run during the analysis
	 * 
	 * @return the interprocedural analysis
	 */
	public static <A extends AbstractState<A>> ModularWorstCaseAnalysis<A> defaultInterproceduralAnalysis() {
		return new ModularWorstCaseAnalysis<>();
	}

	/**
	 * Yields a default {@link TypeDomain} that can be used to run analyses.
	 * 
	 * @return the typedomain
	 */
	public static TypeEnvironment<InferredTypes> defaultTypeDomain() {
		return new TypeEnvironment<>(new InferredTypes());
	}

	/**
	 * Yields a default {@link ValueDomain} that can be used to run analyses.
	 * 
	 * @return the value domain
	 */
	public static ValueEnvironment<Interval> defaultValueDomain() {
		return new ValueEnvironment<>(new Interval());
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
	 * Yields an instance of {@link SimpleAbstractState} uilt using the given
	 * sub-domains.
	 * 
	 * @param <H>   the type of {@link HeapDomain}
	 * @param <V>   the type of {@link ValueDomain}
	 * @param <T>   the type of {@link TypeDomain}
	 * @param heap  the {@link HeapDomain} to embed in the returned state
	 * @param value the {@link ValueDomain} to embed in the returned state
	 * @param type  the {@link TypeDomain} to embed in the returned state
	 * 
	 * @return the abstract state
	 */
	public static <H extends HeapDomain<H>,
			V extends ValueDomain<V>,
			T extends TypeDomain<T>> SimpleAbstractState<H, V, T> simpleState(H heap, V value, T type) {
		return new SimpleAbstractState<>(heap, value, type);
	}

	/**
	 * Yields a default {@link AbstractState} that can be used to run analyses.
	 * 
	 * @return the abstract state
	 */
	public static SimpleAbstractState<
			MonolithicHeap,
			ValueEnvironment<Interval>,
			TypeEnvironment<InferredTypes>> defaultAbstractState() {
		return simpleState(defaultHeapDomain(), defaultValueDomain(), defaultTypeDomain());
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

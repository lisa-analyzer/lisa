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
import it.unive.lisa.interprocedural.ModularWorstCaseAnalysis;
import it.unive.lisa.interprocedural.callgraph.RTACallGraph;

public class DefaultConfiguration extends LiSAConfiguration {

	public static RTACallGraph defaultCallGraph() {
		return new RTACallGraph();
	}

	public static <A extends AbstractState<A>> ModularWorstCaseAnalysis<A> defaultInterproceduralAnalysis() {
		return new ModularWorstCaseAnalysis<>();
	}

	public static TypeEnvironment<InferredTypes> defaultTypeDomain() {
		return new TypeEnvironment<>(new InferredTypes());
	}

	public static ValueEnvironment<Interval> defaultValueDomain() {
		return new ValueEnvironment<>(new Interval());
	}

	public static MonolithicHeap defaultHeapDomain() {
		return new MonolithicHeap();
	}

	public static <H extends HeapDomain<H>,
			V extends ValueDomain<V>,
			T extends TypeDomain<T>> SimpleAbstractState<H, V, T> simpleState(H heap, V value, T type) {
		return new SimpleAbstractState<>(heap, value, type);
	}

	public static SimpleAbstractState<
			MonolithicHeap,
			ValueEnvironment<Interval>,
			TypeEnvironment<InferredTypes>> defaultAbstractState() {
		return simpleState(defaultHeapDomain(), defaultValueDomain(), defaultTypeDomain());
	}

	public DefaultConfiguration() {
		this.callGraph = defaultCallGraph();
		this.interproceduralAnalysis = defaultInterproceduralAnalysis();
	}
}

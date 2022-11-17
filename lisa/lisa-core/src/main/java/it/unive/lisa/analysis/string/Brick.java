package it.unive.lisa.analysis.string;

import java.util.Collection;

public class Brick {
    private final int min;
    private final int max;
    private final Collection<String> strings;

    public Brick(int min, int max, Collection<String> strings){
        this.min = min;
        this.max = max;
        this.strings = strings;
    }
}

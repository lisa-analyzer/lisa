package it.unive.lisa.analysis.string;

import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.analysis.nonrelational.value.BaseNonRelationalValueDomain;
import it.unive.lisa.analysis.representation.DomainRepresentation;

import java.util.Collection;
import java.util.HashSet;
import java.util.Objects;

public class CharInclusion extends BaseNonRelationalValueDomain<CharInclusion> {

    private final Collection<Character> certainlyContained;

    private final Collection<Character> maybeContained;

    private static final CharInclusion TOP = new CharInclusion();
    private static final CharInclusion BOTTOM = new CharInclusion(null, null);

    public CharInclusion() {
        this(new HashSet<>(), getAlphabet());
    }

    public CharInclusion(Collection<Character> certainlyContained,
                         Collection<Character> maybeContained) {
        this.certainlyContained = certainlyContained;
        this.maybeContained = maybeContained;
    }

    @Override
    protected CharInclusion lubAux(CharInclusion other) throws SemanticException { //TODO
        return null;
    }

    @Override
    protected CharInclusion wideningAux(CharInclusion other) throws SemanticException {
        return lubAux(other);
    }

    @Override
    protected boolean lessOrEqualAux(CharInclusion other) throws SemanticException { //WIP
        int CertainlyContainedSize = this.certainlyContained.size();
        int MaybeContainedSize = this.maybeContained.size();

        int otherCertainlyContainedSize = other.certainlyContained.size();
        int otherMaybeContainedSize = other.maybeContained.size();

        if (CertainlyContainedSize > otherCertainlyContainedSize ||
                MaybeContainedSize > otherMaybeContainedSize)
            return false;

        for (Character certainlyContainedCharacter : this.certainlyContained) { //I suppose that the order are the same, which is incorrect (maybe a sorting should be done)
            for (Character otherCertainlyContainedCharacter : other.certainlyContained) {
                if (certainlyContainedCharacter != otherCertainlyContainedCharacter)
                    return false;
            }
        }

        for (Character maybeContainedCharacter : this.maybeContained) {
            for (Character otherMaybeContainedCharacter : other.maybeContained) {
                if (maybeContainedCharacter != otherMaybeContainedCharacter)
                    return false;
            }
        }
        return true;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        CharInclusion that = (CharInclusion) o;
        return Objects.equals(certainlyContained, that.certainlyContained) && Objects.equals(maybeContained, that.maybeContained);
    }

    @Override
    public int hashCode() {
        return Objects.hash(certainlyContained, maybeContained);
    }

    @Override
    public CharInclusion top() { //TODO
        return null;
    }

    @Override
    public CharInclusion bottom() { //TODO
        return null;
    }

    @Override
    public DomainRepresentation representation() { //TODO
        return null;
    }

    private static HashSet<Character> getAlphabet() {
        HashSet<Character> alphabet = new HashSet<>();

        for (char character = 'a'; character <= 'z'; character++) {
            alphabet.add(character);
        }

        return alphabet;
    }
}

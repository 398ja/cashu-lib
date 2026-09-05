package xyz.tcheeric.cashu.crypto.util;

// clone of org.apache.commons.lang3.tuple.Pair;

public class Pair<K, V> {

    private K elementLeft = null;
    private V elementRight = null;

    protected Pair() {
    }

    public static <K, V> Pair<K, V> of(K elementLeft, V elementRight) {
        return new Pair<>(elementLeft, elementRight);
    }

    public Pair(K elementLeft, V elementRight) {
        this.elementLeft = elementLeft;
        this.elementRight = elementRight;
    }

    public K getLeft() {
        return elementLeft;
    }

    public V getRight() {
        return elementRight;
    }

    /**
     * Value equality on both elements.
     *
     * <p>Was {@code equals(Pair<K, V>)}, an overload rather than an override, so every comparison
     * through an {@code Object} reference silently used identity: two Pairs holding equal elements
     * were unequal, and {@code Point.equals}, which delegates here, inherited that. The compiler
     * accepts the overload without complaint, which is why it went unnoticed.
     *
     * <p>Also null-safe now. The old body dereferenced {@code elementLeft} directly, so comparing
     * the point at infinity, whose coordinates are both null, threw NullPointerException instead
     * of answering the question.
     */
    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof Pair<?, ?> p)) {
            return false;
        }
        return java.util.Objects.equals(elementLeft, p.getLeft())
                && java.util.Objects.equals(elementRight, p.getRight());
    }

    /** Required with {@link #equals}, and null-safe for the same reason. */
    @Override
    public int hashCode() {
        return java.util.Objects.hash(elementLeft, elementRight);
    }

    @Override
    public String toString() {
        return "(" + elementLeft + ", " + elementRight + ")";
    }
}

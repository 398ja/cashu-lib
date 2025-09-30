package xyz.tcheeric.cashu.common;

public interface Confidential {

    String getValue();

    default String display() {
        return getValue().length() > 3
                ? "..." + getValue().substring(getValue().length() - 3)
                : "*** hidden ***";
    }
}

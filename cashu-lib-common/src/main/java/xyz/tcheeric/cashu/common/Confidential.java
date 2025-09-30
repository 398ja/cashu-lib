package xyz.tcheeric.cashu.common;

public interface Confidential {

    int DISPLAY_SUFFIX_LENGTH = 3;

    String getValue();

    default String display() {
        return getValue().length() > DISPLAY_SUFFIX_LENGTH
                ? "..." + getValue().substring(getValue().length() - DISPLAY_SUFFIX_LENGTH)
                : "*** hidden ***";
    }
}

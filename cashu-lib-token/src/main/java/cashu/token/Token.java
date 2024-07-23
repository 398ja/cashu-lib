package cashu.token;

import lombok.NonNull;

public interface Token {

    String URI_SCHEME = "cashu:";
    String TOKEN_PREFIX = "cashu";

    enum Version {
        V3('A'),
        V4('B');

        private final Character code;

        Version(@NonNull Character code) {
            this.code = code;
        }

        public Character getCode() {
            return code;
        }
    }

    Version getVersion();

    String getPrefix();

    //String getBase64Json();

    boolean isClickable();

    String serialize();

    Token deserialize(String serializedToken);
}

package cashu.common.model;

import lombok.NonNull;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

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

    String serialize(boolean clickable);

    class TokenUtil {

        static String serialize(@NonNull String cborToken, Version version, boolean clickable) {
            return serialize(cborToken.replaceAll("\\s+", "").getBytes(StandardCharsets.UTF_8), version, clickable);
        }

        static String serialize(@NonNull byte[] cborToken, Version version, boolean clickable) {
            return serialize(cborToken, TOKEN_PREFIX, version, clickable);
        }

        static String serialize(@NonNull byte[] cborToken, @NonNull String prefix, Version version, boolean clickable) {
            String strSerializedToken = prefix + version.getCode() + Base64.getUrlEncoder().encodeToString(cborToken);
            if (clickable) {
                return URI_SCHEME + strSerializedToken;
            }
            return strSerializedToken;
        }
    }
}

package xyz.tcheeric.cashu.common.nut11;

/**
 * Thrown when a P2PK secret violates NUT-11 and the Proof must be rejected as unspendable.
 *
 * <p>Extends {@link IllegalArgumentException} so existing callers that already handle bad input
 * keep working. A mint should catch this specifically and map it to the protocol's
 * unspendable-proof error rather than letting it surface as a server fault.
 *
 * <p><b>Never include key material in the message.</b> Report the position
 * ({@code data}, {@code pubkeys[2]}, {@code refund[0]}) and the reason, not the bytes.
 *
 * @see <a href="https://github.com/cashubtc/nuts/blob/main/11.md">NUT-11</a>
 */
public class MalformedP2PKSecretException extends IllegalArgumentException {

    public MalformedP2PKSecretException(String message) {
        super(message);
    }

    public MalformedP2PKSecretException(String message, Throwable cause) {
        super(message, cause);
    }
}

package xyz.tcheeric.cashu.crypto.exception;

/**
 * Base exception for cryptographic operation failures in the Cashu protocol.
 *
 * <p>This is the superclass for all cryptographic exceptions in cashu-lib.
 * Catching this exception will catch all crypto-related errors.
 *
 * <p>Subclasses provide more specific error information:
 * <ul>
 *   <li>{@link InvalidKeyException} - Invalid or malformed cryptographic keys</li>
 *   <li>{@link SignatureException} - Signature creation or verification failures</li>
 * </ul>
 *
 * <p><b>Security Note:</b> Exception messages are designed to avoid leaking
 * sensitive information such as key values or internal state.
 */
public class CashuCryptoException extends RuntimeException {

    /**
     * Creates a new exception with the specified message.
     *
     * @param message the error message (should not contain sensitive data)
     */
    public CashuCryptoException(String message) {
        super(message);
    }

    /**
     * Creates a new exception with the specified message and cause.
     *
     * @param message the error message (should not contain sensitive data)
     * @param cause the underlying cause
     */
    public CashuCryptoException(String message, Throwable cause) {
        super(message, cause);
    }

    /**
     * Creates a new exception with the specified cause.
     *
     * @param cause the underlying cause
     */
    public CashuCryptoException(Throwable cause) {
        super(cause);
    }
}

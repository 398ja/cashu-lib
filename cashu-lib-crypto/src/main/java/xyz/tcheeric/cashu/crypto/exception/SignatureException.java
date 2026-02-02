package xyz.tcheeric.cashu.crypto.exception;

/**
 * Exception thrown when a signature operation fails.
 *
 * <p>This exception is thrown in the following scenarios:
 * <ul>
 *   <li>Signature verification fails (signature does not match message/key)</li>
 *   <li>Signature creation fails due to invalid inputs</li>
 *   <li>DLEQ proof verification fails</li>
 *   <li>Signature bytes are malformed or have incorrect length</li>
 * </ul>
 *
 * <p><b>Security Note:</b> For signature verification failures, this exception
 * intentionally provides minimal information to prevent oracle attacks.
 * The message will indicate that verification failed without revealing
 * which part of the verification process failed.
 */
public class SignatureException extends CashuCryptoException {

    /**
     * The type of signature operation that failed.
     */
    public enum OperationType {
        /** Signature creation failed */
        SIGN,
        /** Signature verification failed */
        VERIFY,
        /** DLEQ proof verification failed */
        DLEQ_VERIFY,
        /** Blind signature creation failed */
        BLIND_SIGN,
        /** Signature unblinding failed */
        UNBLIND
    }

    private final OperationType operationType;

    /**
     * Creates a new exception for a signature operation failure.
     *
     * @param operationType the type of operation that failed
     * @param message the error message
     */
    public SignatureException(OperationType operationType, String message) {
        super(message);
        this.operationType = operationType;
    }

    /**
     * Creates a new exception for a signature operation failure with a cause.
     *
     * @param operationType the type of operation that failed
     * @param message the error message
     * @param cause the underlying cause
     */
    public SignatureException(OperationType operationType, String message, Throwable cause) {
        super(message, cause);
        this.operationType = operationType;
    }

    /**
     * Returns the type of operation that failed.
     *
     * @return the operation type
     */
    public OperationType getOperationType() {
        return operationType;
    }

    /**
     * Creates an exception for a failed Schnorr signature verification.
     *
     * @return the exception
     */
    public static SignatureException schnorrVerificationFailed() {
        return new SignatureException(OperationType.VERIFY,
                "Schnorr signature verification failed");
    }

    /**
     * Creates an exception for a failed DLEQ proof verification.
     *
     * @return the exception
     */
    public static SignatureException dleqVerificationFailed() {
        return new SignatureException(OperationType.DLEQ_VERIFY,
                "DLEQ proof verification failed");
    }

    /**
     * Creates an exception for a failed signature creation.
     *
     * @param reason the reason the signature creation failed
     * @return the exception
     */
    public static SignatureException signatureFailed(String reason) {
        return new SignatureException(OperationType.SIGN,
                "Signature creation failed: " + reason);
    }

    /**
     * Creates an exception for a failed blind signature operation.
     *
     * @param reason the reason the operation failed
     * @return the exception
     */
    public static SignatureException blindSignatureFailed(String reason) {
        return new SignatureException(OperationType.BLIND_SIGN,
                "Blind signature failed: " + reason);
    }

    /**
     * Creates an exception for a failed unblinding operation.
     *
     * @param reason the reason the unblinding failed
     * @return the exception
     */
    public static SignatureException unblindFailed(String reason) {
        return new SignatureException(OperationType.UNBLIND,
                "Signature unblinding failed: " + reason);
    }
}

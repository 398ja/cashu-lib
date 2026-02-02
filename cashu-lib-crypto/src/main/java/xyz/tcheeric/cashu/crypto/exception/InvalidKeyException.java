package xyz.tcheeric.cashu.crypto.exception;

/**
 * Exception thrown when a cryptographic key is invalid or malformed.
 *
 * <p>This exception is thrown in the following scenarios:
 * <ul>
 *   <li>Private key is not in the valid range [1, n-1] for secp256k1</li>
 *   <li>Public key point is not on the secp256k1 curve</li>
 *   <li>Key bytes have incorrect length</li>
 *   <li>Key encoding is malformed (invalid prefix, etc.)</li>
 * </ul>
 *
 * <p><b>Security Note:</b> Error messages intentionally do not include
 * the actual key values to prevent information leakage through logs or
 * error responses.
 */
public class InvalidKeyException extends CashuCryptoException {

    /**
     * The type of key that was invalid.
     */
    public enum KeyType {
        PRIVATE_KEY,
        PUBLIC_KEY,
        BLINDING_FACTOR
    }

    private final KeyType keyType;

    /**
     * Creates a new exception for an invalid key.
     *
     * @param keyType the type of key that was invalid
     * @param message the error message (should not contain the key value)
     */
    public InvalidKeyException(KeyType keyType, String message) {
        super(message);
        this.keyType = keyType;
    }

    /**
     * Creates a new exception for an invalid key with a cause.
     *
     * @param keyType the type of key that was invalid
     * @param message the error message (should not contain the key value)
     * @param cause the underlying cause
     */
    public InvalidKeyException(KeyType keyType, String message, Throwable cause) {
        super(message, cause);
        this.keyType = keyType;
    }

    /**
     * Returns the type of key that was invalid.
     *
     * @return the key type
     */
    public KeyType getKeyType() {
        return keyType;
    }

    /**
     * Creates an exception for a private key that is out of range.
     *
     * @return the exception
     */
    public static InvalidKeyException privateKeyOutOfRange() {
        return new InvalidKeyException(KeyType.PRIVATE_KEY,
                "Private key must be in range [1, n-1]");
    }

    /**
     * Creates an exception for an invalid private key length.
     *
     * @param actualLength the actual length in bytes
     * @param expectedLength the expected length in bytes
     * @return the exception
     */
    public static InvalidKeyException invalidPrivateKeyLength(int actualLength, int expectedLength) {
        return new InvalidKeyException(KeyType.PRIVATE_KEY,
                "Invalid private key length: " + actualLength + " bytes. Expected " + expectedLength + " bytes.");
    }

    /**
     * Creates an exception for an invalid public key.
     *
     * @param reason the reason the key is invalid
     * @return the exception
     */
    public static InvalidKeyException invalidPublicKey(String reason) {
        return new InvalidKeyException(KeyType.PUBLIC_KEY, "Invalid public key: " + reason);
    }

    /**
     * Creates an exception for an invalid blinding factor.
     *
     * @param reason the reason the blinding factor is invalid
     * @return the exception
     */
    public static InvalidKeyException invalidBlindingFactor(String reason) {
        return new InvalidKeyException(KeyType.BLINDING_FACTOR, "Invalid blinding factor: " + reason);
    }
}
